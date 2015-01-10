package org.area515.resinprinter.job;

public class JobManagerException extends Exception {
	private static final long serialVersionUID = 515355496052000396L;

	public JobManagerException(String message) {
		super(message);
	}
	
	public JobManagerException(String message, Throwable cause) {
		super(message, cause);
	}
}