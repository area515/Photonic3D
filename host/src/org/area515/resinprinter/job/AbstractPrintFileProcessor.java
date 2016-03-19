package org.area515.resinprinter.job;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlTransient;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.printer.SlicingProfile.InkConfig;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.services.PrinterService;
import org.area515.resinprinter.slice.StlError;
import org.area515.util.TemplateEngine;

public abstract class AbstractPrintFileProcessor<G> implements PrintFileProcessor<G>{
	//TODO: You can't have instance variables, PrintFileProcessor is a Singleton pattern!!!  
	//Once two printers are printing at the same time we've got chaos!!!!! 
	//FIXME by pulling these instance variables into the DataAid and getting rid of the DataAid instance variable.
	//Instead of having each implementation keep it's own state in it's own hashtable, we should be doing all of that work in here...
	private long currentSliceTime;
	private InkDetector inkDetector;
	private DataAid data;
	
	public static class DataAid {
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
			scriptEngine = HostProperties.Instance().buildScriptEngine();
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
	
	public DataAid initializeDataAid(PrintJob printJob) throws InappropriateDeviceException {
		data = new DataAid(printJob);
		return data;
	}
	
	public void performHeader() throws InappropriateDeviceException, IOException {
		if (data == null) {
			throw new IllegalStateException("initializeDataAid must be called before this method");
		}
		
		if (data.printer.isProjectorPowerControlSupported()) {
			data.printer.setProjectorPowerStatus(true);
		}
		
		//Set the default exposure time(this is only used if there isn't an exposure time calculator)
		data.printJob.setExposureTime(data.inkConfiguration.getExposureTime());
		
		//Perform the gcode associated with the printer start function
		if (data.slicingProfile.getgCodeHeader() != null && data.slicingProfile.getgCodeHeader().trim().length() > 0) {
			data.printer.getGCodeControl().executeGCodeWithTemplating(data.printJob, data.slicingProfile.getgCodeHeader());
		}
		
		if (data.inkConfiguration != null) {
			inkDetector = data.inkConfiguration.getInkDetector(data.printJob);
		}
		
		//Set the initial values for all variables.
		data.printJob.setExposureTime(data.inkConfiguration.getExposureTime());
		data.printJob.setZLiftDistance(data.slicingProfile.getLiftFeedRate());
		data.printJob.setZLiftSpeed(data.slicingProfile.getLiftDistance());
		
		//Initialize bulb hours only once per print
		data.printer.getBulbHours();
	}
	
