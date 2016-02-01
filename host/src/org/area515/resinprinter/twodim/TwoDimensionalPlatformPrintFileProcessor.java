package org.area515.resinprinter.twodim;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;

public abstract class TwoDimensionalPlatformPrintFileProcessor<T> extends AbstractPrintFileProcessor<T> {
	private Map<PrintJob, TwoDimensionalPrintState> twoDimensionalPrintDataByJob = new HashMap<PrintJob, TwoDimensionalPrintState>();
	
	public interface TwoDimensionalPrintState {
		public BufferedImage getCurrentImage();
		public void setCurrentImage(BufferedImage image);
		public BufferedImage buildImplementationImage(DataAid aid, Graphics2D graphics) throws ExecutionException, InterruptedException;
	}
	
	@Override
	public BufferedImage getCurrentImage(PrintJob printJob) {
		TwoDimensionalPrintState printCube = twoDimensionalPrintDataByJob.get(printJob);
		return printCube.getCurrentImage();
	}

	@Override
	public Double getBuildAreaMM(PrintJob printJob) {
		//TODO: this should call functions from within the org.area515.resinprinter.job.render.StandaloneImageRenderer but need to be piplined with futures first!
		return null;
	}
	
	public void createTwoDimensionlPrintState(PrintJob printJob, TwoDimensionalPrintState state) {
		twoDimensionalPrintDataByJob.put(printJob, state);
	}
	
	@Override
	public JobStatus processFile(PrintJob printJob) throws Exception {
		DataAid data = initializeDataAid(printJob);
		try {
			int border = getSuggestedPrettyBorderWidth();
			performHeader();
		
			TwoDimensionalPrintState printImage = twoDimensionalPrintDataByJob.get(printJob);
			int firstSlices = getSuggestedSolidLayerCountFor2DGraphic();
			int imageSlices = getSuggestedImplementationLayerCountFor2DGraphic();
			printJob.setTotalSlices(firstSlices + imageSlices);
			
			int centerX = data.xResolution / 2;
			int centerY = data.yResolution / 2;
	
			BufferedImage screenImage = new BufferedImage(data.xResolution, data.yResolution, BufferedImage.TYPE_INT_ARGB_PRE);
			BufferedImage image = printImage.buildImplementationImage(data, (Graphics2D)screenImage.getGraphics());
			while (firstSlices > 0 || imageSlices > 0) {
				//Performs all of the duties that are common to most print files
				JobStatus status = performPreSlice(null);
				if (status != null) {
					return status;
				}
				
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
				
				applyBulbMask(graphics, image.getWidth(), image.getHeight());
				data.printer.showImage(screenImage);
				printImage.setCurrentImage(screenImage);
				
				//Performs all of the duties that are common to most print files
				status = performPostSlice();
				if (status != null) {
					return status;
				}
	
				if (firstSlices > 0) {
					firstSlices--;
				} else {
					imageSlices--;
				}
			}
			
			return performFooter();
		} finally {
			twoDimensionalPrintDataByJob.remove(data.printJob);
		}
	}
}
