package org.area515.resinprinter.job;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.inkdetection.PrintMaterialDetector;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.server.Main;

public class InkDetector {
    private static final Logger logger = LogManager.getLogger();
	private Printer printer;
	private PrintMaterialDetector detector;
	private float printMaterialRemainingForEmpty;
	private boolean hasAlreadyPausedWithError;
	
	private class CPUIntensiveActionToDetermineIfEmpty implements Callable<Boolean> {
		@Override
		public Boolean call() throws Exception {
			try {
				float materialRemaining = detector.getPercentageOfPrintMaterialRemaining(printer);
				if (materialRemaining > printMaterialRemainingForEmpty) {
					printer.setStatus(JobStatus.PausedOutOfPrintMaterial);
					return true;
				}
				
				return false;
			} catch (IOException e) {
				logger.error("Error occurred while performing visual detection", e);
				if (hasAlreadyPausedWithError) {
					return false;
				}
				
				hasAlreadyPausedWithError = true;
				printer.setStatus(JobStatus.PausedWithWarning);
				throw e;
			}
		}
	}
	
	public InkDetector(Printer printer, PrintMaterialDetector detector, float percentageConsideredEmpty) {
		this.printer = printer;
		this.detector = detector;
		this.printMaterialRemainingForEmpty = percentageConsideredEmpty;
	}
	
	public float performMeasurement() throws IOException {
		detector.startMeasurement(printer);
		return detector.getPercentageOfPrintMaterialRemaining(printer);
	}
	
	public Future<Boolean> startMeasurement() {
		detector.startMeasurement(printer);
		return Main.GLOBAL_EXECUTOR.submit(new CPUIntensiveActionToDetermineIfEmpty());
	}
}