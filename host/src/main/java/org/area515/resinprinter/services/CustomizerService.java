package org.area515.resinprinter.services;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.exception.NoPrinterFoundException;
import org.area515.resinprinter.exception.SliceHandlingException;
import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.Customizer;
import org.area515.resinprinter.job.Previewable;
import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.util.security.PhotonicUser;
import org.area515.util.PrintFileFilter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Api(value="customizers")
@RolesAllowed(PhotonicUser.FULL_RIGHTS)
@Path("customizers")
public class CustomizerService {
	private static final Logger logger = LogManager.getLogger();
    public static CustomizerService INSTANCE = new CustomizerService();
    private Cache<String, Customizer> customizersByName = CacheBuilder.newBuilder().softValues().build();
    
    @ApiOperation(value="Retrieves all Customizers")
	@GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
	public Map<String, Customizer> getCustomizers() {
		return customizersByName.asMap();
	}
    
    @ApiOperation(value="Find a customizer by name.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("get/{customizerName}")
    @Produces(MediaType.APPLICATION_JSON)
	public Customizer getCustomizer(@PathParam("customizerName")String customizerName, @QueryParam("externalState") String externalState) {
		Customizer customizer = customizersByName.getIfPresent(customizerName);
		if (customizer != null) {
			if (customizer.getExternalImageAffectingState() == null || !customizer.getExternalImageAffectingState().equals(externalState)) {
				customizer.setOrigSliceCache(null);
				//TODO: start building image in a background task before the client asks for it!
			}
			customizer.setExternalImageAffectingState(externalState);
		}
		
		return customizer;
	}
    
	@ApiOperation(value="Save Customizer.")
	@POST
	@Path("upsert")
    @Produces(MediaType.APPLICATION_JSON)
	public Customizer addOrUpdateCustomizer(Customizer customizer) {
		Customizer oldCustomizer = customizersByName.getIfPresent(customizer.getName());
		if (oldCustomizer != null) {
			//If the image was affected by some external state other than the customizer(e.g. calibration), trash the cache.
			if (customizer.getExternalImageAffectingState() != null && 
				customizer.getExternalImageAffectingState().equals(oldCustomizer.getExternalImageAffectingState())) {
				customizer.setOrigSliceCache(oldCustomizer.getOrigSliceCache());
			}
			//else TODO: start building image in a background task before the client asks for it!
		}
		
		customizersByName.put(customizer.getName(), customizer);
		return customizer;
	}

	@ApiOperation(value="Deletes a Customizer given it's name")
	@DELETE
	@Path("delete/{customizerName}")
	public void removeCustomizer(@PathParam("customizerName") String customizerName) {
		customizersByName.invalidate(customizerName);
	}

	@ApiOperation(value="Displays a slice of a printable based on the Customizer to the printer light source")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
        @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("projectCustomizerOnPrinter/{customizerName}")
	public void projectImage(@PathParam("customizerName") String customizerName) throws SliceHandlingException, InappropriateDeviceException, NoPrinterFoundException {
		Customizer customizer = customizersByName.getIfPresent(customizerName);
		if (customizer == null) {
			throw new IllegalArgumentException("Customizer is missing");
		}
		
		String printerName = customizer.getPrinterName();
		if (printerName == null) {
			throw new IllegalArgumentException("No printer available.");
		}
		
		Printer printer = PrinterService.INSTANCE.getPrinter(printerName);
		if (!printer.isStarted()) {
			throw new IllegalArgumentException("Printer must be started");
		}

		if (printer.isPrintInProgress()) {
			throw new IllegalArgumentException("Printer can't preview while print is active!");
		}

		File file = new File(HostProperties.Instance().getUploadDir(), customizer.getPrintableName() + "." + customizer.getPrintableExtension());
		PrintFileProcessor<?,?> processor = PrintFileFilter.INSTANCE.findAssociatedPrintProcessor(file);
		if (!(processor instanceof Previewable)) {
			throw new IllegalArgumentException(processor.getFriendlyName() + " files don't support image preview.");
		}
		
		AbstractPrintFileProcessor previewableProcessor = (AbstractPrintFileProcessor) processor;
		BufferedImage img = previewableProcessor.buildPreviewSlice(customizer, file, (Previewable)processor);
		printer.setStatus(printer.getStatus());//This is to make sure the slicenumber is reset.
		printer.showImage(img);
	}
	
	@ApiOperation(value="Renders a slice of a printable based on the customizer")
	    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("renderPreviewImage/{customizerName}")
	@Produces("image/png")
	public StreamingOutput renderFirstSliceImage(@PathParam("customizerName") String customizerName) throws NoPrinterFoundException, SliceHandlingException {
		Customizer customizer = customizersByName.getIfPresent(customizerName);
		if (customizer == null) {
			throw new IllegalArgumentException("Customizer is missing");
		}
		
		File file = new File(HostProperties.Instance().getUploadDir(), customizer.getPrintableName() + "." + customizer.getPrintableExtension());
		PrintFileProcessor<?,?> processor = PrintFileFilter.INSTANCE.findAssociatedPrintProcessor(file);
		if (!(processor instanceof Previewable)) {
			throw new IllegalArgumentException(processor.getFriendlyName() + " files don't support image preview.");
		}
		
		AbstractPrintFileProcessor previewableProcessor = (AbstractPrintFileProcessor) processor;
		try {
			BufferedImage img = previewableProcessor.buildPreviewSlice(customizer, file, (Previewable)processor);

			logger.debug("just got the bufferedimg from previewSlice");

			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					try {
						ImageIO.write(img, "png", output);

						logger.debug("Writing the img");
					} catch (IOException e) {
						throw e;
					}
				}
			};
			return stream;
		} catch (NoPrinterFoundException|SliceHandlingException |IllegalArgumentException e) {
			throw e;
		}
	}
}
