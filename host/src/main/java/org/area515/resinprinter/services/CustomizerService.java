package org.area515.resinprinter.services;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

@Api(value="customizers")
@Path("customizers")
public class CustomizerService {
    public static CustomizerService INSTANCE = new CustomizerService();
    
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
	
    @ApiOperation(value="Creates but doesn't save a Customizer that has all of the proper support capabilities(booleans) already setup based upon the Printable name that was sent to this method.")
	@GET
    @Path("createTemplateCustomizer/{printableName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Customizer createTemplateCustomizer(@PathParam("printableName")String printableName) {
		//TODO: Pretend this is implemented.
		return null;
	}

	@ApiOperation(value="Test api method")
	@POST
	@Path("testAPITEST") 
	// @Consumes(MediaType.APPLICATION_JSON)
	// @Produces(MediaType.APPLICATION_JSON)
	public String test(Customizer customizer) {
		if (customizer != null) {
			return customizer.getName();
		} else {
			return "Customizer is null";
		}

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
