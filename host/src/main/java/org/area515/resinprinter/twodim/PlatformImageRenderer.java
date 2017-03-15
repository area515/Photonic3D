package org.area515.resinprinter.twodim;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.render.CurrentImageRenderer;
import org.area515.resinprinter.job.render.RenderedData;
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
			imageToDisplay = buildImage(aid.xResolution, aid.yResolution);
		}
		
		TwoDimensionalSettings settings = aid.slicingProfile.getTwoDimensionalSettings();
		if (settings == null) {
			throw new JobManagerException("This printer doesn't have it's 2D File Settings setup properly");
		}
		
		String platformScript = settings.getPlatformCalculator();
		if (platformScript == null) {
			throw new JobManagerException("This printer doesn't have 2D platform rendering calculator setup");
		}
		
		RenderedData data = aid.cache.getOrCreateIfMissing("lastExtrusionImage");
		if (data.getPreTransformedImage() == null) {
			data.setPreTransformedImage(extrusionImageRenderer.renderImage(null));
		}
		
		Map<String, Object> overrides = new HashMap<>();
		overrides.put("totalPlatformSlices", totalPlatformSlices);
		try {
			TemplateEngine.runScriptInImagingContext(imageToDisplay, data.getPreTransformedImage(), aid.printJob, aid.printer, aid.scriptEngine, overrides, platformScript, "2D Platform rendering script", false);
		} catch (ScriptException e) {
			throw new JobManagerException("Failed to execute script", e);
		}
		return imageToDisplay;
	}
}
