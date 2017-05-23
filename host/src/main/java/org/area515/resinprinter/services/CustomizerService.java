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
import java.util.concurrent.ExecutionException;

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
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.Customizer;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.Previewable;
import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.util.security.PhotonicUser;
import org.area515.util.PrintFileFilter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Api(value="customizers")
@RolesAllowed(PhotonicUser.FULL_RIGHTS)
@Path("customizers")
public class CustomizerService {
	private static final Logger logger = LogManager.getLogger();
    public static CustomizerService INSTANCE = new CustomizerService();
    
    //TODO: Should this be a cache? Aren't these really small objects now without the original BufferedImage embedded in them?
    private Cache<String, Customizer> customizersByName = CacheBuilder.newBuilder().softValues().build();
    
    /*
     * This cache is only for previewing purposes:
     *  1. It allows the web to store BufferedImage cache
     *  2. It also allows us to use a more realistic way of previewing images.
     *  3. We don't have to create a DataAid for every preview of an image now...
     */
	private LoadingCache<Customizer, DataAid> dataAidsByCustomizer = CacheBuilder.newBuilder().softValues().build(new CacheLoader<Customizer, DataAid>() {
	     public DataAid load(Customizer customizer) throws JobManagerException, NoPrinterFoundException, InappropriateDeviceException {
	 		//find the first activePrinter
	    	Printer activePrinter = null;
	 		String printerName = customizer.getPrinterName();
	 		if (printerName == null || printerName.isEmpty()) {
	 			//if customizer doesn't have a printer stored, set first active printer as printer
	 			try {
	 				activePrinter = PrinterService.INSTANCE.getFirstAvailablePrinter();
	 			} catch (NoPrinterFoundException e) {
	 				throw new NoPrinterFoundException("No printers found for slice preview. You must have a started printer or specify a valid printer in the Customizer.");
	 			}
	 			
	 		} else {
	 			try {
	 				activePrinter = PrinterService.INSTANCE.getPrinter(printerName);
	 			} catch (InappropriateDeviceException e) {
	 				logger.warn("Could not locate printer {}", printerName, e);
	 			}
	 		}
			
			File file = new File(HostProperties.Instance().getUploadDir(), customizer.getPrintableName() + "." + customizer.getPrintableExtension());
			PrintFileProcessor<?,?> processor = PrintFileFilter.INSTANCE.findAssociatedPrintProcessor(file);
			if (!(processor instanceof Previewable)) {
				if (processor == null) {
					throw new IllegalArgumentException("Couldn't find file processor for file:" + file);
				}
				
				throw new IllegalArgumentException(processor.getFriendlyName() + " files don't support image preview.");
			}
			
			if (!(processor instanceof AbstractPrintFileProcessor)) {
				throw new IllegalArgumentException("Processor:" + processor.getFriendlyName() + " needs to extend AbstractPrintFileProcessor");
			}
			
	 		//instantiate a new print job based on the jobFile and set its printer to activePrinter
	 		PrintJob printJob = new PrintJob(new File(HostProperties.Instance().getUploadDir(), customizer.getPrintableName() + "." + customizer.getPrintableExtension()));
	 		printJob.setPrinter(activePrinter);
	 		printJob.setCustomizer(customizer);
	 		printJob.setPrintFileProcessor(processor);
	 		printJob.setCurrentSlice(customizer.getNextSlice());

			return ((AbstractPrintFileProcessor<?,?>)processor).initializeJobCacheWithDataAid(printJob);
	       }
	     });

    @ApiOperation(value="Retrieves all Customizers")
	@GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
	public Map<String, Customizer> getCustomizers() {
		return customizersByName.asMap();
	}
    
    //TODO: Why don't we read this from disk if it doesn't exist first?
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
				dataAidsByCustomizer.invalidate(customizer);
				//TODO: start building image in a background task before the client asks for it!
			}
			customizer.setExternalImageAffectingState(externalState);
		}
		
		return customizer;
	}
    
    //TODO: Why don't we save this to disk as an async action?
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
				try {
					DataAid oldAid = dataAidsByCustomizer.get(oldCustomizer);
					dataAidsByCustomizer.put(customizer, oldAid);
					oldAid.customizer = customizer;
					oldAid.clearAffineTransformCache();
					
					//TODO: do we have to do this? Shouldn't this be done naturally through the soft references?
					dataAidsByCustomizer.invalidate(oldCustomizer);
				} catch (ExecutionException e) {
					logger.error("Couldn't create dataAid:", e);
				}
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

		try {
			DataAid aid = dataAidsByCustomizer.get(customizer);
			AbstractPrintFileProcessor<?,?> previewableProcessor = (AbstractPrintFileProcessor<?,?>)aid.printJob.getPrintFileProcessor();
			BufferedImage img = previewableProcessor.buildPreviewSlice(customizer, dataAidsByCustomizer.get(customizer));
			printer.setStatus(printer.getStatus());//This is to make sure the slicenumber is reset.
			printer.showImage(img, true);
		} catch (ExecutionException e) {
			throw new IllegalArgumentException("Couldn't build data aid", e);
		}
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
			throw new IllegalArgumentException("Customizer is missing for:" + customizerName);
		}

		try {
			DataAid aid = dataAidsByCustomizer.get(customizer);
			AbstractPrintFileProcessor<?,?> previewableProcessor = (AbstractPrintFileProcessor<?,?>)aid.printJob.getPrintFileProcessor();
			BufferedImage img = previewableProcessor.buildPreviewSlice(customizer, aid);

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
		} catch (ExecutionException e) {
			throw new SliceHandlingException(e.getCause());
		} catch (NoPrinterFoundException|SliceHandlingException |IllegalArgumentException e) {
			throw e;
		}
	}
}
