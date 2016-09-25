package org.area515.resinprinter.job.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.JobManagerException;

public abstract class CurrentImageRenderer implements Callable<RenderedData> {
	private static final Logger logger = LogManager.getLogger();
	protected Object imageIndexToBuild;
	protected AbstractPrintFileProcessor<?,?> processor;
	protected DataAid aid;
	
	public CurrentImageRenderer(DataAid aid, AbstractPrintFileProcessor<?,?> processor, Object imageIndexToBuild) {
		this.aid = aid;
		this.processor = processor;
		this.imageIndexToBuild = imageIndexToBuild;
	}
	
	public BufferedImage buildImage(int renderedWidth, int renderedHeight) {
		return new BufferedImage(renderedWidth, renderedHeight, BufferedImage.TYPE_4BYTE_ABGR);
	}
	
	/*public BufferedImage buildLargestImageBetweenPrinterAndRenderedImage(int renderedWidth, int renderedHeight) {
		int actualWidth = renderedWidth > aid.xResolution?renderedWidth:aid.xResolution;
		int actualHeight = renderedHeight > aid.yResolution?renderedHeight:aid.yResolution;
		return new BufferedImage(actualWidth, actualHeight, BufferedImage.TYPE_4BYTE_ABGR);
	}
	
	public BufferedImage buildLargestImageBetweenPrinterAndRenderedImage(BufferedImage renderedImage) {
		if (renderedImage.getWidth() < aid.xResolution ||
			renderedImage.getHeight() < aid.yResolution) {
				BufferedImage newSize = buildLargestImageBetweenPrinterAndRenderedImage(renderedImage.getWidth(), renderedImage.getHeight());
				Graphics graphics = newSize.getGraphics();
				graphics.setColor(Color.black);
				graphics.fillRect(0, 0, newSize.getWidth(), newSize.getHeight());
				graphics.setColor(Color.white);
				int centerX = aid.xResolution / 2;
				int centerY = aid.yResolution / 2;
				graphics.drawImage(renderedImage, centerX - (newSize.getWidth() / 2), centerY - (newSize.getHeight() / 2), null);
				return newSize;
		}
		
		return renderedImage;
	}*/
	
	public RenderedData call() throws JobManagerException {
		long startTime = System.currentTimeMillis();
		Lock lock = aid.cache.getSpecificLock(imageIndexToBuild);
		lock.lock();
		try {
			RenderedData imageData = aid.cache.get(imageIndexToBuild);
			BufferedImage image = renderImage(imageData.getPreTransformedImage());
			imageData.setPreTransformedImage(image);
			BufferedImage after = processor.applyImageTransforms(aid, image);
			imageData.setPrintableImage(after);
			if (!aid.optimizeWithPreviewMode) {
				long pixelArea = computePixelArea(image);
				imageData.setArea((double)pixelArea);
				logger.info("Loaded {}  with {} non-black pixels in {}ms", imageIndexToBuild, pixelArea, System.currentTimeMillis()-startTime);
			}
			return imageData;
		} catch (ScriptException e) {
			logger.error(e);
			throw new JobManagerException("Unable to render image", e);
		} finally {
			lock.unlock();
		}
	}
	
	abstract public BufferedImage renderImage(BufferedImage image) throws JobManagerException;

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