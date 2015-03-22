package org.area515.resinprinter.services;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.JobManager;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.BuildDirection;
import org.area515.resinprinter.printer.MachineConfig;
import org.area515.resinprinter.printer.MachineConfig.ComPortSettings;
import org.area515.resinprinter.printer.MachineConfig.MonitorDriverConfig;
import org.area515.resinprinter.printer.MachineConfig.MotorsDriverConfig;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.printer.SlicingProfile.InkConfig;
import org.area515.resinprinter.serial.ConsoleCommPort;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.server.HostProperties;
import org.area515.util.TemplateEngine;

@Path("machine")
public class MachineService {
	
	public static MachineService INSTANCE = new MachineService();
	
	private MachineService(){}
	
	 @GET
	 @Path("printers")
	 @Produces(MediaType.APPLICATION_JSON)
	// @RolesAllowed("Admin")
	 public List<String> getPrinters() {
		 List<PrinterConfiguration> identifiers = HostProperties.Instance().getPrinterConfigurations();
		 List<String> identifierStrings = new ArrayList<String>();
		 for (PrinterConfiguration current : identifiers) {
			 identifierStrings.add(current.getName());
		 }
		 
		 return identifierStrings;
	 }
	 
	 @GET
	 @Path("ports")
	 @Produces(MediaType.APPLICATION_JSON)
	 public List<String> getPorts() {
		 List<SerialCommunicationsPort> identifiers = SerialManager.Instance().getSerialDevices();
		 List<String> identifierStrings = new ArrayList<String>();
		 for (SerialCommunicationsPort current : identifiers) {
			 identifierStrings.add(current.getName());
		 }
		 
		 return identifierStrings;
	 }
	 
	 @GET
	 @Path("displays")
	 @Produces(MediaType.APPLICATION_JSON)
	 public List<String> getDisplays() {
		 List<GraphicsDevice> devices = DisplayManager.Instance().getDisplayDevices();
		 List<String> deviceStrings = new ArrayList<String>();
		 for (GraphicsDevice current : devices) {
			 deviceStrings.add(current.getIDstring());
		 }
		 
		 return deviceStrings;
	 }
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 //These are some help methods:
	 public static void main(String[] args) {
		 new MachineService().createDefaultPrintersInMachinesDirectory();
	 }
	 
