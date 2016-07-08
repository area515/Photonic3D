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
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.WebApplicationException;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.job.STLFileProcessor;
import org.area515.resinprinter.job.Customizer;
import org.area515.resinprinter.exception.SlicerException;
import org.area515.resinprinter.exception.NoPrinterFoundException;
import org.area515.resinprinter.server.HostProperties;
import org.area515.util.PrintFileFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Api(value="customizers")
@RolesAllowed(UserService.FULL_RIGHTS)
@Path("customizers")
public class CustomizerService {
	private static final Logger logger = LogManager.getLogger();
    public static CustomizerService INSTANCE = new CustomizerService();
    private static HashMap<String, Customizer> customizers = new HashMap<>();
    
	//TODO: do we want this? getCustomizersByPrinterName(String printerName)

 //    @ApiOperation(value="Saves a Customizer into persistent storage.")
	// @PUT
 //    @Path("save")
	// @Produces(MediaType.APPLICATION_JSON)
	// @Consumes(MediaType.APPLICATION_JSON)
	// public MachineResponse saveCustomizer(Customizer cusomizer) {
	// 	//TODO: Pretend this is implemented.
	// 	return null;
	// }
    
 //    @ApiOperation(value="Deletes a Customizer from persistent storage.")
	// @DELETE
 //    @Path("delete/{customizerName}")
	// @Produces(MediaType.APPLICATION_JSON)
	// public MachineResponse deleteCustomizer(@PathParam("customizerName")String customizerName) {
	// 	//TODO: Pretend this is implemented.
	// 	return null;
	// }
	
 //    @ApiOperation(value="Retrieves all Customizers")
	// @GET
 //    @Path("list")
	// @Produces(MediaType.APPLICATION_JSON)
	// public List<Customizer> getCustomizersByPrintableName() {
	// 	//TODO: Pretend this is implemented.
	// 	return null;
	// }
    
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
	// 	//TODO: Pretend this is implemented.
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

	@ApiOperation(value="Save Customizer to a static HashMap given a printable name")
	@POST
	@Path("upsertCustomizer")
	public void addCustomizer(Customizer customizer) {
		//throw new IllegalArgumentException("fail");
		customizers.put(customizer.getPrintableName(), customizer);
	}

	@ApiOperation(value="Renders the first slice of a printable based on the customizer")
	    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("renderFirstSliceImage/{printableName}")
	@Produces("image/png")
	public StreamingOutput renderFirstSliceImage(@PathParam("printableName") String printableName) throws NoPrinterFoundException, SlicerException {
		Customizer customizer = customizers.get(printableName);
		if (customizer != null) {
			String fileName = customizer.getPrintableName() + "." + customizer.getPrintableExtension();
			File file = new File(HostProperties.Instance().getUploadDir(), fileName);

			PrintFileProcessor<?,?> processor = PrintFileFilter.INSTANCE.findAssociatedPrintProcessor(file);
			if (processor instanceof STLFileProcessor) {
				STLFileProcessor stlfileprocessor = (STLFileProcessor) processor;
				try {
					BufferedImage img = stlfileprocessor.previewSlice(customizer, file);

					logger.debug("just got the bufferedimg from previewSlice");


					StreamingOutput stream = new StreamingOutput() {
						@Override
						public void write(OutputStream output) throws IOException, WebApplicationException {
							try {
								ImageIO.write(img, "PNG", output);

								logger.debug("Writing the img");

							} catch (IOException e) {
								//System.out.println("failed writing");
								throw new IOException("We can't write the image");
							}
						}
					};
					return stream;
				} catch (NoPrinterFoundException e) {
					throw e;
				} catch (SlicerException e) {
					throw e;
				}
			} else {
				throw new IllegalArgumentException("Incorrect file type. Cannot display preview for non STL files as of now");
			}
		}
		return null;
	}
	
    @ApiOperation(value="Renders any given slice based on the customizer and current slice number. This method assumes that the customizer has already been saved.")
	@GET
    @Path("renderSliceImage/{customizerName}/{currentSlice}")
    @Produces("image/png")
	public StreamingOutput renderImage(@PathParam("customizerName") String customizer, @PathParam("currentSlice")int currentSlice) {
		//TODO: Pretend this is implemented.
		return null;
	}
    
    @ApiOperation(value="Renders any given slice based on the provided Customizer and current slice number.")
	@GET
    @Path("testRenderSliceImage/{currentSlice}")
    @Produces("image/png")
	public StreamingOutput testRenderImage(Customizer customizer, @PathParam("currentSlice")int currentSlice) {
		//TODO: Pretend this is implemented.
		return null;
	}
}
