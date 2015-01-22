package org.area515.resinprinter.client;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDevice;

public class Main {
	public static void main(String[] args) throws IOException, InterruptedException {
		UpnpService upnpService = new UpnpServiceImpl();
		upnpService.getControlPoint().search(new STAllHeader());
		URI uri = null;
		long maxLengthToWait = 90000;
		long timeStarted = System.currentTimeMillis();
		while (uri == null && System.currentTimeMillis() - timeStarted < maxLengthToWait) {
			Collection<RemoteDevice> devices = upnpService.getRegistry().getRemoteDevices();
			for (Device currentDevice : devices) {
				if (currentDevice.getType().getType().equals("3DPrinterHost")) {
					uri = currentDevice.getDetails().getPresentationURI();
				}
			}
			
			Thread.currentThread().sleep(100);
		}
		upnpService.shutdown();
		if (uri == null) {
			JOptionPane.showMessageDialog(null, "Couldn't find your printer on this network. Is it possible that your printer is advertising on a different network?", "Couldn't Find Printer", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		if (Desktop.isDesktopSupported()) {
			Desktop.getDesktop().browse(uri);
		} else {
			System.out.println("The printer host software is at the URL:" + uri);
		}
	}
}
