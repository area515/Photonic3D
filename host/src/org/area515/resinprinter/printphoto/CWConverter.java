package org.area515.resinprinter.printphoto;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;


public class CWConverter {
	public static enum MaxIntensityDetermination {
		UseMaxFromColorSpace,
		UseHighAndLowFromImage
	}
	
	public static void convertImage(BufferedImage image, MaxIntensityDetermination searchForHighLowColorDepthFromInput) {
		int maxX = image.getWidth();
		int maxY = image.getHeight();
		float maxColor = 0;
		float minColor = image.getColorModel().getComponentSize()[0];//I assume that all color components are the same as #0
		float data[] = new float[4];
		WritableRaster raster = image.getRaster();
		
		switch (searchForHighLowColorDepthFromInput) {
			case UseHighAndLowFromImage :
				for (int x = 0; x < maxX; x++) {
					for (int y = 0; y < maxY; y++) {
						raster.getPixel(x, y, data);
						float info = (data[0] + data[1] + data[2]) / 3f;
						if (info > maxColor) {
							maxColor = info;
						}
						if (info < minColor) {
							minColor = info;
						}
					}
				}
				break;
				
			case UseMaxFromColorSpace :
				maxColor = image.getColorModel().getComponentSize()[0];
				minColor = 0;
		}

		for (int x = 0; x < maxX; x++) {
			for (int y = 0; y < maxY; y++) {
				raster.getPixel(x, y, data);
				float depthOfMask = maxColor - minColor;
				float unNormalizedAlpha = (data[0] + data[1] + data[2]) / 3f;
				data[0] = 0;
				data[1] = 0;
				data[2] = 0;
				data[3] = depthOfMask - (unNormalizedAlpha - minColor);
				raster.setPixel(x, y, data);
			}
		}
	}
}
