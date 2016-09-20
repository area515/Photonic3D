package org.area515.resinprinter.job;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.render.CurrentImageRenderer;

public class STLImageRenderer extends CurrentImageRenderer {
	private boolean previewMode;
	
	public STLImageRenderer(DataAid aid, AbstractPrintFileProcessor<?,?> processor, Object imageIndexToBuild, boolean previewMode) {
		super(aid, processor, imageIndexToBuild);
		this.previewMode = previewMode;
	}

	@Override
	public BufferedImage renderImage(BufferedImage imageToDisplay) {
		STLDataAid aid = (STLDataAid)this.aid;
		aid.slicer.colorizePolygons(null, null);
		if (imageToDisplay == null) {
			imageToDisplay = buildImage((int)aid.slicer.getWidthPixels(), (int)aid.slicer.getHeightPixels());
		}
		
		Graphics2D g2 = (Graphics2D)imageToDisplay.getGraphics();
		aid.slicer.paintSlice(g2);
		if (previewMode) {
			g2.setColor(Color.RED);
			g2.drawRect(0, 0, (int)aid.slicer.getWidthPixels() - 1, (int)aid.slicer.getHeightPixels() - 1);
		}
		
		//imageToDisplay.setArea((double)aid.slicer.getBuildArea());//TODO: This won't work if affine transforms are applied afterwards!
		return imageToDisplay;
	}
}
