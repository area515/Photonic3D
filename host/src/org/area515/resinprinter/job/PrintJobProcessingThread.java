package org.area515.resinprinter.job;

import java.util.concurrent.Callable;

import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.server.HostProperties;

public class PrintJobProcessingThread implements Callable<JobStatus> {
	private PrintJob printJob = null;
	private Printer printer;
	private PrintFileProcessor processor;
	
	public PrintJobProcessingThread(PrintJob printJob, Printer printer) {
		this.printJob = printJob;
		this.printer = printer;
		for (PrintFileProcessor currentProcessor : HostProperties.Instance().getPrintFileProcessors()) {
			if (currentProcessor.acceptsFile(printJob.getJobFile())) {
				processor = currentProcessor;
			}
		}
	}
	
	public PrintFileProcessor getPrintFileProcessor() {
		return processor;
	}
	
	@Override
	public JobStatus call() throws Exception {
		System.out.println(Thread.currentThread().getName() + " Start");
		printer.setStatus(JobStatus.Printing);
		NotificationManager.jobChanged(printer, printJob);
		
		printJob.setStartTime(System.currentTimeMillis());
		try {
			JobStatus status = processor.processFile(printJob);
			
			printer.setStatus(status);
			
			System.out.println("Job Complete:" + Thread.currentThread().getName());

			//Send a notification that the job is complete
			NotificationManager.jobChanged(printer, printJob);
			
			return status;
		} catch (Throwable e) {
			e.printStackTrace();
			printer.setStatus(JobStatus.Failed);
			NotificationManager.jobChanged(printer, printJob);
			throw e;
		} finally {
			//Don't need to close the printer or dissassociate the serial and display devices
			printer.showBlankImage();
			JobManager.Instance().removeJob(printJob);
			PrinterManager.Instance().removeAssignment(printJob);
			System.out.println(Thread.currentThread().getName() + " ended.");
		}
	}
}