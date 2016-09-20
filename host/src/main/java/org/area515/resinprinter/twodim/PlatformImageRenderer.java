package org.area515.resinprinter.twodim;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.render.CurrentImageRenderer;
import org.area515.resinprinter.printer.SlicingProfile.TwoDimensionalSettings;
import org.area515.util.TemplateEngine;

public class PlatformImageRenderer extends CurrentImageRenderer {
	private int totalPlatformSlices;
	private TwoDimensionalImageRenderer extrusionImageRenderer;
	
	public PlatformImageRenderer(DataAid aid, AbstractPrintFileProcessor<?,?> processor, Object imageIndexToBuild, int totalPlatformSlices, TwoDimensionalImageRenderer extrusionImageRenderer) {
		super(aid, processor, imageIndexToBuild);
		this.totalPlatformSlices = totalPlatformSlices;
		this.extrusionImageRenderer = extrusionImageRenderer;
	}
	
	@Override
	public BufferedImage renderImage(BufferedImage imageToDisplay) throws JobManagerException {
		if (imageToDisplay == null) {
			imageToDisplay = extrusionImageRenderer.call().getPrintableImage();
		}
		
		int centerX = aid.xResolution / 2;
		int centerY = aid.yResolution / 2;

		Graphics graphics = imageToDisplay.getGraphics();
		graphics.setColor(Color.black);
		graphics.fillRect(0, 0, imageToDisplay.getWidth(), imageToDisplay.getHeight());
		graphics.setColor(Color.white);
		
		Map<String, Object> overrides = new HashMap<>();
		overrides.put("platformGraphics", graphics);
		overrides.put("platformRaster", imageToDisplay.getRaster());
		overrides.put("extrusionX", imageToDisplay.getWidth());
		overrides.put("extrusionY", imageToDisplay.getHeight());
		overrides.put("resolutionX", aid.xResolution);
		overrides.put("resolutionY", aid.yResolution);
		overrides.put("centerX", centerX);
		overrides.put("centerY", centerY);
		overrides.put("totalPlatformSlices", totalPlatformSlices);
		
		TwoDimensionalSettings settings = aid.slicingProfile.getTwoDimensionalSettings();
		if (settings == null) {
			throw new IllegalArgumentException("This printer doesn't have it's 2D File Settings setup properly");
		}
		
		String platformScript = settings.getPlatformCalculator();
		if (platformScript == null) {
			throw new IllegalArgumentException("This printer doesn't have 2D platform rendering calculator setup");
		}

		try {
			TemplateEngine.runScript(aid.printJob, aid.printer, aid.scriptEngine, platformScript, "2D Platform rendering script", overrides);
		} catch (ScriptException e) {
			throw new JobManagerException("Error while executing platform rendering script.", e);
		}
		return imageToDisplay;
	}
}
