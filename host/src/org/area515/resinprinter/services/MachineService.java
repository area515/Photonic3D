package org.area515.resinprinter.services;

//http://www.javatutorials.co.in/jax-rs-2-jersey-file-upload-example/

import gnu.io.CommPortIdentifier;

import java.awt.GraphicsDevice;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.JobManager;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.printer.PrinterConfiguration.ComPortSettings;
import org.area515.resinprinter.printer.PrinterConfiguration.MonitorDriverConfig;
import org.area515.resinprinter.printer.PrinterConfiguration.MotorsDriverConfig;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.server.HostProperties;

@Path("machine")
public class MachineService {
	 @GET
	 @Path("printers")
	 @Produces(MediaType.APPLICATION_JSON)
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
		 List<CommPortIdentifier> identifiers = SerialManager.Instance().getSerialDevices();
		 List<String> identifierStrings = new ArrayList<String>();
		 for (CommPortIdentifier current : identifiers) {
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
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  * 
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException
	  */
	 @GET
	 @Path("createprinter/{printername}/{display}/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse createPrinter(@PathParam("printername") String jobid, @PathParam("display") String displayId, @PathParam("comport") String comport) {
		//TODO: This data needs to be set by the user interface...
		//========================================================
		PrinterConfiguration currentConfiguration = new PrinterConfiguration();
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
		currentConfiguration.setMotorsDriverConfig(motors);
		currentConfiguration.setMonitorDriverConfig(monitor);
		currentConfiguration.setOSMonitorID(displayId);
		currentConfiguration.setName(jobid);
		//=========================================================
		try {
			HostProperties.Instance().addPrinterConfiguration(currentConfiguration);
			return new MachineResponse("create", true, "Created:" + currentConfiguration.getName() + "");
		} catch (AlreadyAssignedException e) {
			e.printStackTrace();
			return new MachineResponse("create", false, e.getMessage());
		}
	 }
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  * 
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException
	  */
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
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  * 
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException
	  */
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
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  * 
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException
	  */
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
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  * 
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("startjob/{jobid}/{printername}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse start(@PathParam("jobid") String jobid, @PathParam("printername") String printername) {
		// Create job
		File selectedFile = new File(HostProperties.Instance().getUploadDir(), jobid); //should already be done by marshalling: java.net.URLDecoder.decode(name, "UTF-8"));//name);
		
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
		} catch (JobManagerException | AlreadyAssignedException | InappropriateDeviceException e) {
			JobManager.Instance().removeJob(printJob);
			PrinterManager.Instance().removeAssignment(printJob);
			e.printStackTrace();
			return new MachineResponse("start", false, e.getMessage());
		}
 	}
 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("stop/{jobid}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse stop(@PathParam("jobid") String jobId) {
		PrintJob job = JobManager.Instance().getJob(jobId);
		if (job == null) {
			return new MachineResponse("stop", false, "Job:" + jobId + " not active");
		}
		job.getPrinter().setStatus(JobStatus.Cancelled);
	 	return new MachineResponse("stop", true, "Stopped:" + jobId);
	 }	 
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("togglepause/{jobid}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse togglePause(@PathParam("jobid") String jobId) {
		PrintJob job = JobManager.Instance().getJob(jobId);
		if (job == null) {
			return new MachineResponse("togglepause", false, "Job:" + jobId + " not active");
		}
		
		JobStatus status = job.getPrinter().togglePause();
		return new MachineResponse("togglepause", true, "Job:" + jobId + " " + status);
	 }
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("jobstatus/{jobid}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getJobStatus(@PathParam("jobid") String jobId) {
		PrintJob job = JobManager.Instance().getJob(jobId);
		if (job == null) {
			return new MachineResponse("status", false, "Job:" + jobId + " not active");
		}

		return new MachineResponse("status", true, "Job:" + jobId + " " + job.getPrinter().getStatus());
	 }	 
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
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
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("totalslices/{jobid}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getTotalSlices(@PathParam("jobid") String jobId) {
		PrintJob job = JobManager.Instance().getJob(jobId);
		if (job == null) {
			return new MachineResponse("totalslices", false, "Job:" + jobId + " not active");
		}

		return new MachineResponse("totalslices", true, String.valueOf(job.getTotalSlices()));
	 }
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("currentslice/{jobid}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getCurrentSlice(@PathParam("jobid") String jobId) {
		 PrintJob job = JobManager.Instance().getJob(jobId);
		 if(job != null) {
			 return new MachineResponse("getcurrentslice", true, String.valueOf(job.getCurrentSlice()));
		 } else {
			 return new MachineResponse("getcurrentslice", false, "Job:" + jobId + " not active");
		 }
	 }
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 //X Axis Move (sedgwick open aperature)
	 //MachineControl.cmdMoveX()
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("movex/{distance}/{printername}")
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
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("movey/{distance}/{printername}")
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
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("movez/{distance}/{printername}")
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
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
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
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
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
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
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
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
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
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
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
}