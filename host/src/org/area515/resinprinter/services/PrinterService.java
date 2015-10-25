
package org.area515.resinprinter.services;

import java.awt.GraphicsDevice;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.PrintJobManager;
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
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.server.HostProperties;

@Path("printers")
public class PrinterService {
	
	public static PrinterService INSTANCE = new PrinterService();
	
	private PrinterService(){}
	
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
				e.printStackTrace();
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
	 @Path("getdummyconfiguration")
	 @Produces(MediaType.APPLICATION_JSON)
	 public PrinterConfiguration getDummyConfiguration(){
		 return PrinterService.INSTANCE.createTemplatePrinter("dummy", "none", "none", 134, 75, 185);
	 }
	 
	 @GET
	 @Path("getprinterconfiguration/{printername}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public PrinterConfiguration getPrinterConfiguration(@PathParam("printername") String printerName) throws InappropriateDeviceException {
		
		System.out.println("getting configuration for " + printerName); 
		Printer printer = PrinterManager.Instance().getPrinter(printerName);
		if (printer == null) {
			PrinterConfiguration currentConfiguration = HostProperties.Instance().getPrinterConfiguration(printerName);
			if (currentConfiguration == null) {
				throw new InappropriateDeviceException("No printer with that name:" + printerName);
			}
			
			printer = new Printer(currentConfiguration);
		}
		 
		return printer.getConfiguration();
	 }
	 
	 @GET
	 @POST
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
				e.printStackTrace();
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
			String printerName = printerToSave.getClass().getName();
			Printer currentPrinter = PrinterManager.Instance().getPrinter(printerName);
			if (currentPrinter != null) {
				throw new InappropriateDeviceException("Can't save printer after it's been started:" + printerName);
			}

