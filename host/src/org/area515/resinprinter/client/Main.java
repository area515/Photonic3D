package org.area515.resinprinter.client;

import java.awt.Desktop;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
	public static final String PRINTER_TYPE = "3DPrinterHost";
	
	private static Set<PrintableDevice> foundDevices = new HashSet<PrintableDevice>();
	private static long maxLengthToWait = 5000;
	private static long maxLengthToWaitForAll = 7000;
	
	public static class PrintableDevice {
		public Device device;
		
		public PrintableDevice(Device device) {
			this.device = device;
		}
		
		public String toString() {
			return device.getDisplayString() + " (" + device.getDetails().getPresentationURI() + ")";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((device == null) ? 0 : device.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PrintableDevice other = (PrintableDevice) obj;
			if (device == null) {
				if (other.device != null)
					return false;
			} else if (!device.equals(other.device))
				return false;
			return true;
		}
	}
	
	/**
	 * return 0 for printer found
	 * returns -1 for user cancelled operation
	 * returns -2 for error during operation
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		/*SubnetScanner scanner = new SubnetScanner();
		try {
			scanner.startSubnetScan();
		} catch (SocketException e) {
			e.getStackTrace();//No network available?
		}//*/
		
		JDialog searchPane = null;
		final JOptionPane searchOptionPane = new JOptionPane("Searching for all 3d printers on network...", JOptionPane.INFORMATION_MESSAGE, JOptionPane.CANCEL_OPTION, null, new String[]{"Cancel"}, "Cancel");

		do {
			final CountDownLatch waitForURLFound = new CountDownLatch(1);
			
			Thread searchThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						UpnpService upnpService = new UpnpServiceImpl();
						upnpService.getControlPoint().search(new STAllHeader());
						long timeStarted = System.currentTimeMillis();
						while ((foundDevices.size() > 0 && System.currentTimeMillis() - timeStarted < maxLengthToWait) ||
							   (foundDevices.size() == 0 && System.currentTimeMillis() - timeStarted < maxLengthToWaitForAll)) {
							if (searchOptionPane.getValue() != null && searchOptionPane.getValue().equals("Cancel")) {
								System.exit(-1);
							}
							
							Collection<RemoteDevice> devices = upnpService.getRegistry().getRemoteDevices();
							for (Device currentDevice : devices) {
								if (currentDevice.getType().getType().equals(PRINTER_TYPE)) {
									foundDevices.add(new PrintableDevice(currentDevice));
									System.out.println("Found printer URL here:" + currentDevice.getDetails().getPresentationURI());
								}
							}
							
							Thread.currentThread().sleep(300);
						}
	
						waitForURLFound.countDown();
						upnpService.shutdown();
					} catch (Throwable e) {
						if (foundDevices.size() == 0) {
							e.printStackTrace();
							System.exit(-2);
						}
					}
				}
			});
			searchThread.start();
			
			try {
				waitForURLFound.await(maxLengthToWaitForAll + 2000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e1) {
				//Unlikely to happen
			}
			
			if (foundDevices.size() == 0) {
				System.out.println("3d printer not found after waiting:" + maxLengthToWaitForAll);
				JOptionPane optionPane = new JOptionPane();
				optionPane.setMessage("Couldn't find your printer on this network.\n"
						+ "Make sure your printer has been started and it is advertising on this network.\n"
						+ "If you just configured WIFI capabilities and you are waiting for the printer to restart,\n"
						+ "then click 'Ok' to try this operation again.");
				optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
				optionPane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
				
				JDialog dialog = optionPane.createDialog("Couldn't Find 3D Printer");
				dialog.setResizable(false);
				dialog.setModal(true);
				dialog.setAlwaysOnTop(true);
				dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				
				if (searchPane != null) {
					searchPane.setVisible(false);
				}
				dialog.setVisible(true);
				
				if (optionPane.getValue() == null ||
					optionPane.getValue().equals(JOptionPane.CANCEL_OPTION)) {
					System.exit(-1);
				}

				searchPane = new JDialog();
				searchPane.setTitle("Searching for Printer");
				searchPane.setResizable(false);
				searchPane.setAlwaysOnTop(true);
				searchPane.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				searchPane.add(searchOptionPane);
				searchPane.pack();
				searchPane.setLocationRelativeTo(null);
				searchPane.setVisible(true);
			}
		} while (foundDevices.size() == 0);
		
		if (!Desktop.isDesktopSupported()) {
			for (PrintableDevice device : foundDevices) {
				System.out.println("The printer host software is at the URL:" + device.device.getDetails().getPresentationURI());
			}
		}
		
		PrintableDevice chosenPrinter = null;
		if (foundDevices.size() == 1) {
			chosenPrinter = foundDevices.iterator().next();
		} else {
			chosenPrinter = (PrintableDevice)JOptionPane.showInputDialog(null, 
		        "There were multiple 3d printers found on this network.\nWhich printer would you like to view?", 
		        "Choose a Printer",
		        JOptionPane.QUESTION_MESSAGE,
		        null,
		        foundDevices.toArray(),
		        foundDevices.iterator().next());
			
			if (chosenPrinter == null) {
				System.exit(-1);
			}
		}
		
	    System.out.println("User chose:" + chosenPrinter.device.getDisplayString());
	    
		try {
			Desktop.getDesktop().browse(chosenPrinter.device.getDetails().getPresentationURI());
			System.exit(0);
		} catch (IOException e) {
			JOptionPane optionPane = new JOptionPane();
			optionPane.setMessage("There was a problem starting the default browser for this machine.");
			optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
			JDialog dialog = optionPane.createDialog("Browser Not Found");
			dialog.setAlwaysOnTop(true);
			dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
			System.exit(-2);
		}
	}
}