	 private void createDefaultPrintersInMachinesDirectory() {
		 PrinterConfiguration configuration = createTemplatePrinter(
				 "mUVe 1 DLP (Testing)", 
				 DisplayManager.SIMULATED_DISPLAY, 
				 ConsoleCommPort.CONSOLE_COMM_PORT, 
				 134, 75, 185);
		 configuration.getSlicingProfile().getSelectedInkConfig().setNumberOfBottomLayers(10);
		 configuration.getSlicingProfile().getSelectedInkConfig().setFirstLayerTime(20000);
		 configuration.getSlicingProfile().getSelectedInkConfig().setLayerTime(8000);
		 configuration.getSlicingProfile().setgCodeHeader(
				 "G21 ;Set units to be mm\n" +
				 "G91 ;Relative Positioning\n" +
				 "G28 ; Home Printer\n" +
				 "M650 D$ZLiftDist S$ZLiftRate P0;mUVe 1 Prefs\n" +
				 "M17 ;Enable motors");
		 configuration.getSlicingProfile().setgCodeFooter(
				 "M18 ;Disable Motors");
		 configuration.getSlicingProfile().setgCodeLift(
				 "M651; Do mUVe 1 Peel Move\n" + 
				 "G1 Z(($LayerThickness) * $ZDir)");
		 configuration.getSlicingProfile().setZLiftDistanceGCode("M650 D${ZLiftDist} S${ZLiftRate}");
		 configuration.getSlicingProfile().setZLiftSpeedGCode("M650 D${ZLiftDist} S${ZLiftRate}");
		 configuration.getSlicingProfile().setzLiftSpeedCalculator("var value = 0.25;\n"
		 		+ "if ($CURSLICE > $NumFirstLayers) {\n"
		 		+ " value = 4.6666666666666705e+000 * Math.pow($buildAreaMM,0) + -7.0000000000000184e-003 * Math.pow($buildAreaMM,1) + 3.3333333333333490e-006 * Math.pow($buildAreaMM,2);\n"
		 		+ "}\n"
		 		+ "value");
		 configuration.getSlicingProfile().setzLiftDistanceCalculator("var value = 9;\n"
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
			 	"importPackage(java.awt.geom);\n" +
			    "importPackage(java.awt);\n" +
		 		"function getFractions(count, start, end) {\n" + 
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
	 			"		colors[t] = new Color(0, 0, 0, Math.atan(fractions[t] * atanDivergencePoint) * colorRange + start);\n" +
				"	}\n" + 
				"	//return new Color[]{new Color(0, 0, 0, opacityLevelModel.getValue()/(float)opacityLevelModel.getMaximum()), new Color(0, 0, 0, 0)};\n" +
				"	return colors;\n" + 
	 			"}\n" +
	 			"var bulbCenter = new Point2D.Double($buildPlatformXPixels / 2, $buildPlatformYPixels / 2);\n" +
	 			"var bulbFocus = new Point2D.Double($buildPlatformXPixels / 2, $buildPlatformYPixels / 2);\n" +
	 			"var totalSizeOfGradient = $buildPlatformXPixels > $buildPlatformYPixels?$buildPlatformXPixels:$buildPlatformYPixels;\n" +
	 			"var fractions = getFractions(totalSizeOfGradient, 0, 1);\n" +
	 			"var colors = getColors(fractions, 0.2, 0);//Let's start with 20% opaque in the center of the projector bulb\n" +
	 			"new RadialGradientPaint(\n" +
	 			"	bulbCenter,\n" + 
	 			"	totalSizeOfGradient,\n" +
				"	bulbFocus,\n" +
				"	fractions,\n" + 
				"	colors,\n" +
				"	MultipleGradientPaint.CycleMethod.NO_CYCLE)");
		 
		try {
			HostProperties.Instance().addPrinterConfiguration(configuration);
		} catch (AlreadyAssignedException e) {
			e.printStackTrace();
		}
	 }
	 
	 
	 private PrinterConfiguration createTemplatePrinter(String printername, String displayId, String comport, double physicalProjectionMMX, double physicalProjectionMMY, double buildHeightMMZ) {
			PrinterConfiguration currentConfiguration = new PrinterConfiguration(printername, printername);
			ComPortSettings settings = new ComPortSettings();
			settings.setPortName(comport);
			settings.setDatabits(8);
			settings.setHandshake("None");
			settings.setStopbits("One");
			settings.setParity("None");
			settings.setSpeed(115200);
			MotorsDriverConfig motors = new MotorsDriverConfig();
			motors.setComPortSettings(settings);
			MonitorDriverConfig monitor = new MonitorDriverConfig();
			
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
				e.printStackTrace();
				throw new IllegalArgumentException("Couldn't get screen device");
			}
			
			InkConfig ink = new InkConfig();
			ink.setName("Default");
			ink.setNumberOfBottomLayers(3);
			ink.setResinPriceL(65.0);
			ink.setSliceHeight(0.1);
			ink.setFirstLayerTime(5000);
			ink.setLayerTime(1000);
			
			List<InkConfig> configs = new ArrayList<InkConfig>();
			configs.add(ink);
			
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
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 //The following methods are for Printers
	 //======================================

	 @GET
	 @Path("createprinter/{printername}/{display}/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse createPrinter(@PathParam("printername") String printername, @PathParam("display") String displayId, @PathParam("comport") String comport) {
		//TODO: This data needs to be set by the user interface...
		//========================================================
		PrinterConfiguration currentConfiguration = createTemplatePrinter(printername, displayId, comport, 134, 75, 185);
		//=========================================================
		try {
			HostProperties.Instance().addPrinterConfiguration(currentConfiguration);
			return new MachineResponse("create", true, "Created:" + currentConfiguration.getName() + "");
		} catch (AlreadyAssignedException e) {
			e.printStackTrace();
			return new MachineResponse("create", false, e.getMessage());
		}
	 }
	 
