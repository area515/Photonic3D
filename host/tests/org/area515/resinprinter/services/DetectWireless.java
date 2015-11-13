package org.area515.resinprinter.services;

import org.area515.resinprinter.network.WirelessNetwork;
import org.junit.Test;

public class DetectWireless {
	@Test
	public void noErrorsDetectingNetworks() {
		System.out.println("Network detection test");
	
		for (WirelessNetwork wireless : MachineService.INSTANCE.getWirelessNetworks()) {
			System.out.println(" " + wireless.getParentInterfaceName() + ":" + wireless.getSsid());
		}
	}
}

