package org.area515.resinprinter.display;

import gnu.io.CommPortIdentifier;

import java.awt.GraphicsDevice;

import org.area515.resinprinter.job.PrintJob;

public class AlreadyAssignedException extends Exception {
	private static final long serialVersionUID = 5346661559747947463L;

	private PrintJob assignedJob;
	private GraphicsDevice graphicsDevice;
	private CommPortIdentifier comPort;
	
	public AlreadyAssignedException(String message, PrintJob assignedJob) {
		super(message);
		this.assignedJob = assignedJob;
	}
	
	public AlreadyAssignedException(String message, CommPortIdentifier comPort) {
		super(message);
		this.comPort = comPort;
	}
	
	public AlreadyAssignedException(String message, GraphicsDevice graphicsDevice) {
		super(message);
		this.graphicsDevice = graphicsDevice;
	}

	public PrintJob getAssignedJob() {
		return assignedJob;
	}
	
	public GraphicsDevice getGraphicsDevice() {
		return graphicsDevice;
	}
}
