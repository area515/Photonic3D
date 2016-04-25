package org.area515.resinprinter.network;

import java.util.List;

public interface NetworkManager {
	public List<NetInterface> getNetworkInterfaces();
	public void connectToWirelessNetwork(WirelessNetwork net);
}
