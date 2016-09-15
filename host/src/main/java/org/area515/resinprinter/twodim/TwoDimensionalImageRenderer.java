package org.area515.resinprinter.twodim;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.area515.resinprinter.inkdetection.visual.CannyEdgeDetector8BitGray;
import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.render.CurrentImageRenderer;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.server.Main;

public abstract class TwoDimensionalImageRenderer extends CurrentImageRenderer {
	protected Future<BufferedImage> newImage;
	
	public TwoDimensionalImageRenderer(DataAid aid, AbstractPrintFileProcessor<?, ?> processor, Object imageIndexToBuild) {
		super(aid, processor, imageIndexToBuild);
		newImage = startImageLoad(aid.printJob);
	}

	private Future<BufferedImage> startImageLoad(final PrintJob printJob) {
		return Main.GLOBAL_EXECUTOR.submit(new Callable<BufferedImage>() {
			@Override
			public BufferedImage call() throws Exception {
				return loadImageFromFile(printJob);
			}
		});
	}
	
	public final BufferedImage renderImage(BufferedImage imageToDisplay) throws JobManagerException {
		if (imageToDisplay != null) {
			return imageToDisplay;
		}
		
		try {
			imageToDisplay = scaleImageAndDetectEdges(aid.printJob);
		} catch (InterruptedException |ExecutionException e) {
			throw new JobManagerException("Couldn't load image", e);
		}

		
		return imageToDisplay;
	}

	public BufferedImage scaleImageAndDetectEdges(PrintJob printJob) throws InterruptedException, ExecutionException {
		SlicingProfile profile = printJob.getPrinter().getConfiguration().getSlicingProfile();
		BufferedImage image = newImage.get();
		Boolean scaleToFit = printJob.getPrinter().getConfiguration().getSlicingProfile().getTwoDimensionalSettings().isScaleImageToFitPrintArea();
		if (scaleToFit != null && scaleToFit) {
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
			image = new BufferedImage(scaledImage.getWidth(null), scaledImage.getHeight(null), BufferedImage.TYPE_4BYTE_ABGR);
			Graphics g = image.createGraphics();
			g.drawImage(scaledImage, 0, 0, null);
			g.dispose();
		}
		
		Boolean edgeDetectionDisabled = printJob.getPrinter().getConfiguration().getSlicingProfile().getTwoDimensionalSettings().isEdgeDetectionDisabled();
		if (edgeDetectionDisabled == null || !edgeDetectionDisabled) {
			CannyEdgeDetector8BitGray edgeDetector = new CannyEdgeDetector8BitGray();
			edgeDetector.setLowThreshold(.01f);
			edgeDetector.setHighThreshold(3f);
			edgeDetector.setSourceImage(image);
			edgeDetector.process();
			image = edgeDetector.getEdgesImage();
		}
		
		return image;
	}

	public abstract BufferedImage loadImageFromFile(PrintJob job) throws JobManagerException;
}
