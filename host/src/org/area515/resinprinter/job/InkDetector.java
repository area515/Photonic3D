package org.area515.resinprinter.job;

import java.util.concurrent.Callable;

import org.area515.resinprinter.inkdetection.PrintMaterialDetector;
import org.area515.resinprinter.printer.Printer;

public class InkDetector implements Callable<Boolean> {
	private Printer printer;
	private PrintMaterialDetector detector;
	private float printMaterialRemainingForEmpty;
	
	public InkDetector(Printer printer, PrintMaterialDetector detector, float percentageConsideredEmpty) {
		this.printer = printer;
		this.detector = detector;
		this.printMaterialRemainingForEmpty = percentageConsideredEmpty;
	}
	
	@Override
	public Boolean call() throws Exception {
		float materialRemaining = detector.getPercentageOfPrintMaterialRemaining(printer);
		return materialRemaining > printMaterialRemainingForEmpty;
	}
}