package org.area515.resinprinter.job;

public class JobManagerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JobManagerException() {
		super();
	}
	
	public JobManagerException(String message) {
		super(message);
	}
	
	public JobManagerException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public JobManagerException(Throwable cause) {
		super(cause);
	}
}