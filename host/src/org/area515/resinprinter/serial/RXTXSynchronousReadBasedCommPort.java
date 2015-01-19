package org.area515.resinprinter.serial;

import gnu.io.SerialPort;

import java.io.IOException;
import java.util.TooManyListenersException;

import org.area515.resinprinter.printer.Printer;

public class RXTXSynchronousReadBasedCommPort extends RXTXCommPort implements SerialCommunicationsPort {
	@Override
	public String readUntilOkOrStoppedPrinting(Printer printer) throws IOException {
    	StringBuilder builder = new StringBuilder();

		String response = "";
		while (response != null && !response.matches("(?is:ok.*)")) {
			response = readLine(printer);
			if (response != null) {
				builder.append(response);
			}
		}
		
		return builder.toString();
	}

	@Override
	public void init(SerialPort serialPort) throws TooManyListenersException {
	}
}
