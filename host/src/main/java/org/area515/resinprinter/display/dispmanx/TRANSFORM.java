package org.area515.resinprinter.display.dispmanx;

public enum TRANSFORM {
	TRANSFORM_HFLIP(1<<0),
	TRANSFORM_VFLIP(1<<1),
	TRANSFORM_TRANSPOSE (1<<2);
	private int cConst;
	
	TRANSFORM(int cConst) {
		this.cConst = cConst;
	}
	
	public int getcConst() {
		return cConst;
	}
}