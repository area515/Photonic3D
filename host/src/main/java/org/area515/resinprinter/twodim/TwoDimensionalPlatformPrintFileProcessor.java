package org.area515.resinprinter.twodim;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.annotation.XmlTransient;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.render.RenderingFileData;
import org.area515.resinprinter.printer.SlicingProfile.TwoDimensionalSettings;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.services.PrinterService;

public abstract class TwoDimensionalPlatformPrintFileProcessor<T,E> extends AbstractPrintFileProcessor<T,E> {
	private Map<PrintJob, TwoDimensionalPrintState> twoDimensionalPrintDataByJob = new HashMap<PrintJob, TwoDimensionalPrintState>();
	
	public static abstract class TwoDimensionalPrintState extends RenderingFileData {
		private BufferedImage twoDimensionalImage;
		
		public abstract BufferedImage buildExtrusionImage(DataAid aid) throws ExecutionException, InterruptedException;
		
		public BufferedImage getCachedExtrusionImage() {
			return twoDimensionalImage;
		}
		
		public void cacheExtrusionImage(DataAid aid) throws ExecutionException, InterruptedException {
			this.twoDimensionalImage = buildExtrusionImage(aid);
		}
	}
	
	@Override
	public BufferedImage getCurrentImage(PrintJob printJob) {
		TwoDimensionalPrintState printState = twoDimensionalPrintDataByJob.get(printJob);
		if (printState == null) {
			return null;
		}
		
		ReentrantLock lock = printState.getCurrentLock();
		lock.lock();
		try {
			BufferedImage currentImage = printState.getCurrentImage();
			if (currentImage == null)
				return null;
			
			return currentImage.getSubimage(0, 0, currentImage.getWidth(), currentImage.getHeight());
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Double getBuildAreaMM(PrintJob printJob) {
		//TODO: this should call functions from within the org.area515.resinprinter.job.render.StandaloneImageRenderer but need to be pipelined with futures first!
		return null;
	}
	
	public void createTwoDimensionalPrintState(PrintJob printJob, TwoDimensionalPrintState state) {
		twoDimensionalPrintDataByJob.put(printJob, state);
	}
	
	@Override
	public JobStatus processFile(PrintJob printJob) throws Exception {
		DataAid dataAid = initializeDataAid(printJob);
		try {
			performHeader(dataAid);
			
			TwoDimensionalPrintState printState = twoDimensionalPrintDataByJob.get(printJob);
			int platformSlices = getSuggestedPlatformLayerCount(dataAid);
			int totalPlatformSlices = platformSlices;
			int extrusionSlices = getSuggested2DExtrusionLayerCount(dataAid);
			printJob.setTotalSlices(platformSlices + extrusionSlices);
			
			printState.cacheExtrusionImage(dataAid);
			Object nextRenderingPointer = printState.getCurrentRenderingPointer();
			Future<BufferedImage> currentImage = Main.GLOBAL_EXECUTOR.submit(new RenderPlatformImage(dataAid, this, printState, nextRenderingPointer, dataAid.xResolution, dataAid.yResolution, totalPlatformSlices));
			while (platformSlices > 0 || extrusionSlices > 0) {
				
				//Performs all of the duties that are common to most print files
				JobStatus status = performPreSlice(dataAid, null);
				if (status != null) {
					return status;
				}
				
				//Wait until the image has been properly rendered. Most likely, it's already done though...
				BufferedImage image = currentImage.get();
				
				//Now that the image has been rendered, we can make the switch to use the pointer that we were using while we were rendering
				printState.setCurrentRenderingPointer(nextRenderingPointer);
				
				//Cure the current image
				dataAid.printer.showImage(image);
				
				//Get the next pointer in line to start rendering the image into
				nextRenderingPointer = printState.getNextRenderingPointer();
				
				//Render the next image while we are waiting for the current image to cure
				if (platformSlices > 0) {
					currentImage = Main.GLOBAL_EXECUTOR.submit(new RenderPlatformImage(dataAid, this, printState, nextRenderingPointer, dataAid.xResolution, dataAid.yResolution, totalPlatformSlices));
				} else if (extrusionSlices > 1) {
					currentImage = Main.GLOBAL_EXECUTOR.submit(new RenderExtrusionImage(dataAid, this, printState, nextRenderingPointer, dataAid.xResolution, dataAid.yResolution));
				}

				//Performs all of the duties that are common to most print files
				status = performPostSlice(dataAid);
				if (status != null) {
					return status;
				}
	
				if (platformSlices > 0) {
					platformSlices--;
				} else {
					extrusionSlices--;
				}
			}
			
			return performFooter(dataAid);
		} finally {
			twoDimensionalPrintDataByJob.remove(dataAid.printJob);
		}
	}
	
	@XmlTransient
	public int getSuggestedPlatformLayerCount(DataAid aid) {
		int fallbackAmount = aid.inkConfiguration.getNumberOfFirstLayers() * 2;
		TwoDimensionalSettings settings = aid.slicingProfile.getTwoDimensionalSettings();
		if (settings == null) {
			return fallbackAmount;
		}
		Double platformHeight = settings.getPlatformHeightMM();
		if (platformHeight == null) {
			return fallbackAmount;
		}
		
		return (int)Math.round(platformHeight / aid.inkConfiguration.getSliceHeight());
	}
	
	@XmlTransient
	public int getSuggested2DExtrusionLayerCount(DataAid aid) {
		int fallbackAmount = aid.inkConfiguration.getNumberOfFirstLayers() * 3;
		TwoDimensionalSettings settings = aid.slicingProfile.getTwoDimensionalSettings();
		if (settings == null) {
			return fallbackAmount;
		}
		Double extrusionHeight = settings.getExtrusionHeightMM();
		if (extrusionHeight == null) {
			return fallbackAmount;
		}
		
		return (int)Math.round(extrusionHeight / aid.inkConfiguration.getSliceHeight());
	}
	
	public Font buildFont(DataAid data) {
		TwoDimensionalSettings cwhTwoDim = data.slicingProfile.getTwoDimensionalSettings();
		org.area515.resinprinter.printer.SlicingProfile.Font cwhFont = cwhTwoDim != null?cwhTwoDim.getFont():new org.area515.resinprinter.printer.SlicingProfile.Font();
		if (cwhFont == null) {
			cwhFont = PrinterService.DEFAULT_FONT;
		}
		
		if (cwhFont.getName() == null) {
			cwhFont.setName(PrinterService.DEFAULT_FONT.getName());
		}
		
		if (cwhFont.getSize() == 0) {
			cwhFont.setSize(PrinterService.DEFAULT_FONT.getSize());
		}
		
		return new Font(cwhFont.getName(), Font.PLAIN, cwhFont.getSize());
	}
}
