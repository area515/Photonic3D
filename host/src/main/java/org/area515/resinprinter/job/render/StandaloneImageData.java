package org.area515.resinprinter.job.render;

import java.awt.image.BufferedImage;

public class StandaloneImageData {
	private BufferedImage image;
	private double area;

	public StandaloneImageData(BufferedImage image, double area) {
		this.image = image;
		this.area = area;
	}
	
	public BufferedImage getImage() {
		return this.image;
	}
	public double getArea() {
		return this.area;
	}

}
