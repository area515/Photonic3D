package org.area515.resinprinter.services;

//http://www.javatutorials.co.in/jax-rs-2-jersey-file-upload-example/

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.awt.GraphicsDevice;
import java.io.File;
import java.io.IOException;
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
import org.area515.resinprinter.job.ManualControl;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.server.HostProperties;

@Path("machine")
public class MachineService {
	
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
	 @Path("start/{jobid}/{display}/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse start(@PathParam("jobid") String jobid, @PathParam("display") String displayId, @PathParam("comport") String comportId) {
		// Create job
		File selectedFile = new File(HostProperties.Instance().getUploadDir(), jobid); //should already be done by marshalling: java.net.URLDecoder.decode(name, "UTF-8"));//name);
		
		// Delete and Create handled in jobManager
		PrintJob printJob = null;
		try {
			printJob = JobManager.Instance().createJob(selectedFile);
			GraphicsDevice graphicsDevice = DisplayManager.Instance().getDisplayDevice(displayId);
			if (graphicsDevice == null) {
				throw new JobManagerException("Couldn't find graphicsDevice called:" + displayId);
			}
			
			DisplayManager.Instance().assignDisplay(printJob, graphicsDevice);
			CommPortIdentifier port = SerialManager.Instance().getSerialDevice(comportId);
			if (port == null) {
				throw new JobManagerException("Couldn't find communications device called:" + comportId);
			}
			
			SerialManager.Instance().assignSerialPort(printJob, port);
			Future<JobStatus> status = JobManager.Instance().startJob(printJob);
			
			//if you want to wait for the job to end, call  ideastatus.get();  but that seems like a bad idea
			
			return new MachineResponse("start", true, printJob.getId() + "");
		} catch (JobManagerException e) {
			JobManager.Instance().removeJob(printJob);
			DisplayManager.Instance().removeAssignment(printJob);
			SerialManager.Instance().removeAssignment(printJob);
			if (printJob != null) {
				printJob.close();
			}
			e.printStackTrace();
			return new MachineResponse("start", false, "Problem creating job:" + e.getMessage());
		} catch (AlreadyAssignedException e) {
			JobManager.Instance().removeJob(printJob);
			DisplayManager.Instance().removeAssignment(printJob);
			SerialManager.Instance().removeAssignment(printJob);
			if (printJob != null) {
				printJob.close();
			}
			e.printStackTrace();
			return new MachineResponse("start", false, "Device already used:" + e.getMessage());
		} catch (InappropriateDeviceException e) {
			JobManager.Instance().removeJob(printJob);
			DisplayManager.Instance().removeAssignment(printJob);
			SerialManager.Instance().removeAssignment(printJob);
			if (printJob != null) {
				printJob.close();
			}
			e.printStackTrace();
			return new MachineResponse("start", false, "Illegal argument passed to method:" + e.getMessage());
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
		job.setStatus(JobStatus.Cancelled);
	 	return new MachineResponse("stop", true, "");
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
		
		return new MachineResponse("togglepause", true, job.togglePause() + "");
	 }
	 
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("status/{jobid}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse status(@PathParam("jobid") String jobId) {
		PrintJob job = JobManager.Instance().getJob(jobId);
		if (job == null) {
			return new MachineResponse("status", false, "Job:" + jobId + " not active");
		}

		return new MachineResponse("status", true, job.getStatus() + "");
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
		 if(job != null && job.isPrintInProgress()){
			 return new MachineResponse("getcurrentslice", true, String.valueOf(job.getCurrentSlice()));
		 } else {
			 return new MachineResponse("getcurrentslice", true, "-1");
		 }
	 }
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 //Z Axis Up
	 //MachineControl.cmdUp_Click()
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("zaxisup/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse zAxisUp(@PathParam("comport") String comport) {
			PrintJob job = new PrintJob();
			try {
				SerialManager.Instance().assignSerialPort(job, SerialManager.Instance().getSerialDevice(comport));
				new ManualControl(job).cmdUp_Click();
				return new MachineResponse("zaxisup", true, "");
			} catch (AlreadyAssignedException e) {
				e.printStackTrace();
				return new MachineResponse("zaxisup", false, e.getMessage());
			} catch (InappropriateDeviceException e) {
				e.printStackTrace();
				return new MachineResponse("zaxisup", false, e.getMessage());
			} finally {
				job.close();
				SerialManager.Instance().removeAssignment(job);
			}
	 }
	 
	 //Z Axis Down
	 //MachineControl.cmdDown_Click()
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("zaxisdown/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse zAxisDown(@PathParam("comport") String comport) {
			PrintJob job = new PrintJob();
			try {
				SerialManager.Instance().assignSerialPort(job, SerialManager.Instance().getSerialDevice(comport));
				new ManualControl(job).cmdDown_Click();
				return new MachineResponse("zaxisdown", true, "");
			} catch (AlreadyAssignedException e) {
				e.printStackTrace();
				return new MachineResponse("zaxisdown", false, e.getMessage());
			} catch (InappropriateDeviceException e) {
				e.printStackTrace();
				return new MachineResponse("zaxisdown", false, e.getMessage());
			} finally {
				job.close();
				SerialManager.Instance().removeAssignment(job);
			}
	 }
	 
	 //X Axis Move (sedgwick open apeture)
	 //MachineControl.cmdMoveX()
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("movex/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse moveX(@PathParam("comport") String comport) {
			PrintJob job = new PrintJob();
			try {
				SerialManager.Instance().assignSerialPort(job, SerialManager.Instance().getSerialDevice(comport));
				new ManualControl(job).cmdMoveX();
				return new MachineResponse("movex", true, "");
			} catch (AlreadyAssignedException e) {
				e.printStackTrace();
				return new MachineResponse("movex", false, e.getMessage());
			} catch (InappropriateDeviceException e) {
				e.printStackTrace();
				return new MachineResponse("movex", false, e.getMessage());
			} finally {
				job.close();
				SerialManager.Instance().removeAssignment(job);
			}
	 }
	 
	 //Y Axis Move (sedgwick close apeture)
	 //MachineControl.cmdMoveY()
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("movey/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse moveY(@PathParam("comport") String comport) {
			PrintJob job = new PrintJob();
			try {
				SerialManager.Instance().assignSerialPort(job, SerialManager.Instance().getSerialDevice(comport));
				new ManualControl(job).cmdMoveY();
				return new MachineResponse("movey", true, "");
			} catch (AlreadyAssignedException e) {
				e.printStackTrace();
				return new MachineResponse("movey", false, e.getMessage());
			} catch (InappropriateDeviceException e) {
				e.printStackTrace();
				return new MachineResponse("movey", false, e.getMessage());
			} finally {
				job.close();
				SerialManager.Instance().removeAssignment(job);
			}
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
	 @Path("movez/{distance}/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse moveZ(@PathParam("distance") String dist, @PathParam("comport") String comport) {
			PrintJob job = new PrintJob();
			try {
				SerialManager.Instance().assignSerialPort(job, SerialManager.Instance().getSerialDevice(comport));
				new ManualControl(job).cmdMoveZ(Double.parseDouble(dist));
				return new MachineResponse("movez", true, dist);
			} catch (AlreadyAssignedException e) {
				e.printStackTrace();
				return new MachineResponse("movez", false, e.getMessage());
			} catch (InappropriateDeviceException e) {
				e.printStackTrace();
				return new MachineResponse("movez", false, e.getMessage());
			} finally {
				job.close();
				SerialManager.Instance().removeAssignment(job);
			}
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
	 @Path("motorson/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse motorsOn(@PathParam("comport") String comport) {
			PrintJob job = new PrintJob();
			try {
				SerialManager.Instance().assignSerialPort(job, SerialManager.Instance().getSerialDevice(comport));
				new ManualControl(job).cmdMotorsOn();
				return new MachineResponse("motorsOn", true, "");
			} catch (AlreadyAssignedException e) {
				e.printStackTrace();
				return new MachineResponse("motorsOn", false, e.getMessage());
			} catch (InappropriateDeviceException e) {
				e.printStackTrace();
				return new MachineResponse("motorsOn", false, e.getMessage());
			} finally {
				job.close();
				SerialManager.Instance().removeAssignment(job);
			}
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
	 @Path("motorsoff/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse motorsOff(@PathParam("comport") String comport) {
			PrintJob job = new PrintJob();
			try {
				SerialManager.Instance().assignSerialPort(job, SerialManager.Instance().getSerialDevice(comport));
				new ManualControl(job).cmdMotorsOff();
				return new MachineResponse("motorsoff", true, "");
			} catch (AlreadyAssignedException e) {
				e.printStackTrace();
				return new MachineResponse("motorsoff", false, e.getMessage());
			} catch (InappropriateDeviceException e) {
				e.printStackTrace();
				return new MachineResponse("motorsoff", false, e.getMessage());
			} finally {
				job.close();
				SerialManager.Instance().removeAssignment(job);
			}
	 }
	 
	 //MachineControl.cmdGetZRate()
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("getzrate/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getZRate(@PathParam("comport") String comport) {
			PrintJob job = new PrintJob();
			try {
				SerialManager.Instance().assignSerialPort(job, SerialManager.Instance().getSerialDevice(comport));
				return new MachineResponse("getzrate", true, new ManualControl(job).getZRate() + "");
			} catch (AlreadyAssignedException e) {
				e.printStackTrace();
				return new MachineResponse("getzrate", false, e.getMessage());
			} catch (InappropriateDeviceException e) {
				e.printStackTrace();
				return new MachineResponse("getzrate", false, e.getMessage());
			} finally {
				job.close();
				SerialManager.Instance().removeAssignment(job);
			}
	 }
	 
	 //MachineControl.cmdGetXYRate()
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  *
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException 
	  */
	 @GET
	 @Path("getxyrate/{comport}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getXYRate(@PathParam("comport") String comport) {
			PrintJob job = new PrintJob();
			try {
				SerialManager.Instance().assignSerialPort(job, SerialManager.Instance().getSerialDevice(comport));
				return new MachineResponse("getxyrate", true, new ManualControl(job).getXYRate() + "");
			} catch (AlreadyAssignedException e) {
				e.printStackTrace();
				return new MachineResponse("getxyrate", false, e.getMessage());
			} catch (InappropriateDeviceException e) {
				e.printStackTrace();
				return new MachineResponse("getxyrate", false, e.getMessage());
			} finally {
				job.close();
				SerialManager.Instance().removeAssignment(job);
			}
	 }

}