package org.area515.resinprinter.printphoto.micoin;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.render.RenderedData;
import org.area515.resinprinter.twodim.TwoDimensionalImageRenderer;
import org.area515.util.Log4jUtil;

public class CoinRenderer extends TwoDimensionalImageRenderer {
	private static final Logger logger = LogManager.getLogger();
	private static final String FIRST_IMAGE = "FirstImage";
	private static final String MIDDLE_IMAGE = "MiddleImage";
	private static final String LAST_IMAGE = "LastImage";
	
	public CoinRenderer(DataAid aid, AbstractPrintFileProcessor<?, ?> processor, Object imageIndexToBuild) {
		super(aid, processor, imageIndexToBuild);
	}
	
	public BufferedImage renderImage(BufferedImage imageToDisplay) throws JobManagerException {
		if (imageToDisplay == null) {
			imageToDisplay = scaleImageAndDetectEdges(aid.printJob);
			RenderedData firstData = aid.cache.getOrCreateIfMissing(FIRST_IMAGE);
			RenderedData middleData = aid.cache.getOrCreateIfMissing(MIDDLE_IMAGE);
			RenderedData lastData = aid.cache.getOrCreateIfMissing(LAST_IMAGE);
			int width = imageToDisplay.getWidth();
			int height = imageToDisplay.getHeight();
			firstData.setPreTransformedImage(imageToDisplay.getSubimage(0, 0, width/3, height));
			middleData.setPreTransformedImage(imageToDisplay.getSubimage(width/3, 0, width/3, height));
			lastData.setPreTransformedImage(imageToDisplay.getSubimage(width * 2/3, 0, width/3, height));
			
			logger.trace("Writing scaleImageAndDetectEdges1" + FIRST_IMAGE + ":{}", () -> Log4jUtil.logImage(firstData.getPreTransformedImage(), "scaleImageAndDetectEdges1" + FIRST_IMAGE + ".png"));
			logger.trace("Writing scaleImageAndDetectEdges2" + MIDDLE_IMAGE + ":{}", () -> Log4jUtil.logImage(middleData.getPreTransformedImage(), "scaleImageAndDetectEdges2" + MIDDLE_IMAGE + ".png"));
			logger.trace("Writing scaleImageAndDetectEdges3" + LAST_IMAGE + ":{}", () -> Log4jUtil.logImage(lastData.getPreTransformedImage(), "scaleImageAndDetectEdges3" + LAST_IMAGE + ".png"));
		}
		
		CoinFileProcessor processor = (CoinFileProcessor)aid.printJob.getPrintFileProcessor();
		
		int extrusionCount = processor.getSuggested2DExtrusionLayerCount(aid);
		int platformCount = processor.getSuggestedPlatformLayerCount(aid);
		int imageIndex = aid.printJob.getCurrentSlice();
		
		if (imageIndex < extrusionCount) {
			logger.trace("Writing call1" + FIRST_IMAGE + ":{}", () -> Log4jUtil.logImage(aid.cache.getOrCreateIfMissing(FIRST_IMAGE).getPreTransformedImage(), "call1" + FIRST_IMAGE + ".png"));
			return aid.cache.getOrCreateIfMissing(FIRST_IMAGE).getPreTransformedImage();
		}
		
		if (imageIndex < extrusionCount + platformCount) {
			logger.trace("Writing call2" + MIDDLE_IMAGE + ":{}", () -> Log4jUtil.logImage(aid.cache.getOrCreateIfMissing(MIDDLE_IMAGE).getPreTransformedImage(), "call2" + MIDDLE_IMAGE + ".png"));
			return aid.cache.getOrCreateIfMissing(MIDDLE_IMAGE).getPreTransformedImage();
		}
		
		logger.trace("Writing call3" + LAST_IMAGE + ":{}", () -> Log4jUtil.logImage(aid.cache.getOrCreateIfMissing(LAST_IMAGE).getPreTransformedImage(), "call3" + LAST_IMAGE + ".png"));
		return aid.cache.getOrCreateIfMissing(LAST_IMAGE).getPreTransformedImage();
	}
	
	public BufferedImage loadImageFromFile(PrintJob job) throws JobManagerException {
		try {
			return ImageIO.read(job.getJobFile());
		} catch (IOException e) {
			throw new JobManagerException("Couldn't load image file:" + job.getJobFile(), e);
		}
	}
}
