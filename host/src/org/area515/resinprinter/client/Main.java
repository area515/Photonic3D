package org.area515.resinprinter.client;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;

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
		long maxLengthToWait = 7000;
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
		
		if (Desktop.isDesktopSupported()) {
			Desktop.getDesktop().browse(uri);
		} else {
			System.out.println("The printer host software is at the URL:" + uri);
		}
	}
}
