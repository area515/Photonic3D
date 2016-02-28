
package org.area515.resinprinter.services;

import java.awt.GraphicsDevice;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.InkDetector;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.PrintJobManager;
import org.area515.resinprinter.job.render.StubPrintFileProcessor;
import org.area515.resinprinter.printer.BuildDirection;
import org.area515.resinprinter.printer.ComPortSettings;
import org.area515.resinprinter.printer.MachineConfig;
import org.area515.resinprinter.printer.MachineConfig.MonitorDriverConfig;
import org.area515.resinprinter.printer.MachineConfig.MotorsDriverConfig;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.printer.SlicingProfile.Font;
import org.area515.resinprinter.printer.SlicingProfile.InkConfig;
import org.area515.resinprinter.serial.ConsoleCommPort;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.services.TestingResult.ChartData;
import org.area515.util.TemplateEngine;

import freemarker.template.TemplateException;

@Path("printers")
public class PrinterService {
    private static final Logger logger = LogManager.getLogger();
	public static PrinterService INSTANCE = new PrinterService();
	public static Font DEFAULT_FONT = new Font("Dialog", 20);
	private PrinterService(){}
	
	private MachineResponse openShutter(String printerName, boolean shutter) throws InappropriateDeviceException {
		String name = shutter?"OpenShutter":"CloseShutter";
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		PrintJob job = buildStubJob(printer);
		if (printer == null) {
			return new MachineResponse(name, false, "Printer:" + printerName + " not started");
		}
		
		String shutterGCode = printer.getConfiguration().getSlicingProfile().getgCodeShutter();
		if (shutterGCode != null && shutterGCode.trim().length() > 0) {
			printer.setShutterOpen(shutter);
			return new MachineResponse(name, true, printer.getGCodeControl().executeGCodeWithTemplating(job, printer.getConfiguration().getSlicingProfile().getgCodeShutter()));
		}
		
		return new MachineResponse(name, false, "This printer doesn't support a shutter.");
	}
	
	@GET
	@Path("openshutter/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse openShutter(@PathParam("printername") String printerName) throws InappropriateDeviceException {
		return openShutter(printerName, true);
	}
	
	@GET
	@Path("closeshutter/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse closeShutter(@PathParam("printername") String printerName) throws InappropriateDeviceException {
		return openShutter(printerName, false);
	}
	
	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Printer> getPrinters() {
		List<PrinterConfiguration> identifiers = HostProperties.Instance().getPrinterConfigurations();
		List<Printer> printers = new ArrayList<Printer>();
		for (PrinterConfiguration current : identifiers) {
			try {
				Printer printer = PrinterManager.Instance().getPrinter(current.getName());
				if (printer == null) {
					printer = new Printer(current);
				}
				printers.add(printer);
			} catch (InappropriateDeviceException e) {
			    logger.error("Error getting printer list", e);
			}
		}
		
		return printers;
	}
 
	@GET
	@Path("get/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public Printer getPrinter(@PathParam("printername") String printerName) throws InappropriateDeviceException {
	Printer printer = PrinterManager.Instance().getPrinter(printerName);
	if (printer == null) {
		PrinterConfiguration currentConfiguration = HostProperties.Instance().getPrinterConfiguration(printerName);
		if (currentConfiguration == null) {
			throw new InappropriateDeviceException("No printer with that name:" + printerName);
			}
			
			printer = new Printer(currentConfiguration);
		}
		 
		return printer;
	}
	
	@GET
	@POST
	@DELETE
	@Path("delete/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse deletePrinter(@PathParam("printername") String printerName) {
		try {
			Printer currentPrinter = PrinterManager.Instance().getPrinter(printerName);
			if (currentPrinter != null) {
				throw new InappropriateDeviceException("Can't delete printer when it's started:" + printerName);
			}
			
			PrinterConfiguration currentConfiguration = HostProperties.Instance().getPrinterConfiguration(printerName);
			if (currentConfiguration == null) {
				throw new InappropriateDeviceException("No printer with that name:" + printerName);
			}				
			
			HostProperties.Instance().removePrinterConfiguration(currentConfiguration);
			return new MachineResponse("deletePrinter", true, "Deleted:" + printerName);
		} catch (InappropriateDeviceException e) {
		    logger.error("Error deleting printer:" + printerName, e);
			return new MachineResponse("deletePrinter", false, e.getMessage());
		}
	}

