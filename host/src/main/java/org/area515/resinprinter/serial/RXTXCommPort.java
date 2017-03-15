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
import org.area515.resinprinter.printer.ComPortSettings;
import org.area515.resinprinter.printer.Printer;

public abstract class RXTXCommPort implements SerialCommunicationsPort {
	private String name = null;
	protected InputStream inputStream;
	private OutputStream outputStream;
	private SerialPort serialPort;
	private ComPortSettings settings;
	private int timeout;
	
	@Override
	public void open(String printerName, int timeout, ComPortSettings settings) throws AlreadyAssignedException, InappropriateDeviceException {
		if (settings == null) {
			throw new InappropriateDeviceException("Port settings haven't been configured for this device.");
		}
		if (settings.getPortName() == null) {
			throw new InappropriateDeviceException("Port name hasn't been configured for this device.");
		}
		if (settings.getParity() == null) {
			throw new InappropriateDeviceException("Parity hasn't been configured for this device(" + settings.getPortName() + ").");
		}
		if (settings.getStopbits() == null) {
			throw new InappropriateDeviceException("Stopbits havn't been configured for this device(" + settings.getPortName() + ").");
		}
		if (settings.getDatabits() == null) {
			throw new InappropriateDeviceException("Databits havn't been configured for this device(" + settings.getPortName() + ").");
		}
		if (settings.getSpeed() == null || settings.getSpeed() == 0) {
			throw new InappropriateDeviceException("Speed hasn't been configured for this device(" + settings.getPortName() + ").");
		}
		
		this.settings = settings;
		this.timeout = timeout;
		String portName = settings.getPortName();
		try {
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
			serialPort.setSerialPortParams(settings.getSpeed().intValue(), settings.getDatabits(), stopBits, parity);

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
		if (serialPort != null) {
			serialPort.close();
		}
	}

	@Override
	public String getName() {
		return name;
	}	
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public void write(byte[] gcode) throws IOException {
		outputStream.write(gcode);
	}

	@Override
	public void restartCommunications() throws AlreadyAssignedException, InappropriateDeviceException {
		close();
		open(name, timeout, settings);
	}

	public String toString() {
		return name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RXTXCommPort other = (RXTXCommPort) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
