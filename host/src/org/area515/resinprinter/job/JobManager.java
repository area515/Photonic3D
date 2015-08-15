package org.area515.resinprinter.job;


import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;

public class JobManager {
	private static JobManager INSTANCE;
	
	private ConcurrentHashMap<String, PrintJob> printJobsByName = new ConcurrentHashMap<String, PrintJob>();
	
	public static JobManager Instance() {
		if (INSTANCE == null) {
			INSTANCE = new JobManager();
		}
		return INSTANCE;
	}

	private JobManager() {
	}
	
	public PrintJob createJob(File job) throws JobManagerException {
		PrintJob newJob = new PrintJob(job);
		PrintJob otherJob = printJobsByName.putIfAbsent(newJob.getJobFile().getName(), newJob);
		if (otherJob != null) {
			throw new JobManagerException("The selected job is already running");
		}
		
		if (!job.exists()) {
			printJobsByName.remove(job.getName());
			throw new JobManagerException("The selected job does not exist");
		}
		if (!job.isFile()) {
			printJobsByName.remove(job.getName());
			throw new JobManagerException("The selected job is not a file");
		}
		
		newJob.setCurrentSlice(0);
		newJob.setTotalSlices(0);

		try {
			for (PrintFileProcessor currentProcessor : HostProperties.Instance().getPrintFileProcessors()) {
				if (currentProcessor.acceptsFile(job)) {
					currentProcessor.prepareEnvironment(job, newJob);
				}
			}
		} catch (JobManagerException e) {
			printJobsByName.remove(job.getName());
			throw e;
		}
		return newJob;
	}
	
	public Future<JobStatus> startJob(final PrintJob printJob, final Printer printer) throws AlreadyAssignedException {
		PrinterManager.Instance().assignPrinter(printJob, printer);
		PrintJobProcessingThread worker = new PrintJobProcessingThread(printJob, printer);
		final Future<JobStatus> futureJobStatus = Main.GLOBAL_EXECUTOR.submit(worker);
		printJob.setFutureJobStatus(futureJobStatus);
		printJob.setPrintFileProcessor(worker.getPrintFileProcessor());

		//Trigger all job completion tasks after job is complete
		Main.GLOBAL_EXECUTOR.submit(new Runnable() {
			@Override
			public void run() {
				try {
					printer.setStatus(futureJobStatus.get());
					System.out.println("Job Success:" + Thread.currentThread().getName());
					NotificationManager.jobChanged(printer, printJob);
				} catch (Throwable e) {
					System.out.println("Job Failed:" + Thread.currentThread().getName());
					e.printStackTrace();
					printer.setStatus(JobStatus.Failed);
					NotificationManager.jobChanged(printer, printJob);
				} finally {
					//Don't need to close the printer or dissassociate the serial and display devices
					printer.showBlankImage();
					JobManager.Instance().removeJob(printJob);
					PrinterManager.Instance().removeAssignment(printJob);
					System.out.println("Job Ended:" + Thread.currentThread().getName());
				}
			}
		});
		
		return futureJobStatus;
	}
	
	public PrintJob getJob(String jobId) {
		return printJobsByName.get(jobId);
	}
	
	public void removeJob(PrintJob job) {
		if (job == null)
			return;
		
		printJobsByName.remove(job.getJobFile().getName());
	}

}