	//TODO: We need to synchronize on this printer to make sure it isn't in use.
	@POST
	@Path("save")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public MachineResponse savePrinter(Printer printerToSave) {
		try {
			String printerName = printerToSave.getConfiguration().getName();
			Printer currentPrinter = PrinterManager.Instance().getPrinter(printerName);
			if (currentPrinter != null) {
				throw new InappropriateDeviceException("Can't save printer after it's been started:" + printerName);
			}
			
			try {
				HostProperties.Instance().addOrUpdatePrinterConfiguration(printerToSave.getConfiguration());
				return new MachineResponse("savePrinter", true, "Created:" + printerToSave.getName() + "");
			} catch (AlreadyAssignedException e) {
			    logger.error("Error saving printer:" + printerToSave, e);
				return new MachineResponse("savePrinter", false, e.getMessage());
			}
		} catch (InappropriateDeviceException e) {
		    logger.error("Error saving printer:" + printerToSave, e);
			return new MachineResponse("savePrinter", false, e.getMessage());
		}
	}
	
	@GET
	@POST
	@Path("start/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse startPrinter(@PathParam("printername") String printerName) {
		Printer printer = null;
		try {
			PrinterConfiguration currentConfiguration = HostProperties.Instance().getPrinterConfiguration(printerName);
			if (currentConfiguration == null) {
				throw new InappropriateDeviceException("No printer with that name:" + printerName);
			}
			
			printer = PrinterManager.Instance().startPrinter(currentConfiguration);
			return new MachineResponse("startPrinter", true, "Started:" + printer.getName() + "");
		} catch (JobManagerException | AlreadyAssignedException | InappropriateDeviceException e) {
		    logger.error("Error starting printer:" + printerName, e);
			return new MachineResponse("startPrinter", false, e.getMessage());
		}
	}	 
	 
	@GET
	@POST
	@Path("stop/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse stopPrinter(@PathParam("printername") String printerName) {
		try {
			Printer printer = PrinterManager.Instance().getPrinter(printerName);
			if (printer == null) {
				throw new InappropriateDeviceException("This printer isn't started:" + printerName);
			}
			if (printer.isPrintInProgress()) {
				throw new InappropriateDeviceException("Can't stop printer while a job is in progress. Please stop the active printjob first.");
			}
			//You must stop the printer before removing assignments otherwise the serial ports won't get closed.
			PrinterManager.Instance().stopPrinter(printer);
			DisplayManager.Instance().removeAssignment(printer);
			SerialManager.Instance().removeAssignments(printer);
			return new MachineResponse("stopPrinter", true, "Stopped:" + printerName);
		} catch (InappropriateDeviceException e) {
		    logger.error("Error stop printer:" + printerName, e);
			return new MachineResponse("stopPrinter", false, e.getMessage());
		}
	}
 
