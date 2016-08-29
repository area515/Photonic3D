package org.area515.resinprinter.exception;

public class NoPrinterFoundException extends Exception {
	public NoPrinterFoundException () {

    }

    public NoPrinterFoundException (String message) {
        super (message);
    }

    public NoPrinterFoundException (Throwable cause) {
        super (cause);
    }

    public NoPrinterFoundException (String message, Throwable cause) {
        super (message, cause);
    }
}