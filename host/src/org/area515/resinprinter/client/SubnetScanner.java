package org.area515.resinprinter.client;

import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.JSchException;

public class SubnetScanner {
	private Vector<Box> foundAddresses = new Vector<>();
	private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(256);

	public static class Box {
		private String name;
		private boolean isRaspberryPi;
		
		public Box(String name, boolean isRaspberryPi) {
			this.name = name;
			this.isRaspberryPi = isRaspberryPi;
		}
		
		public String getName() {
			return name;
		}

		public boolean isRaspberryPi() {
			return isRaspberryPi;
		}
		
		public String toString() {
			return name + (isRaspberryPi?" (Remote install available)":" (Not yet supported)");
		}
	}
	
	public List<Box> waitForDevicesWithPossibleRemoteInstallCapability() throws InterruptedException {
		executor.shutdown();
		executor.awaitTermination(2, TimeUnit.MINUTES);
		
		return foundAddresses;
	}
	
	public void startSubnetScan() throws SocketException {
		final List<byte[]> addresses = new ArrayList<byte[]>();
		Enumeration<NetworkInterface> ifaceEnum;

		ifaceEnum = NetworkInterface.getNetworkInterfaces();
		while (ifaceEnum.hasMoreElements()) {
			NetworkInterface face = ifaceEnum.nextElement();
			if (face.isLoopback())
				continue;
			
			for (InterfaceAddress currentAddress : face.getInterfaceAddresses()) {
				if (currentAddress.getBroadcast() != null) {
					addresses.add(currentAddress.getAddress().getAddress());
				}
			}
		}
		
		for (int t = Byte.MIN_VALUE; t <= Byte.MAX_VALUE; t++) {
			final byte digit4 = (byte)(t & 0xff);
			executor.submit(new Runnable() {
				@Override
				public void run() {
					final byte digit3 = addresses.get(0)[2];
					String currentAddress = (addresses.get(0)[0] & 0xff)+ "." + (addresses.get(0)[1] & 0xff) + "." + (digit3 & 0xff) + "." + (digit4 & 0xff);
					try {
						Socket socket = new Socket(currentAddress, 22);
						socket.close();
						SSHClient client = new SSHClient();
						client.connect("pi", "raspberry", currentAddress, 22);
						client.disconnect();
						foundAddresses.add(new Box(currentAddress, true));
					} catch (IOException e) {
						//Probably not a valid address
					} catch (JSchException e) {
						foundAddresses.add(new Box(currentAddress, false));
					}
				}
			});
		}
	}
}