	@POST
	@Path("createTemplatePrinter")
	@Produces(MediaType.APPLICATION_JSON)
	public Printer createTemplatePrinter() throws InappropriateDeviceException {
		//TODO: Return a nice unused name for this printer instead of the hardcoded value below
		PrinterConfiguration configuration = createTemplatePrinter(
			 "CWH Template Printer", //"mUVe 1 DLP (Testing)", 
			 DisplayManager.SIMULATED_DISPLAY, 
			 ConsoleCommPort.GCODE_RESPONSE_SIMULATION, 
			 134, 75, 185);
		configuration.getSlicingProfile().getSelectedInkConfig().setNumberOfFirstLayers(10);
		configuration.getSlicingProfile().getSelectedInkConfig().setFirstLayerExposureTime(20000);
		configuration.getSlicingProfile().getSelectedInkConfig().setExposureTime(8000);
		configuration.getSlicingProfile().getSelectedInkConfig().setPercentageOfInkConsideredEmpty(10);
		configuration.getSlicingProfile().setgCodeHeader(
			 "G21 ;Set units to be mm\n" +
			 "G91 ;Relative Positioning\n" +
			 "G28 ; Home Printer\n" +
			 "M650 D$ZLiftDist S$ZLiftRate P0; CWH Template Preferences\n" + //mUVe 1 Prefs\n" +
			 "M17 ;Enable motors");
		configuration.getSlicingProfile().setgCodeFooter(
			 "M18 ;Disable Motors");
		configuration.getSlicingProfile().setgCodeLift(
			 "M651; Do CWH Template Peel Move\n" + //Do mUVe 1 Peel Move\n" + 
			 "G1 Z${((LayerThickness) * ZDir)}");
		configuration.getSlicingProfile().setZLiftDistanceGCode("M650 D${ZLiftDist} S${ZLiftRate}");
		configuration.getSlicingProfile().setZLiftSpeedGCode("M650 D${ZLiftDist} S${ZLiftRate}");
		configuration.getSlicingProfile().setzLiftSpeedCalculator("var value = 0.25;\n"
				+ "if ($CURSLICE > $NumFirstLayers) {\n"
				+ " value = 4.6666666666666705e+000 * Math.pow($buildAreaMM,0) + -7.0000000000000184e-003 * Math.pow($buildAreaMM,1) + 3.3333333333333490e-006 * Math.pow($buildAreaMM,2);\n"
				+ "}\n"
				+ "value");
		configuration.getSlicingProfile().setzLiftDistanceCalculator("var value = 9.0;\n"
				+ "if ($CURSLICE > $NumFirstLayers) {\n"
		 	+ " value = 3.5555555555555420e+000 * Math.pow($buildAreaMM,0) + 4.3333333333334060e-003 * Math.pow($buildAreaMM,1) + 1.1111111111110492e-006 * Math.pow($buildAreaMM,2);\n"
		 	+ "}\n"
		 	+ "value");
		configuration.getSlicingProfile().setExposureTimeCalculator("var value = $FirstLayerTime;\n"
				+ "if ($CURSLICE > $NumFirstLayers) {\n"
				+ "	value = $LayerTime\n"
				+ "}\n"
		 	+ "value");
		configuration.getSlicingProfile().setProjectorGradientCalculator(
				/*"function getFractions(count, start, end) {\n" + 
			"	var incrementAmount = (end - start) / count;\n" +
			"	var fractions = [];\n" + 
			"	for (t = 0; t < count; t++) {\n" +
			"		fractions[t] = start + incrementAmount * t;\n" +
			"	}\n" +
			"	//return new float[]{0, 1};\n" +
			"	return fractions;\n" + 
				"}\n" +
				"function getColors(fractions, start, stop) {\n" + 
				"	var colors = [];\n" +
				"	var colorRange = stop - start;\n" + 
				"	var atanDivergencePoint = Math.PI / 2;\n" + 
				"	for (t = 0; t < fractions.length; t++) {\n" +
				"		colors[t] = new Packages.java.awt.Color(0, 0, 0, (java.lang.Float)(Math.atan(fractions[t] * atanDivergencePoint) * colorRange + start));\n" + 
			"	}\n" + 
			"	//return new Packages.java.awt.Color[]{new Packages.java.awt.Color(0, 0, 0, (float)(opacityLevelModel.getValue()/(float)opacityLevelModel.getMaximum())), new Packages.java.awt.Color(0, 0, 0, 0)};\n" +
			"	return colors;\n" + 
				"}\n" +*/
				"var bulbCenter = new Packages.java.awt.geom.Point2D.Double($buildPlatformXPixels / 2, $buildPlatformYPixels / 2);\n" +
				"var bulbFocus = new Packages.java.awt.geom.Point2D.Double($buildPlatformXPixels / 2, $buildPlatformYPixels / 2);\n" +
				"var totalSizeOfGradient = $buildPlatformXPixels > $buildPlatformYPixels?$buildPlatformXPixels:$buildPlatformYPixels;\n" +
				"var fractions = [0.0, 1.0];\n" +
				"//Let's start with 20% opaque in the center of the projector bulb\n" + 
				"var colors = [new Packages.java.awt.Color(0.0, 0.0, 0.0, 0.2), new Packages.java.awt.Color(0.0, 0.0, 0.0, 0.0)];\n" +
				"new Packages.java.awt.RadialGradientPaint(\n" +
				"	bulbCenter,\n" + 
				"	totalSizeOfGradient,\n" +
				"	bulbFocus,\n" +
				"	fractions,\n" +
				"	colors,\n" +
				"	java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE)");

		return new Printer(configuration);
	}
	
