package org.area515.resinprinter.network;

import java.util.List;

import org.area515.resinprinter.network.NetInterface.WirelessNetwork;

public interface NetworkManager {
	public List<NetInterface> getNetworkInterfaces();
	public void connectToWirelessNetwork(WirelessNetwork net, String password);
}
