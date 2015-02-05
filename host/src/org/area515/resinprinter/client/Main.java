package org.area515.resinprinter.client;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDevice;

public class Main {
	public static void main(String[] args) {
		URI uri = null;
		try {
			UpnpService upnpService = new UpnpServiceImpl();
			upnpService.getControlPoint().search(new STAllHeader());
			long maxLengthToWait = 7000;
			long timeStarted = System.currentTimeMillis();
			while (uri == null && System.currentTimeMillis() - timeStarted < maxLengthToWait) {
				Collection<RemoteDevice> devices = upnpService.getRegistry().getRemoteDevices();
				for (Device currentDevice : devices) {
					if (currentDevice.getType().getType().equals("3DPrinterHost")) {
						uri = currentDevice.getDetails().getPresentationURI();
					}
				}
				
				Thread.currentThread().sleep(300);
			}
			
			upnpService.shutdown();
		} catch (Throwable e) {
			if (uri != null) {
				e.printStackTrace();
				return;
			}
		}

		if (uri == null) {
			JOptionPane optionPane = new JOptionPane();
			optionPane.setMessage("Couldn't find your printer on this network. \nAre you sure the print host has been started?\nIs it possible that your printer is advertising on a different network?");
			optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
			JDialog dialog = optionPane.createDialog("Couldn't Find 3D Printer");
			dialog.setAlwaysOnTop(true);
			dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
			System.exit(-1);
		}
		
		if (!Desktop.isDesktopSupported()) {
			System.out.println("The printer host software is at the URL:" + uri);
		}
			
		try {
			Desktop.getDesktop().browse(uri);
		} catch (IOException e) {
			JOptionPane optionPane = new JOptionPane();
			optionPane.setMessage("There was a problem starting the default browser for this machine.");
			optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
			JDialog dialog = optionPane.createDialog("Browser Not Found");
			dialog.setAlwaysOnTop(true);
			dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
			System.exit(-1);
		}
	}
}
