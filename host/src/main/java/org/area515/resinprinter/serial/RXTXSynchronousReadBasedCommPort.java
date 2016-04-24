package org.area515.resinprinter.serial;

import gnu.io.SerialPort;

import java.io.IOException;
import java.util.TooManyListenersException;

public class RXTXSynchronousReadBasedCommPort extends RXTXCommPort {
	@Override
	public byte[] read() throws IOException {
		if (inputStream.available() > 0) {
			byte[] buffer = new byte[inputStream.available()];
			inputStream.read(buffer);
			return buffer;
		}
		
		return null;
	}
	
	@Override
	public void init(SerialPort serialPort) throws TooManyListenersException {
	}
}
