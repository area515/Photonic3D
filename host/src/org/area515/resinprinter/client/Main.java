package org.area515.resinprinter.client;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDevice;

public class Main {
	private static URI uri = null;
	private static long maxLengthToWait = 7000;
	
	public static void main(String[] args) {
		final CountDownLatch waitForURLFound = new CountDownLatch(1);
		
		Thread searchThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					UpnpService upnpService = new UpnpServiceImpl();
					upnpService.getControlPoint().search(new STAllHeader());
					long timeStarted = System.currentTimeMillis();
					while (uri == null && System.currentTimeMillis() - timeStarted < maxLengthToWait) {
						Collection<RemoteDevice> devices = upnpService.getRegistry().getRemoteDevices();
						for (Device currentDevice : devices) {
							if (currentDevice.getType().getType().equals("3DPrinterHost")) {
								uri = currentDevice.getDetails().getPresentationURI();
								System.out.println("Found printer URL here:" + uri);
								waitForURLFound.countDown();
							}
						}
						
						Thread.currentThread().sleep(300);
					}

					upnpService.shutdown();
				} catch (Throwable e) {
					if (uri == null) {
						e.printStackTrace();
						System.exit(-1);
					}
				}
			}
		});
		searchThread.start();
		
		try {
			waitForURLFound.await(maxLengthToWait + 2000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e1) {
			//Unlikely to happen
		}
		
		if (uri == null) {
			System.out.println("3d printer not found after waiting:" + maxLengthToWait);
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
			System.exit(0);
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
