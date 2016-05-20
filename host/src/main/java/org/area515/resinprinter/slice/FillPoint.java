package org.area515.resinprinter.slice;

public class FillPoint {
	private int x;
	private int y;
	private Integer sliceNumber;
	
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	
	public Integer getSliceNumber() {
		return sliceNumber;
	}
	public void setSliceNumber(Integer sliceNumber) {
		this.sliceNumber = sliceNumber;
	}
	
	public String toString() {
		return "x:" + x + " y:" + y + " slice#:" + sliceNumber;
	}
}
