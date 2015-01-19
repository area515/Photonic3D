package org.area515.resinprinter.serial;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration.ComPortSettings;

public class RXTXEventBasedCommPort implements SerialCommunicationsPort, SerialPortEventListener {
	private String name = null;
	private InputStream inputStream;
	private OutputStream outputStream;
	private SerialPort serialPort;
	private long waitForGCodeTimeout = 0;
	private Lock asynchReadLock = new ReentrantLock();
	private Condition dataAvailable = asynchReadLock.newCondition();
	private Condition consumerAvailable = asynchReadLock.newCondition();
	private Condition dataConsumed = asynchReadLock.newCondition();
	private boolean isConsumerAvailable = false;
	
	@Override
	public void open(String printerName, int timeout, ComPortSettings settings) throws AlreadyAssignedException, InappropriateDeviceException {
		String portName = settings.getPortName();
		try {
			this.waitForGCodeTimeout = 1000 * 60 * 2;//Maximum time for a single gcode to execute.
			CommPortIdentifier identifier = CommPortIdentifier.getPortIdentifier(portName);
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort)identifier.open(printerName, timeout);
			serialPort.enableReceiveTimeout(timeout);
			serialPort.addEventListener(this);

			int parity = 0;
			if (settings.getParity().equals("None")) {
				parity = SerialPort.PARITY_NONE;
			} else if (settings.getParity().equals("Even")) {
				parity = SerialPort.PARITY_EVEN;
			} else if (settings.getParity().equals("Mark")) {
				parity = SerialPort.PARITY_MARK;
			} else if (settings.getParity().equals("Odd")) {
				parity = SerialPort.PARITY_ODD;
			} else if (settings.getParity().equals("Space")) {
				parity = SerialPort.PARITY_SPACE;
			}				
			int stopBits = 0;
			if (settings.getStopbits().equalsIgnoreCase("One") || settings.getStopbits().equals("1")) {
				stopBits = SerialPort.STOPBITS_1;
			} else if (settings.getStopbits().equals("1.5")) {
				stopBits = SerialPort.STOPBITS_1_5;
			} else if (settings.getStopbits().equalsIgnoreCase("Two") || settings.getStopbits().equals("2")) {
				stopBits = SerialPort.STOPBITS_2;
			}
			serialPort.setSerialPortParams((int)settings.getSpeed(), settings.getDatabits(), stopBits, parity);

			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();
		} catch (TooManyListenersException e) {
			throw new InappropriateDeviceException("Port doesn't support listeners:" + portName, e);
		} catch (PortInUseException e) {
			throw new AlreadyAssignedException("Comport already assigned another process:" + e.currentOwner, (Printer)null);
		} catch (UnsupportedCommOperationException e) {
			throw new InappropriateDeviceException("Port doesn't support an open or setting of port parameters:" + portName, e);
		} catch (IOException e) {
			throw new InappropriateDeviceException("Problem getting streams from serialPort:" + portName, e);
		} catch (NoSuchPortException e) {
			throw new InappropriateDeviceException("Comm port not found:" + portName, e);
		}
	}

	@Override
	public void close() {
		if (inputStream != null) {
			try {inputStream.close();} catch (IOException e) {}
		}		
		if (outputStream != null) {
			try {outputStream.close();} catch (IOException e) {}
		}
		serialPort.close();
	}

	@Override
	public String getName() {
		return name;
	}	
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public void write(String gcode) throws IOException {
		
		outputStream.write(gcode.getBytes());
	}

	private String readLine(Printer printer) throws IOException {
		long startTime = System.currentTimeMillis();
		StringBuilder builder = new StringBuilder();
		
		int value = -1;
		do {
			value = inputStream.read();
			if (value > -1) {
				builder.append((char)value);
			}
		} while (System.currentTimeMillis() - startTime < waitForGCodeTimeout &&
				((value > -1 && value != '\n') || (printer != null && value == -1 && printer.isPrintInProgress())));
		if (builder.length() == 0) {
			return null;
		}
		return builder.toString();
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
				response = readLine(null);
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
