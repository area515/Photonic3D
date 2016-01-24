package org.area515.resinprinter.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class EmailTests {
    private static final Logger logger = LogManager.getLogger();

    @Test
	public void noFailuresInDianosticEmail() {
		logger.info("Testing dianostic support emailing capability");
		MachineService.INSTANCE.emailSupportLogs();
	}
}

