package org.area515.resinprinter.display;

import gnu.io.CommPortIdentifier;

import java.awt.GraphicsDevice;

import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.serial.SerialCommunicationsPort;

public class AlreadyAssignedException extends Exception {
	private static final long serialVersionUID = 5346661559747947463L;

	private Printer printer;
	private GraphicsDevice graphicsDevice;
	private SerialCommunicationsPort comPort;
	
	public AlreadyAssignedException(String message, Printer printer) {
		super(message);
		this.printer = printer;
	}
	
	public AlreadyAssignedException(String message, SerialCommunicationsPort comPort) {
		super(message);
		this.comPort = comPort;
	}
	
	public AlreadyAssignedException(String message, GraphicsDevice graphicsDevice) {
		super(message);
		this.graphicsDevice = graphicsDevice;
	}

	public Printer getPrinter() {
		return printer;
	}
	
	public GraphicsDevice getGraphicsDevice() {
		return graphicsDevice;
	}

	public SerialCommunicationsPort getComPort() {
		return comPort;
	}
}
