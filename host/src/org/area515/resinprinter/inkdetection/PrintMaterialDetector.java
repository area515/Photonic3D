package org.area515.resinprinter.inkdetection;

import org.area515.resinprinter.printer.Printer;

/**
 * PrintMaterialDetectors shouldn't worry about the amount of time they use to determine how much print material is left.
 * It is assumed that this method is executed at times when the CPU is not under stress.
 */
public interface PrintMaterialDetector {
	public Float getPercentageOfPrintMaterialRemaining(Printer printer);
}
