package org.area515.resinprinter.display.dispmanx;

import org.area515.resinprinter.display.InappropriateDeviceException;

import com.wgilster.dispmanx.SCREEN;

public class RaspberryPiForceTVScreen extends DispManXDevice {
	public RaspberryPiForceTVScreen() throws InappropriateDeviceException {
		super("Raspberry Pi Force TV", SCREEN.DISPMANX_ID_FORCE_TV);
	}
}