			try {
				HostProperties.Instance().addOrUpdatePrinterConfiguration(printerToSave.getConfiguration());
				return new MachineResponse("savePrinter", true, "Created:" + printerToSave.getName() + "");
			} catch (AlreadyAssignedException e) {
				e.printStackTrace();
				return new MachineResponse("savePrinter", false, e.getMessage());
			}
		} catch (InappropriateDeviceException e) {
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
			return new MachineResponse("stopPrinter", false, e.getMessage());
		}
	 }
	 
	 @POST
	 @Path("createTemplatePrinter")
	 @Produces(MediaType.APPLICATION_JSON)
	 public Printer createTemplatePrinter() {
		 //TODO: Return a nice unused name for this printer instead of the hardcoded value below
		 PrinterConfiguration configuration = createTemplatePrinter(
				 "CWH Template Printer", //"mUVe 1 DLP (Testing)", 
				 DisplayManager.SIMULATED_DISPLAY, 
				 ConsoleCommPort.CONSOLE_COMM_PORT, 
				 134, 75, 185);
		 configuration.getSlicingProfile().getSelectedInkConfig().setNumberOfFirstLayers(10);
		 configuration.getSlicingProfile().getSelectedInkConfig().setFirstLayerExposureTime(20000);
		 configuration.getSlicingProfile().getSelectedInkConfig().setExposureTime(8000);
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
	 			"		colors[t] = new Packages.java.awt.Color(0, 0, 0, (java.lang.Integer)(Math.atan(fractions[t] * atanDivergencePoint) * colorRange + start));\n" +
				"	}\n" + 
				"	//return new Packages.java.awt.Color[]{new Packages.java.awt.Color(0, 0, 0, (java.lang.Integer)(opacityLevelModel.getValue()/(float)opacityLevelModel.getMaximum())), new Packages.java.awt.Color(0, 0, 0, 0)};\n" +
				"	return colors;\n" + 
	 			"}\n" +
	 			"var bulbCenter = new Packages.java.awt.geom.Point2D.Double($buildPlatformXPixels / 2, $buildPlatformYPixels / 2);\n" +
	 			"var bulbFocus = new Packages.java.awt.geom.Point2D.Double($buildPlatformXPixels / 2, $buildPlatformYPixels / 2);\n" +
	 			"var totalSizeOfGradient = $buildPlatformXPixels > $buildPlatformYPixels?$buildPlatformXPixels:$buildPlatformYPixels;\n" +
	 			"var fractions = getFractions(totalSizeOfGradient, 0, 1);\n" +
	 			"var colors = getColors(fractions, 0.2, 0);//Let's start with 20% opaque in the center of the projector bulb\n" +
	 			"new Packages.java.awt.RadialGradientPaint(\n" +
	 			"	bulbCenter,\n" + 
	 			"	totalSizeOfGradient,\n" +
				"	bulbFocus,\n" +
				"	fractions,\n" + 
				"	colors,\n" +
				"	java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE)");
		try {
			return new Printer(configuration);
		} catch (InappropriateDeviceException e) {
			//TODO: Throw an error if this fails!!!
			e.printStackTrace();
			return null;
		}
	 }
	 
	 PrinterConfiguration createTemplatePrinter(String printername, String displayId, String comport, double physicalProjectionMMX, double physicalProjectionMMY, double buildHeightMMZ) {
			PrinterConfiguration currentConfiguration = new PrinterConfiguration(printername, printername, false);
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
			ink.setNumberOfFirstLayers(3);
			ink.setResinPriceL(65.0);
			ink.setSliceHeight(0.1);
			ink.setFirstLayerExposureTime(5000);
			ink.setExposureTime(1000);
			
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

	 @GET
	 @Path("showCalibrationScreen/{printername}/{pixels}")
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
	 @Path("showBlankScreen/{printername}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse showBlankScreen(@PathParam("printername") String printerName) {
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
				e.printStackTrace();
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
				e.printStackTrace();
				return new MachineResponse("stopProjector", false, e.getMessage());
			}
	 }
	 
	 @GET
	 @Path("startJob/{fileName}/{printername}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse startJob(@PathParam("fileName") String fileName, @PathParam("printername") String printername) {
		// Create job
		File selectedFile = new File(HostProperties.Instance().getUploadDir(), fileName); //should already be done by marshalling: java.net.URLDecoder.decode(name, "UTF-8"));//name);
		
		// Delete and Create handled in jobManager
		PrintJob printJob = null;
		try {
			printJob = PrintJobManager.Instance().createJob(selectedFile);
			Printer printer = PrinterManager.Instance().getPrinter(printername);
			if (printer == null) {
				throw new InappropriateDeviceException("Printer not started:" + printername);
			}
			
			Future<JobStatus> status = PrintJobManager.Instance().startJob(printJob, printer);
			return new MachineResponse("start", true, "Started:" + printJob.getId());
		} catch (JobManagerException | AlreadyAssignedException e) {
			PrintJobManager.Instance().removeJob(printJob);
			PrinterManager.Instance().removeAssignment(printJob);
			e.printStackTrace();
			return new MachineResponse("start", false, e.getMessage());
		} catch (InappropriateDeviceException e) {
			PrintJobManager.Instance().removeJob(printJob);
			e.printStackTrace();
			return new MachineResponse("start", false, e.getMessage());
		}
 	}

	 /*@GET    //Not ready for this yet...
	 @Path("remainingResin/{printername}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getRemainingResin(@PathParam("printername") String printername) {
		// Create job
		File selectedFile = new File(HostProperties.Instance().getUploadDir(), jobId); //should already be done by marshalling: java.net.URLDecoder.decode(name, "UTF-8"));//name);
		
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
 	}*/

	 
	 
	 
	 
	 
	 
	 //This creates a template printer and saves it.
	 /*public static void main(String[] args) {
		 PrinterConfiguration configuration = new PrinterService().createTemplatePrinter().getConfiguration();
		 try {
				HostProperties.Instance().addOrUpdatePrinterConfiguration(configuration);
			} catch (AlreadyAssignedException e) {
				e.printStackTrace();
			}

	 }*/
}