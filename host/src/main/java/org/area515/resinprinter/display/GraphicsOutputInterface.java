package org.area515.resinprinter.display;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

//TODO: This needs to be the interface we use that is returned from the DisplayManager instead of java.awt.GraphicsDevice
public interface GraphicsOutputInterface {
	public void resetSliceCount();
	public void dispose();
	public void showBlankImage();
	public void showCalibrationImage(int xPixels, int yPixels);
	public void showGridImage(int pixels);
	public void showImage(BufferedImage image);
	public Rectangle getBoundry();
}