	PrinterConfiguration createTemplatePrinter(String printername, String displayId, String comport, double physicalProjectionMMX, double physicalProjectionMMY, double buildHeightMMZ) {
		PrinterConfiguration currentConfiguration = new PrinterConfiguration(printername, printername, false);
		ComPortSettings firmwareComSettings = new ComPortSettings();
		firmwareComSettings.setPortName(comport);
		firmwareComSettings.setDatabits(8);
		firmwareComSettings.setHandshake("None");
		firmwareComSettings.setStopbits("One");
		firmwareComSettings.setParity("None");
		firmwareComSettings.setSpeed(115200L);
		
		MotorsDriverConfig motors = new MotorsDriverConfig();
		motors.setComPortSettings(firmwareComSettings);
		MonitorDriverConfig monitor = new MonitorDriverConfig();
		ComPortSettings projectorComSettings = new ComPortSettings();
		projectorComSettings.setPortName(null);//We aren't going to be using a projector on our template
		projectorComSettings.setDatabits(8);
		projectorComSettings.setHandshake("None");
		projectorComSettings.setStopbits("One");
		projectorComSettings.setParity("None");
		projectorComSettings.setSpeed(9600L);
		monitor.setComPortSettings(projectorComSettings);
		
		MachineConfig machineConfig = new MachineConfig();
		machineConfig.setMotorsDriverConfig(motors);
		machineConfig.setMonitorDriverConfig(monitor);
		machineConfig.setOSMonitorID(displayId);
		machineConfig.setName(printername);
		machineConfig.setPlatformXSize(physicalProjectionMMX);
		machineConfig.setPlatformYSize(physicalProjectionMMY);
		machineConfig.setPlatformZSize(buildHeightMMZ);
		
		SlicingProfile slicingProfile = new SlicingProfile();
		slicingProfile.setLiftDistance(5.0);
		slicingProfile.setLiftFeedRate(50);
		slicingProfile.setDirection(BuildDirection.Bottom_Up);
		try {
			GraphicsDevice device = DisplayManager.Instance().getDisplayDevice(DisplayManager.LAST_AVAILABLE_DISPLAY);
			monitor.setDLP_X_Res(device.getDefaultConfiguration().getBounds().getWidth());
			monitor.setDLP_Y_Res(device.getDefaultConfiguration().getBounds().getHeight());
			machineConfig.setxRenderSize((int)monitor.getDLP_X_Res());
			machineConfig.setyRenderSize((int)monitor.getDLP_Y_Res());
			slicingProfile.setxResolution((int)monitor.getDLP_X_Res());
			slicingProfile.setyResolution((int)monitor.getDLP_Y_Res());
		} catch (InappropriateDeviceException e) {
		    logger.error("Error creating graphics device for printer:" + printername, e);
			throw new IllegalArgumentException("Couldn't get screen device");
		}
				
		InkConfig ink = new InkConfig();
		ink.setName("Default");
		ink.setNumberOfFirstLayers(3);
		ink.setResinPriceL(65.0);
		ink.setSliceHeight(0.1);
		ink.setFirstLayerExposureTime(5000);
		ink.setExposureTime(1000);
		
		List<InkConfig> configs = new ArrayList<InkConfig>();
		configs.add(ink);
		
		slicingProfile.setFont(DEFAULT_FONT);
		slicingProfile.setInkConfigs(configs);
		slicingProfile.setSelectedInkConfigName("Default");
		slicingProfile.setDotsPermmX(monitor.getDLP_X_Res() / physicalProjectionMMX);
		slicingProfile.setDotsPermmY(monitor.getDLP_Y_Res() / physicalProjectionMMY);
		slicingProfile.setFlipX(false);
		slicingProfile.setFlipY(true);
		
		currentConfiguration.setSlicingProfile(slicingProfile);
		currentConfiguration.setMachineConfig(machineConfig);
			
		currentConfiguration.setName(printername);
		return currentConfiguration;
	}
	
