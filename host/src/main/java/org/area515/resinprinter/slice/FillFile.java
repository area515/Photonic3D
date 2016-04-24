package org.area515.resinprinter.slice;

import java.util.List;

public class FillFile {
	private List<FillPoint> points;
	private String fileName;
	private double stlScale;
	private double pixelsPerMMX;
	private double pixelsPerMMY;
	private double zSliceResolution;
	private double zSliceOffset;
	
	public List<FillPoint> getPoints() {
		return points;
	}
	public void setPoints(List<FillPoint> points) {
		this.points = points;
	}
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public double getStlScale() {
		return stlScale;
	}
	public void setStlScale(double stlScale) {
		this.stlScale = stlScale;
	}
	
	public double getPixelsPerMMX() {
		return pixelsPerMMX;
	}
	public void setPixelsPerMMX(double pixelsPerMMX) {
		this.pixelsPerMMX = pixelsPerMMX;
	}
	
	public double getPixelsPerMMY() {
		return pixelsPerMMY;
	}
	public void setPixelsPerMMY(double pixelsPerMMY) {
		this.pixelsPerMMY = pixelsPerMMY;
	}
	
	public double getzSliceResolution() {
		return zSliceResolution;
	}
	public void setzSliceResolution(double zSliceResolution) {
		this.zSliceResolution = zSliceResolution;
	}
	
	public double getzSliceOffset() {
		return zSliceOffset;
	}
	public void setzSliceOffset(double zSliceOffset) {
		this.zSliceOffset = zSliceOffset;
	}
}
