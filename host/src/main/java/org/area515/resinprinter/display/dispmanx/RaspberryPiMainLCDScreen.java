package org.area515.resinprinter.display.dispmanx;

import org.area515.resinprinter.display.InappropriateDeviceException;

public class RaspberryPiMainLCDScreen extends DispManXDevice {
	public RaspberryPiMainLCDScreen() throws InappropriateDeviceException {
		super("Raspberry Pi Main LCD", SCREEN.DISPMANX_ID_MAIN_LCD);
	}
}
