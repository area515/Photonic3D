package org.area515.resinprinter.display.dispmanx;

public enum PROTECTION {
	DISPMANX_PROTECTION_MAX(0x0f),
	DISPMANX_PROTECTION_NONE(0),
	DISPMANX_PROTECTION_HDCP(11);
	private int cConst;
	
	PROTECTION(int cConst) {
		this.cConst = cConst;
	}
	
	public int getcConst() {
		return cConst;
	}
}
