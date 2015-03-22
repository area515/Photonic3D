package org.area515.resinprinter.serial;

import java.io.IOException;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.MachineConfig.ComPortSettings;
import org.area515.resinprinter.printer.Printer;

public interface SerialCommunicationsPort {
	public void open(String controllingDevice, int timeout, ComPortSettings settings) throws AlreadyAssignedException, InappropriateDeviceException;
	public void close();
	public void setName(String name);
	public String getName();
	public void write(String gcode) throws IOException;
	public String readUntilOkOrStoppedPrinting(Printer printer) throws IOException;
}
