package org.area515.resinprinter.job.render;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.JobManagerException;

public class StandaloneImageRenderer implements Callable<StandaloneImageData> {
	private static final Logger logger = LogManager.getLogger();

	private File imageFile;
	private AbstractPrintFileProcessor<?,?> processor;
	private DataAid aid;
	
	public StandaloneImageRenderer(DataAid aid, File imageFile, AbstractPrintFileProcessor<?,?> processor) {
		this.imageFile = imageFile;
		this.processor = processor;
		this.aid = aid;
	}
	
	public StandaloneImageData call() throws ScriptException, JobManagerException, IOException {
		long startTime = System.currentTimeMillis();
		BufferedImage image = ImageIO.read(imageFile);
		long pixelArea = computePixelArea(image);
		image = processor.applyImageTransforms(aid, image, image.getWidth(), image.getHeight());
		// processor.applyBulbMask(aid, (Graphics2D)image.getGraphics(), image.getWidth(), image.getHeight());
		//WILBUR:
		//processor.applyImageTransforms(aid, image, image.getWidth(), image.getHeight());
		logger.info("Loaded {}  with {} non-black pixels in {}ms", imageFile.getName(), pixelArea, System.currentTimeMillis()-startTime);
		return new StandaloneImageData(image, pixelArea);
	}
	
	/**
	 * Compute the number of non-black pixels in an image as a measure of its
	 * area as a pixel count. We can only handle 3 and 4 byte formats though.
	 * 
	 * @param image
	 * @return
	 */
	private static long computePixelArea(BufferedImage image) throws JobManagerException {
		int type = image.getType();
		if (type != BufferedImage.TYPE_3BYTE_BGR
				&& type != BufferedImage.TYPE_4BYTE_ABGR
				&& type != BufferedImage.TYPE_4BYTE_ABGR_PRE
				&& type != BufferedImage.TYPE_BYTE_GRAY) {
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
		
		// except for TYPE_BYTE_GRAY, where the pixel is just one byte
		if (type == BufferedImage.TYPE_BYTE_GRAY) {
			pixLen = 1;
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
			} else if (pixLen == 1) {
				if (pixels[i] != 0) {
					area++;
				}
			}
		}
		
		return area;
	}

}