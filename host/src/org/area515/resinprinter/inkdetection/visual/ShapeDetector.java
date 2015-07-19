package org.area515.resinprinter.inkdetection.visual;

public interface ShapeDetector<S> {
	public int getScaleCount();
	public int getMinimumScaleIndex();
	public int getMaximumScaleIndex();
	public int getScaleIncrement();
	public int getSamplesPerScaleIndex(int scaleIndex);
	public int[] getHoughSpaceSizeAndGenerateLUT(int imageWidth, int imageHeight);
	public int[] getSignificantPointOfShape(int x, int y, int sample, int scaleIndex);
	public S buildShape(int x, int y, int scaleIndex, int votes);
}
