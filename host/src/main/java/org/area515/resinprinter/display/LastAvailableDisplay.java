package org.area515.resinprinter.display;

import java.awt.GraphicsDevice;
import java.awt.HeadlessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class LastAvailableDisplay extends GraphicsDeviceOutputInterface {
	public static final String NAME = "Last available display";
	
	public LastAvailableDisplay() throws HeadlessException {
		super(NAME, null);
	}
	
	public GraphicsDevice getGraphicsDevice() {
		try {
			ArrayList<GraphicsDevice> devices = new ArrayList<GraphicsDevice>();
			devices.addAll(Arrays.asList(DisplayManager.Instance().getGraphicsEnvironment().getScreenDevices()));
			Collections.reverse(devices);
			for (GraphicsDevice currentDevice : devices) {
				if (DisplayManager.Instance().isGraphicsDeviceDisplayAvailable(currentDevice.getIDstring())) {
					return currentDevice;
				}
			}
			
			return null;
		} catch (InappropriateDeviceException e) {
			throw new IllegalArgumentException("Couldn't initialize graphics environment", e);
		}
	}
	
	@Override
	public String buildIDString() {
		GraphicsDevice lastDevice = getGraphicsDevice();
		if (lastDevice == null) {
			return null;
		}
		return lastDevice.getIDstring();
	}
}