	@GET
	@Path("showGridScreen/{printername}/{pixels}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse showGridScreen(@PathParam("printername") String printerName, @PathParam("pixels") int pixels) {
		try {
			Printer currentPrinter = PrinterManager.Instance().getPrinter(printerName);
			if (currentPrinter == null) {
				throw new InappropriateDeviceException("Printer:" + printerName + " not started");
			}
			
			currentPrinter.showGridImage(pixels);
			return new MachineResponse("gridscreenshown", true, "Showed calibration screen on:" + printerName);
		} catch (InappropriateDeviceException e) {
		    logger.error("Error showing grid screen for printer:" + printerName, e);
			return new MachineResponse("gridscreenshown", false, e.getMessage());
		}
	}
	
	@GET
	@Path("showCalibrationScreen/{printername}/{xpixels}/{ypixels}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse showCalibrationScreen(@PathParam("printername") String printerName, @PathParam("xpixels") int xPixels,  @PathParam("ypixels") int yPixels) {
		try {
			Printer currentPrinter = PrinterManager.Instance().getPrinter(printerName);
			if (currentPrinter == null) {
				throw new InappropriateDeviceException("Printer:" + printerName + " not started");
			}
			
			currentPrinter.showCalibrationImage(xPixels, yPixels);
			return new MachineResponse("calibrationscreenshown", true, "Showed calibration screen on:" + printerName);
		} catch (InappropriateDeviceException e) {
		    logger.error("Error showing calibration screen for printer:" + printerName, e);
			return new MachineResponse("calibrationscreenshown", false, e.getMessage());
		}
	}
	
	//This is the only method that breaks the rules that says that you can't save a printer while it is started...
	@GET
	@Path("calibrate/{printername}/{xpixels}/{ypixels}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse calibrate(@PathParam("printername") String printerName, @PathParam("xpixelspermm") double xPixelsPerMM,  @PathParam("ypixelspermm") double yPixelsPerMM) {
		try {
			PrinterConfiguration currentConfiguration = HostProperties.Instance().getPrinterConfiguration(printerName);
			if (currentConfiguration == null) {
				throw new InappropriateDeviceException("No printer with that name:" + printerName);
			}				
			
			currentConfiguration.getSlicingProfile().setDotsPermmX(xPixelsPerMM);
			currentConfiguration.getSlicingProfile().setDotsPermmY(xPixelsPerMM);
			
			HostProperties.Instance().addOrUpdatePrinterConfiguration(currentConfiguration);
			return new MachineResponse("calibratePrinter", true, "Calibrated printer:" + printerName + "");
		} catch (InappropriateDeviceException e) {
		    logger.error("Error calibrating printer:" + printerName, e);
			return new MachineResponse("calibratePrinter", false, e.getMessage());
		} catch (AlreadyAssignedException e) {
		    logger.error("Error saving printer:" + printerName, e);
			return new MachineResponse("calibratePrinter", false, e.getMessage());
		}
	}
	
	@GET
	@Path("showBlankScreen/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse showBlankScreen(@PathParam("printername") String printerName) {
		try {
			Printer currentPrinter = PrinterManager.Instance().getPrinter(printerName);
			if (currentPrinter == null) {
				throw new InappropriateDeviceException("Printer:" + printerName + " not started");
			}
			
			currentPrinter.showBlankImage();
			return new MachineResponse("blankscreenshown", true, "Showed blank screen on:" + printerName);
		} catch (InappropriateDeviceException e) {
		    logger.error("Error showing blank screen for printer:" + printerName, e);
			return new MachineResponse("blankscreenshown", false, e.getMessage());
		}
	}
	
