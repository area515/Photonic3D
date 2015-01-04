package org.area515.resinprinter.display;

public class InappropriateDeviceException extends Exception {
	private static final long serialVersionUID = 4586082875895308024L;

	public InappropriateDeviceException(String message) {
		super(message);
	}
	
	public InappropriateDeviceException(String message, Exception e) {
		super(message, e);
	}
}
