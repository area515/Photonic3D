package org.area515.resinprinter.job.render;

import java.awt.image.BufferedImage;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.ScriptEngine;

import org.area515.resinprinter.server.HostProperties;

public class RenderedData {
	private BufferedImage image;
	private BufferedImage preTransformedImage;
	private Double area;
	private ReentrantLock lock = new ReentrantLock();
	private ScriptEngine scriptEngine = HostProperties.Instance().buildScriptEngine();
	
	public RenderedData() {
	}
	
	public void setPrintableImage(BufferedImage image) {
		this.image = image;
	}
	public BufferedImage getPrintableImage() {
		return this.image;
	}

	public BufferedImage getPreTransformedImage() {
		return preTransformedImage;
	}
	public void setPreTransformedImage(BufferedImage preTransformedImage) {
		this.preTransformedImage = preTransformedImage;
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

	public ScriptEngine getScriptEngine() {
		return scriptEngine;
	}
}
