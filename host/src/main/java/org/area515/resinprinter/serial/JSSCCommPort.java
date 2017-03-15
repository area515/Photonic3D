package org.area515.resinprinter.serial;

import java.io.IOException;

import jssc.SerialPort;
import jssc.SerialPortException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.ComPortSettings;
import org.area515.resinprinter.printer.Printer;

public class JSSCCommPort implements SerialCommunicationsPort {
    private static final Logger logger = LogManager.getLogger();
	private SerialPort port;
	private String name;
	private ComPortSettings settings;
	private int timeout;
	
	@Override
	public void open(String controllingDevice, int timeout,
			ComPortSettings settings) throws AlreadyAssignedException,
			InappropriateDeviceException {
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
		
		this.timeout = timeout;
		this.settings = new ComPortSettings(settings);
		this.port = new SerialPort(settings.getPortName());
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
			port.setParams(settings.getSpeed().intValue(), settings.getDatabits(), stopBits, parity);
			if (!port.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR)) {
				throw new InappropriateDeviceException("Comm port couldn't be purged:" + settings.getPortName());
			}
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
			port.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
		} catch (SerialPortException e) {
			logger.error("Error closing:" + port.getPortName(), e);
		} finally {
			try {
				port.closePort();
			} catch (SerialPortException e) {
				//We don't really care if the comm port is closed...
			}
		}
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void write(byte[] gcode) throws IOException {
		try {
			port.writeBytes(gcode);
		} catch (SerialPortException e) {
			throw new IOException("Couldn't write gcode to " + name, e);
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
	
	private void printDiagnostic() {
		try {
			logger.info("Printing diagnostic for:" + this.settings);
			logger.info("open:" + port.isOpened());
			logger.info("CTS:" + port.isCTS());
			logger.info("DSR:" + port.isDSR());
			logger.info("RING:" + port.isRING());
			logger.info("RLSD:" + port.isRLSD());
			logger.info("flow:" + port.getFlowControlMode());
		} catch (SerialPortException e) {
			logger.error("Couldn't print diagnostic", e);
		}
	}
	
	@Override
	public void restartCommunications() throws AlreadyAssignedException, InappropriateDeviceException {
		printDiagnostic();
		close();
		open(name, timeout, settings);
		printDiagnostic();
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
		JSSCCommPort other = (JSSCCommPort) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
