package org.area515.resinprinter.exception;

public class SlicerException extends Exception {
	public SlicerException () {

    }

    public SlicerException (String message) {
        super (message);
    }

    public SlicerException (Throwable cause) {
        super (cause);
    }

    public SlicerException (String message, Throwable cause) {
        super (message, cause);
    }
}