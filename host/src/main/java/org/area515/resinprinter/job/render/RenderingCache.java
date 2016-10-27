package org.area515.resinprinter.job.render;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class RenderingCache {
	private static final Logger logger = LogManager.getLogger();
	private LoadingCache<Object, RenderedData> imageSync = CacheBuilder.newBuilder().softValues().build(
			new CacheLoader<Object, RenderedData>() {
				@Override
				public RenderedData load(Object key) throws Exception {
					return new RenderedData();
				}
			});
	
	private Object currentImagePointer = Boolean.TRUE;

	public RenderedData getOrCreateIfMissing(Object imageToBuild) {
		try {
			return imageSync.get(imageToBuild);
		} catch (ExecutionException e) {
			logger.error(e);
			return null;
		}
	}
	
	public void clearCache(Object imageToBuild) {
		imageSync.invalidate(imageToBuild);
	}
	
	public ReentrantLock getSpecificLock(Object imageToBuild) {
		return getOrCreateIfMissing(imageToBuild).getLock();
	}
	
	public ReentrantLock getCurrentLock() {
		return getOrCreateIfMissing(currentImagePointer).getLock();
	}
	
	public BufferedImage getCurrentImage() {
		return getOrCreateIfMissing(currentImagePointer).getPrintableImage();
	}
	
	public Double getCurrentArea() {
		return getOrCreateIfMissing(currentImagePointer).getArea();
	}
	
	public Object getCurrentRenderingPointer() {
		return currentImagePointer;
	}
	
	public void setCurrentRenderingPointer(Object pointer) {
		currentImagePointer = pointer;
	}
}