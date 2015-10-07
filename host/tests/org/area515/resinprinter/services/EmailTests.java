package org.area515.resinprinter.services;

import org.junit.Test;

public class EmailTests {
	@Test
	public void noFailuresInDianosticEmail() {
		System.out.println("Testing dianostic support emailing capability");
		MachineService.INSTANCE.emailSupportLogs();
	}
}

