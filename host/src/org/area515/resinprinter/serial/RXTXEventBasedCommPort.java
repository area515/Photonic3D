package org.area515.resinprinter.serial;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.util.TooManyListenersException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.area515.resinprinter.printer.Printer;

public class RXTXEventBasedCommPort extends RXTXCommPort implements SerialPortEventListener {
	private Lock asynchReadLock = new ReentrantLock();
	private Condition dataAvailable = asynchReadLock.newCondition();
	private Condition consumerAvailable = asynchReadLock.newCondition();
	private Condition dataConsumed = asynchReadLock.newCondition();
	private boolean isConsumerAvailable = false;
	
	public void init(SerialPort serialPort) throws TooManyListenersException {
		serialPort.notifyOnBreakInterrupt(true);
		serialPort.notifyOnCarrierDetect(true);
		serialPort.notifyOnCTS(true);
		serialPort.notifyOnDataAvailable(true);
		serialPort.notifyOnDSR(true);
		serialPort.notifyOnFramingError(true);
		serialPort.notifyOnOutputEmpty(true);
		serialPort.notifyOnOverrunError(true);
		serialPort.notifyOnParityError(true);
		serialPort.notifyOnRingIndicator(true);
		serialPort.addEventListener(this);
	}
	
	private boolean waitForDataAvailable() {
		asynchReadLock.lock();
		try {
			isConsumerAvailable = true;
			consumerAvailable.signal();
			dataAvailable.await(waitForGCodeTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		} finally {
			asynchReadLock.unlock();
		}
		
		return true;
	}
	
	@Override
	public String readUntilOkOrStoppedPrinting(Printer printer) throws IOException {
		asynchReadLock.lock();
		try {
	    	StringBuilder builder = new StringBuilder();
	    	
	    	if (!waitForDataAvailable()) {
	    		return null;
	    	}
	    	
			String response = "";
			while (response != null && !response.matches("(?is:ok.*)")) {
				response = readLine(printer);
				if (response != null) {
					builder.append(response);
				}
				System.out.println("lineRead:" + response);
			}
			
			dataConsumed.signal();
			return builder.toString();
		} finally {
			asynchReadLock.unlock();
		}
	}

	/**
	 * All of this sychronization is to abide by the theory that maybe RXTX restricts all reading  to be confined to the execution bounds of the 
	 * serialEvent thread.
	 */
	@Override
	public void serialEvent(SerialPortEvent event) {
		asynchReadLock.lock();
		try {
			if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
				try {
					if (!isConsumerAvailable) {
						consumerAvailable.await();
					}
					dataAvailable.signal();
					dataConsumed.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Type:" + event.getEventType() + " from:" + event.getOldValue() + " to:" + event.getNewValue());
			} 
		} finally {
			asynchReadLock.unlock();
		}
	}
}
