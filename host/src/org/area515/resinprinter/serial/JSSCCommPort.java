package org.area515.resinprinter.serial;

import java.io.IOException;

import jssc.SerialPort;
import jssc.SerialPortException;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.MachineConfig.ComPortSettings;
import org.area515.resinprinter.printer.Printer;

public class JSSCCommPort implements SerialCommunicationsPort {
	private SerialPort port;
	private String cwhName;
	
	@Override
	public void open(String controllingDevice, int timeout,
			ComPortSettings settings) throws AlreadyAssignedException,
			InappropriateDeviceException {
		port = new SerialPort(settings.getPortName());
		int parity = 0;
		if (settings.getParity().equalsIgnoreCase("EVEN")) {
			parity = SerialPort.PARITY_EVEN;
		} else if (settings.getParity().equalsIgnoreCase("MARK")) {
			parity = SerialPort.PARITY_MARK;
		} else if (settings.getParity().equalsIgnoreCase("NONE")) {
			parity = SerialPort.PARITY_NONE;
		} else if (settings.getParity().equalsIgnoreCase("ODD")) {
			parity = SerialPort.PARITY_ODD;
		} else if (settings.getParity().equalsIgnoreCase("SPACE")) {
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
		try {
			port.openPort();
			port.setParams((int)settings.getSpeed(), settings.getDatabits(), stopBits, parity);
		} catch (SerialPortException e) {
			if (e.getExceptionType().equals(SerialPortException.TYPE_PORT_BUSY) ||
				e.getExceptionType().equals(SerialPortException.TYPE_PORT_ALREADY_OPENED)) {
				throw new AlreadyAssignedException("Comport already assigned to another process", (Printer)null);
			} else if (e.getExceptionType().equals(SerialPortException.TYPE_PORT_NOT_FOUND)) {
				throw new InappropriateDeviceException("Comm port not found:" + settings.getPortName(), e);
			} else if (e.getExceptionType().equals(SerialPortException.TYPE_INCORRECT_SERIAL_PORT)) {
				throw new InappropriateDeviceException("Port doesn't support an open or setting of port parameters:" + settings.getPortName(), e);
			} else if (e.getExceptionType().equals(SerialPortException.TYPE_PERMISSION_DENIED)) {
				throw new InappropriateDeviceException("You don't have permissions to open this port:" + settings.getPortName(), e);
			}
		}
	}

	@Override
	public void close() {
		try {
			port.closePort();
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setName(String name) {
		cwhName = name;
	}

	@Override
	public String getName() {
		return cwhName;
	}

	@Override
	public void write(byte[] gcode) throws IOException {
		try {
			port.writeBytes(gcode);
		} catch (SerialPortException e) {
			throw new IOException("Couldn't write gcode to " + cwhName, e);
		}
	}

	@Override
	public byte[] read() throws IOException {
		try {
			if (port.getInputBufferBytesCount() > 0) {
				return port.readBytes(port.getInputBufferBytesCount());
			}
			
			return null;
		} catch (SerialPortException e) {
			throw new IOException("Couldn't read bytes from serial port.", e);
		}
	}
}
