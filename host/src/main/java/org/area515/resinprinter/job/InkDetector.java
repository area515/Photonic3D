package org.area515.resinprinter.job;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.inkdetection.PrintMaterialDetector;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.server.Main;
import org.area515.util.DynamicJSonSettings;

public class InkDetector {
	public static final String DETECTION_ERROR = "Error occurred while performing ink detection";
    private static final Logger logger = LogManager.getLogger();
	private Printer printer;
	private PrintJob printJob;
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
	        		NotificationManager.jobChanged(printer, printJob);
					return true;
				}
				
				return false;
			} catch (IOException e) {
				logger.error(DETECTION_ERROR, e);
				if (hasAlreadyPausedWithError) {
					return false;
				}
				
				hasAlreadyPausedWithError = true;
				printJob.setErrorDescription(DETECTION_ERROR);
				printer.setStatus(JobStatus.PausedWithWarning);
				NotificationManager.jobChanged(printer, printJob);
				throw e;
			}
		}
	}
	
	public InkDetector(Printer printer, PrintJob job, PrintMaterialDetector detector, DynamicJSonSettings settings, float percentageConsideredEmpty) {
		this.printer = printer;
		this.detector = detector;
		this.printMaterialRemainingForEmpty = percentageConsideredEmpty;
		detector.initializeDetector(settings);
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