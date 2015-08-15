package org.area515.resinprinter.job;

import java.util.concurrent.Callable;

import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.server.HostProperties;

public class PrintJobProcessingThread implements Callable<JobStatus> {
	private PrintJob printJob = null;
	private Printer printer;
	private PrintFileProcessor<?> processor;
	
	public PrintJobProcessingThread(PrintJob printJob, Printer printer) {
		this.printJob = printJob;
		this.printer = printer;
		for (PrintFileProcessor<?> currentProcessor : HostProperties.Instance().getPrintFileProcessors()) {
			if (currentProcessor.acceptsFile(printJob.getJobFile())) {
				processor = currentProcessor;
			}
		}
	}
	
	public PrintFileProcessor<?> getPrintFileProcessor() {
		return processor;
	}
	
	@Override
	public JobStatus call() throws Exception {
		System.out.println("Starting:" + printJob + " on Printer:" + printer + " executing on Thread:" + Thread.currentThread().getName());
		printer.setStatus(JobStatus.Printing);
		NotificationManager.jobChanged(printer, printJob);
		
		printJob.setStartTime(System.currentTimeMillis());
		return processor.processFile(printJob);
	}
}