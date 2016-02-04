package org.area515.resinprinter.projector;

import java.io.IOException;

import org.area515.resinprinter.printer.ComPortSettings;
import org.area515.resinprinter.serial.SerialCommunicationsPort;


public interface ProjectorModel {
	public String getName();
	public ComPortSettings getComPortSettings();
	public boolean autodetect(SerialCommunicationsPort port);
	public void setPowerState(boolean state, SerialCommunicationsPort port) throws IOException;
	public boolean getPowerState(SerialCommunicationsPort port) throws IOException;
	public Integer getBulbHours(SerialCommunicationsPort port) throws IOException;
}
