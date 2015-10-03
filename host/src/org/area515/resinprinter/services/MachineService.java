package org.area515.resinprinter.services;

import java.awt.GraphicsDevice;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;
import javax.mail.Transport;
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
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.PrintJobManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.serial.ConsoleCommPort;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.services.NetInterface.WirelessNetwork;
import org.area515.util.MailUtilities;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;

@Path("machine")
public class MachineService {
	public static MachineService INSTANCE = new MachineService();
	
	private MachineService() {}
	
	private static ZipEntry zipFile(File fileToZip, ZipOutputStream output) {
		ZipEntry entry = new ZipEntry(fileToZip.getName());
		InputStream inStream = null;
		try {
			inStream = new BufferedInputStream(new FileInputStream(fileToZip));
			ByteStreams.copy(inStream, output);
			inStream.close();
			return entry;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			IOUtils.closeQuietly(inStream);
		}
	}
	
	private static ZipEntry zipStream(String name, InputStream inStream, ZipOutputStream output) {
		ZipEntry entry = new ZipEntry(name);
		try {
			output.putNextEntry(entry);
			ByteStreams.copy(inStream, output);
			inStream.close();
			return entry;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			IOUtils.closeQuietly(inStream);
		}
	}
	
	@GET
	@Path("executeDiagnostic")
	@Produces(MediaType.APPLICATION_JSON)
	public void emailSupportLogs() {
		String MASK = "Masked by CWH";
		MessageFormat dumpStack = new MessageFormat(HostProperties.Instance().getDumpStackTraceCommand());

		String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		getLinesOfText(dumpStack, null, pid);
		
		ZipOutputStream zipOutputStream = null;
		try {
			zipOutputStream = new ZipOutputStream(new FileOutputStream("LogBundle.zip"));
			String logFiles[] = new String[]{"log.scrout", "log.screrr", "log.out", "log.err"};
			for (String logFile : logFiles) {
				File file = new File(logFile);
				if (file.exists()) {
					ZipEntry entry = zipFile(file, zipOutputStream);
					zipOutputStream.putNextEntry(entry);
				}
			}
			
			Properties properties = new Properties();
			properties.putAll(HostProperties.Instance().getConfigurationProperties());
			properties.put("CWH3DPrinterRealm.clientPassword", MASK);
			properties.put("keypairPassword", MASK);
			properties.put("keystorePassword", MASK);
			properties.put("password", MASK);
			
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			properties.store(byteStream, "Stored on " + new Date());
			zipStream("config.properties", new ByteArrayInputStream(byteStream.toByteArray()), zipOutputStream);
			
			byteStream = new ByteArrayOutputStream();
			System.getProperties().store(byteStream, "Stored on " + new Date());
			zipStream("System.properties", new ByteArrayInputStream(byteStream.toByteArray()), zipOutputStream);
			
			zipOutputStream.finish();
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failure creating log bundle.", e);
		} finally {
			IOUtils.closeQuietly(zipOutputStream);
		}
		
		Transport transport = null;
		try {
			MailUtilities.setMailProperties(HostProperties.Instance().getConfigurationProperties());
			String emailAddress = (String)HostProperties.Instance().getConfigurationProperties().get("serviceEmailAddresses");
			String[] serviceEmailAddresses = emailAddress.split("[;,]");
			transport = MailUtilities.openTransportFromProperties();
			MailUtilities.executeSMTPSend (
					HostProperties.Instance().getDeviceName().replace(" ", "") + "@My3DPrinter", 
					Arrays.asList(serviceEmailAddresses),
					"Service Request", 
					"Attached diagnostic information", 
					transport,
					(File[])null);
		} catch (MessagingException | IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failure emailing log bundle.");
		} finally {
			if (transport != null) {
				try {transport.close();} catch (MessagingException e) {}
			}
		}
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
	 
	private String[] getLinesOfText(MessageFormat command, String friendlyErrorMessage, String... arguments) throws RuntimeException {
		Process listSSIDProcess;
		try {
			listSSIDProcess = Runtime.getRuntime().exec(command.format(arguments));
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			IOUtils.copy(listSSIDProcess.getInputStream(), output);
			return new String(output.toString()).split("\r?\n");
		} catch (IOException e) {
			if (friendlyErrorMessage == null) {
				e.printStackTrace();
				return new String[]{};
			}
			
			throw new RuntimeException(friendlyErrorMessage, e);
		}
	}
	
	 @GET
	 @Path("networkInterfaces/list")
	 @Produces(MediaType.APPLICATION_JSON)
	 public List<NetInterface> getNetworkInterfaces() {
		MessageFormat discoverCommand = new MessageFormat(HostProperties.Instance().getDiscoverSSIDCommand());
		List<NetInterface> ifaces = new ArrayList<NetInterface>();
		
		//If the format doesn't understand interfaces then we can skip some actions
		if (discoverCommand.getFormatsByArgumentIndex().length == 0) {
			String[] ssids = getLinesOfText(discoverCommand, null);
			NetInterface netFace = new NetInterface();
			netFace.setName("WiFi Profiles");
			for (String ssid : ssids) {
				if (ssid == null || ssid.trim().equals("")) {
					continue;
				}
				WirelessNetwork wNet = new WirelessNetwork();
				wNet.setSsid(ssid);
				netFace.getWirelessNetworks().add(wNet);
			}
			
			return Collections.singletonList(netFace);
		}
		
		try {
			Enumeration<NetworkInterface> networkEnum = NetworkInterface.getNetworkInterfaces();
			while (networkEnum.hasMoreElements()) {
				NetworkInterface iface = networkEnum.nextElement();
				NetInterface netFace = new NetInterface();
				netFace.setName(iface.getName());
				String[] ssids = getLinesOfText(discoverCommand, null, iface.getName());
				for (String ssid : ssids) {
					if (ssid == null || ssid.trim().equals("")) {
						continue;
					}
					WirelessNetwork wNet = new WirelessNetwork();
					wNet.setSsid(ssid);
					netFace.getWirelessNetworks().add(wNet);
				}
				ifaces.add(netFace);
			}
			
			return ifaces;
		} catch (SocketException e) {
			e.printStackTrace();
			throw new RuntimeException("An error occurred looking for network interfaces.", e);
		}
	 }
	 
	 @GET
	 @Path("networkInterfaces/get/{networkInterfaceName}/wireless/{ssid}/connect/{password}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public void connectToWifiSSID(@PathParam("networkInterfaceName") String networkInterfaceName, @PathParam("ssid") String ssid, @PathParam("password") String password) {
		    //http://unix.stackexchange.com/questions/92799/connecting-to-wifi-network-through-command-line
			MessageFormat connectCommand = new MessageFormat(HostProperties.Instance().getConnectToWifiSSIDCommand());
			String[] data = getLinesOfText(connectCommand, "An error occurred attempting to connect to wireless network.", networkInterfaceName, ssid, password);
			System.out.println(Arrays.toString(data));
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
			DisplayManager.Instance().removeAssignment(printer);
			SerialManager.Instance().removeAssignments(printer);
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