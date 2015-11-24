package org.area515.resinprinter.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LinuxManualWifiConnect {
	public static void main(String[] args) throws IOException {
		LinuxNetworkManager manager = new LinuxNetworkManager();
		List<NetInterface> interfaces = manager.getNetworkInterfaces();
		List<WirelessNetwork> wnetworks = new ArrayList<WirelessNetwork>();
		int wirelessNetwork = 0;
		
		for (NetInterface iface : interfaces) {
			for (WirelessNetwork wnet: iface.getWirelessNetworks()) {
				System.out.println(wirelessNetwork++ + ". " + wnet.getSsid());
				wnetworks.add(wnet);
			}
		}
		
		BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Type the number of the network you would like to connect to(then press enter):");
		String input = inputStream.readLine();
		int netIndex = Integer.valueOf(input);
		System.out.println("Type your password:");
		input = inputStream.readLine();
		WirelessNetwork wFace = wnetworks.get(netIndex);
		wFace.setPassword(input);
		manager.connectToWirelessNetwork(wFace);
	}
}
