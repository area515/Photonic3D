package org.area515.resinprinter.printphoto;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import org.area515.resinprinter.inkdetection.visual.CannyEdgeDetector8BitGray;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintFileProcessingAid;
import org.area515.resinprinter.job.PrintFileProcessingAid.DataAid;
import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.server.Main;

public class ImagePrintFileProcessor implements PrintFileProcessor<Object> {
	private Map<PrintJob, PrintImage> printImagesByPrintJob = new HashMap<PrintJob, PrintImage>();
	
	private class PrintImage {
		Future<BufferedImage> futureImage;
		BufferedImage currentImage;
	}
	
	@Override
	public String[] getFileExtensions() {
		return new String[]{"gif", "jpg", "jpeg", "png"};
	}
	
	@Override
	public boolean acceptsFile(File processingFile) {
		//TODO: this could be smarter by loading the file instead of just checking the file type
		String name = processingFile.getName().toLowerCase();
		return name.endsWith("gif") || name.endsWith("jpg") || name.endsWith("jpeg") || name.endsWith("png");
	}
	
	@Override
	public BufferedImage getCurrentImage(PrintJob printJob) {
		PrintImage printImage = printImagesByPrintJob.get(printJob);
		return printImage.currentImage;
	}
	
	@Override
	public double getBuildAreaMM(PrintJob printJob) {
		//TODO: haven't built any of this
		return -1;
	}
	
	@Override
	public JobStatus processFile(PrintJob printJob) throws Exception {
		int border = 50;
		PrintFileProcessingAid aid = new PrintFileProcessingAid();
		DataAid data = aid.performHeader(printJob);
	
		PrintImage printImage = printImagesByPrintJob.get(printJob);
		printJob.setTotalSlices(data.inkConfiguration.getNumberOfFirstLayers() * 5);

		int centerX = data.xResolution / 2;
		int centerY = data.yResolution / 2;

		int firstSlices = data.inkConfiguration.getNumberOfFirstLayers() * 2;
		int imageSlices = data.inkConfiguration.getNumberOfFirstLayers() * 3;

		BufferedImage image = printImage.futureImage.get();
		while (firstSlices > 0 || imageSlices > 0) {
			//Performs all of the duties that are common to most print files
			JobStatus status = aid.performPreSlice(null);
			if (status != null) {
				return status;
			}
			
			BufferedImage screenImage = new BufferedImage(data.xResolution, data.yResolution, BufferedImage.TYPE_INT_ARGB_PRE);
			Graphics2D graphics = (Graphics2D)screenImage.getGraphics();
			graphics.setColor(Color.black);
			graphics.fillRect(0, 0, data.xResolution, data.yResolution);
			graphics.setColor(Color.white);
			
			if (firstSlices > 0) {
				int actualWidth = image.getWidth() + border > screenImage.getWidth()?screenImage.getWidth():image.getWidth() + border;
				int actualHeight = image.getHeight() + border > screenImage.getHeight()?screenImage.getHeight(): image.getHeight() + border;
				
				graphics.fillRoundRect(centerX - (actualWidth / 2), centerY - (actualHeight / 2), actualWidth, actualHeight, border, border);
			} else {
				int actualWidth = image.getWidth();
				int actualHeight = image.getHeight();
				
				graphics.drawImage(image, centerX - (actualWidth / 2), centerY - (actualHeight / 2), null);
			}
			
			aid.applyBulbMask(graphics);
			data.printer.showImage(screenImage);
			printImage.currentImage = screenImage;
			
			//Performs all of the duties that are common to most print files
			status = aid.performPostSlice(this);
			if (status != null) {
				return status;
			}

			if (firstSlices > 0) {
				firstSlices--;
			} else {
				imageSlices--;
			}
		}
		
		return aid.performFooter();
	}
	
	@Override
	public void prepareEnvironment(final File processingFile, final PrintJob printJob) throws JobManagerException {
		Future<BufferedImage> future = Main.GLOBAL_EXECUTOR.submit(new Callable<BufferedImage>() {
			@Override
			public BufferedImage call() throws Exception {
				SlicingProfile profile = printJob.getPrinter().getConfiguration().getSlicingProfile();
				BufferedImage image = ImageIO.read(processingFile);
				int actualWidth = profile.getxResolution() - image.getWidth();
				int actualHeight = profile.getyResolution() - image.getHeight();
				
				if (actualWidth < actualHeight && actualHeight < 0) {
					actualWidth = profile.getxResolution();
					actualHeight = -1;
				} else if (actualWidth < actualHeight) {
					actualWidth = image.getWidth();
					actualHeight = -1;
				} else if (actualHeight < actualWidth && actualWidth < 0) {
					actualHeight = profile.getyResolution();
					actualWidth = -1;
				} else {
					actualHeight = image.getHeight();
					actualWidth = -1;
				}
				Image scaledImage = image.getScaledInstance(actualWidth, actualHeight, Image.SCALE_SMOOTH);
				BufferedImage newImage = new BufferedImage(scaledImage.getWidth(null), scaledImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
				Graphics g = newImage.createGraphics();
				g.drawImage(scaledImage, 0, 0, null);
				g.dispose();
				
				CannyEdgeDetector8BitGray edgeDetector = new CannyEdgeDetector8BitGray();
				edgeDetector.setLowThreshold(.01f);
				edgeDetector.setHighThreshold(3f);
				edgeDetector.setSourceImage(newImage);
				edgeDetector.process();
				return edgeDetector.getEdgesImage();
			}
		});
		PrintImage printCube = new PrintImage();
		printCube.futureImage = future;
		printImagesByPrintJob.put(printJob, printCube);
	}

	@Override
	public void cleanupEnvironment(File processingFile) throws JobManagerException {
		//Nothing to cleanup everything is done in memory.
	}

	@Override
	public Object getGeometry(PrintJob printJob) throws JobManagerException {
		return null;
	}

	@Override
	public String getFriendlyName() {
		return "Image";
	}
}
