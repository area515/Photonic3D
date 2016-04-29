package org.area515.resinprinter.image;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

public class ConvertCWMaskToTransparencyMask {
	@Test
	public void testMaskConversion() throws Exception {
        BufferedImage sourceImage = javax.imageio.ImageIO.read(ConvertCWMaskToTransparencyMask.class.getResourceAsStream("WorkingCWMask.png"));
        BufferedImage convertedImage = new java.awt.image.BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        convertedImage.getGraphics().drawImage(sourceImage, 0, 0, null);
        org.area515.resinprinter.printphoto.CWConverter.convertImage(convertedImage, org.area515.resinprinter.printphoto.CWConverter.MaxIntensityDetermination.UseHighAndLowFromImage);
        Rectangle2D rect = new java.awt.geom.Rectangle2D.Double(0, 0, sourceImage.getWidth(), sourceImage.getHeight());
        //new java.awt.TexturePaint(convertedImage, rect);

        Assert.assertTrue(ImageIO.write(convertedImage, "png", new File("images/sanePhotonicMask.png")));
	}
	
	public static void main(String[] args) throws Exception {
		new ConvertCWMaskToTransparencyMask().testMaskConversion();
	}
}
