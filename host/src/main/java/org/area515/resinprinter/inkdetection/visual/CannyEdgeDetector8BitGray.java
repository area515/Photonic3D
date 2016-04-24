package org.area515.resinprinter.inkdetection.visual;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class CannyEdgeDetector8BitGray extends CannyEdgeDetector {
	public void writeEdges(int pixels[]) {
		if (edgesImage == null) {
			edgesImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		
		byte bytePixels[] = new byte[pixels.length];
		for (int t = 0; t < pixels.length; t++) {
			if (pixels[t] == Color.white.getRGB()) {
				bytePixels[t] = Byte.MAX_VALUE;
			} else {
				bytePixels[t] = 0;
			}
		}
		
		edgesImage.getWritableTile(0, 0).setDataElements(0, 0, width, height, bytePixels);
	}
}