package org.area515.resinprinter.services;

//http://www.javatutorials.co.in/jax-rs-2-jersey-file-upload-example/

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;












import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.job.GCodeParseThread;
import org.area515.resinprinter.job.JobManager;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.ManualControl;
import org.area515.resinprinter.job.JobManager.MachineAction;
import org.area515.resinprinter.server.HostProperties;
import org.apache.commons.io.FileUtils;

@Path("machine")
public class MachineService {
 /**
  * Method handling HTTP GET requests. The returned object will be sent
  * to the client as "text/plain" media type.
  *
  * @return String that will be returned as a text/plain response.
  * @throws IOException 
  */
	
 @GET
 @Path("start/{name}")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse start(@PathParam("name") String name) throws IOException {
 	
 	if (JobManager.Status != MachineAction.RUNNING) {
			
			// Create job
			File selectedFile = new File(HostProperties.Instance().getUploadDir(),java.net.URLDecoder.decode(name, "UTF-8"));//name);
			
			// Delete and Create handled in jobManager
			JobManager jobManager = null;
			try {
				jobManager = new JobManager(selectedFile);
			} catch (JobManagerException | IOException e) {
				e.printStackTrace();
				MachineResponse machineResponse = new MachineResponse("start",false,e.getMessage());
				return machineResponse;
			}

			// Parse File
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Runnable worker = new GCodeParseThread(jobManager);
//			Runnable worker = new GCodeParseThread(jobManager);//jobManager.getGCode());
			executor.execute(worker);
			DisplayManager.Instance().ShowBlank();
//			JobManager.Status = MachineAction.STOPPED;
			System.out.println("Finished parsing Gcode file");
			System.out.println("Exiting");
			
			return new MachineResponse("start", true, "");
		} else{
			return new MachineResponse("start",false,"Can't start a new job.  Machine is busy.  Job in progress");
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
 @Path("stop")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse stop() {
 	JobManager.Status = MachineAction.STOPPED;
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
 @Path("status")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse status() {
 	return new MachineResponse("status", true, JobManager.Status.toString());
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
 @Path("zaxisup")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse zAxisUp() {
 	ManualControl.Instance().cmdUp_Click();
 	return new MachineResponse("zaxisup", true, "");
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
 @Path("zaxisdown")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse zAxisDown() {
 	ManualControl.Instance().cmdDown_Click();
 	return new MachineResponse("zaxisdown", true, "");
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
 @Path("movex")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse moveX() {
 	ManualControl.Instance().cmdMoveX();
 	return new MachineResponse("movex", true, "");
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
 @Path("movey")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse moveY() {
 	ManualControl.Instance().cmdMoveY();
 	return new MachineResponse("movey", true, "");
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
 @Path("movez/{dist}")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse moveZ(@PathParam("dist") String dist) {
 	ManualControl.Instance().cmdMoveZ(Double.parseDouble(dist));
 	return new MachineResponse("movez", true, dist);
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
 @Path("motorson")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse motorsOn() {
// 	try{
 	try {
			ManualControl.Instance().cmdMotorsOn();
			return new MachineResponse("motorsOn", true, "");
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new MachineResponse("motorsOn", false, e.getMessage());
		}
 	
// 	}catch(IOException, InterruptedException)
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
 @Path("motorsoff")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse motorsOff() {
 	try {
			ManualControl.Instance().cmdMotorsOff();
			return new MachineResponse("motorsoff", true, "");
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new MachineResponse("motorsoff", false, e.getMessage());
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
 @Path("getzrate")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse getZRate() {
 	double zRate = ManualControl.Instance().getZRate();
 	return new MachineResponse("getzrate", true, String.valueOf(zRate));
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
 @Path("getxyrate")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse getXYRate() {
 	double xyRate = ManualControl.Instance().getXYRate();
 	return new MachineResponse("getxyrate", true, String.valueOf(xyRate));
 }
 
 /**
  * Method handling HTTP GET requests. The returned object will be sent
  * to the client as "text/plain" media type.
  *
  * @return String that will be returned as a text/plain response.
  * @throws IOException 
  */
 @GET
 @Path("totalslices")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse getTotalSlices() {
 	return new MachineResponse("totalslices", true, String.valueOf(JobManager.getTotalSlices()));
 }
 
 /**
  * Method handling HTTP GET requests. The returned object will be sent
  * to the client as "text/plain" media type.
  *
  * @return String that will be returned as a text/plain response.
  * @throws IOException 
  */
 @GET
 @Path("currentslice")
 @Produces(MediaType.APPLICATION_JSON)
 public MachineResponse getCurrentSlice() {
 	if(JobManager.Status == MachineAction.RUNNING){
 		return new MachineResponse("getcurrentslice", true, String.valueOf(JobManager.getCurrentSlice()));
 	}else{
 		return new MachineResponse("getcurrentslice", true, "-1");
 	}
 }
 
}