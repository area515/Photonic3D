package org.area515.resinprinter.display;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;

import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.server.HostProperties;

public class DisplayManager {
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
		ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	}

	public void assignDisplay(Printer newPrinter, GraphicsDevice device) throws AlreadyAssignedException, InappropriateDeviceException {
		if (device.getIDstring().equals(LAST_AVAILABLE_DISPLAY)) {
			ArrayList<GraphicsDevice> devices = new ArrayList<GraphicsDevice>();
			devices.addAll(Arrays.asList(ge.getScreenDevices()));
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
		
		//kill the window decorations
		JFrame window = new JFrame();
		if (device.getIDstring().equalsIgnoreCase(SIMULATED_DISPLAY)) {
			window.setTitle("Printer Simulation");
			window.setVisible(true);
			window.setExtendedState(JFrame.MAXIMIZED_BOTH);
			window.setMinimumSize(new Dimension(500, 500));
			
			newPrinter.setGraphicsData(window, window.getGraphicsConfiguration(), device.getIDstring());
		} else {
			window.setUndecorated(true);
			device.setFullScreenWindow(window);
			newPrinter.setGraphicsData(window, device.getDefaultConfiguration(), device.getIDstring());
			
			//This can't be done in the setGraphicsData() method since it would reassign the printer Simulation
			newPrinter.getConfiguration().setOSMonitorID(device.getDefaultConfiguration().getDevice().getIDstring());
			
			// hide mouse in full screen
			Toolkit toolkit = Toolkit.getDefaultToolkit();
		    Point hotSpot = new Point(0,0);
		    BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT); 
		    Cursor invisibleCursor = toolkit.createCustomCursor(cursorImage, hotSpot, "InvisibleCursor");        
		    window.setCursor(invisibleCursor);
		}

		newPrinter.showBlankImage();
	}
	
	public List<GraphicsDevice> getDisplayDevices() {
		List<GraphicsDevice> devices = new ArrayList<GraphicsDevice>();
		devices.addAll(Arrays.asList(ge.getScreenDevices()));
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
		return ge.getScreenDevices()[index];
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