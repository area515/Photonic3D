package org.area515.resinprinter.twodim;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.render.CurrentImageRenderer;
import org.area515.resinprinter.printer.SlicingProfile.TwoDimensionalSettings;
import org.area515.util.TemplateEngine;

public class RenderPlatformImage extends CurrentImageRenderer {
	private int totalPlatformSlices;
	
	public RenderPlatformImage(DataAid aid, AbstractPrintFileProcessor<?,?> processor, Object imageIndexToBuild, int totalPlatformSlices) {
		super(aid, processor, imageIndexToBuild);
		this.totalPlatformSlices = totalPlatformSlices;
	}
	
	@Override
	public BufferedImage renderImage(BufferedImage imageToDisplay) throws ScriptException {
		if (imageToDisplay == null) {
			imageToDisplay = new BufferedImage(aid.xResolution, aid.yResolution, BufferedImage.TYPE_4BYTE_ABGR);
		}
		
		int centerX = aid.xResolution / 2;
		int centerY = aid.yResolution / 2;
		int extrusionX = aid.xResolution;
		int extrusionY = aid.yResolution;

		Graphics graphics = imageToDisplay.getGraphics();
		graphics.setColor(Color.black);
		graphics.fillRect(0, 0, aid.xResolution, aid.yResolution);
		graphics.setColor(Color.white);
		
		Map<String, Object> overrides = new HashMap<>();
		overrides.put("platformGraphics", graphics);
		overrides.put("platformRaster", imageToDisplay.getRaster());
		overrides.put("extrusionX", extrusionX);
		overrides.put("extrusionY", extrusionY);
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

		TemplateEngine.runScript(aid.printJob, aid.printer, aid.scriptEngine, platformScript, "2D Platform rendering script", overrides);
		return imageToDisplay;
	}
}
