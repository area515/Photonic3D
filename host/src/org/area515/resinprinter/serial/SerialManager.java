package org.area515.resinprinter.serial;


import gnu.io.CommPortIdentifier;
import gnu.io.ConsoleCommPortIdentifier;
import gnu.io.ConsoleSerialPort;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration.ComPortSettings;
import org.area515.resinprinter.server.HostProperties;

public class SerialManager {
	private static SerialManager INSTANCE = null;
	
	private ConcurrentHashMap<Printer, CommPortIdentifier> serialPortsByPrinter = new ConcurrentHashMap<Printer, CommPortIdentifier>();
	private ConcurrentHashMap<CommPortIdentifier, Printer> printersBySerialPort = new ConcurrentHashMap<CommPortIdentifier, Printer>();

	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;
	/** Default bits per second for COM port. */
	private static final int DATA_RATE = 9600;
	
	public static SerialManager Instance() {
		if (INSTANCE == null) {
			INSTANCE = new SerialManager();
		}
		return INSTANCE;
	}
	
	private SerialManager() {
	}
	
	public void assignSerialPort(Printer printer, CommPortIdentifier identifier) throws AlreadyAssignedException, InappropriateDeviceException {
		if (identifier.getPortType() != CommPortIdentifier.PORT_SERIAL) {
			throw new InappropriateDeviceException(identifier + " is not a serial port");
		}
		
		CommPortIdentifier otherIdentifier = serialPortsByPrinter.putIfAbsent(printer, identifier);
		if (otherIdentifier != null) {
			throw new AlreadyAssignedException("Job already assigned to this SerialPort:" + otherIdentifier, otherIdentifier);
		}
		
		Printer otherPrintJob = printersBySerialPort.putIfAbsent(identifier, printer);
		if (otherPrintJob != null) {
			serialPortsByPrinter.remove(printer);
			throw new AlreadyAssignedException("SerialPort already assigned to this job:" + otherPrintJob, otherPrintJob);
		}
		
		try {
			SerialPort serialPort = null;
			if (identifier instanceof ConsoleCommPortIdentifier) {
				serialPort = new ConsoleSerialPort();
			} else {
				// open serial port, and use class name for the appName.
				serialPort = (SerialPort)identifier.open(printer.getName(), TIME_OUT);
				serialPort.enableReceiveTimeout(TIME_OUT);
				// set port parameters
				ComPortSettings settings = printer.getConfiguration().getMotorsDriverConfig().getComPortSettings();
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
			}

			printer.setSerialPort(serialPort);
		} catch (PortInUseException e) {
			throw new AlreadyAssignedException("Comport already assigned another process:" + e.currentOwner, (Printer)null);
		} catch (UnsupportedCommOperationException e) {
			throw new InappropriateDeviceException("Port doesn't support an open or setting of port parameters:" + identifier.getName(), e);
		} catch (IOException e) {
			throw new InappropriateDeviceException("Problem getting streams from serialPort:" + identifier, e);
		}
	}
	
	public CommPortIdentifier getSerialDevice(String comport) throws InappropriateDeviceException {
		if (comport.equals(ConsoleCommPortIdentifier.NAME)) {
			for (CommPortIdentifier current : getSerialDevices()) {
				if  (current.getName().equals(ConsoleCommPortIdentifier.NAME)) {
					return current;
				}
			}
		}
		
		try {
			return CommPortIdentifier.getPortIdentifier(comport);
		} catch (NoSuchPortException e) {
			throw new InappropriateDeviceException("CommPort doesn't exist:" + comport, e);
		}
	}
	
	public List<CommPortIdentifier> getSerialDevices() {
		List<CommPortIdentifier> idents = new ArrayList<CommPortIdentifier>();
		Enumeration<CommPortIdentifier> identifiers = CommPortIdentifier.getPortIdentifiers();
		while (identifiers.hasMoreElements()) {
			CommPortIdentifier identifier = identifiers.nextElement();
			if (identifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				idents.add(identifier);
			}
		}
		
		if (HostProperties.Instance().getFakeSerial()) {
			ConsoleCommPortIdentifier consolePort = new ConsoleCommPortIdentifier();
			idents.add(consolePort);
		}
		
		return idents;
	}
	
	/**
	 * This should be called when you stop using the port.
	 * This will prevent port locking on platforms like Linux.
	 */
	public void removeAssignment(Printer printer) {
		if (printer == null)
			return;
		
		CommPortIdentifier identifier = serialPortsByPrinter.remove(printer);
		if (identifier != null) {
			printersBySerialPort.remove(identifier);
		}
	}
}