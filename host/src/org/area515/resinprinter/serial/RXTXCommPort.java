package org.area515.resinprinter.serial;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration.ComPortSettings;

public abstract class RXTXCommPort implements SerialCommunicationsPort {
	private String name = null;
	private InputStream inputStream;
	private OutputStream outputStream;
	private SerialPort serialPort;
	protected long waitForGCodeTimeout = 0;

	@Override
	public void open(String printerName, int timeout, ComPortSettings settings) throws AlreadyAssignedException, InappropriateDeviceException {
		String portName = settings.getPortName();
		try {
			this.waitForGCodeTimeout = 1000 * 60 * 2;//Maximum time for a single gcode to execute.
			CommPortIdentifier identifier = CommPortIdentifier.getPortIdentifier(portName);
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort)identifier.open(printerName, timeout);
			serialPort.enableReceiveTimeout(timeout);
			init(serialPort);
			
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
	
	public abstract void init(SerialPort serialPort) throws TooManyListenersException;
	
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

	protected String readLine(Printer printer) throws IOException {
		long startTime = System.currentTimeMillis();
		StringBuilder builder = new StringBuilder();
		
		int value = -1;
		do {
			value = inputStream.read();
			if (value > -1) {
				builder.append((char)value);
			}
		} while (System.currentTimeMillis() - startTime < waitForGCodeTimeout || 
				(value > -1 && value != '\n') || 
				(printer != null && value == -1 && printer.isPrintInProgress()));
		if (builder.length() == 0) {
			return null;
		}
		return builder.toString();
	}
}
