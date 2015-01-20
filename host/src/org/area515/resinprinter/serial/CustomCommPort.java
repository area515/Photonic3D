package org.area515.resinprinter.serial;

import java.io.IOException;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration.ComPortSettings;

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
	public void write(String gcode) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public String readUntilOkOrStoppedPrinting(Printer printer)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
