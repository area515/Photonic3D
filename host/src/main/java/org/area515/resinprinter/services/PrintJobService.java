package org.area515.resinprinter.services;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.imageio.ImageIO;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.PrintJobManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.slice.StlError;
import org.area515.resinprinter.stl.Triangle3d;
import org.area515.resinprinter.util.security.PhotonicUser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Api(value="printJobs")
@RolesAllowed(PhotonicUser.FULL_RIGHTS)
@Path("printJobs")
public class PrintJobService {
    private static final Logger logger = LogManager.getLogger();
	public static PrintJobService INSTANCE = new PrintJobService();
	
	private PrintJobService() {}
		
    @ApiOperation(value="Returns a list of all active and inactive printjobs on Phontonic 3D.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PrintJob> getPrintJobs() {
		return PrintJobManager.Instance().getPrintJobs();
	}	 

    @ApiOperation(value="Returns the specific printjob designated by it's internal job id.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
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
	 
	
    @ApiOperation(value="Returns the specific PrintJob that is currently active for the specified printer name.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("getByPrinterName/{printerName}")
	@Produces(MediaType.APPLICATION_JSON)
	public PrintJob getByPrinterName(@PathParam("printerName") String printerName) throws InappropriateDeviceException {
		if (printerName == null) {
			throw new IllegalArgumentException("Missing printer name.");
		}
		
		return PrintJobManager.Instance().getPrintJobByPrinterName(printerName);
	}
	
    @ApiOperation(value="Returns the image that is currently being exposed for the PrintJob designated by the specified job id.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
    @Path("currentSliceImage/{jobId}")
    @Produces("image/png")
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
		 			logger.error("EofException from jetty are common when the browser cancels image queries", e);
		 		}
			}  
	    };
    }
	 
    @ApiOperation(value="Stops/cancels the PrintJob designated by the specified job id.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@POST
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
		Printer printer = job.getPrinter();
		if (printer == null) {
			return new MachineResponse("stop", false, "There isn't a printer assigned to job:" + jobId);
		}
		job.getPrinter().setStatus(JobStatus.Cancelling);//This properly closes the printJob by setting status, removing job assignments and stubbing the printjobprocessor
		return new MachineResponse("stop", true, "Stopped:" + jobId);
	}
	
    @ApiOperation(value="Deletes the PrintJob designated by the specified job id.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@DELETE
	@POST
	@Path("delete/{jobId}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse delete(@PathParam("jobId") String jobId) {
		UUID uuid = null;
		PrintJob job = null;
		try {
			uuid = UUID.fromString(jobId);
			job = PrintJobManager.Instance().getJob(uuid);
		} catch (IllegalArgumentException e) {
			return new MachineResponse("delete", false, "Invalid jobId: "+ jobId);
		}
		
		if (job == null) {
			return new MachineResponse("delete", false, "Job not found");
		}
		
		boolean found = PrintJobManager.Instance().removeJob(job);
	 	return new MachineResponse("delete", found, found?"Job deleted": "Job not found (concurrent)");
	}
	
    @ApiOperation(value="Pause/resumes the PrintJob designated by the specified job id.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
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
		Printer printer = job.getPrinter();
		if (printer == null) {
			return new MachineResponse("togglepause", false, "Job:" + job.getJobFile().getName() + " not assigned to a printer");
		}
		
		JobStatus status = printer.togglePause();
		return new MachineResponse("togglepause", true, "Job:" + job.getJobFile().getName() + " " + status);
	}
	 
    @ApiOperation(value="Overrides the lift distance variable for the PrintJob designated by the specified job id.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
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
			logger.error("Job:" + jobId + " distance:" + liftDistance, e);
			return new MachineResponse("LiftDistance", false, e.getMessage());
		}
		return new MachineResponse("LiftDistance", true, "Set lift distance to:" + liftDistance);
	}
	 
    @ApiOperation(value="Overrides the lift speed variable for the PrintJob designated by the specified job id.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
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
			logger.error("Job:" + jobId + " speed:" + speed, e);
			return new MachineResponse("zliftspeed", false, e.getMessage());
		}
		return new MachineResponse("zliftspeed", true, "Set lift speed to:" + speed);
	}
	 
    @ApiOperation(value="Overrides the exposure time variable for the PrintJob designated by the specified job id.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
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
	
    @ApiOperation(value="Retrieves the geometry data associated with the PrintJob designated by the specified job id. "
    		+ "The geometry data returned by this method is highly dependant upon the org.area515.resinprinter.job.PrintFileProcessor<G> that took responsibility for the printable file that this PrintJob is printing. ")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = "Invalid job id"),
            @ApiResponse(code = 400, message = "Job not found"),
            @ApiResponse(code = 400, message = "Couldn't convert geometry to JSON"),
            @ApiResponse(code = 400, message = "(A job manager problem)")
            })
	@GET
	@Path("geometry/{jobId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGeometry(@PathParam("jobId") String jobId) {
		UUID uuid = null;
		PrintJob printJob = null;
		try {
			uuid = UUID.fromString(jobId);
			printJob = PrintJobManager.Instance().getJob(uuid);
		} catch (IllegalArgumentException e) {
			return Response.status(Status.BAD_REQUEST).entity("Invalid jobId: "+ jobId).build();
		}
		if (printJob == null) {
			return Response.status(Status.BAD_REQUEST).entity("Job not found: "+ jobId).build();
		}
		try {
			Object data = printJob.getPrintFileProcessor().getGeometry(printJob);
			ObjectMapper mapper = new ObjectMapper(new JsonFactory());
			//TODO: Eventually these should be put into a hashmap and called up based on the format that the restful client asks for
			SimpleModule simpleModule = new SimpleModule(Photonic3dTriangleSerializer.class.getSimpleName(), Version.unknownVersion());
			simpleModule.addSerializer(Triangle3d.class, new Photonic3dTriangleSerializer());
			mapper.registerModule(simpleModule);
			String json = mapper.writeValueAsString(data);
			return Response.status(Status.OK).encoding(MediaType.APPLICATION_JSON).entity(json).build();
		} catch (JobManagerException e) {
			logger.error("Job:" + jobId, e);
			return Response.status(Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
		} catch (JsonProcessingException e) {
			logger.error("Job:" + jobId, e);
			return Response.status(Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity("Couldn't convert geometry to JSON").build();
		}
	}
    
    @ApiOperation(value="Retrieves the error geometry data associated with the PrintJob designated by the specified job id. "
    		+ "This function will return a list of indicies that are determined to be in error.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = "Invalid job id"),
            @ApiResponse(code = 400, message = "Job not found"),
            @ApiResponse(code = 400, message = "Couldn't convert geometry to JSON"),
            @ApiResponse(code = 400, message = "(A job manager problem)")
            })
	@GET
	@Path("geometryErrors/{jobId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGeometryErrors(@PathParam("jobId") String jobId) {
		UUID uuid = null;
		PrintJob printJob = null;
		try {
			uuid = UUID.fromString(jobId);
			printJob = PrintJobManager.Instance().getJob(uuid);
		} catch (IllegalArgumentException e) {
			return Response.status(Status.BAD_REQUEST).entity("Invalid jobId: "+ jobId).build();
		}
		if (printJob == null) {
			return Response.status(Status.BAD_REQUEST).entity("Job not found: "+ jobId).build();
		}
		try {
			Object data = printJob.getPrintFileProcessor().getErrors(printJob);
			ObjectMapper mapper = new ObjectMapper(new JsonFactory());
			//TODO: Eventually these should be put into a hashmap and called up based on the format that the restful client asks for
			SimpleModule simpleModule = new SimpleModule(Photonic3dSTLErrorSerializer.class.getSimpleName(), Version.unknownVersion());
			simpleModule.addSerializer(StlError.class, new Photonic3dSTLErrorSerializer());
			mapper.registerModule(simpleModule);			
			String json = mapper.writeValueAsString(data);
			return Response.status(Status.OK).encoding(MediaType.APPLICATION_JSON).entity(json).build();
		} catch (JobManagerException e) {
			logger.error("Job:" + jobId, e);
			return Response.status(Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
		} catch (JsonProcessingException e) {
			logger.error("Job:" + jobId, e);
			return Response.status(Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity("Couldn't convert geometry to JSON").build();
		}
	}
}
