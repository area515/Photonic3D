package org.area515.resinprinter.job;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.printer.SlicingProfile.InkConfig;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.slice.StlError;
import org.area515.util.TemplateEngine;

//TODO: Should this be an abstract Class that implements PrintFileProcessor?
public class PrintFileProcessingAid {
	private long currentSliceTime;
	private Future<Boolean> outOfInk;
	private InkDetector inkDetector;
	private DataAid data;
	
	public class DataAid {
		public ScriptEngine scriptEngine;
		public Printer printer;
		public PrintJob printJob;
		public PrinterConfiguration configuration;
		public SlicingProfile slicingProfile;
		public InkConfig inkConfiguration;
		public double xPixelsPerMM;
		public double yPixelsPerMM;
		public int xResolution;
		public int yResolution;
		public double sliceHeight;
		
		public DataAid(PrintJob printJob) throws InappropriateDeviceException {
			this.printJob = printJob;
			scriptEngine = new ScriptEngineManager().getEngineByExtension("js");
			printer = printJob.getPrinter();
			printJob.setStartTime(System.currentTimeMillis());
		    configuration = printer.getConfiguration();
			slicingProfile = configuration.getSlicingProfile();
			inkConfiguration = slicingProfile.getSelectedInkConfig();
			xPixelsPerMM = slicingProfile.getDotsPermmX();
			yPixelsPerMM = slicingProfile.getDotsPermmY();
			xResolution = slicingProfile.getxResolution();
			yResolution = slicingProfile.getyResolution();
			
			//This file processor requires an ink configuration
			if (inkConfiguration == null) {
				throw new InappropriateDeviceException("Your printer doesn't have a selected ink configuration.");
			}
			
			//TODO: how do I integrate slicingProfile.getLiftDistance()
			sliceHeight = inkConfiguration.getSliceHeight();
		}
	}
	
	public DataAid performHeader(PrintJob printJob) throws InappropriateDeviceException {
		data = new DataAid(printJob);
		
		//Set the default exposure time(this is only used if there isn't an exposure time calculator)
		printJob.setExposureTime(data.inkConfiguration.getExposureTime());
		
		//Perform the gcode associated with the printer start function
		if (data.slicingProfile.getgCodeHeader() != null && data.slicingProfile.getgCodeHeader().trim().length() > 0) {
			data.printer.getGCodeControl().executeGCodeWithTemplating(printJob, data.slicingProfile.getgCodeHeader());
		}
		
		if (data.inkConfiguration != null) {
			inkDetector = data.inkConfiguration.getInkDetector(data.printer);
		}
		
		//Only start ink detection if we have an ink detector
		if (inkDetector != null) {
			outOfInk = Main.GLOBAL_EXECUTOR.submit(inkDetector);
		}
		
		//Set the initial values for all variables.
		data.printJob.setExposureTime(data.inkConfiguration.getExposureTime());
		data.printJob.setZLiftDistance(data.slicingProfile.getLiftFeedRate());
		data.printJob.setZLiftSpeed(data.slicingProfile.getLiftDistance());
		return data;
	}
	
	public JobStatus performPreSlice(List<StlError> errors) throws InappropriateDeviceException {
		currentSliceTime = System.currentTimeMillis();

		//Perform two actions at once here:
		// 1. Pause if the user asked us to pause
		// 2. Get out if the print is cancelled
		if (!data.printer.waitForPauseIfRequired()) {
			return data.printer.getStatus();
		}
		
		//Show the errors to our users if the stl file is broken, but we'll keep on processing like normal
		if (errors != null && !errors.isEmpty()) {
			NotificationManager.errorEncountered(data.printJob, errors);
		}
		
		//Execute preslice gcode
		if (data.slicingProfile.getgCodePreslice() != null && data.slicingProfile.getgCodePreslice().trim().length() > 0) {
			data.printer.getGCodeControl().executeGCodeWithTemplating(data.printJob, data.slicingProfile.getgCodePreslice());
		}
		
		return null;
	}
	
