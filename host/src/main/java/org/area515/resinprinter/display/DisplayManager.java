package org.area515.resinprinter.display;

import java.awt.AWTError;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.server.HostProperties;

public class DisplayManager {
    private static final Logger logger = LogManager.getLogger();

    private static DisplayManager INSTANCE = null;
	
	private GraphicsEnvironment ge = null;
	private ConcurrentHashMap<Printer, String> displayIdsByPrinter = new ConcurrentHashMap<Printer, String>();
	private ConcurrentHashMap<String, Printer> printersByDisplayIDString = new ConcurrentHashMap<String, Printer>();

	public static DisplayManager Instance() {
		if (INSTANCE == null) {
			INSTANCE = new DisplayManager();
		}
		return INSTANCE;
	}
	
	private DisplayManager(){
	}

	public boolean isGraphicsDeviceDisplayAvailable(String displayId) {
		try {
			GraphicsOutputInterface display = getDisplayDevice(displayId);
			return !printersByDisplayIDString.containsKey(display.getIDstring());
		} catch (InappropriateDeviceException e) {
			throw new IllegalArgumentException("Couldn't getDisplayDevice for:" + displayId, e);
		}
	}
	
	public void assignDisplay(Printer newPrinter, GraphicsOutputInterface device) throws AlreadyAssignedException, InappropriateDeviceException {
		String nextIdString = device.buildIDString();//Note: Do NOT call getIDstring() since it's not appropriate
		if (nextIdString == null) {
			throw new InappropriateDeviceException(device + " didn't return an available display");
		}
		String otherDevice = displayIdsByPrinter.putIfAbsent(newPrinter, nextIdString);
		if (otherDevice != null) {
			throw new AlreadyAssignedException("Printer already assigned to:" + otherDevice, otherDevice);
		}
		
		Printer otherJob = printersByDisplayIDString.putIfAbsent(nextIdString, newPrinter);
		if (otherJob != null) {
			displayIdsByPrinter.remove(newPrinter);
			throw new AlreadyAssignedException("Display already assigned to:" + otherJob, otherJob);
		}

		newPrinter.initializeAndAssignGraphicsOutputInterface(device, nextIdString);
		newPrinter.showBlankImage();
		logger.info("Display:{} assigned to Printer:{}", device, newPrinter);
	}
	
	public List<GraphicsOutputInterface> getDisplayDevices() {
		List<GraphicsOutputInterface> devices = new ArrayList<GraphicsOutputInterface>();
		try {
			for (GraphicsDevice device : getGraphicsEnvironment().getScreenDevices()) {
				devices.add(new GraphicsDeviceOutputInterface(device.getIDstring(), device));
			}
		} catch (InappropriateDeviceException e) {
			logger.error("Continuing after error...", e);
		}
		
		devices.addAll(HostProperties.Instance().getDisplayDevices());
		return devices;
	}

	public GraphicsOutputInterface getDisplayDevice(String deviceId) throws InappropriateDeviceException {
		GraphicsOutputInterface newDevice = null;
		for (GraphicsOutputInterface currentDevice : getDisplayDevices()) {
			if (currentDevice.getIDstring().equals(deviceId)) {
				newDevice = currentDevice;
			}
		}
		
		return newDevice;
	}	
	
	public GraphicsOutputInterface getDisplayDevice(int index) throws InappropriateDeviceException {
		return getDisplayDevice(getGraphicsEnvironment().getScreenDevices()[index].getIDstring());
	}
	
	GraphicsEnvironment getGraphicsEnvironment() throws InappropriateDeviceException {
		if (ge != null) {
			return ge;
		}
		
		try {
			ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		} catch (NoClassDefFoundError | HeadlessException | AWTError error) {
			throw new InappropriateDeviceException("It doesn't look like your graphics environment is properly setup", error);
		}

		return ge;
	}
	
	public void removeAssignment(Printer printer){
		if (printer == null)
			return;
		
		String removalId = printer.getDisplayDeviceID();
		printer.disassociateDisplay();
		
		String otherId = displayIdsByPrinter.remove(printer);
		if (otherId != removalId && !otherId.equals(removalId)) {
			logger.error("otherId:" + otherId + " different than printerDisplayId:" + removalId);
		}
		
		if (removalId != null) {
			Printer otherPrinter = printersByDisplayIDString.remove(removalId);
			if (!printer.equals(otherPrinter)) {
				logger.error("otherPrinter:" + otherPrinter + " different than printer:" + printer);
			}
		}
	}
}