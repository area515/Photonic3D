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
import java.io.FileReader;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.twodim.TwoDimensionalPlatformPrintFileProcessor;

public class TextFilePrintFileProcessor extends TwoDimensionalPlatformPrintFileProcessor<Object> {
    private static final Logger logger = LogManager.getLogger();
	
	private class TextData implements TwoDimensionalPrintState {
		Object[] lines;
		BufferedImage currentImage;
		
		@Override
		public BufferedImage getCurrentImage() {
			return currentImage;
		}
		
		@Override
		public void setCurrentImage(BufferedImage image) {
			currentImage = image;
		}
		
		@Override
		public BufferedImage buildImplementationImage(DataAid data, Graphics2D graphics) {
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font font = buildFont();
			graphics.setFont(font);
			FontMetrics metrics = graphics.getFontMetrics();
			double maxWidth = 0;
			double totalHeight = 0;
	        for (int t = 0; t < lines.length; t++) {
	        	String currentLine = ((String)lines[t]);
	        	Rectangle2D rect = metrics.getStringBounds(currentLine.toCharArray(), 0, currentLine.length(), graphics);
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

	@Override
	public void prepareEnvironment(File processingFile, PrintJob printJob) throws JobManagerException {
		try (BufferedReader reader = new BufferedReader(new FileReader(processingFile))) {
			TextData textData = new TextData();
			textData.lines = reader.lines().toArray();
			createTwoDimensionlPrintState(printJob, textData);
		} catch (IOException e) {
			logger.error("IO error while reading file:" + processingFile, e);
			throw new JobManagerException("There was a problem reading this file.");
		}
	}

	@Override
	public void cleanupEnvironment(File processingFile) throws JobManagerException {
		//Nothing to cleanup everything is done in memory.
	}

	@Override
	public Object getGeometry(PrintJob printJob) throws JobManagerException {
		return null;
	}

	@Override
	public String getFriendlyName() {
		return "Simple Text";
	}
}
