package org.area515.resinprinter.job;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.area515.resinprinter.job.render.CurrentImageRenderer;
import org.area515.resinprinter.job.render.RenderingFileData;
import org.area515.resinprinter.job.render.StandaloneImageData;
import org.area515.resinprinter.printer.BuildDirection;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.slice.ZSlicer;
import org.area515.resinprinter.stl.Triangle3d;

public class ZipImagesFileProcessor extends CreationWorkshopSceneFileProcessor {
	private Map<PrintJob, StandaloneImageData> currentImageByJob = new HashMap<>();

	@Override
	public String[] getFileExtensions() {
		return new String[]{"imgzip"};
	}

	@Override
	public boolean acceptsFile(File processingFile) {
		return processingFile.getName().toLowerCase().endsWith(".imgzip");
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
		DataAid dataAid = initializeDataAid(printJob);

		SortedMap<String, File> imageFiles = findImages(printJob.getJobFile());
		
		printJob.setTotalSlices(imageFiles.size());

		performHeader();

		for (File imageFile : imageFiles.values()) {
			// Not really necessary, but a starting point for more logic later to allow
			// loading and masking to occur during the previous layer's lifting
			Future<StandaloneImageData> prepareImage = Main.GLOBAL_EXECUTOR.submit(() -> {
					BufferedImage image = ImageIO.read(imageFile);
					long pixelArea = computePixelArea(image);
					applyBulbMask((Graphics2D)image.getGraphics(), image.getWidth(), image.getHeight());
					return new StandaloneImageData(image, pixelArea);
			});
			
			JobStatus status = performPreSlice(null);
			if (status != null) {
				return status;
			}
			
			StandaloneImageData oldImage = currentImageByJob.get(printJob);
			StandaloneImageData imageData = prepareImage.get();
			currentImageByJob.put(printJob, imageData);
			
			dataAid.printer.showImage(imageData.getImage());
			
			status = performPostSlice();
			if (status != null) {
				return status;
			}
		}
		return performFooter();
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

		TreeMap<String, File> images = new TreeMap<>();

		for (File file : files) {
			images.put(file.getName(), file);
		}
		
		return images;
	}

	/**
	 * Compute the number of non-black pixels in an image as a measure of its
	 * area as a pixel count. We can only handle 3 and 4 byte formats though.
	 * 
	 * @param image
	 * @return
	 */
	private long computePixelArea(BufferedImage image) throws JobManagerException {
		int type = image.getType();
		if (type != BufferedImage.TYPE_3BYTE_BGR
				&& type != BufferedImage.TYPE_4BYTE_ABGR
				&& type != BufferedImage.TYPE_4BYTE_ABGR_PRE) {
			// BufferedImage is not any of the types that are currently supported.
			throw(new JobManagerException(
					"Slice image is not in a 3 or 4 byte BGR/ABGR format."
					+"Please open an issue about this and let us you know have an image of type: "
					+type)
					);
		}
		
		long area = 0;
		
		// We only need a count pixels, without regard to the X,Y orientation,
		// so use the method described at:
		// http://stackoverflow.com/questions/6524196/java-get-pixel-array-from-image
		// to get the byte buffer backing the BufferedImage and iterate through it
		boolean hasAlpha = image.getAlphaRaster() != null;
		byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		
		// Pixels are in groups of 3 if there is no alpha, 4 if there is an alpha
		int pixLen = 3;
		if (hasAlpha) {
			pixLen = 4;
		}
		
		// Iterate linearly across the pixels, summing up cases where the color
		// is not black (e.g. any color channel nonzero)
		for (int i = 0; i<pixels.length; i+=pixLen) {
			if (pixLen == 3) {
				if (pixels[i] != 0 || pixels[i+1] != 0 || pixels[i+2] != 0) {
					area++;
				}
			} else if (pixLen == 4) {
				if (pixels[i+1] != 0 || pixels[i+2] != 0 || pixels[i+3] != 0) {
					area++;
				}
			}
		}
		
		return area;
	}
}
