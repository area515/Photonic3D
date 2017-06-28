package org.area515.resinprinter.services;

import java.util.UUID;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.Printer;
import org.junit.Assert;
import org.junit.Test;

public class PrinterServiceTest {
	@Test
	public void deletePrinterTest() throws InappropriateDeviceException {
		Printer printer = PrinterService.INSTANCE.createTemplatePrinter();
		String printerName = UUID.randomUUID().toString();
		printer.getConfiguration().setName(printerName);
		PrinterService.INSTANCE.savePrinter(printer);
		PrinterService.INSTANCE.deletePrinter(printerName);
		Assert.assertTrue(MachineServiceTest.isFound(PrinterService.INSTANCE.getPrinters(), printerName));
	}
}
