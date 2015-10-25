package org.area515.resinprinter.services;

import org.area515.resinprinter.network.NetInterface;
import org.area515.resinprinter.network.NetInterface.WirelessNetwork;
import org.junit.Test;

public class DetectWireless {
	@Test
	public void noErrorsDetectingNetworks() {
		System.out.println("Network detection test");
	
		for (NetInterface iFace : MachineService.INSTANCE.getNetworkInterfaces()) {
			System.out.println(iFace.getName());
			for (WirelessNetwork wireless : iFace.getWirelessNetworks()) {
				System.out.println(" " + wireless.getSsid());
			}
		}
	}
}