	 @GET
	 @Path("deleteprinter/{printername}")
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
				return new MachineResponse("delete", true, "Deleted:" + printerName);
			} catch (InappropriateDeviceException e) {
				e.printStackTrace();
				return new MachineResponse("delete", false, e.getMessage());
			}
	 }

	 @GET
	 @Path("startprinter/{printername}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse startPrinter(@PathParam("printername") String printerName) {
		Printer printer = null;
		try {
			PrinterConfiguration currentConfiguration = HostProperties.Instance().getPrinterConfiguration(printerName);
			if (currentConfiguration == null) {
				throw new InappropriateDeviceException("No printer with that name:" + printerName);
			}
			
			printer = PrinterManager.Instance().startPrinter(currentConfiguration);
			return new MachineResponse("start", true, "Started:" + printer.getName() + "");
		} catch (JobManagerException | AlreadyAssignedException | InappropriateDeviceException e) {
			e.printStackTrace();
			return new MachineResponse("start", false, e.getMessage());
		}
	 }	 
	 
	 @GET
	 @Path("stopprinter/{printername}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse stopPrinter(@PathParam("printername") String printerName) {
		try {
			Printer printer = PrinterManager.Instance().getPrinter(printerName);
			if (printer == null) {
				throw new InappropriateDeviceException("This printer isn't started:" + printerName);
			}
			if (printer.isPrintInProgress()) {
				throw new InappropriateDeviceException("Can't stop printer while a job is in progress");
			}
			DisplayManager.Instance().removeAssignment(printer);
			SerialManager.Instance().removeAssignment(printer);
			if (printer != null) {
				printer.close();
			}
			PrinterManager.Instance().stopPrinter(printer);
			return new MachineResponse("stop", true, "Stopped:" + printerName);
		} catch (InappropriateDeviceException e) {
			e.printStackTrace();
			return new MachineResponse("stop", false, e.getMessage());
		}
	 }

	 @GET
	 @Path("printerstatus/{printername}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getPrinterStatus(@PathParam("printername") String printerName) {
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			return new MachineResponse("status", false, "Printer:" + printerName + " not started");
		}

		return new MachineResponse("status", true, "Printer:" + printerName + " " + printer.getStatus());
	 }
	 
	 @GET
	 @Path("showcalibrationscreen/{printername}/{pixels}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse showCalibrationScreen(@PathParam("printername") String printerName, @PathParam("pixels") int pixels) {
		try {
			Printer currentPrinter = PrinterManager.Instance().getPrinter(printerName);
			if (currentPrinter == null) {
				throw new InappropriateDeviceException("Printer:" + printerName + " not started");
			}
			
			currentPrinter.showCalibrationImage(pixels);
			return new MachineResponse("calibrationscreenshown", true, "Showed calibration screen on:" + printerName);
		} catch (InappropriateDeviceException e) {
			e.printStackTrace();
			return new MachineResponse("calibrationscreenshown", false, e.getMessage());
		}
	 }
	 
	 @GET
	 @Path("showblankscreen/{printername}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse showblankscreen(@PathParam("printername") String printerName) {
		try {
			Printer currentPrinter = PrinterManager.Instance().getPrinter(printerName);
			if (currentPrinter == null) {
				throw new InappropriateDeviceException("Printer:" + printerName + " not started");
			}
			
			currentPrinter.showBlankImage();
			return new MachineResponse("calibrationscreenshown", true, "Showed blank screen on:" + printerName);
		} catch (InappropriateDeviceException e) {
			e.printStackTrace();
			return new MachineResponse("calibrationscreenshown", false, e.getMessage());
		}
	 }

	 @GET
	 @Path("gcode/{printername}/{gcode}")
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
	 @Path("movex/{printername}/{distance}")
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
	 @Path("movey/{printername}/{distance}")
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
	 @Path("movez/{printername}/{distance}")
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
	 @Path("homez/{printername}")
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
	 @Path("homex/{printername}")
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
	 @Path("homey/{printername}")
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
	 @Path("motorsoff/{printername}")
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
	 @Path("motorson/{printername}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse motorsOn(@PathParam("printername") String printerName) {
			Printer printer = PrinterManager.Instance().getPrinter(printerName);
			if (printer == null) {
				return new MachineResponse("motorson", false, "Printer:" + printerName + " not started");
			}
			
			return new MachineResponse("motorson", true, printer.getGCodeControl().executeMotorsOn());
	 }

	 @GET
	 @Path("startjob/{jobname}/{printername}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse startJob(@PathParam("jobname") String jobname, @PathParam("printername") String printername) {
		// Create job
		File selectedFile = new File(HostProperties.Instance().getUploadDir(), jobname); //should already be done by marshalling: java.net.URLDecoder.decode(name, "UTF-8"));//name);
		
		// Delete and Create handled in jobManager
		PrintJob printJob = null;
		try {
			printJob = JobManager.Instance().createJob(selectedFile);
			Printer printer = PrinterManager.Instance().getPrinter(printername);
			if (printer == null) {
				throw new InappropriateDeviceException("Printer not started:" + printername);
			}
			
			Future<JobStatus> status = JobManager.Instance().startJob(printJob, printer);
			return new MachineResponse("start", true, "Started:" + printJob.getId());
		} catch (JobManagerException | AlreadyAssignedException e) {
			JobManager.Instance().removeJob(printJob);
			PrinterManager.Instance().removeAssignment(printJob);
			e.printStackTrace();
			return new MachineResponse("start", false, e.getMessage());
		} catch (InappropriateDeviceException e) {
			JobManager.Instance().removeJob(printJob);
			e.printStackTrace();
			return new MachineResponse("start", false, e.getMessage());
		}
 	}

	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 //The following methods are for Jobs
	 //======================================
	 @GET
     @Path("currentsliceImage/{jobname}")
     @Produces("image/png")
     /**
      * Another way to stream:
      * http://stackoverflow.com/questions/9204287/how-to-return-a-png-image-from-jersey-rest-service-method-to-the-browser
      * 
      * @param jobname
      * @return
      */
     public StreamingOutput getImage(@PathParam("jobname") String jobname) {
 		final PrintJob job = JobManager.Instance().getJob(jobname);
 		
	    return new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				BufferedImage image = null;
				if (job == null) {
		 			IOUtils.copy(getClass().getResourceAsStream("noimageavailable.png"), output);
		 			return;
		 		}

		 		image = job.getPrintFileProcessor().getCurrentImage(job);
		 		if (image == null) {
		 			IOUtils.copy(getClass().getResourceAsStream("noimageavailable.png"), output);
		 			return;
		 		}
		 		
				ImageIO.write(image, "png", output);
			}  
	    };
     }
	 
	 @GET
	 @Path("stopjob/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse stopJob(@PathParam("jobname") String jobname) {
		PrintJob job = JobManager.Instance().getJob(jobname);
		if (job == null) {
			return new MachineResponse("stop", false, "Job:" + jobname + " not active");
		}
		job.getPrinter().setStatus(JobStatus.Cancelled);
		//TODO: need to unassign the printer!!!!!!!!!!
	 	return new MachineResponse("stop", true, "Stopped:" + jobname);
	 }	 
	 
	 @GET
	 @Path("togglepause/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse togglePause(@PathParam("jobname") String jobname) {
		PrintJob job = JobManager.Instance().getJob(jobname);
		if (job == null) {
			return new MachineResponse("togglepause", false, "Job:" + jobname + " not active");
		}
		
		JobStatus status = job.getPrinter().togglePause();
		return new MachineResponse("togglepause", true, "Job:" + jobname + " " + status);
	 }
	 
	 @GET
	 @Path("jobstatus/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getJobStatus(@PathParam("jobname") String jobname) {
		PrintJob job = JobManager.Instance().getJob(jobname);
		if (job == null) {
			return new MachineResponse("status", false, "Job:" + jobname + " not active");
		}

		return new MachineResponse("status", true, "Job:" + jobname + " " + job.getPrinter().getStatus());
	 }	 
	 
	 @GET
	 @Path("totalslices/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getTotalSlices(@PathParam("jobname") String jobname) {
		PrintJob job = JobManager.Instance().getJob(jobname);
		if (job == null) {
			return new MachineResponse("totalslices", false, "Job:" + jobname + " not active");
		}

		return new MachineResponse("totalslices", true, String.valueOf(job.getTotalSlices()));
	 }

	 @GET
	 @Path("currentslice/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getCurrentSlice(@PathParam("jobname") String jobname) {
		 PrintJob job = JobManager.Instance().getJob(jobname);
		 if(job == null) {
			 return new MachineResponse("currentslice", false, "Job:" + jobname + " not active");
		 }
		 
		 return new MachineResponse("currentslice", true, String.valueOf(job.getCurrentSlice()));
	 }

	 @GET
	 @Path("currentslicetime/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getCurrentSliceTime(@PathParam("jobname") String jobname) {
		 PrintJob job = JobManager.Instance().getJob(jobname);
		 if(job == null) {
			 return new MachineResponse("slicetime", false, "Job:" + jobname + " not active");
		 }
		 
		 return new MachineResponse("slicetime", true, String.valueOf(job.getCurrentSliceTime()));
	 }
	 
	 @GET
	 @Path("averageslicetime/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getAverageslicetime(@PathParam("jobname") String jobname) {
		 PrintJob job = JobManager.Instance().getJob(jobname);
		 if(job == null) {
			 return new MachineResponse("slicetime", false, "Job:" + jobname + " not active");
		 }
		 
		 return new MachineResponse("slicetime", true, String.valueOf(job.getAverageSliceTime()));
	 }
	 
	 @GET
	 @Path("zliftdistance/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getLiftDistance(@PathParam("jobname") String jobName) {
			PrintJob printJob = JobManager.Instance().getJob(jobName);
			if (printJob == null) {
				return new MachineResponse("zliftdistance", false, "Job:" + jobName + " not started");
			}
			
			return new MachineResponse("zliftdistance", true, String.format("%1.3f", printJob.getZLiftDistance()));
	 }	 
	 
	 @GET
	 @Path("zliftdistance/{jobname}/{distance}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse setLiftDistance(@PathParam("jobname") String jobName, @PathParam("distance") double liftDistance) {
			PrintJob printJob = JobManager.Instance().getJob(jobName);
			if (printJob == null) {
				return new MachineResponse("LiftDistance", false, "Job:" + jobName + " not started");
			}
			
			try {
				printJob.overrideZLiftDistance(liftDistance);
			} catch (InappropriateDeviceException e) {
				e.printStackTrace();
				return new MachineResponse("LiftDistance", false, e.getMessage());
			}
			return new MachineResponse("LiftDistance", true, "Set lift distance to:" + liftDistance);
	 }
	 
	 @GET
	 @Path("zliftspeed/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getLiftSpeed(@PathParam("jobname") String jobName) {
			PrintJob printJob = JobManager.Instance().getJob(jobName);
			if (printJob == null) {
				return new MachineResponse("zliftspeed", false, "Job:" + jobName + " not started");
			}
			
			return new MachineResponse("zliftspeed", true, String.format("%1.3f", printJob.getZLiftSpeed()));
	 }
	 
	 @GET
	 @Path("zliftspeed/{jobname}/{speed}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse setLiftSpeed(@PathParam("jobname") String jobName, @PathParam("speed") double speed) {
			PrintJob printJob = JobManager.Instance().getJob(jobName);
			if (printJob == null) {
				return new MachineResponse("zliftspeed", false, "Job:" + jobName + " not started");
			}
			
			try {
				printJob.overrideZLiftSpeed(speed);
			} catch (InappropriateDeviceException e) {
				e.printStackTrace();
				return new MachineResponse("zliftspeed", false, e.getMessage());
			}
			return new MachineResponse("zliftspeed", true, "Set lift speed to:" + speed);
	 }
	 	
	 @GET
	 @Path("exposuretime/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getExposureTime(@PathParam("jobname") String jobName) {
			PrintJob printJob = JobManager.Instance().getJob(jobName);
			if (printJob == null) {
				return new MachineResponse("exposureTime", false, "Job:" + jobName + " not started");
			}
			
			return new MachineResponse("exposureTime", true, printJob.getExposureTime() + "");
	 }
	 
	 @GET
	 @Path("exposuretime/{jobname}/{exposureTime}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse setExposureTime(@PathParam("jobname") String jobName, @PathParam("exposureTime") int exposureTime) {
			PrintJob printJob = JobManager.Instance().getJob(jobName);
			if (printJob == null) {
				return new MachineResponse("exposureTime", false, "Job:" + jobName + " not started");
			}
			
			printJob.overrideExposureTime(exposureTime);
			return new MachineResponse("exposureTime", true, "Exposure time set");
	 }	 
}