package org.area515.resinprinter.serial;

import java.io.IOException;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.ComPortSettings;

public class CustomCommPort implements SerialCommunicationsPort {
	private String name;
	
	public CustomCommPort(String name) {
		this.name = name;
	}
	
	@Override
	public void open(String controllingDevice, int timeout, ComPortSettings settings) throws AlreadyAssignedException, InappropriateDeviceException {
	}

	@Override
	public void close() {
	}

	@Override
	public void setName(String name) {
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void write(byte[] gcode) throws IOException {
		//Do nothing
	}

	@Override
	public byte[] read() throws IOException {
		//Do nothing
		return null;
	}
	
	@Override
	public void restartCommunications() {
	}

	public String toString() {
		return name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		CustomCommPort other = (CustomCommPort) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
