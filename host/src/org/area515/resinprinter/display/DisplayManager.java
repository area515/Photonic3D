package org.area515.resinprinter.display;

import java.awt.Cursor;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;

import org.area515.resinprinter.job.PrintJob;

public class DisplayManager {
	private static DisplayManager INSTANCE = null;
	
	private GraphicsEnvironment ge = null;
	private ConcurrentHashMap<GraphicsDevice, PrintJob> jobsByDevice = new ConcurrentHashMap<GraphicsDevice, PrintJob>();
	private ConcurrentHashMap<PrintJob, GraphicsDevice> devicesByJob = new ConcurrentHashMap<PrintJob, GraphicsDevice>();

	public static DisplayManager Instance() {
		if (INSTANCE == null) {
			INSTANCE = new DisplayManager();
		}
		return INSTANCE;
	}
	
	private DisplayManager(){
		ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	}

	public void assignDisplay(PrintJob newJob, GraphicsDevice device) throws AlreadyAssignedException, InappropriateDeviceException {
		GraphicsDevice otherDevice = devicesByJob.putIfAbsent(newJob, device);
		if (otherDevice != null) {
			throw new AlreadyAssignedException("Job already assigned to:" + otherDevice.getIDstring(), otherDevice);
		}

		PrintJob otherJob = jobsByDevice.putIfAbsent(device, newJob);
		if (otherJob != null) {
			devicesByJob.remove(device);
			throw new AlreadyAssignedException("Display already assigned to:" + otherJob, otherJob);
		}
		
		if (!device.isFullScreenSupported()) {
			throw new InappropriateDeviceException("Full screen not supported");
		}
		
		//kill the window decorations
		JFrame window = new JFrame();
		window.setUndecorated(true);
		device.setFullScreenWindow(window);
		newJob.setGraphicsData(window, device.getDefaultConfiguration());
		
		// hide mouse in full screen
		Toolkit toolkit = Toolkit.getDefaultToolkit();
	    Point hotSpot = new Point(0,0);
	    BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT); 
	    Cursor invisibleCursor = toolkit.createCustomCursor(cursorImage, hotSpot, "InvisibleCursor");        
	    window.setCursor(invisibleCursor);
		newJob.showBlankImage();
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
	
	public void removeAssignment(PrintJob job){
		if (job == null)
			return;
		
		devicesByJob.remove(job);
		
		GraphicsDevice device = job.getGraphicsDevice();
		if (device == null)
			return;
		
		jobsByDevice.remove(device);
	}
}