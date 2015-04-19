package org.area515.resinprinter.job;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.printer.BuildDirection;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.printer.SlicingProfile.InkConfig;
import org.area515.resinprinter.slice.ZSlicer;
import org.area515.util.TemplateEngine;

public class STLFileProcessor implements PrintFileProcessor {
	private Map<PrintJob, STLFileData> dataByPrintJob = new HashMap<PrintJob, STLFileData>();
	private AtomicInteger threads = new AtomicInteger();
	public class STLFileData {
		public ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByExtension("js");
		public BufferedImage trueImage;
		public BufferedImage falseImage;
		public AtomicBoolean currentImagePointer;
		public Lock renderingImage = new ReentrantLock();
		public ZSlicer slicer;
	}

	private ScheduledExecutorService EXECUTOR = new ScheduledThreadPoolExecutor(3, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "STLFileProcessorThread-" + threads.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		}
	});
	
	public class CurrentImageRenderer implements Callable<BufferedImage> {
		private BufferedImage currentImage = null;
		private STLFileData data;
		private PrintJob printJob;
		
		public CurrentImageRenderer(PrintJob printJob, STLFileData data, int width, int height) {
			this.data = data;
			this.printJob = printJob;
			
			if (data.currentImagePointer.get()) {
				if (data.falseImage == null) {
					data.falseImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
				}
				data.renderingImage.lock();
				try {
					data.currentImagePointer.set(false);
					currentImage = data.falseImage;
				} finally {
					data.renderingImage.unlock();
				}
			} else {
				if (data.trueImage == null) {
					data.trueImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
				}
				data.renderingImage.lock();
				try {
					data.currentImagePointer.set(true);
					currentImage = data.trueImage;
				} finally {
					data.renderingImage.unlock();
				}
			}
		}
		
		public BufferedImage call() {
			SlicingProfile slicingProfile = printJob.getPrinter().getConfiguration().getSlicingProfile();
			int xResolution = slicingProfile.getxResolution();
			int yResolution = slicingProfile.getyResolution();
			
			data.slicer.colorizePolygons();
			Graphics2D g2 = (Graphics2D)currentImage.getGraphics();
			data.slicer.paintSlice(g2);
			if (slicingProfile.getProjectorGradientCalculator() != null && slicingProfile.getProjectorGradientCalculator().length() > 0) {
				Paint maskPaint;
				try {
					maskPaint = (Paint)TemplateEngine.runScript(printJob, data.scriptEngine, slicingProfile.getProjectorGradientCalculator());
					g2.setPaint(maskPaint);
					g2.fillRect(0, 0, xResolution, yResolution);
				} catch (ScriptException e) {
					e.printStackTrace();
				}
			}
			return currentImage;
		}
	}

	@Override
	public String[] getFileExtensions() {
		return new String[]{"stl"};
	}

	@Override
	public boolean acceptsFile(File processingFile) {
		return processingFile.getName().toLowerCase().endsWith("stl");
	}
	
	@Override
	public double getBuildAreaMM(PrintJob printJob) {
		STLFileData data = dataByPrintJob.get(printJob);
		SlicingProfile slicingProfile = printJob.getPrinter().getConfiguration().getSlicingProfile();
		return data.slicer.getBuildArea() / (slicingProfile.getDotsPermmX() * slicingProfile.getDotsPermmY());
	}
	
	//TODO: Why does the image on the web show a scan line defect with the north side gray and the south side white?
	@Override
	public BufferedImage getCurrentImage(PrintJob processingFile) {
		STLFileData data = dataByPrintJob.get(processingFile);
		if (data == null) {
			return null;
		}
		
		if (data.currentImagePointer == null) {
			return null;
		}
		
		data.renderingImage.lock();
		try {
			BufferedImage currentImage = data.currentImagePointer.get() && data.falseImage != null?data.falseImage:data.trueImage;
			return currentImage.getSubimage(0, 0, currentImage.getWidth(), currentImage.getHeight());
		} finally {
			data.renderingImage.unlock();
		}
	}

	@Override
	public JobStatus processFile(PrintJob printJob) throws Exception {
		Printer printer = printJob.getPrinter();
		printJob.setStartTime(System.currentTimeMillis());
		STLFileData data = new STLFileData();
		PrinterConfiguration configuration = printer.getConfiguration();
		SlicingProfile slicingProfile = configuration.getSlicingProfile();
		dataByPrintJob.put(printJob, data);
		double xPixelsPerMM = slicingProfile.getDotsPermmX();
		double yPixelsPerMM = slicingProfile.getDotsPermmY();
		int xResolution = slicingProfile.getxResolution();
		int yResolution = slicingProfile.getyResolution();
		InkConfig inkConfiguration = slicingProfile.getSelectedInkConfig();
		data.slicer = new ZSlicer(
				printJob.getJobFile(),
				1, xPixelsPerMM, yPixelsPerMM, 
				inkConfiguration.getSliceHeight(), true);
		try {
			data.slicer.loadFile(new Double(xResolution), new Double(yResolution));
			printJob.setTotalSlices(data.slicer.getZMax() - data.slicer.getZMin());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return JobStatus.Failed;
		}
		
		//Get the slicer queued up for the first image;
		data.slicer.setZ(data.slicer.getZMin());
		
		data.currentImagePointer = new AtomicBoolean(true);
		Future<BufferedImage> currentImage = EXECUTOR.submit(new CurrentImageRenderer(printJob, data, xResolution, yResolution));
		try {
			long currentSliceTime;
			if (slicingProfile.getgCodeHeader() != null && slicingProfile.getgCodeHeader().trim().length() > 0) {
				printer.getGCodeControl().executeGCodeWithTemplating(printJob, slicingProfile.getgCodeHeader());
			}
			
			int startPoint = slicingProfile.getDirection() == BuildDirection.Bottom_Up?(data.slicer.getZMin() + 1): (data.slicer.getZMax() + 1);
			int endPoint = slicingProfile.getDirection() == BuildDirection.Bottom_Up?(data.slicer.getZMax() + 1): (data.slicer.getZMin() + 1);
			for (int z = startPoint; z <= endPoint && printer.isPrintInProgress(); z += slicingProfile.getDirection().getVector()) {
				currentSliceTime = System.currentTimeMillis();
				//Get out if the print is cancelled
				if (!printer.waitForPauseIfRequired()) {
					return printer.getStatus();
				}

				if (slicingProfile.getgCodePreslice() != null && slicingProfile.getgCodePreslice().trim().length() > 0) {
					printer.getGCodeControl().executeGCodeWithTemplating(printJob, slicingProfile.getgCodePreslice());
				}
				printer.showImage(currentImage.get());

				//We don't need to waste CPU rendering the last image
				if (z < data.slicer.getZMax() + 1) {
					data.slicer.setZ(z);
					currentImage = EXECUTOR.submit(new CurrentImageRenderer(printJob, data, xResolution, yResolution));
				}

				if (slicingProfile.getExposureTimeCalculator() != null && slicingProfile.getExposureTimeCalculator().trim().length() > 0) {
					printJob.setExposureTime(((Number)TemplateEngine.runScript(printJob, data.scriptEngine, slicingProfile.getExposureTimeCalculator())).intValue());
				}
				Thread.sleep(printJob.getExposureTime());
				printer.showBlankImage();
				
				
				//Get out if the print is cancelled
				if (!printer.waitForPauseIfRequired()) {
					return printer.getStatus();
				}
				
				if (slicingProfile.getzLiftDistanceCalculator() != null && slicingProfile.getzLiftDistanceCalculator().trim().length() > 0) {
					printJob.setZLiftDistance((Double)TemplateEngine.runScript(printJob, data.scriptEngine, slicingProfile.getzLiftDistanceCalculator()));
				}
				if (slicingProfile.getzLiftSpeedCalculator() != null && slicingProfile.getzLiftSpeedCalculator().trim().length() > 0) {
					printJob.setZLiftSpeed((Double)TemplateEngine.runScript(printJob, data.scriptEngine, slicingProfile.getzLiftSpeedCalculator()));
				}
				if (slicingProfile.getZLiftDistanceGCode() != null && slicingProfile.getZLiftDistanceGCode().trim().length() > 0) {
					printer.getGCodeControl().executeGCodeWithTemplating(printJob, slicingProfile.getZLiftDistanceGCode());
				}
				if (slicingProfile.getZLiftSpeedGCode() != null && slicingProfile.getZLiftSpeedGCode().trim().length() > 0) {
					printer.getGCodeControl().executeGCodeWithTemplating(printJob, slicingProfile.getZLiftSpeedGCode());
				}
				printer.getGCodeControl().executeGCodeWithTemplating(printJob, slicingProfile.getgCodeLift());
				
				printJob.addNewSlice(System.currentTimeMillis() - currentSliceTime, getBuildAreaMM(printJob));
				
				//Notify the client that the printJob has increased the currentSlice
				NotificationManager.jobChanged(printer, printJob);
			}
			
			if (!printer.isPrintInProgress()) {
				return printer.getStatus();
			}
			
			if (slicingProfile.getgCodeFooter() != null && slicingProfile.getgCodeFooter().trim().length() == 0) {
				printer.getGCodeControl().executeGCodeWithTemplating(printJob, slicingProfile.getgCodeFooter());
			}
			return JobStatus.Completed;
		} catch (InterruptedException | ExecutionException | InappropriateDeviceException | ScriptException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public void prepareEnvironment(File processingFile) throws JobManagerException {
	}

	@Override
	public void cleanupEnvironment(File processingFile) throws JobManagerException {
	}
}
