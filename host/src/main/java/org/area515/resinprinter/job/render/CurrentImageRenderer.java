package org.area515.resinprinter.job.render;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import javax.script.ScriptException;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;

public abstract class CurrentImageRenderer implements Callable<BufferedImage> {
	protected RenderingFileData data;
	private Object imageIndexToBuild;
	protected int width;
	protected int height;
	protected AbstractPrintFileProcessor<?,?> processor;
	protected DataAid aid;
	
	public CurrentImageRenderer(DataAid aid, AbstractPrintFileProcessor<?,?> processor, RenderingFileData data, Object imageIndexToBuild, int width, int height) {
		this.aid = aid;
		this.data = data;
		this.processor = processor;
		this.imageIndexToBuild = imageIndexToBuild;
		data.initialize(imageIndexToBuild, width, height);
		this.width = width;
		this.height = height;
	}
	
	public BufferedImage call() throws ScriptException {
		Lock lock = data.getSpecificLock(imageIndexToBuild);
		lock.lock();
		try {
			RenderingFileData.ImageData imageData = data.get(imageIndexToBuild);
			BufferedImage image = data.getCurrentImage();
			Graphics2D graphics = (Graphics2D)image.getGraphics();
			renderImage(image, graphics, imageData);
			processor.applyBulbMask(aid, graphics, width, height);
			return data.getCurrentImage();
		} finally {
			lock.unlock();
		}
	}
	
	abstract public void renderImage(BufferedImage image, Graphics2D graphics, RenderingFileData.ImageData imageData) throws ScriptException;
}