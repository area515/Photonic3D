package org.area515.resinprinter.services;

import org.junit.Test;

public class EmailTests {
	@Test
	public void noFailuresInDianosticEmail() {
		MachineService.INSTANCE.emailSupportLogs();
		System.out.println("Emailed logs");
	}
}

