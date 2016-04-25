package org.area515.resinprinter.display;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.HeadlessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class CustomNamedDisplayDevice extends GraphicsDevice {
	private String displayName;
	
	public CustomNamedDisplayDevice(String displayName) {
		this.displayName = displayName;
	}
	
	@Override
	public int getType() {
		return TYPE_IMAGE_BUFFER;
	}
	
	@Override
	public String getIDstring() {
		return displayName;
	}

	@Override
	public GraphicsConfiguration[] getConfigurations() {
		return null;
	}

	@Override
	public GraphicsConfiguration getDefaultConfiguration() {
		GraphicsDevice[] devices;
		try {
			devices = DisplayManager.Instance().getGraphicsEnvironment().getScreenDevices();
			return devices[devices.length - 1].getDefaultConfiguration();
		} catch (HeadlessException | InappropriateDeviceException e) {
			throw new IllegalArgumentException("Graphics environment not supported?", e);
		}
	}

	public String toString() {
		return displayName;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((displayName == null) ? 0 : displayName.hashCode());
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
		CustomNamedDisplayDevice other = (CustomNamedDisplayDevice) obj;
		if (displayName == null) {
			if (other.displayName != null)
				return false;
		} else if (!displayName.equals(other.displayName))
			return false;
		return true;
	}
}
