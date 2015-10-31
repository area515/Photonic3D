package org.area515.resinprinter.services;

import java.awt.GraphicsDevice;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.PrintJobManager;
import org.area515.resinprinter.network.NetInterface;
import org.area515.resinprinter.network.NetInterface.WirelessNetwork;
import org.area515.resinprinter.network.NetworkManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.serial.ConsoleCommPort;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.server.CwhEmailSettings;
import org.area515.resinprinter.server.HostInformation;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;
import org.area515.util.IOUtilities;
import org.area515.util.MailUtilities;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("machine")
public class MachineService {
	public static MachineService INSTANCE = new MachineService();
	
	private Future<Boolean> restartProcess;
	
	private MachineService() {}
	
	private String getRemoteIpAddress(HttpServletRequest request) {
		String forwardHeader = HostProperties.Instance().getForwardHeader();
		if (forwardHeader != null) {
			String ipAddress = request.getHeader(forwardHeader);
			if (ipAddress == null || ipAddress.trim().equals("") || ipAddress.equalsIgnoreCase("unknown")) {
				return null;
			}
			return ipAddress;
		}

		return request.getRemoteAddr();
	}
	
	@POST
	@Path("cancelNetworkRestartProcess")
	@Produces(MediaType.APPLICATION_JSON)
	public void cancelRestartOperation() {
		if (restartProcess != null) {
			if (!restartProcess.cancel(true)) {
				throw new IllegalArgumentException("Couldn't cancel the network reconfiguration process.");
			}
			
			try {
				restartProcess.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Problem occurred while waiting for the network reconfiguraiton process to terminate.");
			}
		}
	}
	
