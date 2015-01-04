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
import org.area515.resinprinter.server.HostProperties;

public class SerialManager {
	private static SerialManager INSTANCE = null;
	
	private ConcurrentHashMap<PrintJob, CommPortIdentifier> serialPortsByPrintJob = new ConcurrentHashMap<PrintJob, CommPortIdentifier>();
	private ConcurrentHashMap<CommPortIdentifier, PrintJob> printJobsBySerialPort = new ConcurrentHashMap<CommPortIdentifier, PrintJob>();

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
	
	public void assignSerialPort(PrintJob job, CommPortIdentifier identifier) throws AlreadyAssignedException, InappropriateDeviceException {
		if (identifier.getPortType() != CommPortIdentifier.PORT_SERIAL) {
			throw new InappropriateDeviceException(identifier + " is not a serial port");
		}
		
		CommPortIdentifier otherIdentifier = serialPortsByPrintJob.putIfAbsent(job, identifier);
		if (otherIdentifier != null) {
			throw new AlreadyAssignedException("Job already assigned to this SerialPort:" + otherIdentifier, otherIdentifier);
		}
		
		PrintJob otherPrintJob = printJobsBySerialPort.putIfAbsent(identifier, job);
		if (otherPrintJob != null && !otherPrintJob.isManual()) {
			serialPortsByPrintJob.remove(job);
			throw new AlreadyAssignedException("SerialPort already assigned to this job:" + otherPrintJob, otherPrintJob);
		}
		
		try {
			SerialPort serialPort = null;
			if (identifier instanceof ConsoleCommPortIdentifier) {
				serialPort = new ConsoleSerialPort();
			} else {
				// open serial port, and use class name for the appName.
				serialPort = (SerialPort)identifier.open(job.getJobFile().getName(), TIME_OUT);
				
				// set port parameters
				serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			}

			job.setSerialPort(serialPort);
		} catch (PortInUseException e) {
			//TODO: I don't have the PrintJob already assigned, I'd need another hashmap for that.
			throw new AlreadyAssignedException("Comport already assigned to job:" + e.currentOwner, (PrintJob)null);
		} catch (UnsupportedCommOperationException e) {
			throw new InappropriateDeviceException("Port doesn't support an open or setting of port parameters:" + identifier, e);
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
	public void removeAssignment(PrintJob job) {
		if (job == null)
			return;
		
		serialPortsByPrintJob.remove(job);
	}
}