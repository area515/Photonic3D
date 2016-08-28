package org.area515.resinprinter.exception;

public class SliceHandlingException extends Exception {
	public SliceHandlingException() {
    }

    public SliceHandlingException(String message) {
        super (message);
    }

    public SliceHandlingException(Throwable cause) {
        super (cause);
    }

    public SliceHandlingException(String message, Throwable cause) {
        super (message, cause);
    }
}