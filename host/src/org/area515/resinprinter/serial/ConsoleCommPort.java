package org.area515.resinprinter.serial;

import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration.ComPortSettings;

public class ConsoleCommPort implements SerialCommunicationsPort {
	private String name = "Console Testing";
	
	@Override
	public void open(String printerName, int timeout, ComPortSettings settings) {
		System.out.println("Printer opened");
	}

	@Override
	public void close() {
		System.out.println("Printer closed");
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void write(String gcode) {
		System.out.println("Printer received:" + gcode);
	}

	@Override
	public String readUntilOkOrStoppedPrinting(Printer printer) {
		return "ok";
	}
}
