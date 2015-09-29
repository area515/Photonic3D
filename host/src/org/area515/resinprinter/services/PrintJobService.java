package org.area515.resinprinter.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.PrintJobManager;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("printJobs")
public class PrintJobService {
	public static PrintJobService INSTANCE = new PrintJobService();
		
	private PrintJobService() {}
		
	@GET
	@Path("get/{jobId}")
	@Produces(MediaType.APPLICATION_JSON)
	public PrintJob getById(@PathParam("jobId") String jobId) {
		UUID uuid = null;
		PrintJob job = null;
		try {
			uuid = UUID.fromString(jobId);
			job = PrintJobManager.Instance().getJob(uuid);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid jobId: "+ jobId);
		}
		
		if (job == null) {
			throw new IllegalArgumentException("Job not found");
		}

		return job;
	 }	 
	 
	
	@GET
	@Path("getByPrinterName/{printerName}")
	@Produces(MediaType.APPLICATION_JSON)
	public PrintJob getByPrinterName(@PathParam("printerName") String printerName) throws InappropriateDeviceException {
		if (printerName == null) {
			throw new IllegalArgumentException("Missing printer name.");
		}
		
		return PrintJobManager.Instance().getPrintJobByPrinterName(printerName);
	}
	
	@GET
    @Path("currentSliceImage/{jobId}")
    @Produces("image/png")
    /**
     * Another way to stream:
     * http://stackoverflow.com/questions/9204287/how-to-return-a-png-image-from-jersey-rest-service-method-to-the-browser
     * 
     * @param jobId
     * @return
     */
    public StreamingOutput getImage(@PathParam("jobId") final String jobId) {		
	    return new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				UUID uuid = null;
				try {
					uuid = UUID.fromString(jobId);
				} catch (IllegalArgumentException e) {
		 			IOUtils.copy(getClass().getResourceAsStream("noimageavailable.png"), output);
		 			return;
				}
				
				PrintJob job = PrintJobManager.Instance().getJob(uuid);
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
	 
	 @GET
	 @Path("stopJob/{jobId}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse stopJob(@PathParam("jobId") String jobId) {
		UUID uuid = null;
		PrintJob job = null;
		try {
			uuid = UUID.fromString(jobId);
			job = PrintJobManager.Instance().getJob(uuid);
		} catch (IllegalArgumentException e) {
			return new MachineResponse("stop", false, "Invalid jobId: "+ jobId);
		}
		
		if (job == null) {
			return new MachineResponse("stop", false, "Job not found");
		}
		job.getPrinter().setStatus(JobStatus.Cancelled);
	 	return new MachineResponse("stop", true, "Stopped:" + jobId);
	 }	 
	 
	 @GET
	 @Path("togglePause/{jobId}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse togglePause(@PathParam("jobId") String jobId) {
			UUID uuid = null;
			PrintJob job = null;
			try {
				uuid = UUID.fromString(jobId);
				job = PrintJobManager.Instance().getJob(uuid);
			} catch (IllegalArgumentException e) {
				return new MachineResponse("togglepause", false, "Invalid jobId: "+ jobId);
			}

			if (job == null) {
				return new MachineResponse("togglepause", false, "Job not found");
			}
			
			JobStatus status = job.getPrinter().togglePause();
			return new MachineResponse("togglepause", true, "Job:" + job.getJobFile().getName() + " " + status);
	 }
	 
	 @GET
	 @Path("overrideZLiftDistance/{jobId}/{distance}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse overrideZLiftDistance(@PathParam("jobId") String jobId, @PathParam("distance") double liftDistance) {
			UUID uuid = null;
			PrintJob printJob = null;
			try {
				uuid = UUID.fromString(jobId);
				printJob = PrintJobManager.Instance().getJob(uuid);
			} catch (IllegalArgumentException e) {
				return new MachineResponse("zliftdistance", false, "Invalid jobId: "+ jobId);
			}

			if (printJob == null) {
				return new MachineResponse("LiftDistance", false, "Job not found");
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
	 @Path("overrideZLiftSpeed/{jobId}/{speed}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse overrideZLiftSpeed(@PathParam("jobId") String jobId, @PathParam("speed") double speed) {
			UUID uuid = null;
			PrintJob printJob = null;
			try {
				uuid = UUID.fromString(jobId);
				printJob = PrintJobManager.Instance().getJob(uuid);
			} catch (IllegalArgumentException e) {
				return new MachineResponse("zliftdistance", false, "Invalid jobId: "+ jobId);
			}

			if (printJob == null) {
				return new MachineResponse("zliftspeed", false, "Job:" + jobId + " not found");
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
	 @Path("overrideExposuretime/{jobId}/{exposureTime}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse overrideExposuretime(@PathParam("jobId") String jobId, @PathParam("exposureTime") int exposureTime) {
			UUID uuid = null;
			PrintJob printJob = null;
			try {
				uuid = UUID.fromString(jobId);
				printJob = PrintJobManager.Instance().getJob(uuid);
			} catch (IllegalArgumentException e) {
				return new MachineResponse("zliftdistance", false, "Invalid jobId: "+ jobId);
			}

			if (printJob == null) {
				return new MachineResponse("exposureTime", false, "Job not found");
			}
			
			printJob.overrideExposureTime(exposureTime);
			return new MachineResponse("exposureTime", true, "Exposure time set");
	 }
	 
	 @GET
	 @Path("geometry/{jobId}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse getGeometry(@PathParam("jobId") String jobId) {
			UUID uuid = null;
			PrintJob printJob = null;
			try {
				uuid = UUID.fromString(jobId);
				printJob = PrintJobManager.Instance().getJob(uuid);
			} catch (IllegalArgumentException e) {
				return new MachineResponse("geometry", false, "Invalid jobId: "+ jobId);
			}

			if (printJob == null) {
				return new MachineResponse("geometry", false, "Job not found.");
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
