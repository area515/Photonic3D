package org.area515.resinprinter.job;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.io.FileUtils;
import org.area515.resinprinter.job.render.StandaloneImageData;
import org.area515.resinprinter.job.render.StandaloneImageRenderer;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.server.Main;
import org.area515.util.Log4jTimer;

import se.sawano.java.text.AlphanumericComparator;

public class ZipImagesFileProcessor extends CreationWorkshopSceneFileProcessor {
	private static final Logger logger = LogManager.getLogger();

	private Map<PrintJob, StandaloneImageData> currentImageByJob = new HashMap<>();

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
	public Double getBuildAreaMM(PrintJob printJob) {
		StandaloneImageData curSliceImg = currentImageByJob.get(printJob);
		if (curSliceImg == null) {
			return null;
		}
		
		SlicingProfile slicingProfile = printJob.getPrinter().getConfiguration().getSlicingProfile();
		return curSliceImg.getArea() / (slicingProfile.getDotsPermmX() * slicingProfile.getDotsPermmY());
	}
	
	@Override
	public BufferedImage getCurrentImage(PrintJob printJob) {
		StandaloneImageData data = currentImageByJob.get(printJob);
		if (data == null) {
			return null;
		}
		
		synchronized (data) {
			BufferedImage currentImage = data.getImage();
			if (currentImage == null)
				return null;
			
			return currentImage.getSubimage(0, 0, currentImage.getWidth(), currentImage.getHeight());
		}
	}

	@Override
	public JobStatus processFile(PrintJob printJob) throws Exception {
		try {
			DataAid dataAid = initializeDataAid(printJob);
	
			SortedMap<String, File> imageFiles = findImages(printJob.getJobFile());
			
			printJob.setTotalSlices(imageFiles.size());
	
			performHeader(dataAid);
	
			Iterator<File> imgIter = imageFiles.values().iterator();
	
			// Preload first image then loop
			if (imgIter.hasNext()) {
				File imageFile = imgIter.next();
				
				Future<StandaloneImageData> prepareImage =
						Main.GLOBAL_EXECUTOR.submit(new StandaloneImageRenderer(dataAid, imageFile, this));
				boolean slicePending = true;
				
				do {
	
					JobStatus status = performPreSlice(dataAid, null);
					if (status != null) {
						return status;
					}
					
					StandaloneImageData oldImage = currentImageByJob.get(printJob);
					StandaloneImageData imageData = prepareImage.get();
					currentImageByJob.put(printJob, imageData);
					
					//Start the exposure timer
					logger.info("ExposureStart:{}", ()->Log4jTimer.completeTimer(EXPOSURE_TIMER));
					
					dataAid.printer.showImage(imageData.getImage());
					
					if (oldImage != null) {
						oldImage.getImage().flush();
					}
					
					if (imgIter.hasNext()) {
						imageFile = imgIter.next();
						prepareImage = Main.GLOBAL_EXECUTOR.submit(new StandaloneImageRenderer(dataAid, imageFile, this));
					} else {
						slicePending = false;
					}
					
					status = performPostSlice(dataAid);
					if (status != null) {
						return status;
					}
				} while (slicePending);
			}
			
			return performFooter(dataAid);
		} finally {
			currentImageByJob.remove(printJob);
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
