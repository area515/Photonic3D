package org.area515.resinprinter.display;

import java.awt.Cursor;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;

import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;

public class DisplayManager {
	private static DisplayManager INSTANCE = null;
	
	private GraphicsEnvironment ge = null;
	private ConcurrentHashMap<GraphicsDevice, Printer> printersByGraphicsDevice = new ConcurrentHashMap<GraphicsDevice, Printer>();
	private ConcurrentHashMap<Printer, GraphicsDevice> graphicsDevicesByPrinter = new ConcurrentHashMap<Printer, GraphicsDevice>();

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
		GraphicsDevice otherDevice = graphicsDevicesByPrinter.putIfAbsent(newPrinter, device);
		if (otherDevice != null) {
			throw new AlreadyAssignedException("Printer already assigned to:" + otherDevice.getIDstring(), otherDevice);
		}
		
		/*TODO: Doesn't work in Linux it goes into simulated full screen mode, not exclusive mode
		if (!device.isFullScreenSupported()) {
			throw new InappropriateDeviceException("Full screen not supported");
		}*/
		

		Printer otherJob = printersByGraphicsDevice.putIfAbsent(device, newPrinter);
		if (otherJob != null) {
			graphicsDevicesByPrinter.remove(newPrinter);
			throw new AlreadyAssignedException("Display already assigned to:" + otherJob, otherJob);
		}
		
		//kill the window decorations
		JFrame window = new JFrame();
		window.setUndecorated(true);
		device.setFullScreenWindow(window);
		newPrinter.setGraphicsData(window, device.getDefaultConfiguration());
		
		// hide mouse in full screen
		Toolkit toolkit = Toolkit.getDefaultToolkit();
	    Point hotSpot = new Point(0,0);
	    BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT); 
	    Cursor invisibleCursor = toolkit.createCustomCursor(cursorImage, hotSpot, "InvisibleCursor");        
	    window.setCursor(invisibleCursor);
		newPrinter.showBlankImage();
	}
	
	public List<GraphicsDevice> getDisplayDevices() {
		return Arrays.asList(ge.getScreenDevices());
	}

	public GraphicsDevice getDisplayDevice(String deviceId) throws InappropriateDeviceException {
		GraphicsDevice newDevice = null;
		for (GraphicsDevice currentDevice : ge.getScreenDevices()) {
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
		
		GraphicsDevice device = printer.getGraphicsDevice();
		if (device == null)
			return;
		
		printersByGraphicsDevice.remove(device);
	}
}