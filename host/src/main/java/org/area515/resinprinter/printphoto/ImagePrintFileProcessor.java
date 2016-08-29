package org.area515.resinprinter.printphoto;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.script.ScriptException;

import org.area515.resinprinter.exception.SliceHandlingException;
import org.area515.resinprinter.inkdetection.visual.CannyEdgeDetector8BitGray;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.twodim.RenderExtrusionImage;
import org.area515.resinprinter.twodim.TwoDimensionalPlatformPrintFileProcessor;

public class ImagePrintFileProcessor extends TwoDimensionalPlatformPrintFileProcessor<Object,Object> {
	private class PrintImage extends TwoDimensionalPrintState {
		Future<BufferedImage> futureImage;

		@Override
		public BufferedImage buildExtrusionImage(DataAid aid) throws ExecutionException, InterruptedException {
			return futureImage.get();
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
	
	private void startImageLoad(PrintImage printImage, final File processingFile, final PrintJob printJob) {
		Future<BufferedImage> future = Main.GLOBAL_EXECUTOR.submit(new Callable<BufferedImage>() {
			@Override
			public BufferedImage call() throws Exception {
				SlicingProfile profile = printJob.getPrinter().getConfiguration().getSlicingProfile();
				BufferedImage image = aquireImageFromFile(processingFile);
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
				
				Boolean edgeDetectionDisabled = printJob.getPrinter().getConfiguration().getSlicingProfile().getTwoDimensionalSettings().isEdgeDetectionDisabled();
				if (edgeDetectionDisabled != null && edgeDetectionDisabled) {
					return newImage;
				}
				
				CannyEdgeDetector8BitGray edgeDetector = new CannyEdgeDetector8BitGray();
				edgeDetector.setLowThreshold(.01f);
				edgeDetector.setHighThreshold(3f);
				edgeDetector.setSourceImage(newImage);
				edgeDetector.process();
				return edgeDetector.getEdgesImage();
			}
		});
		printImage.futureImage = future;
		
	}
	
	protected BufferedImage aquireImageFromFile(File processingFile) throws IOException {
		return ImageIO.read(processingFile);
	}
	
	@Override
	public void prepareEnvironment(final File processingFile, final PrintJob printJob) throws JobManagerException {
		PrintImage printImage = new PrintImage();
		startImageLoad(printImage, processingFile, printJob);
		createTwoDimensionalPrintState(printJob, printImage);
	}
	
	@Override
	public void cleanupEnvironment(File processingFile) throws JobManagerException {
		//Nothing to cleanup everything is done in memory.
	}

	@Override
	public Object getGeometry(PrintJob printJob) throws JobManagerException {
		throw new JobManagerException("You can't get geometry from this type of file");
	}

	@Override
	public Object getErrors(PrintJob printJob) throws JobManagerException {
		throw new JobManagerException("You can't get error geometry from this type of file");
	}

	@Override
	public String getFriendlyName() {
		return "Image";
	}

	@Override
	public BufferedImage renderPreviewImage(org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid dataAid) throws SliceHandlingException {
		try {
			//We need to avoid caching images outside of the Customizer cache or we will fill up memory quick
			PrintImage printData = new PrintImage() {
				@Override
				public BufferedImage getCachedExtrusionImage() {
					try {
						return super.buildExtrusionImage(dataAid);
					} catch (ExecutionException | InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			};
			
			startImageLoad(printData, dataAid.printJob.getJobFile(), dataAid.printJob);
			
			printData.setCurrentRenderingPointer(Boolean.TRUE);
			RenderExtrusionImage extrusion = new RenderExtrusionImage(dataAid, this, printData, Boolean.TRUE, dataAid.xResolution, dataAid.yResolution);
			return extrusion.call();
		} catch (ScriptException e) {
			throw new SliceHandlingException(e);
		}
	}
}
