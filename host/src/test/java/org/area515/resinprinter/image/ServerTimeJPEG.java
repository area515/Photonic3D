package org.area515.resinprinter.image;

import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

public class ServerTimeJPEG {
	public static void main(String[] args) throws IOException {
		int width = Integer.parseInt(args[0]);
		int height = Integer.parseInt(args[1]);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics g = image.getGraphics();
		String date = new Date().toString();
		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(date, g);
		g.drawString(date, width / 2 - (int)stringBounds.getWidth() / 2, height / 2);
		ImageIO.write(image, "jpg", System.out);
	}
}