	public JobStatus performPreSlice(List<StlError> errors) throws InappropriateDeviceException {
		if (data == null) {
			throw new IllegalStateException("initializeDataAid must be called before this method");
		}
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
	
	public JobStatus performPostSlice() throws ExecutionException, InterruptedException, InappropriateDeviceException, ScriptException {
		if (data == null) {
			throw new IllegalStateException("initializeDataAid must be called before this method");
		}

		//Start but don't wait for a potentially heavy weight operation to determine if we are out of ink.
		if (inkDetector != null) {
			inkDetector.startMeasurement();
		}
		
		//Determine the dynamic amount of time we should expose our resin
		if (!data.printJob.isExposureTimeOverriden() && data.slicingProfile.getExposureTimeCalculator() != null && data.slicingProfile.getExposureTimeCalculator().trim().length() > 0) {
			Number value = calculate(data.slicingProfile.getExposureTimeCalculator(), "exposure time script");
			if (value != null) {
				data.printJob.setExposureTime(value.intValue());
			}
		}
		
		if (data.slicingProfile.getgCodeShutter() != null && data.slicingProfile.getgCodeShutter().trim().length() > 0) {
			data.printer.setShutterOpen(true);
			data.printer.getGCodeControl().executeGCodeWithTemplating(data.printJob, data.slicingProfile.getgCodeShutter());
		}
		
		//Sleep for the amount of time that we are exposing the resin.
		Thread.sleep(data.printJob.getExposureTime());
		
		if (data.slicingProfile.getgCodeShutter() != null && data.slicingProfile.getgCodeShutter().trim().length() > 0) {
			data.printer.setShutterOpen(false);
			data.printer.getGCodeControl().executeGCodeWithTemplating(data.printJob, data.slicingProfile.getgCodeShutter());
		}
		
		//Blank the screen in the case that our printer doesn't have a shutter
		data.printer.showBlankImage();
		
		//Perform two actions at once here:
		// 1. Pause if the user asked us to pause
		// 2. Get out if the print is cancelled
		if (!data.printer.waitForPauseIfRequired()) {
			return data.printer.getStatus();
		}
		
		if (!data.printJob.isZLiftDistanceOverriden() && data.slicingProfile.getzLiftDistanceCalculator() != null && data.slicingProfile.getzLiftDistanceCalculator().trim().length() > 0) {
			Number value = calculate(data.slicingProfile.getzLiftDistanceCalculator(), "lift distance script");
			if (value != null) {
				data.printJob.setZLiftDistance(value.doubleValue());
			}
		}
		if (!data.printJob.isZLiftSpeedOverriden() && data.slicingProfile.getzLiftSpeedCalculator() != null && data.slicingProfile.getzLiftSpeedCalculator().trim().length() > 0) {
			Number value = calculate(data.slicingProfile.getzLiftSpeedCalculator(), "lift speed script");
			if (value != null) {
				data.printJob.setZLiftSpeed(value.doubleValue());
			}
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
		data.printJob.addNewSlice(System.currentTimeMillis() - currentSliceTime, getBuildAreaMM(data.printJob));
		
		//Notify the client that the printJob has increased the currentSlice
		NotificationManager.jobChanged(data.printer, data.printJob);
		
		return null;
	}

	public JobStatus performFooter() throws IOException, InappropriateDeviceException {
		if (data == null) {
			throw new IllegalStateException("initializeDataAid must be called before this method");
		}

		if (!data.printer.isPrintActive()) {
			return data.printer.getStatus();
		}
		
		if (data.slicingProfile.getgCodeFooter() != null && data.slicingProfile.getgCodeFooter().trim().length() == 0) {
			data.printer.getGCodeControl().executeGCodeWithTemplating(data.printJob, data.slicingProfile.getgCodeFooter());
		}
		
		if (data.printer.isProjectorPowerControlSupported()) {
			data.printer.setProjectorPowerStatus(false);
		}

		return JobStatus.Completed;
	}

	private Number calculate(String calculator, String calculationName) throws ScriptException {
		try {
			Number num = (Number)TemplateEngine.runScript(data.printJob, data.printer, data.scriptEngine, calculator, calculationName, null);
			if (num == null || Double.isNaN(num.doubleValue())) {
				return null;
			}
			return num;
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("The result of your " + calculationName + " needs to evaluate to an instance of java.lang.Number");
		}
	}
	
	public void applyBulbMask(Graphics2D g2, int width, int height) throws ScriptException {
		if (data == null) {
			throw new IllegalStateException("initializeDataAid must be called before this method");
		}

		if (data.slicingProfile.getProjectorGradientCalculator() != null && data.slicingProfile.getProjectorGradientCalculator().length() > 0) {
			Paint maskPaint;
			try {
				maskPaint = (Paint)TemplateEngine.runScript(data.printJob, data.printer, data.scriptEngine, data.slicingProfile.getProjectorGradientCalculator(), "projector gradient script", null);
				g2.setPaint(maskPaint);
				g2.fillRect(0, 0, width, height);
			} catch (ClassCastException e) {
				throw new IllegalArgumentException("The result of your bulb mask script needs to evaluate to an instance of java.awt.Paint");
			}
		}
	}
	
	public Font buildFont() {
		org.area515.resinprinter.printer.SlicingProfile.Font cwhFont = data.slicingProfile.getFont();
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
	
	@XmlTransient
	public int getSuggestedSolidLayerCountFor2DGraphic() {
		return data.inkConfiguration.getNumberOfFirstLayers() * 2;
	}
	
	@XmlTransient
	public int getSuggestedImplementationLayerCountFor2DGraphic() {
		return data.inkConfiguration.getNumberOfFirstLayers() * 3;
	}
	
	@XmlTransient
	public int getSuggestedPrettyBorderWidth() {
		return 50;
	}
}
