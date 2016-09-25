package org.area515.resinprinter.job;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Future;

import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.exception.SliceHandlingException;
import org.area515.resinprinter.job.render.RenderedData;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.twodim.SimpleImageRenderer;

import se.sawano.java.text.AlphanumericComparator;

public class ZipImagesFileProcessor extends CreationWorkshopSceneFileProcessor implements Previewable {
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
	
			// Preload first image then loop
			if (imgIter.hasNext()) {
				File imageFile = null;
				int sliceIndex = dataAid.customizer.getNextSlice();
				while (imgIter.hasNext() && sliceIndex > 0) {
					sliceIndex--;
					imgIter.next();
				}
				
				Future<RenderedData> prepareImage = Main.GLOBAL_EXECUTOR.submit(new SimpleImageRenderer(dataAid, this, imageFile));
				boolean slicePending = true;
				
				do {
	
					JobStatus status = performPreSlice(dataAid, null);
					if (status != null) {
						return status;
					}
					
					RenderedData imageData = prepareImage.get();
					if (imgIter.hasNext()) {
						imageFile = imgIter.next();
						prepareImage = Main.GLOBAL_EXECUTOR.submit(new SimpleImageRenderer(dataAid, this, imageFile));
					} else {
						slicePending = false;
					}

					status = printImageAndPerformPostProcessing(dataAid, imageData.getPrintableImage());

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
	public BufferedImage renderPreviewImage(DataAid dataAid) throws SliceHandlingException {
		try {
			prepareEnvironment(dataAid.printJob.getJobFile(), dataAid.printJob);
			
			SortedMap<String, File> imageFiles = findImages(dataAid.printJob.getJobFile());
			
			dataAid.printJob.setTotalSlices(imageFiles.size());
			Iterator<File> imgIter = imageFiles.values().iterator();
	
			// Preload first image then loop
			int sliceIndex = dataAid.customizer.getNextSlice();
			while (imgIter.hasNext() && sliceIndex > 0) {
				sliceIndex--;
				imgIter.next();
			}
			
			if (!imgIter.hasNext()) {
				throw new IOException("No Image Found for index:" + dataAid.customizer.getNextSlice());
			}
			File imageFile = imgIter.next();
			
			SimpleImageRenderer renderer = new SimpleImageRenderer(dataAid, this, imageFile);
			RenderedData stdImage = renderer.call();
			return stdImage.getPrintableImage();
		} catch (IOException | JobManagerException e) {
			throw new SliceHandlingException(e);
		}
	}

	@Override
	public String getFriendlyName() {
		return "Zip of Slice Images";
	}
	
	private SortedMap<String, File> findImages(File jobFile) throws JobManagerException {
		String [] extensions = {"png", "PNG"};
		boolean recursive = true;
		
		Collection<File> files =
				FileUtils.listFiles(buildExtractionDirectory(jobFile.getName()),
				extensions, recursive);

		TreeMap<String, File> images = new TreeMap<>(new AlphanumericComparator());

		for (File file : files) {
			images.put(file.getName(), file);
		}
		
		return images;
	}
}
