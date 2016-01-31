package org.area515.resinprinter.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.network.WirelessNetwork;
import org.junit.Test;

public class DetectWireless {
    private static final Logger logger = LogManager.getLogger();

    @Test
	public void noErrorsDetectingNetworks() {
		logger.info("Network detection test");
	
		for (WirelessNetwork wireless : MachineService.INSTANCE.getWirelessNetworks()) {
			logger.info(" {}:{}" +  wireless.getParentInterfaceName(), wireless.getSsid());
		}
	}
}

