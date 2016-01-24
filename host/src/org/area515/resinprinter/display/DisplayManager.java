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
	public static final String LAST_AVAILABLE_DISPLAY = "Last available display";
	public static final String SIMULATED_DISPLAY = "Simulated display";
	
	private GraphicsEnvironment ge = null;
	private ConcurrentHashMap<Printer, GraphicsDevice> graphicsDevicesByPrinter = new ConcurrentHashMap<Printer, GraphicsDevice>();
	private ConcurrentHashMap<String, Printer> printersByDisplayIDString = new ConcurrentHashMap<String, Printer>();

	public static DisplayManager Instance() {
		if (INSTANCE == null) {
			INSTANCE = new DisplayManager();
		}
		return INSTANCE;
	}
	
	private DisplayManager(){
	}

	public void assignDisplay(Printer newPrinter, GraphicsDevice device) throws AlreadyAssignedException, InappropriateDeviceException {
		if (device.getIDstring().equals(LAST_AVAILABLE_DISPLAY)) {
			ArrayList<GraphicsDevice> devices = new ArrayList<GraphicsDevice>();
			devices.addAll(Arrays.asList(getGraphicsEnvironment().getScreenDevices()));
			Collections.reverse(devices);
			for (GraphicsDevice currentDevice : devices) {
				if (!printersByDisplayIDString.containsKey(currentDevice.getIDstring())) {
					device = currentDevice;
					break;
				}
			}
			
			if (device.getIDstring().equals(LAST_AVAILABLE_DISPLAY)) {
				throw new InappropriateDeviceException("No displays left to assign");
			}
		}
		
		GraphicsDevice otherDevice = graphicsDevicesByPrinter.putIfAbsent(newPrinter, device);
		if (otherDevice != null) {
			throw new AlreadyAssignedException("Printer already assigned to:" + otherDevice.getIDstring(), otherDevice);
		}
		
		Printer otherJob = printersByDisplayIDString.putIfAbsent(device.getIDstring(), newPrinter);
		if (otherJob != null) {
			graphicsDevicesByPrinter.remove(newPrinter);
			throw new AlreadyAssignedException("Display already assigned to:" + otherJob, otherJob);
		}

		newPrinter.setGraphicsData(device);
		newPrinter.showBlankImage();
		logger.info("Display:{} assigned to Printer:{}", device, newPrinter);
	}
	
	public List<GraphicsDevice> getDisplayDevices() {
		List<GraphicsDevice> devices = new ArrayList<GraphicsDevice>();
		try {
			devices.addAll(Arrays.asList(getGraphicsEnvironment().getScreenDevices()));
		} catch (InappropriateDeviceException e) {
			logger.error("Continuing after error...", e);
		}
		
		devices.add(new CustomNamedDisplayDevice(LAST_AVAILABLE_DISPLAY));
		if (HostProperties.Instance().getFakeDisplay()) {
			devices.add(new CustomNamedDisplayDevice(SIMULATED_DISPLAY));
		}
		
		return devices;
	}

	public GraphicsDevice getDisplayDevice(String deviceId) throws InappropriateDeviceException {
		GraphicsDevice newDevice = null;
		for (GraphicsDevice currentDevice : getDisplayDevices()) {
			if (currentDevice.getIDstring().equals(deviceId)) {
				newDevice = currentDevice;
			}
		}
		
		return newDevice;
	}	
	
	public GraphicsDevice getDisplayDevice(int index) throws InappropriateDeviceException {
		return getGraphicsEnvironment().getScreenDevices()[index];
	}
	
	GraphicsEnvironment getGraphicsEnvironment() throws InappropriateDeviceException {
		if (ge != null) {
			return ge;
		}
		
		try {
			ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		} catch (HeadlessException | AWTError error) {
			throw new InappropriateDeviceException("It doesn't look like your graphics environment is properly setup", error);
		}

		return ge;
	}
	
	public void removeAssignment(Printer printer){
		if (printer == null)
			return;
		
		graphicsDevicesByPrinter.remove(printer);
		
		String deviceId = printer.getDisplayDeviceID();
		if (deviceId == null)
			return;
		
		printersByDisplayIDString.remove(deviceId);
	}
}