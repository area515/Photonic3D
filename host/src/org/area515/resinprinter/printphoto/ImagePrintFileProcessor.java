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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import org.area515.resinprinter.inkdetection.visual.CannyEdgeDetector8BitGray;
import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.twodim.TwoDimensionalPlatformPrintFileProcessor;

public class ImagePrintFileProcessor extends TwoDimensionalPlatformPrintFileProcessor<Object> {
	private class PrintImage implements TwoDimensionalPrintState {
		Future<BufferedImage> futureImage;
		BufferedImage currentImage;
		
		@Override
		public BufferedImage getCurrentImage() {
			return currentImage;
		}
		
		@Override
		public void setCurrentImage(BufferedImage image) {
			currentImage = image;
		}
		@Override
		public BufferedImage buildImplementationImage(DataAid aid, Graphics2D graphics) throws ExecutionException, InterruptedException {
			currentImage = futureImage.get();
			return currentImage;
		}
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
		createTwoDimensionlPrintState(printJob, printCube);
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