	/**
	 * Here is how the network reconfiguration process works.
	 * 1. The Restful client should first reconfigure the WIFI AP of their choice with the connectToWifiSSID() method.
	 * 2. Once that step is successful, this method should be called immediately afterwards.
	 * 3. If this method returns false, the NetworkInterface was not found and the configuration process ENDS HERE.
	 * 4. If this method is able to return true, it means the proper NetworkInterface managing this socket connection was found.
	 * 5. The NetworkInterface will be monitored for a disruption in network connectivity and Websocket ping events will start being 
	 * 	produced from this host at the interval of "millisecondsBetweenPings".
	 * 7. Once the Restful client receives it's first ping event, it should ask the user to unplug the ethernet cable.
	 * 8. The user should then either cancel the operation, or unplug the ethernet cable.
	 * 9. If the user cancels the operation, the Restful client needs to call cancelRestartOperation() to prevent an unnecessary outage.
	 * 10. If the user unplugs the ethernet cable, the proper NetworkInterface(discovered in step 3) is found to be down and this 
	 * 	Host(and it's network) are restarted.
	 * 11. Since this Host is now in the middle of restarting, it isn't able to send WebSocket ping events any longer.
	 * 12. The Restful client then discovers that ping events are no longer coming and it's timeout period hasn't been exhausted, 
	 * 	so it let's the user know that they should shut down the Restful client(probably a browser) and restart the printer with the Multicast client.
	 * 13. The Multicast client eventually finds the Raspberry Pi on the new Wifi IP address and we are back in business...
	 * 
	 * @param request
	 * @param timeoutMilliseconds
	 * @param millisecondsBetweenPings
	 * @return
	 */
	@POST
	@Path("startNetworkRestartProcess")
	@Produces(MediaType.APPLICATION_JSON)
	public boolean restartHostAfterNetworkCableUnplugged(
			@Context HttpServletRequest request, 
			@PathParam("timeoutMilliseconds") final long timeoutMilliseconds,
			@PathParam("millisecondsBetweenPings") final long millisecondsBetweenPings ) {
		
		String ipAddress = request.getLocalAddr();
		try {
			final NetworkInterface iFace = NetworkInterface.getByInetAddress(InetAddress.getByName(ipAddress));
			final long startTime = System.currentTimeMillis();
			restartProcess = Main.GLOBAL_EXECUTOR.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					while (iFace.isUp() || (timeoutMilliseconds > 0 && System.currentTimeMillis() - startTime < timeoutMilliseconds)) {
						try {
							Thread.sleep(millisecondsBetweenPings);
						} catch (InterruptedException e) {
							return false;
						}
					}
		
					if (!iFace.isUp()) {
						//After executing this method, don't expect this VM to stick around much longer
						IOUtilities.executeNativeCommand(HostProperties.Instance().getRebootCommand(), null, null);
					}
					
					return true;
				}
			});
			
			return true;
		} catch (SocketException | UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@GET
	@Path("executeDiagnostic")
	@Produces(MediaType.APPLICATION_JSON)
	public void emailSupportLogs() {
		File zippedFile = new File("LogBundle.zip");
		String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		IOUtilities.executeNativeCommand(HostProperties.Instance().getDumpStackTraceCommand(), null, pid);
		
		ZipOutputStream zipOutputStream = null;
		try {
			zipOutputStream = new ZipOutputStream(new FileOutputStream(zippedFile));
			String logFiles[] = new String[]{"log.scrout", "log.screrr", "log.out", "log.err"};
			for (String logFile : logFiles) {
				File file = new File(logFile);
				if (file.exists()) {
					IOUtilities.zipFile(file, zipOutputStream);
				}
			}
			
			HostProperties.Instance().exportDiagnostic(zipOutputStream);
			
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			System.getProperties().store(byteStream, "Stored on " + new Date());
			IOUtilities.zipStream("System.properties", new ByteArrayInputStream(byteStream.toByteArray()), zipOutputStream);
			
			zipOutputStream.finish();
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failure creating log bundle.", e);
		} finally {
			IOUtils.closeQuietly(zipOutputStream);
		}
		
		Transport transport = null;
		try {
			CwhEmailSettings settings = HostProperties.Instance().loadEmailSettings();
			HostInformation info = HostProperties.Instance().loadHostInformation();
			transport = MailUtilities.openTransportFromSettings(settings);
			MailUtilities.executeSMTPSend(
					info.getDeviceName().replace(" ", "") + "@My3DPrinter",
					settings.getServiceEmailAddresses(),
					"Service Request",
					"Attached diagnostic information",
					transport,
					zippedFile);
		} catch (MessagingException | IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failure emailing log bundle.");
		} finally {
			zippedFile.delete();
			if (transport != null) {
				try {transport.close();} catch (MessagingException e) {}
			}
		}
	}
	
	@POST
	@Path("/stageOfflineInstall")
	@Consumes("multipart/form-data")
	public Response stageOfflineInstall(MultipartFormDataInput input) {
		return FileService.uploadFile(input, HostProperties.Instance().getUpgradeDir());
	}

	 @Deprecated
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
	 
	 @Deprecated
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
	 @Path("networkInterfaces/list")
	 @Produces(MediaType.APPLICATION_JSON)
	 public List<NetInterface> getNetworkInterfaces() {
		Class<NetworkManager> managerClass = HostProperties.Instance().getNetworkManagerClass();
		try {
			NetworkManager networkManager = managerClass.newInstance();
			return networkManager.getNetworkInterfaces();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	 }
	 
	 @PUT
	 @Path("wirelessConnect/{password}")
	 @Consumes(MediaType.APPLICATION_JSON)
	 public void connectToWifiSSID(WirelessNetwork network, @PathParam("password") String password) {
		Class<NetworkManager> managerClass = HostProperties.Instance().getNetworkManagerClass();
		try {
			NetworkManager networkManager = managerClass.newInstance();
			networkManager.connectToWirelessNetwork(network, password);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	 }
	 
	 @GET
	 @Path("serialPorts/list")
	 @Produces(MediaType.APPLICATION_JSON)
	 public List<String> getSerialPorts() {
		 return getPorts();
	 }
	 
	 @Deprecated
	 @GET
	 @Path("displays")
	 @Produces(MediaType.APPLICATION_JSON)
	 public List<String> getDisplaysOld() {
		 List<GraphicsDevice> devices = DisplayManager.Instance().getDisplayDevices();
		 List<String> deviceStrings = new ArrayList<String>();
		 for (GraphicsDevice current : devices) {
			 deviceStrings.add(current.getIDstring());
		 }
		 
		 return deviceStrings;
	 }
	 
	 @GET
	 @Path("graphicsDisplays/list")
	 @Produces(MediaType.APPLICATION_JSON)
	 public List<String> getDisplays() {
		 return getDisplaysOld();
	 }
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 

	 
	 
	 
	 
	 
	 
	 
	 
	 
	 //The following methods are for Printers
	 //======================================
	 @Deprecated
	 @GET
	 @Path("createprinter/{printername}/{display}/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse createPrinter(@PathParam("printername") String printername, @PathParam("display") String displayId, @PathParam("comport") String comport) {
		//TODO: This data needs to be set by the user interface...
		//========================================================
		PrinterConfiguration currentConfiguration = PrinterService.INSTANCE.createTemplatePrinter(printername, displayId, comport, 134, 75, 185);
		if (displayId.equals(DisplayManager.SIMULATED_DISPLAY) &&
			comport.equals(ConsoleCommPort.CONSOLE_COMM_PORT)) {
			currentConfiguration.getSlicingProfile().setgCodeLift("Lift Z; Lift the platform");
			currentConfiguration.getSlicingProfile().getSelectedInkConfig().setNumberOfFirstLayers(3);
			currentConfiguration.getSlicingProfile().getSelectedInkConfig().setFirstLayerExposureTime(10000);
			currentConfiguration.getSlicingProfile().getSelectedInkConfig().setExposureTime(3000);
		}
		//=========================================================
		try {
			HostProperties.Instance().addOrUpdatePrinterConfiguration(currentConfiguration);
			return new MachineResponse("create", true, "Created:" + currentConfiguration.getName() + "");
		} catch (AlreadyAssignedException e) {
			e.printStackTrace();
			return new MachineResponse("create", false, e.getMessage());
		}
	 }
	 
	 @Deprecated
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

	 @Deprecated
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
	 
	 @Deprecated
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
			PrinterManager.Instance().stopPrinter(printer);
			DisplayManager.Instance().removeAssignment(printer);
			SerialManager.Instance().removeAssignments(printer);
			return new MachineResponse("stop", true, "Stopped:" + printerName);
		} catch (InappropriateDeviceException e) {
			e.printStackTrace();
			return new MachineResponse("stop", false, e.getMessage());
		}
	 }

	 @Deprecated
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
	 
	 @Deprecated
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
	 
	 @Deprecated
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

	 @Deprecated
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
	 @Deprecated
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
	 @Deprecated
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
	 @Deprecated
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
	 
	 @Deprecated
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
	 
	 @Deprecated
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

	 @Deprecated
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
	 @Deprecated
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
	 @Deprecated
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

	 @Deprecated
	 @GET
	 @Path("startjob/{jobname}/{printername}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse startJob(@PathParam("jobname") String jobname, @PathParam("printername") String printername) {
		// Create job
		File selectedFile = new File(HostProperties.Instance().getUploadDir(), jobname); //should already be done by marshalling: java.net.URLDecoder.decode(name, "UTF-8"));//name);
		
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

	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 //The following methods are for Jobs
	 //======================================
	 @Deprecated
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
 		final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobname);
 		final PrintJob job = jobs.size() > 0?jobs.get(0):null;
 		
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
		 		
		 		try {
		 			ImageIO.write(image, "png", output);
		 		} catch (IOException e) {
		 			//TODO: For some reason we are getting an org.eclipse.jetty.io.EofException
		 			e.printStackTrace();
		 		}
			}  
	    };
     }
	 
	 @Deprecated
	 @GET
	 @Path("stopjob/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse stopJob(@PathParam("jobname") String jobname) {
 		final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobname);
 		final PrintJob job = jobs.size() > 0?jobs.get(0):null;
	 		
		if (job == null) {
			return new MachineResponse("stop", false, "Job:" + jobname + " not active");
		}
		
		job.getPrinter().setStatus(JobStatus.Cancelled);
	 	return new MachineResponse("stop", true, "Stopped:" + jobname);
	 }	 
	 
	 @Deprecated
	 @GET
	 @Path("togglepause/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse togglePause(@PathParam("jobname") String jobname) {
	 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobname);
	 	final PrintJob job = jobs.size() > 0?jobs.get(0):null;
		 		
		if (job == null) {
			return new MachineResponse("togglepause", false, "Job:" + jobname + " not active");
		}
		
		JobStatus status = job.getPrinter().togglePause();
		return new MachineResponse("togglepause", true, "Job:" + jobname + " " + status);
	 }
	 
	 @Deprecated
	 @GET
	 @Path("jobstatus/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getJobStatus(@PathParam("jobname") String jobname) {
	 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobname);
	 	final PrintJob job = jobs.size() > 0?jobs.get(0):null;
			 		
		if (job == null) {
			return new MachineResponse("status", false, "Job:" + jobname + " not active");
		}

		return new MachineResponse("status", true, "Job:" + jobname + " " + job.getPrinter().getStatus());
	 }	 
	 
	 @Deprecated
	 @GET
	 @Path("totalslices/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getTotalSlices(@PathParam("jobname") String jobname) {
	 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobname);
	 	final PrintJob job = jobs.size() > 0?jobs.get(0):null;
			 		
		if (job == null) {
			return new MachineResponse("totalslices", false, "Job:" + jobname + " not active");
		}

		return new MachineResponse("totalslices", true, String.valueOf(job.getTotalSlices()));
	 }

	 @Deprecated
	 @GET
	 @Path("currentslice/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getCurrentSlice(@PathParam("jobname") String jobname) {
	 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobname);
	 	final PrintJob job = jobs.size() > 0?jobs.get(0):null;
			 		
		 if(job == null) {
			 return new MachineResponse("currentslice", false, "Job:" + jobname + " not active");
		 }
		 
		 return new MachineResponse("currentslice", true, String.valueOf(job.getCurrentSlice()));
	 }

	 @Deprecated
	 @GET
	 @Path("currentslicetime/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getCurrentSliceTime(@PathParam("jobname") String jobname) {
	 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobname);
	 	final PrintJob job = jobs.size() > 0?jobs.get(0):null;
			 		
		 if(job == null) {
			 return new MachineResponse("slicetime", false, "Job:" + jobname + " not active");
		 }
		 
		 return new MachineResponse("slicetime", true, String.valueOf(job.getCurrentSliceTime()));
	 }
	 
	 @Deprecated
	 @GET
	 @Path("averageslicetime/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getAverageslicetime(@PathParam("jobname") String jobname) {
	 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobname);
	 	final PrintJob job = jobs.size() > 0?jobs.get(0):null;
			 		
		 if(job == null) {
			 return new MachineResponse("slicetime", false, "Job:" + jobname + " not active");
		 }
		 
		 return new MachineResponse("slicetime", true, String.valueOf(job.getAverageSliceTime()));
	 }
	 
	 @Deprecated
	 @GET
	 @Path("zliftdistance/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getLiftDistance(@PathParam("jobname") String jobName) {
		 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobName);
		 	final PrintJob printJob = jobs.size() > 0?jobs.get(0):null;
			 		
			if (printJob == null) {
				return new MachineResponse("zliftdistance", false, "Job:" + jobName + " not started");
			}
			
			return new MachineResponse("zliftdistance", true, String.format("%1.3f", printJob.getZLiftDistance()));
	 }	 
	 
	 @Deprecated
	 @GET
	 @Path("zliftdistance/{jobname}/{distance}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse setLiftDistance(@PathParam("jobname") String jobName, @PathParam("distance") double liftDistance) {
		 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobName);
		 	final PrintJob printJob = jobs.size() > 0?jobs.get(0):null;
			 		
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
	 
	 @Deprecated
	 @GET
	 @Path("zliftspeed/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getLiftSpeed(@PathParam("jobname") String jobName) {
		 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobName);
		 	final PrintJob printJob = jobs.size() > 0?jobs.get(0):null;
			 		
			if (printJob == null) {
				return new MachineResponse("zliftspeed", false, "Job:" + jobName + " not started");
			}
			
			return new MachineResponse("zliftspeed", true, String.format("%1.3f", printJob.getZLiftSpeed()));
	 }
	 
	 @Deprecated
	 @GET
	 @Path("zliftspeed/{jobname}/{speed}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse setLiftSpeed(@PathParam("jobname") String jobName, @PathParam("speed") double speed) {
		 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobName);
		 	final PrintJob printJob = jobs.size() > 0?jobs.get(0):null;
			 		
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
	 	
	 @Deprecated
	 @GET
	 @Path("exposuretime/{jobname}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getExposureTime(@PathParam("jobname") String jobName) {
		 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobName);
		 	final PrintJob printJob = jobs.size() > 0?jobs.get(0):null;
			 		
			if (printJob == null) {
				return new MachineResponse("exposureTime", false, "Job:" + jobName + " not started");
			}
			
			return new MachineResponse("exposureTime", true, printJob.getExposureTime() + "");
	 }
	 
	 @Deprecated
	 @GET
	 @Path("exposuretime/{jobname}/{exposureTime}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse setExposureTime(@PathParam("jobname") String jobName, @PathParam("exposureTime") int exposureTime) {
		 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobName);
		 	final PrintJob printJob = jobs.size() > 0?jobs.get(0):null;
			 		
			if (printJob == null) {
				return new MachineResponse("exposureTime", false, "Job:" + jobName + " not started");
			}
			
			printJob.overrideExposureTime(exposureTime);
			return new MachineResponse("exposureTime", true, "Exposure time set");
	 }
	 
	 @Deprecated
	 @GET
	 @Path("geometry/{jobName}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getGeometry(@PathParam("jobName") String jobName) {
		 	final List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(jobName);
		 	final PrintJob printJob = jobs.size() > 0?jobs.get(0):null;
			 		
			if (printJob == null) {
				return new MachineResponse("geometry", false, "Job:" + jobName + " must be started or a simulation must be in progress to get geometry.");
			}

			try {
				Object data = printJob.getPrintFileProcessor().getGeometry(printJob);
				ObjectMapper mapper = new ObjectMapper(new JsonFactory());
				String json = mapper.writeValueAsString(data);
				return new MachineResponse("geometry", true, json);
			} catch (JobManagerException e) {
				e.printStackTrace();
				return new MachineResponse("geometry", false, e.getMessage());
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return new MachineResponse("geometry", false, "Couldn't convert geometry to JSON");
			}
	 }
}