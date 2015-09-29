package org.area515.resinprinter.projector;

import java.io.IOException;

import org.area515.resinprinter.serial.SerialCommunicationsPort;


public interface ProjectorModel {
	public String getName();
	public boolean autodetect(SerialCommunicationsPort port);
	public void setProjectorState(boolean state, SerialCommunicationsPort port) throws IOException;
	public boolean getProjectorState(SerialCommunicationsPort port) throws IOException;
}
