package org.area515.resinprinter.job.render;

import java.awt.image.BufferedImage;
import java.util.concurrent.locks.ReentrantLock;

public class RenderedData {
	private BufferedImage image;
	private Double area;
	private ReentrantLock lock = new ReentrantLock();

	public RenderedData() {
	}
	
	public void setImage(BufferedImage image) {
		this.image = image;
	}
	public BufferedImage getImage() {
		return this.image;
	}

	public void setArea(Double area) {
		this.area = area;
	}
	public Double getArea() {
		return this.area;
	}
	
	public ReentrantLock getLock() {
		return lock;
	}
}
