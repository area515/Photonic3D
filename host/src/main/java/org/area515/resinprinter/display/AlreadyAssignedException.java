package org.area515.resinprinter.display;

import java.awt.GraphicsDevice;

import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.serial.SerialCommunicationsPort;

public class AlreadyAssignedException extends Exception {
	private static final long serialVersionUID = 5346661559747947463L;

	private Printer printer;
	private String graphicsDevice;
	private SerialCommunicationsPort comPort;
	private PrintJob printJob;
	
	public AlreadyAssignedException(String message, Printer printer) {
		super(message);
		this.printer = printer;
	}
	
	public AlreadyAssignedException(String message, PrintJob printJob) {
		super(message);
		this.printJob = printJob;
	}
	
	public AlreadyAssignedException(String message, SerialCommunicationsPort comPort) {
		super(message);
		this.comPort = comPort;
	}
	
	public AlreadyAssignedException(String message, String graphicsDevice) {
		super(message);
		this.graphicsDevice = graphicsDevice;
	}

	public PrintJob getPrintJob() {
		return printJob;
	}

	public Printer getPrinter() {
		return printer;
	}
	
	public String getGraphicsDeviceId() {
		return graphicsDevice;
	}

	public SerialCommunicationsPort getComPort() {
		return comPort;
	}
}
