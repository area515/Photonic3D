package org.area515.resinprinter.security;

public class UserManagementException extends Exception {
	private static final long serialVersionUID = 3261094829353819020L;

	public UserManagementException(String message, Throwable cause) {
		super(message, cause);
	}

	public UserManagementException(String message) {
		super(message);
	}

	public UserManagementException(Throwable cause) {
		super(cause);
	}
}
