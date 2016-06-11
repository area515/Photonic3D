package org.area515.resinprinter.twodim;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.render.CurrentImageRenderer;
import org.area515.resinprinter.job.render.RenderingFileData;
import org.area515.resinprinter.job.render.RenderingFileData.ImageData;
import org.area515.resinprinter.twodim.TwoDimensionalPlatformPrintFileProcessor.TwoDimensionalPrintState;

public class RenderExtrusionImage extends CurrentImageRenderer {
	public RenderExtrusionImage(DataAid aid, AbstractPrintFileProcessor<?,?> processor, RenderingFileData data, Object imageIndexToBuild, int width, int height) {
		super(aid, processor, data, imageIndexToBuild, width, height);
	}

	@Override
	public void renderImage(BufferedImage image, Graphics2D graphics, ImageData imageData) {
		int centerX = width / 2;
		int centerY = height / 2;
		BufferedImage imageToDisplay = ((TwoDimensionalPrintState)data).getCachedExtrusionImage();
		int actualWidth = imageToDisplay.getWidth() > width?width:imageToDisplay.getWidth();
		int actualHeight = imageToDisplay.getHeight() > height?height: imageToDisplay.getHeight();

		graphics.setColor(Color.black);
		graphics.fillRect(0, 0, width, height);
		graphics.setColor(Color.white);

		graphics.drawImage(imageToDisplay, centerX - (actualWidth / 2), centerY - (actualHeight / 2), null);
	}
}
