package org.area515.resinprinter.job.render;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.ScriptEngine;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.slice.ZSlicer;

public class RenderingFileData {
	public static class ImageData {
		private BufferedImage image;
		private double area;
		private ReentrantLock lock = new ReentrantLock();
		
		public ImageData(BufferedImage image, double area) {
			this.image = image;
			this.area = area;
		}

		public void setArea(double area) {
			this.area = area;
		}
	}
	
	private ScriptEngine scriptEngine;
	private Map<Object, RenderingFileData.ImageData> imageSync = new HashMap<>();
	public ZSlicer slicer;
	private AbstractPrintFileProcessor<?> aid;
	private Boolean currentImagePointer;
	
	public RenderingFileData(AbstractPrintFileProcessor<?> aid, ScriptEngine scriptEngine) {
		this.scriptEngine = scriptEngine;
		this.aid = aid;
		this.currentImagePointer = Boolean.TRUE;
	}
	
	public ImageData get(Object imageToBuild) {
		return imageSync.get(imageToBuild);
	}
	
	public void initialize(Object imageToBuild, int width, int height) {
		ImageData imageData = imageSync.get(imageToBuild);
		if (imageData == null) {
			imageData = new ImageData(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE), 0.0);
			imageSync.put(imageToBuild, imageData);
		}
	}
	
	public ReentrantLock getSpecificLock(Object lockPointer) {
		return imageSync.get(lockPointer).lock;
	}
	
	public ReentrantLock getCurrentLock() {
		return imageSync.get(currentImagePointer).lock;
	}
	
	public BufferedImage getCurrentImage() {
		return imageSync.get(currentImagePointer).image;
	}
	
	public double getCurrentArea() {
		return imageSync.get(currentImagePointer).area;
	}
	
	public Object getNextRenderingPointer() {
		return !currentImagePointer;
	}
	
	public Object getCurrentRenderingPointer() {
		return currentImagePointer;
	}
	
	public void setCurrentRenderingPointer(Object pointer) {
		currentImagePointer = (Boolean)pointer;
	}

	public AbstractPrintFileProcessor<?> getPrintFileProcessingAid() {
		return aid;
	}
}