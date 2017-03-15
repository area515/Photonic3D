package org.area515.resinprinter.display.dispmanx;

public enum SCREEN {
	DISPMANX_ID_MAIN_LCD(0),
	DISPMANX_ID_AUX_LCD(1),
	DISPMANX_ID_HDMI(2),
	DISPMANX_ID_SDTV(3),
	DISPMANX_ID_FORCE_LCD(4),
	DISPMANX_ID_FORCE_TV(5),
	DISPMANX_ID_FORCE_OTHER(6);
	private int id;
	
	SCREEN(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
