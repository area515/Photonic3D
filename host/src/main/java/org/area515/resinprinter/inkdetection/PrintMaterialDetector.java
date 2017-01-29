package org.area515.resinprinter.inkdetection;

import java.io.IOException;

import org.area515.resinprinter.printer.Printer;

public interface PrintMaterialDetector {
	public void initializeDetector(PrintMaterialDetectorSettings settings);
	/**
	 * This method is executed synchronously with the printing process to ensure the print material is settled to a 
	 * point where it can be measured properly. For example, a VisualPrintMaterialDetector would want to take a
	 * picture when the resin tank isn't being actively sloshed around.
	 */
	public void startMeasurement(Printer printer);
	
	/**
	 * This method shouldn't worry about the amount of time they use to determine how much print material is left.
	 * It is assumed that this method is executed at times when the CPU is not under stress.
	 */
	public float getPercentageOfPrintMaterialRemaining(Printer printer) throws IOException;
}
