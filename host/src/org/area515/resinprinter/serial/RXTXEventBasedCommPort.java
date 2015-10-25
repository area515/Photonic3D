package org.area515.resinprinter.serial;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.util.TooManyListenersException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RXTXEventBasedCommPort extends RXTXCommPort implements SerialPortEventListener {
	private Lock asynchReadLock = new ReentrantLock();
	private byte[] buffer;
	private IOException exceptionThrown;
	
	public void init(SerialPort serialPort) throws TooManyListenersException {
		serialPort.addEventListener(this);
		serialPort.notifyOnDataAvailable(true);
		serialPort.notifyOnBreakInterrupt(true);
		serialPort.notifyOnCarrierDetect(true);
		serialPort.notifyOnCTS(true);
		serialPort.notifyOnDSR(true);
		serialPort.notifyOnFramingError(true);
		serialPort.notifyOnOutputEmpty(true);
		serialPort.notifyOnOverrunError(true);
		serialPort.notifyOnParityError(true);
		serialPort.notifyOnRingIndicator(true);
	}
	
	@Override
	public byte[] read() throws IOException {
		asynchReadLock.lock();
		try {
			if (buffer != null) {
				byte[] returnbuffer = buffer;
				buffer = null;
				return returnbuffer;
			}
			
			if (exceptionThrown != null) {
				IOException throwException = exceptionThrown;
				exceptionThrown = null;
				throw throwException;
			}
			
			return null;
		} finally {
			asynchReadLock.unlock();
		}
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		if (event.getEventType() != SerialPortEvent.DATA_AVAILABLE) {
			System.out.println("Type:" + event.getEventType() + " from:" + event.getOldValue() + " to:" + event.getNewValue());
			return;
		}
		
		asynchReadLock.lock();
		try {
			buffer = new byte[inputStream.available()];
			inputStream.read(buffer);
		} catch (IOException e) {
			exceptionThrown = e;
		} finally {
			asynchReadLock.unlock();
		}
	}
}
