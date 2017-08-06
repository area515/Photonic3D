package org.area515.resinprinter.job;

import java.io.File;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.job.render.RenderingContext;

public class ZipImagesFileProcessor extends CreationWorkshopSceneFileProcessor {
	private static final Logger logger = LogManager.getLogger();

	@Override
	public String[] getFileExtensions() {
		return new String[]{"imgzip"};
	}
	
	@Override
	public boolean acceptsFile(File processingFile) {
		if (processingFile.getName().toLowerCase().endsWith(".imgzip") || processingFile.getName().toLowerCase().endsWith(".zip")) {
			if (zipHasGCode(processingFile) == false) {
				// if the zip does not have GCode, treat it as a zip of pngs
				logger.info("Accepting new printable {} as a {}", processingFile.getName(), this.getFriendlyName());
				return true;
			}
		}
		return false;
	}
	
	@Override
	public JobStatus processFile(PrintJob printJob) throws Exception {
		try {
			DataAid dataAid = initializeJobCacheWithDataAid(printJob);

			SortedMap<String, File> imageFiles = findImages(printJob.getJobFile());

			printJob.setTotalSlices(imageFiles.size());

			performHeader(dataAid);

			Iterator<File> imgIter = imageFiles.values().iterator();
			
			// Iterate the image stack up to the slice index requested by the customizer
			if (imgIter.hasNext()) {
				int sliceIndex = dataAid.customizer.getNextSlice();
				while (imgIter.hasNext() && sliceIndex > 0) {
					sliceIndex--;
					imgIter.next();
				}
			}

			// Preload first image then loop
			if (imgIter.hasNext()) {
				File imageFile = imgIter.next();
				Future<RenderingContext> prepareImage = startImageRendering(dataAid, imageFile);
				boolean slicePending = true;

				do {

					JobStatus status = performPreSlice(dataAid, dataAid.currentlyRenderingImage.getScriptEngine(), null);
					if (status != null) {
						return status;
					}

					RenderingContext imageData = prepareImage.get();
					dataAid.cache.setCurrentRenderingPointer(imageFile);
					
					if (imgIter.hasNext()) {
						imageFile = imgIter.next();
						prepareImage = startImageRendering(dataAid, imageFile);
					} else {
						slicePending = false;
					}

					status = printImageAndPerformPostProcessing(dataAid, imageData.getScriptEngine(), imageData.getPrintableImage());

					if (status != null) {
						return status;
					}
				} while (slicePending);
			}

			return performFooter(dataAid);
		} finally {
			clearDataAid(printJob);
		}
	}
	
	@Override
	public Double getBuildAreaMM(PrintJob processingFile) {
		DataAid aid = getDataAid(processingFile);
		aid.cache.getOrCreateIfMissing(aid.cache.getCurrentRenderingPointer());
		if (aid == null || aid.cache.getCurrentArea() == null) {
			return null;
		}
		
		return aid.cache.getCurrentArea() / (aid.xPixelsPerMM * aid.yPixelsPerMM);
	}

	@Override
	public String getFriendlyName() {
		return "Zip of Slice Images";
	}
}
