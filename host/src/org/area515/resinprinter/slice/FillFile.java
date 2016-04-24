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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		long temp;
		temp = Double.doubleToLongBits(pixelsPerMMX);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(pixelsPerMMY);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(stlScale);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(zSliceOffset);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(zSliceResolution);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FillFile other = (FillFile) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (Double.doubleToLongBits(pixelsPerMMX) != Double.doubleToLongBits(other.pixelsPerMMX))
			return false;
		if (Double.doubleToLongBits(pixelsPerMMY) != Double.doubleToLongBits(other.pixelsPerMMY))
			return false;
		if (Double.doubleToLongBits(stlScale) != Double.doubleToLongBits(other.stlScale))
			return false;
		if (Double.doubleToLongBits(zSliceOffset) != Double.doubleToLongBits(other.zSliceOffset))
			return false;
		if (Double.doubleToLongBits(zSliceResolution) != Double.doubleToLongBits(other.zSliceResolution))
			return false;
		return true;
	}
}
