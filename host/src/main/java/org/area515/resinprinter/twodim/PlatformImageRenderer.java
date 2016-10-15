package org.area515.resinprinter.twodim;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

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
		
		BufferedImage extrudedImage = extrusionImageRenderer.call().getPreTransformedImage();
		Map<String, Object> overrides = new HashMap<>();
		overrides.put("totalPlatformSlices", totalPlatformSlices);
		imageToDisplay = TemplateEngine.runScriptInImagingContext(imageToDisplay, extrudedImage, aid, overrides, platformScript);
		return imageToDisplay;
	}
}
