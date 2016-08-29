package org.area515.resinprinter.services;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.imageio.ImageIO;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.exception.NoPrinterFoundException;
import org.area515.resinprinter.exception.SliceHandlingException;
import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.Customizer;
import org.area515.resinprinter.job.Previewable;
import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.util.security.PhotonicUser;
import org.area515.util.PrintFileFilter;

@Api(value="customizers")
@RolesAllowed(PhotonicUser.FULL_RIGHTS)
@Path("customizers")
public class CustomizerService {
	private static final Logger logger = LogManager.getLogger();
    public static CustomizerService INSTANCE = new CustomizerService();
    private static HashMap<String, Customizer> customizers = new HashMap<>();
    

 //    @ApiOperation(value="Saves a Customizer into persistent storage.")
	// @PUT
 //    @Path("save")
	// @Produces(MediaType.APPLICATION_JSON)
	// @Consumes(MediaType.APPLICATION_JSON)
	// public MachineResponse saveCustomizer(Customizer cusomizer) {
	// 	return null;
	// }
    
 //    @ApiOperation(value="Deletes a Customizer from persistent storage.")
	// @DELETE
 //    @Path("delete/{customizerName}")
	// @Produces(MediaType.APPLICATION_JSON)
	// public MachineResponse deleteCustomizer(@PathParam("customizerName")String customizerName) {
	// 	return null;
	// }
	
    @ApiOperation(value="Retrieves all Customizers from static HashMap")
	@GET
    @Path("list")
	public Map<String, Customizer> getCustomizers() {
		return customizers;
	}
    
 //    @ApiOperation(value="Retrieves all Customizers that have been created for a given Printable.")
	// @GET
 //    @Path("getByPrintableName/{printableName}")
	// @Produces(MediaType.APPLICATION_JSON)
	// public List<Customizer> getCustomizersByPrintableName(@PathParam("printableName")String printableName) {
	// 	//TODO: Pretend this is implemented.
	// 	return null;
	// }
	
 //    @ApiOperation(value="Creates but doesn't save a Customizer that has all of the proper support capabilities(booleans) already setup based upon the Printable name that was sent to this method.")
	// @GET
 //    @Path("createTemplateCustomizer/{printableName}")
	// @Produces(MediaType.APPLICATION_JSON)
	// public Customizer createTemplateCustomizer(@PathParam("printableName")String printableName) {
	// 	return null;
	// }

	// @ApiOperation(value="Test api method")
	// @POST
	// @Path("testAPITEST") 
	// // @Consumes(MediaType.APPLICATION_JSON)
	// @Produces(MediaType.APPLICATION_JSON)
	// public String test(Customizer customizer) {
	// 	if (customizer != null) {
	// 		return "\"" + new String(JsonStringEncoder.getInstance().quoteAsString(customizer.getName())) + "\"";
	// 	} else {
	// 		return "\"" +  new String(JsonStringEncoder.getInstance().quoteAsString("Customizer is null")) + "\"";
	// 	}

	// }

	// @ApiOperation(value="Test to return a customizer")
	// @POST
	// @Path("customizerTest")
	// @Produces(MediaType.APPLICATION_JSON)
	// public Customizer customizerTest(Customizer customizer) {
	// 	return customizer;
	// }

	public Customizer getCustomizer(String filename) {
		if (!(customizers.containsKey(filename))) {
			// Fix handling of this error case (shouldn't happen)
			throw new IllegalArgumentException("Could not find customizer for " + filename);
		}
		return customizers.get(filename);
	}

    @ApiOperation(value="Attempt to start a print by specifying the name of the printable file. "
    		+ "For this operation to be successful, there must be exactly one Printer started.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@POST
	@Path("print/{filename}")
	public PrintJob print(@PathParam("filename")String fileName) {
		return PrintableService.INSTANCE.print(fileName, true);
	}


	@ApiOperation(value="Save Customizer to a static HashMap given a file name")
	@POST
	@Path("upsertCustomizer")
	public void addCustomizer(Customizer customizer) {
		//throw new IllegalArgumentException("fail");
		String fileName = customizer.getPrintableName() + "." + customizer.getPrintableExtension();
		// logger.debug("Add to customizers with key " + fileName + " and the customizer affineTransform is" + customizer.createAffineTransform());
		
		if (customizers.get(fileName) != null) {
			customizer.setOrigSliceCache(customizers.get(fileName).getOrigSliceCache());
		}
		customizers.put(fileName, customizer);
	}

	@ApiOperation(value="Deletes a Customizer from a static HashMap given a file name")
	@DELETE
	@Path("removeCustomizer/{fileName}")
	public void removeCustomizer(@PathParam("fileName") String fileName) {
		//throw new IllegalArgumentException("fail");
		customizers.remove(fileName);
	}

	@ApiOperation(value="Renders the first slice of a printable based on the customizer")
	    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("renderFirstSliceImage/{fileName}")
	@Produces("image/jpeg")
	public StreamingOutput renderFirstSliceImage(@PathParam("fileName") String fileName, @QueryParam("projectImage") boolean projectImage) throws NoPrinterFoundException, SliceHandlingException {
		// logger.debug("Filename is " + fileName);
		Customizer customizer = getCustomizer(fileName);
		if (customizer == null) {
			throw new IllegalArgumentException("Customizer is null");
		}

		logger.debug("projectImage is " + projectImage);
		// String fileName = customizer.getPrintableName() + "." + customizer.getPrintableExtension();
		File file = new File(HostProperties.Instance().getUploadDir(), fileName);

		PrintFileProcessor<?,?> processor = PrintFileFilter.INSTANCE.findAssociatedPrintProcessor(file);
		if (!(processor instanceof Previewable)) {
			throw new IllegalArgumentException(processor.getFriendlyName() + " files don't support image preview.");
		}
		
		AbstractPrintFileProcessor previewableProcessor = (AbstractPrintFileProcessor) processor;
		try {
			BufferedImage img = previewableProcessor.buildPreviewSlice(customizer, file, (Previewable)processor, projectImage);

			logger.debug("just got the bufferedimg from previewSlice");

			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					try {
						ImageIO.write(img, "JPG", output);

						logger.debug("Writing the img");
					} catch (IOException e) {
							//System.out.println("failed writing");
						throw e;
					}
				}
			};
			return stream;
		} catch (NoPrinterFoundException|SliceHandlingException |IllegalArgumentException e) {
			// Loggers already warned or had error messages so just throw these up the stack
			throw e;
		}
	}
	
 //    @ApiOperation(value="Renders any given slice based on the customizer and current slice number. This method assumes that the customizer has already been saved.")
	// @GET
 //    @Path("renderSliceImage/{customizerName}/{currentSlice}")
 //    @Produces("image/png")
	// public StreamingOutput renderImage(@PathParam("customizerName") String customizer, @PathParam("currentSlice")int currentSlice) {
	// 	return null;
	// }
    
 //    @ApiOperation(value="Renders any given slice based on the provided Customizer and current slice number.")
	// @GET
 //    @Path("testRenderSliceImage/{currentSlice}")
 //    @Produces("image/png")
	// public StreamingOutput testRenderImage(Customizer customizer, @PathParam("currentSlice")int currentSlice) {
	// 	return null;
	// }
}
