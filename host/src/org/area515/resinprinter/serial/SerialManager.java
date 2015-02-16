package org.area515.resinprinter.serial;


import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration.ComPortSettings;
import org.area515.resinprinter.server.HostProperties;

public class SerialManager {
	private static SerialManager INSTANCE = null;
	public static final String FIRST_AVAILABLE_PORT = "First available serial port";
	private ConcurrentHashMap<Printer, SerialCommunicationsPort> serialPortsByPrinter = new ConcurrentHashMap<Printer, SerialCommunicationsPort>();
	private ConcurrentHashMap<SerialCommunicationsPort, Printer> printersBySerialPort = new ConcurrentHashMap<SerialCommunicationsPort, Printer>();

	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;
	
	public static SerialManager Instance() {
		if (INSTANCE == null) {
			INSTANCE = new SerialManager();
		}
		return INSTANCE;
	}
	
	private SerialManager() {
	}
	
	public void assignSerialPort(Printer printer, SerialCommunicationsPort identifier) throws AlreadyAssignedException, InappropriateDeviceException {
		ComPortSettings newComPortSettings = new ComPortSettings(printer.getConfiguration().getMotorsDriverConfig().getComPortSettings());
		if (identifier.getName().equals(FIRST_AVAILABLE_PORT)) {
			identifier = null;
			ArrayList<CommPortIdentifier> identifiers = new ArrayList<CommPortIdentifier>(Collections.list(CommPortIdentifier.getPortIdentifiers()));
			for (CommPortIdentifier currentIdentifier : identifiers) {
				SerialCommunicationsPort check = getSerialDevice(currentIdentifier.getName());
				if (!printersBySerialPort.containsKey(check)) {
					identifier = check;
				}
			}
			if (identifier == null) {
				throw new InappropriateDeviceException("No serial ports are available for auto assignment");
			}
			
			newComPortSettings.setPortName(identifier.getName());
		}
		
		SerialCommunicationsPort otherIdentifier = serialPortsByPrinter.putIfAbsent(printer, identifier);
		if (otherIdentifier != null) {
			throw new AlreadyAssignedException("Job already assigned to this SerialPort:" + otherIdentifier, otherIdentifier);
		}
		
		Printer otherPrintJob = printersBySerialPort.putIfAbsent(identifier, printer);
		if (otherPrintJob != null) {
			serialPortsByPrinter.remove(printer);
			throw new AlreadyAssignedException("SerialPort already assigned to this job:" + otherPrintJob, otherPrintJob);
		}
		
		identifier.open(printer.getName(), TIME_OUT, newComPortSettings);
		printer.setSerialPort(identifier);
	}
	
	public SerialCommunicationsPort getSerialDevice(String comport) throws InappropriateDeviceException {
		for (SerialCommunicationsPort current : getSerialDevices()) {
			if  (current.getName().equals(comport)) {
				return current;
			}
		}
		
		if (comport.equals(FIRST_AVAILABLE_PORT)) {
			return new CustomCommPort(FIRST_AVAILABLE_PORT);
		}
		
		throw new InappropriateDeviceException("CommPort doesn't exist:" + comport);
	}
	
	public List<SerialCommunicationsPort> getSerialDevices() {
		List<SerialCommunicationsPort> idents = new ArrayList<SerialCommunicationsPort>();
		Enumeration<CommPortIdentifier> identifiers = CommPortIdentifier.getPortIdentifiers();
		while (identifiers.hasMoreElements()) {
			CommPortIdentifier identifier = identifiers.nextElement();
			if (identifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				Class<SerialCommunicationsPort> communicationsClass = HostProperties.Instance().getSerialCommunicationsClass();
				SerialCommunicationsPort comPortInstance;
				try {
					comPortInstance = communicationsClass.newInstance();
					comPortInstance.setName(identifier.getName());
					idents.add(comPortInstance);
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		
		idents.add(new CustomCommPort(FIRST_AVAILABLE_PORT));

		if (HostProperties.Instance().getFakeSerial()) {
			ConsoleCommPort consolePort = new ConsoleCommPort();
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
		
		SerialCommunicationsPort identifier = serialPortsByPrinter.remove(printer);
		if (identifier != null) {
			printersBySerialPort.remove(identifier);
		}
	}
}