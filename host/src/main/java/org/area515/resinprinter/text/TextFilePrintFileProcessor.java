package org.area515.resinprinter.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.exception.SliceHandlingException;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.STLImageRenderer;
import org.area515.resinprinter.job.render.RenderingFileData;
import org.area515.resinprinter.slice.CloseOffMend;
import org.area515.resinprinter.slice.ZSlicer;
import org.area515.resinprinter.twodim.RenderExtrusionImage;
import org.area515.resinprinter.twodim.TwoDimensionalPlatformPrintFileProcessor;
import org.area515.resinprinter.twodim.TwoDimensionalPlatformPrintFileProcessor.TwoDimensionalPrintState;

public class TextFilePrintFileProcessor extends TwoDimensionalPlatformPrintFileProcessor<Object, Object> {
    private static final Logger logger = LogManager.getLogger();
	
	private class TextData extends TwoDimensionalPrintState {
		Object[] lines;
		
		@Override
		public BufferedImage buildExtrusionImage(DataAid aid) {
	        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);
	        Graphics2D chickenEggGraphics = img.createGraphics();

	        chickenEggGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	        chickenEggGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font font = buildFont(aid);
			chickenEggGraphics.setFont(font);
			FontMetrics metrics = chickenEggGraphics.getFontMetrics();
			double maxWidth = 0;
			double totalHeight = 0;
	        for (int t = 0; t < lines.length; t++) {
	        	String currentLine = ((String)lines[t]);
	        	Rectangle2D rect = metrics.getStringBounds(currentLine.toCharArray(), 0, currentLine.length(), chickenEggGraphics);
	        	if (rect.getWidth() > maxWidth) {
	        		maxWidth = rect.getWidth();
	        	}
	        	totalHeight += rect.getHeight();
	        }
	        
			BufferedImage textImage = new BufferedImage((int)maxWidth, (int)totalHeight, BufferedImage.TYPE_INT_ARGB_PRE);
			Graphics2D textGraphics = (Graphics2D)textImage.getGraphics();
			textGraphics.setFont(font);
			textGraphics.setColor(Color.WHITE);
			textGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			textGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	        for (int t = 0; t < lines.length; t++) {
	        	String currentLine = ((String)lines[t]);
	        	textGraphics.drawString(currentLine, 0, metrics.getAscent() + (t * metrics.getHeight()));
	        }
	        
	        return textImage;
		}
	}
	
	@Override
	public String[] getFileExtensions() {
		return new String[]{"txt"};
	}
	
	@Override
	public boolean acceptsFile(File processingFile) {
		return processingFile.getName().toLowerCase().endsWith("txt");
	}
	
	private void readTextDataFromFile(TextData textData, File processingFile, PrintJob printJob) throws JobManagerException {
		try (BufferedReader reader = new BufferedReader(new FileReader(processingFile))) {
			textData.lines = reader.lines().toArray();
		} catch (IOException e) {
			logger.error("IO error while reading file:" + processingFile, e);
			throw new JobManagerException("There was a problem reading this file.");
		}
	}
	
	@Override
	public void prepareEnvironment(File processingFile, PrintJob printJob) throws JobManagerException {
		TextData textData = new TextData();
		readTextDataFromFile(textData, processingFile, printJob);
		createTwoDimensionalPrintState(printJob, textData);
	}

	@Override
	public void cleanupEnvironment(File processingFile) throws JobManagerException {
		//Nothing to cleanup everything is done in memory.
	}

	@Override
	public Object getGeometry(PrintJob printJob) throws JobManagerException {
		throw new JobManagerException("You can't get geometry from this type of file");
	}

	@Override
	public Object getErrors(PrintJob printJob) throws JobManagerException {
		throw new JobManagerException("You can't get error geometry from this type of file");
	}

	@Override
	public String getFriendlyName() {
		return "Simple Text";
	}

	@Override
	public BufferedImage renderPreviewImage(final org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid dataAid) throws SliceHandlingException {
		try {
			//We need to avoid caching images outside of the Customizer cache or we will fill up memory quick
			TextData data = new TextData() {
				@Override
				public BufferedImage getCachedExtrusionImage() {
					return super.buildExtrusionImage(dataAid);
				}
			};
			readTextDataFromFile(data, dataAid.printJob.getJobFile(), dataAid.printJob);
			data.setCurrentRenderingPointer(Boolean.TRUE);
			RenderExtrusionImage extrusion = new RenderExtrusionImage(dataAid, this, data, Boolean.TRUE, dataAid.xResolution, dataAid.yResolution);
			return extrusion.call();
		} catch (JobManagerException | ScriptException e) {
			throw new SliceHandlingException(e);
		}
	}

	@Override
	public boolean isThreeDimensionalGeometryAvailable() {
		return false;
	}
}
