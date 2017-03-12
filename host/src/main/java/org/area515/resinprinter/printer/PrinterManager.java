package org.area515.resinprinter.printer;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.display.GraphicsOutputInterface;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;

public class PrinterManager {
    private static final Logger logger = LogManager.getLogger();
	private static PrinterManager INSTANCE;

	private ConcurrentHashMap<Printer, PrintJob> printJobsByPrinter = new ConcurrentHashMap<Printer, PrintJob>();
	private ConcurrentHashMap<String, Printer> printersByName = new ConcurrentHashMap<String, Printer>();
	private ConcurrentHashMap<PrintJob, Printer> printersByJob = new ConcurrentHashMap<PrintJob, Printer>();
	private ConcurrentHashMap<String, Lock> inProgressLocksByName = new ConcurrentHashMap<String, Lock>();
	
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
				logger.error("Error loading configuration:" + currentConfiguration, e);
			} catch (AlreadyAssignedException e) {
				DisplayManager.Instance().removeAssignment(printer);
				SerialManager.Instance().removeAssignment(printer);
				if (printer != null) {
					printer.close();
				}
				logger.error("Error loading configuration:" + currentConfiguration, e);
			} catch (InappropriateDeviceException e) {
				DisplayManager.Instance().removeAssignment(printer);
				SerialManager.Instance().removeAssignment(printer);
				if (printer != null) {
					printer.close();
				}
				logger.error("Error loading configuration:" + currentConfiguration, e);
			}
		}*/
	}
	
	public Printer getPrinter(String name) {
		return printersByName.get(name);
	}
	
	public void stopPrinter(Printer printer) throws InappropriateDeviceException {
		logger.debug("Attempting to stop printer:{}", printer);
		
		if (printer.isPrintInProgress()) {
			throw new InappropriateDeviceException("Can't stop printer while printer:" + printer + " is in status:" + printer.getStatus());
		}

		printersByName.remove(printer.getName());
		printer.close();
		
		logger.debug("Stopped printer:{}", printer);
	}
	
	public Printer startPrinter(PrinterConfiguration currentConfiguration) throws JobManagerException, AlreadyAssignedException, InappropriateDeviceException {
		logger.debug("Attempting to start printer:{}", currentConfiguration);
		
		Printer printer = null;
		if (printersByName.containsKey(currentConfiguration.getName())) {
			throw new AlreadyAssignedException("Printer already started:" + currentConfiguration.getName(), (Printer)null);
		}
		
		Lock printerLock = new ReentrantLock();
		printerLock.lock();
		Lock oldLock = inProgressLocksByName.putIfAbsent(currentConfiguration.getName(), printerLock);
		if (oldLock != null) {
			if (!oldLock.tryLock()) {
				throw new JobManagerException("This printer:" + currentConfiguration.getName() + " is being started. Can't start again.");
			}
			
			//If the oldLock is still in play, we don't care about the printerLock we just made...
			printerLock = oldLock;
		}
		try {
			printer = new Printer(currentConfiguration);
			String monitorId = currentConfiguration.getMachineConfig().getOSMonitorID();
			GraphicsOutputInterface graphicsDevice = null;
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
			logger.debug("Assigned display:{} to:{}", graphicsDevice, printer);

			ComPortSettings settings = printer.getConfiguration().getMachineConfig().getMonitorDriverConfig().getComPortSettings();
			if (settings != null && settings.getPortName() != null) {
				String projectorComportId = settings.getPortName();
				SerialCommunicationsPort projectorPort = SerialManager.Instance().getSerialDevice(projectorComportId);
				if (projectorPort != null) {
					SerialManager.Instance().assignSerialPortToProjector(printer, projectorPort);
					logger.debug("Assigned projector:{} to:{}", projectorPort, printer);
				}
			}
			
			String firmwareComportId = printer.getConfiguration().getMachineConfig().getMotorsDriverConfig().getComPortSettings().getPortName();
			SerialCommunicationsPort firmwarePort = SerialManager.Instance().getSerialDevice(firmwareComportId);
			if (firmwarePort == null) {
				throw new JobManagerException("Couldn't find communications device called:" + firmwareComportId);
			}
			SerialManager.Instance().assignSerialPortToFirmware(printer, firmwarePort);
			logger.debug("Assigned 3dprinter firmware:{} to:{}", firmwarePort, printer);
			
			printersByName.put(printer.getName(), printer);
			printer.setStarted(true);
			logger.info("Printer started:{}", printer);
			return printer;
		} catch (JobManagerException | AlreadyAssignedException | InappropriateDeviceException e) {
			handleError(printer, currentConfiguration, e);
			throw e;
		} catch (Throwable e) {
			handleError(printer, currentConfiguration, e);
			throw new InappropriateDeviceException("Internal error on server", e);
		} finally {
			printerLock.unlock();
		}
	}
	
	private void handleError(Printer printer, PrinterConfiguration currentConfiguration, Throwable e) {
		logger.error("Error starting printer:" + currentConfiguration, e);
		try {
			if (printer != null) {
				printer.close();
			}
		} finally {
			DisplayManager.Instance().removeAssignment(printer);
			SerialManager.Instance().removeAssignments(printer);
		}		
	}
	
	public void assignPrinter(PrintJob newJob, Printer printer) throws AlreadyAssignedException {
		logger.debug("Attempting to assign job:{} to printer:{}", newJob, printer);
		
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
		logger.info("Assigned job:{} to printer:{}", newJob, printer);
	}

	public void removeAssignment(PrintJob job) {
		logger.debug("Attempting to dissassociate job:{} from printer", job);
		
		if (job == null) {
			return;
		}
		
		printersByJob.remove(job);
		Printer printer = job.getPrinter();
		if (printer != null) {
			printJobsByPrinter.remove(printer);
			job.setPrinter(null);
		}
		logger.info("Disassociated job:{} from printer:{}", job, printer);
	}
	
	public List<Printer> getPrinters() {
		return new ArrayList<Printer>(printersByName.values());
	}
}