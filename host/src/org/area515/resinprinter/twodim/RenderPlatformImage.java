package org.area515.resinprinter.twodim;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.render.CurrentImageRenderer;
import org.area515.resinprinter.job.render.RenderingFileData;
import org.area515.resinprinter.job.render.RenderingFileData.ImageData;
import org.area515.resinprinter.printer.SlicingProfile.TwoDimensionalSettings;
import org.area515.resinprinter.twodim.TwoDimensionalPlatformPrintFileProcessor.TwoDimensionalPrintState;
import org.area515.util.TemplateEngine;

public class RenderPlatformImage extends CurrentImageRenderer {
	public RenderPlatformImage(DataAid aid, AbstractPrintFileProcessor<?> processor, RenderingFileData data, Object imageIndexToBuild, int width, int height, int slicesLeft) {
		super(aid, processor, data, imageIndexToBuild, width, height);
	}
	
	@Override
	public void renderImage(Graphics2D graphics, ImageData imageData) throws ScriptException {
		int centerX = width / 2;
		int centerY = height / 2;
		BufferedImage imageToDisplay = ((TwoDimensionalPrintState)data).getCachedExtrusionImage();
		int extrusionX = imageToDisplay.getWidth() > width?width:imageToDisplay.getWidth();
		int extrusionY = imageToDisplay.getHeight() > height?height: imageToDisplay.getHeight();

		graphics.setColor(Color.black);
		graphics.fillRect(0, 0, width, height);
		graphics.setColor(Color.white);
		
		Map<String, Object> overrides = new HashMap<>();
		overrides.put("platformGraphics", graphics);
		overrides.put("extrusionX", extrusionX);
		overrides.put("extrusionY", extrusionY);
		overrides.put("centerX", centerX);
		overrides.put("centerY", centerY);
		
		TwoDimensionalSettings settings = aid.slicingProfile.getTwoDimensionalSettings();
		if (settings == null) {
			throw new IllegalArgumentException("This printer doesn't have it's 2D File Settings setup properly");
		}
		
		String platformScript = settings.getPlatformCalculator();
		if (platformScript == null) {
			throw new IllegalArgumentException("This printer doesn't have 2D platform rendering calculator setup");
		}
		
		TemplateEngine.runScript(aid.printJob, aid.printer, aid.scriptEngine, platformScript, "2D Platform rendering script", overrides);
	}
}
