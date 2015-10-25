package org.area515.resinprinter.printer;


import java.awt.GraphicsDevice;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;

public class PrinterManager {
	private static PrinterManager INSTANCE;

	private ConcurrentHashMap<Printer, PrintJob> printJobsByPrinter = new ConcurrentHashMap<Printer, PrintJob>();
	private ConcurrentHashMap<String, Printer> printersByName = new ConcurrentHashMap<String, Printer>();
	private ConcurrentHashMap<PrintJob, Printer> printersByJob = new ConcurrentHashMap<PrintJob, Printer>();
	
	public static PrinterManager Instance() {
		if (INSTANCE == null) {
			INSTANCE = new PrinterManager();
		}
		return INSTANCE;
	}

	private PrinterManager() {
		//We can't load all the printers by default!
		/*for (PrinterConfiguration currentConfiguration : HostProperties.Instance().getPrinterConfigurations()) {
			Printer printer = null;
			try {
				printer = new Printer(currentConfiguration);
				GraphicsDevice graphicsDevice = DisplayManager.Instance().getDisplayDevice(printer.getConfiguration().getDisplayIndex());
				if (graphicsDevice == null) {
					throw new JobManagerException("Couldn't find graphicsDevice called:" + currentConfiguration.getName());
				}
				DisplayManager.Instance().assignDisplay(printer, graphicsDevice);
				
				String comportId = printer.getConfiguration().getMotorsDriverConfig().getComPortSettings().getPortName();
				CommPortIdentifier port = SerialManager.Instance().getSerialDevice(comportId);
				if (port == null) {
					throw new JobManagerException("Couldn't find communications device called:" + comportId);
				}
				
				SerialManager.Instance().assignSerialPort(printer, port);
			} catch (JobManagerException e) {
				DisplayManager.Instance().removeAssignment(printer);
				SerialManager.Instance().removeAssignment(printer);
				if (printer != null) {
					printer.close();
				}
				e.printStackTrace();
			} catch (AlreadyAssignedException e) {
				DisplayManager.Instance().removeAssignment(printer);
				SerialManager.Instance().removeAssignment(printer);
				if (printer != null) {
					printer.close();
				}
				e.printStackTrace();
			} catch (InappropriateDeviceException e) {
				DisplayManager.Instance().removeAssignment(printer);
				SerialManager.Instance().removeAssignment(printer);
				if (printer != null) {
					printer.close();
				}
				e.printStackTrace();
			}
		}*/
	}
	
	public Printer getPrinter(String name) {
		return printersByName.get(name);
	}
	
	public void stopPrinter(Printer printer) throws InappropriateDeviceException {
		if (printer.isPrintInProgress()) {
			throw new InappropriateDeviceException("Can't stop printer while printer:" + printer + " is in status:" + printer.getStatus());
		}

		printersByName.remove(printer.getName());
		printer.close();
	}
	
	public Printer startPrinter(PrinterConfiguration currentConfiguration) throws JobManagerException, AlreadyAssignedException, InappropriateDeviceException {
		Printer printer = null;
		if (printersByName.containsKey(currentConfiguration.getName())) {
			throw new AlreadyAssignedException("Printer already started:" + currentConfiguration.getName(), (Printer)null);
		}
		
		try {
			printer = new Printer(currentConfiguration);
			String monitorId = currentConfiguration.getMachineConfig().getOSMonitorID();
			GraphicsDevice graphicsDevice = null;
			if (monitorId != null) {
				graphicsDevice = DisplayManager.Instance().getDisplayDevice(currentConfiguration.getMachineConfig().getOSMonitorID());
			} else {
				graphicsDevice = DisplayManager.Instance().getDisplayDevice(currentConfiguration.getMachineConfig().getDisplayIndex());
			}
			
			if (graphicsDevice == null) {
				if (monitorId != null) {
					throw new JobManagerException("Couldn't find graphicsDevice called:" + monitorId);
				} else {
					throw new JobManagerException("Couldn't find graphicsDevice called:" + currentConfiguration.getMachineConfig().getDisplayIndex());
				}
			}
			DisplayManager.Instance().assignDisplay(printer, graphicsDevice);
			
			String comportId = printer.getConfiguration().getMachineConfig().getMotorsDriverConfig().getComPortSettings().getPortName();
			SerialCommunicationsPort port = SerialManager.Instance().getSerialDevice(comportId);
			if (port == null) {
				throw new JobManagerException("Couldn't find communications device called:" + comportId);
			}
			
			SerialManager.Instance().assignSerialPortToFirmware(printer, port);
			SerialManager.Instance().assignSerialPortToProjector(printer, port);
			
			printersByName.put(printer.getName(), printer);
			printer.setStarted(true);
			return printer;
		} catch (JobManagerException | AlreadyAssignedException | InappropriateDeviceException e) {
			DisplayManager.Instance().removeAssignment(printer);
			SerialManager.Instance().removeAssignments(printer);
			if (printer != null) {
				printer.close();
			}
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			DisplayManager.Instance().removeAssignment(printer);
			SerialManager.Instance().removeAssignments(printer);
			if (printer != null) {
				printer.close();
			}
			throw new InappropriateDeviceException("Internal error on server");
		}
	}
	
	public void assignPrinter(PrintJob newJob, Printer printer) throws AlreadyAssignedException {
		Printer otherPrinter = printersByJob.putIfAbsent(newJob, printer);
		if (otherPrinter != null) {
			throw new AlreadyAssignedException("Job already assigned to:" + otherPrinter.getName(), otherPrinter);
		}
		
		PrintJob otherJob = printJobsByPrinter.putIfAbsent(printer, newJob);
		if (otherJob != null) {
			printersByJob.remove(newJob);
			throw new AlreadyAssignedException("Printer already working on job:" + otherJob.getJobFile().getName(), otherJob);
		}
		
		newJob.setPrinter(printer);
	}

	public void removeAssignment(PrintJob job) {
		if (job == null) {
			return;
		}
		
		printersByJob.remove(job);
		Printer printer = job.getPrinter();
		if (printer != null) {
			printJobsByPrinter.remove(printer);
			job.setPrinter(null);
		}
	}
	
	public List<Printer> getPrinters() {
		return new ArrayList<Printer>(printersByName.values());
	}
}