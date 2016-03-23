package org.area515.resinprinter.job;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.render.CurrentImageRenderer;
import org.area515.resinprinter.job.render.RenderingFileData;
import org.area515.resinprinter.job.render.RenderingFileData.ImageData;

public class STLImageRenderer extends CurrentImageRenderer {
	public STLImageRenderer(DataAid aid, AbstractPrintFileProcessor<?> processor, RenderingFileData data, Object imageIndexToBuild, int width, int height) {
		super(aid, processor, data, imageIndexToBuild, width, height);
	}

	@Override
	public void renderImage(BufferedImage image, Graphics2D g2, ImageData imageData) {
		data.slicer.colorizePolygons();
		data.slicer.paintSlice(g2);
		imageData.setArea((double)data.slicer.getBuildArea());
	}
}