	@GET
	@Path("executeGCode/{printername}/{gcode}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse executeGCode(@PathParam("printername") String printerName, @PathParam("gcode") String gcode) {
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("gcode", false, "Printer:" + printerName + " not started");
		}
		
		return new MachineResponse("gcode", true, printer.getGCodeControl().sendGcode(gcode));
	}
	
	//X Axis Move (sedgwick open aperature)
	//MachineControl.cmdMoveX()
	@GET
	@Path("moveX/{printername}/{distance}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse moveX(@PathParam("distance") String dist, @PathParam("printername") String printerName) {
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("movex", false, "Printer:" + printerName + " not started");
		}
		
		printer.getGCodeControl().executeSetRelativePositioning();
		return new MachineResponse("movex", true, printer.getGCodeControl().executeMoveX(Double.parseDouble(dist)));
	}
	
	//Y Axis Move (sedgwick close aperature)
	//MachineControl.cmdMoveY()
	@GET
	@Path("moveY/{printername}/{distance}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse moveY(@PathParam("distance") String dist, @PathParam("printername") String printerName) {
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("movey", false, "Printer:" + printerName + " not started");
		}
		
		printer.getGCodeControl().executeSetRelativePositioning();
		return new MachineResponse("movey", true, printer.getGCodeControl().executeMoveY(Double.parseDouble(dist)));
	}
	
	//Z Axis Move(double dist)
	//MachineControl.cmdMoveZ(double dist)
	// (.025 small reverse)
	// (1.0 medium reverse)
	// (10.0 large reverse)
	// (-.025 small reverse)
	// (-1.0 medium reverse)
	// (-10.0 large reverse)
	@GET
	@Path("moveZ/{printername}/{distance}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse moveZ(@PathParam("distance") String dist, @PathParam("printername") String printerName) {
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("movez", false, "Printer:" + printerName + " not started");
		}
	
		printer.getGCodeControl().executeSetRelativePositioning();
		String response = printer.getGCodeControl().executeMoveZ(Double.parseDouble(dist));
		return new MachineResponse("movez", true, response);
	}
	 
	@GET
	@Path("homeZ/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse homeZ(@PathParam("printername") String printerName) {
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("homez", false, "Printer:" + printerName + " not started");
		}
		printer.getGCodeControl().executeSetRelativePositioning();
		return new MachineResponse("homez", true, printer.getGCodeControl().executeZHome());
	 }
	 
	@GET
	@Path("homeX/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse homeX(@PathParam("printername") String printerName) {
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("homex", false, "Printer:" + printerName + " not started");
		}
			
		printer.getGCodeControl().executeSetRelativePositioning();
		return new MachineResponse("homex", true, printer.getGCodeControl().executeXHome());
	}	 
	
	@GET
	@Path("homeY/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse homeY(@PathParam("printername") String printerName) {
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("homey", false, "Printer:" + printerName + " not started");
		}
			
		printer.getGCodeControl().executeSetRelativePositioning();
		return new MachineResponse("homey", true, printer.getGCodeControl().executeYHome());
	}

	// Disable Motors
	//MachineControl.cmdMotorsOff()
	@GET
	@Path("motorsOff/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse motorsOff(@PathParam("printername") String printerName) {
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("motorsoff", false, "Printer:" + printerName + " not started");
		}
			
		return new MachineResponse("motorsoff", true, printer.getGCodeControl().executeMotorsOff());
	}
	 
	// Enable Motors
	//MachineControl.cmdMotorsOn()
	@GET
	@Path("motorsOn/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse motorsOn(@PathParam("printername") String printerName) {
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("motorson", false, "Printer:" + printerName + " not started");
		}
		
		return new MachineResponse("motorson", true, printer.getGCodeControl().executeMotorsOn());
	}

	@GET
	@Path("startProjector/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse startProjector(@PathParam("printername") String printerName) {
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("startProjector", false, "Printer:" + printerName + " not started");
		}
		
		try {
			printer.setProjectorPowerStatus(true);
			return new MachineResponse("startProjector", true, "Projector started.");
		} catch (IOException e) {
		    logger.error("Error starting projector for printer:" + printerName, e);
			return new MachineResponse("startProjector", false, e.getMessage());
		}
	}
	 
	@GET
	@Path("stopProjector/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse stopProjector(@PathParam("printername") String printerName) {
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("stopProjector", false, "Printer:" + printerName + " not started");
		}
		
		try {
			printer.setProjectorPowerStatus(false);
			return new MachineResponse("stopProjector", true, "Projector stopped.");
		} catch (IOException e) {
		    logger.error("Error stopping projector for printer:" + printerName, e);
			return new MachineResponse("stopProjector", false, e.getMessage());
		}
	}
	 
	@GET
	@Path("startJob/{fileName}/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse print(@PathParam("fileName") String fileName, @PathParam("printername") String printername) {
		Printer printer = PrinterManager.Instance().getPrinter(printername);
		if (printer == null) {
			return new MachineResponse("start", false, "Printer not started:" + printername);
		}
		
		//Printer must have been calibrated before it can print
		if (!printer.getConfiguration().isCalibrated()) {
		    logger.error("Printer:{} can't print because it wasn't calibrated", printername);
			return new MachineResponse("startPrinter", false, "Printer:" + printername + " must be calibrated before it's first use.");
		}
		
		// Create job
		File selectedFile = new File(HostProperties.Instance().getUploadDir(), fileName); //should already be done by marshalling: java.net.URLDecoder.decode(name, "UTF-8"));//name);
		
		// Delete and Create handled in jobManager
		PrintJob printJob = null;
		try {
			printJob = PrintJobManager.Instance().createJob(selectedFile, printer);
			return new MachineResponse("start", true, printJob.getId() + "");
		} catch (JobManagerException | AlreadyAssignedException e) {
		    logger.error("Error starting job:" + fileName + " printer:" + printername, e);
			return new MachineResponse("start", false, e.getMessage());
		}
	}
	
	private List<ChartData> getChartData(String seriesString) {
		List<ChartData> seriesData = new ArrayList<ChartData>();
		Matcher matcher = Pattern.compile("([^\\(]+)\\(([^\\)]+)\\)").matcher(seriesString);
		while (matcher.find()) {
			Double start = null;
			Double stop = null;
			Double increment = null;
			StringTokenizer numbers = new StringTokenizer(matcher.group(2), ",");
			while (start == null || stop == null || increment == null) {
				if (start == null) {
					start = new Double(numbers.nextToken());
				} else if (stop == null) {
					stop = new Double(numbers.nextToken());
				} else if (increment == null) {
					increment = new Double(numbers.nextToken());
					seriesData.add(new ChartData(start, stop, increment, matcher.group(1)));
					break;
				}
			}
		}
		
		return seriesData;
	}
	
	private static PrintJob buildStubJob(Printer printer) {
		PrintJob job = new PrintJob(null);//Null job for testing...
		StubPrintFileProcessor<Object> processor = new StubPrintFileProcessor<>();
		job.setPrintFileProcessor(processor);
		job.setPrinter(printer);
		return job;
	}
	
	@POST
	@Path("testScript/{printername}/{scriptname}/{returnType}")
	@Produces(MediaType.APPLICATION_JSON)
	public TestingResult testScript(@PathParam("printername")String printerName, @PathParam("scriptname")String scriptName, String javascript, @PathParam("returnType")String returnTypeExpectation) throws InappropriateDeviceException {
		Printer printer = getPrinter(printerName);
		PrintJob job = buildStubJob(printer);

		try {
			Class returnType = null;
			List<ChartData> chartData = null;
			Matcher matcher = Pattern.compile("([^\\[]+)\\[([^\\]]+)\\]").matcher(returnTypeExpectation);
			if (matcher.matches()) {
				returnTypeExpectation = matcher.group(1);
				chartData = getChartData(matcher.group(2));
				returnType = Class.forName(returnTypeExpectation);
			} else {
				returnType = Class.forName(returnTypeExpectation);
			}

			ScriptEngine engine = HostProperties.Instance().buildScriptEngine();
			
			if (chartData == null) {
				Object returnObject = TemplateEngine.runScript(job, printer, engine, javascript, scriptName, null);
				if (!returnType.isAssignableFrom(returnObject.getClass())) {
					TestingResult result = new TestingResult("This method expects a return type of:" + returnTypeExpectation + " you provided:" + returnObject.getClass(), -1);
					return result;
				}
				
				return new TestingResult(returnObject);
			}
			
			Map<String, Object> bindings = new HashMap<>();
			ChartData currentSeries = chartData.get(0);
			ChartData currentData = chartData.get(1);
			double currentSeriesValue = currentSeries.getStart();
			TestingResult.Chart chart = new TestingResult.Chart(new double[currentSeries.getSize()][], new String[currentData.getSize()], new String[currentSeries.getSize()], scriptName);
			for (int currentSeriesIndex = 0; currentSeriesIndex < currentSeries.getSize(); currentSeriesIndex++) {
				double[] dataArray = new double[currentData.getSize()];
				bindings.put(currentSeries.getName(), currentSeriesValue);
				
				double currentDataValue = currentData.getStart();
				for (int valueIndex = 0; valueIndex < currentData.getSize(); valueIndex++) {
					bindings.put(currentData.getName(), currentDataValue);
					chart.getLabels()[valueIndex] = currentData.getName() + ":" + currentDataValue;
					
					Object returnObject = TemplateEngine.runScript(job, printer, engine, javascript, scriptName, bindings);
					if (returnObject == null) {
						TestingResult result = new TestingResult("You returned a null from your script.", -1);
						return result;
					}
					if (!returnType.isAssignableFrom(returnObject.getClass())) {
						TestingResult result = new TestingResult("This script expects a return type of:" + returnTypeExpectation + " you provided:" + returnObject.getClass(), -1);
						return result;
					}
					
					dataArray[valueIndex] = ((Number)returnObject).doubleValue();
					currentDataValue += currentData.getIncrement();
				}
				chart.getSeries()[currentSeriesIndex] = currentSeries.getName() + ":" + currentSeriesValue;
				chart.getData()[currentSeriesIndex] = dataArray;
				currentSeriesValue += currentSeries.getIncrement();
			}

			return new TestingResult(chart);
		} catch (ScriptException e) {
			TestingResult result = new TestingResult(e.getMessage(), e.getLineNumber());
			return result;
		} catch (ClassNotFoundException e) {
			TestingResult result = new TestingResult("Couldn't find type:" + returnTypeExpectation, -1);
			return result;
		}
	}
	
	@POST
	@Path("testTemplate/{printername}/{templatename}")
	@Produces(MediaType.APPLICATION_JSON)
	public TestingResult testTemplate(@PathParam("printername")String printerName, @PathParam("templatename")String templateName, String template) throws InappropriateDeviceException {
		Printer printer = getPrinter(printerName);
		PrintJob job = buildStubJob(printer);
		
		try {
			String returnValue = TemplateEngine.buildData(job, printer, template);
			return new TestingResult(returnValue);
		} catch (IOException e) {
			TestingResult result = new TestingResult(e.getMessage(), -1);
			return result;
		} catch (TemplateException e) {
			TestingResult result = new TestingResult(e.getMessage(), e.getLineNumber());
			return result;
		}
	}
	
	@GET
	@Path("remainingPrintMaterial/{printername}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse getRemainingResin(@PathParam("printername") String printerName) throws InappropriateDeviceException {
		Printer printer = getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("remainingPrintMaterial", false, "Printer not started:" + printerName);
		}
		
		InkDetector detector = printer.getConfiguration().getSlicingProfile().getSelectedInkConfig().getInkDetector(printer);
		if (detector == null) {
			return new MachineResponse("remainingPrintMaterial", false, "This printer doesn't have a PrintMaterialDetector configured. Save and restart the printer.");
		}
		float materialLeft;
		try {
			materialLeft = detector.performMeasurement();
			return new MachineResponse("remainingPrintMaterial", true, materialLeft + "");
		} catch (IOException e) {
		    logger.error("Error remaining print material for printer:" + printerName, e);
			return new MachineResponse("remainingPrintMaterial", false, e.getMessage());
		}
	}
}