	public JobStatus performPostSlice(PrintFileProcessor<?> processor) throws ExecutionException, InterruptedException, InappropriateDeviceException, ScriptException {

		//Start but don't wait for a potentially heavy weight operation to determine if we are out of ink.
		if (inkDetector != null) {
			outOfInk = Main.GLOBAL_EXECUTOR.submit(inkDetector);
		}
		
		//Determine the dynamic amount of time we should expose our resin
		if (!data.printJob.isExposureTimeOverriden() && data.slicingProfile.getExposureTimeCalculator() != null && data.slicingProfile.getExposureTimeCalculator().trim().length() > 0) {
			data.printJob.setExposureTime(((Number)TemplateEngine.runScript(data.printJob, data.scriptEngine, data.slicingProfile.getExposureTimeCalculator())).intValue());
		}
		
		//TODO: Open shutter here:
		
		//Sleep for the amount of time that we are exposing the resin.
		Thread.sleep(data.printJob.getExposureTime());
		
		//TODO: close shutter here:
		
		//Blank the screen in the case that our printer doesn't have a shutter
		data.printer.showBlankImage();
		
		//Is the printer out of ink?
		if (outOfInk != null && outOfInk.get()) {
			data.printer.setStatus(JobStatus.PausedOutOfPrintMaterial);
		}
		
		//Perform two actions at once here:
		// 1. Pause if the user asked us to pause
		// 2. Get out if the print is cancelled
		if (!data.printer.waitForPauseIfRequired()) {
			return data.printer.getStatus();
		}
		
		if (!data.printJob.isZLiftDistanceOverriden() && data.slicingProfile.getzLiftDistanceCalculator() != null && data.slicingProfile.getzLiftDistanceCalculator().trim().length() > 0) {
			data.printJob.setZLiftDistance(((Number)TemplateEngine.runScript(data.printJob, data.scriptEngine, data.slicingProfile.getzLiftDistanceCalculator())).doubleValue());
		}
		if (!data.printJob.isZLiftSpeedOverriden() && data.slicingProfile.getzLiftSpeedCalculator() != null && data.slicingProfile.getzLiftSpeedCalculator().trim().length() > 0) {
			data.printJob.setZLiftSpeed(((Number)TemplateEngine.runScript(data.printJob, data.scriptEngine, data.slicingProfile.getzLiftSpeedCalculator())).doubleValue());
		}
		if (data.slicingProfile.getZLiftDistanceGCode() != null && data.slicingProfile.getZLiftDistanceGCode().trim().length() > 0) {
			data.printer.getGCodeControl().executeGCodeWithTemplating(data.printJob, data.slicingProfile.getZLiftDistanceGCode());
		}
		if (data.slicingProfile.getZLiftSpeedGCode() != null && data.slicingProfile.getZLiftSpeedGCode().trim().length() > 0) {
			data.printer.getGCodeControl().executeGCodeWithTemplating(data.printJob, data.slicingProfile.getZLiftSpeedGCode());
		}
		
		//Perform the lift gcode manipulation
		data.printer.getGCodeControl().executeGCodeWithTemplating(data.printJob, data.slicingProfile.getgCodeLift());
		
		//Perform area and cost manipulations for current slice
		data.printJob.addNewSlice(System.currentTimeMillis() - currentSliceTime, processor.getBuildAreaMM(data.printJob));
		
		//Notify the client that the printJob has increased the currentSlice
		NotificationManager.jobChanged(data.printer, data.printJob);
		
		return null;
	}
	
	public JobStatus performFooter() throws InappropriateDeviceException {
		if (!data.printer.isPrintInProgress()) {
			return data.printer.getStatus();
		}
		
		if (data.slicingProfile.getgCodeFooter() != null && data.slicingProfile.getgCodeFooter().trim().length() == 0) {
			data.printer.getGCodeControl().executeGCodeWithTemplating(data.printJob, data.slicingProfile.getgCodeFooter());
		}
		
		return JobStatus.Completed;
	}
	
	public void applyBulbMask(Graphics2D g2) {
		if (data.slicingProfile.getProjectorGradientCalculator() != null && data.slicingProfile.getProjectorGradientCalculator().length() > 0) {
			Paint maskPaint;
			try {
				maskPaint = (Paint)TemplateEngine.runScript(data.printJob, data.scriptEngine, data.slicingProfile.getProjectorGradientCalculator());
				g2.setPaint(maskPaint);
				g2.fillRect(0, 0, data.xResolution, data.yResolution);
			} catch (ScriptException e) {
				e.printStackTrace();
			}
		}
	}
}
