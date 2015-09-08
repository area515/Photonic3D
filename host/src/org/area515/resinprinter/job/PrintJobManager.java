package org.area515.resinprinter.job;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;

public class PrintJobManager {
	private static PrintJobManager INSTANCE;
	
	private ConcurrentHashMap<UUID, PrintJob> printJobsByJobId = new ConcurrentHashMap<UUID, PrintJob>();
	
	public static PrintJobManager Instance() {
		if (INSTANCE == null) {
			INSTANCE = new PrintJobManager();
		}
		return INSTANCE;
	}

	private PrintJobManager() {
	}
	
	public PrintJob createJob(File job) throws JobManagerException {
		PrintJob newJob = new PrintJob(job);
		PrintJob otherJob = printJobsByJobId.putIfAbsent(newJob.getId(), newJob);
		
		//This can't happen...
		if (otherJob != null) {
			throw new JobManagerException("The selected job is already running");
		}

		if (!job.exists()) {
			printJobsByJobId.remove(newJob.getId());
			throw new JobManagerException("The selected job does not exist");
		}
		if (!job.isFile()) {
			printJobsByJobId.remove(newJob.getId());
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
			printJobsByJobId.remove(newJob.getId());
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
					PrintJobManager.Instance().removeJob(printJob);
					PrinterManager.Instance().removeAssignment(printJob);
					System.out.println("Job Ended:" + Thread.currentThread().getName());
				}
			}
		});
		
		return futureJobStatus;
	}
	
	public PrintJob getJob(UUID jobId) {
		return printJobsByJobId.get(jobId);
	}
	
	public PrintJob getPrintJobByPrinterName(String printerName) {
		for (PrintJob job : printJobsByJobId.values()) {
			if (printerName.equals(job.getPrinter().getName())) {
				return job;
			}
		}
		
		return null;
	}
	
	public List<PrintJob> getJobsByFilename(String fileName) {
		List<PrintJob> jobs = new ArrayList<PrintJob>();
		for (PrintJob currentJob : printJobsByJobId.values()) {
			if (currentJob.getJobFile().getName().equals(fileName)) {
				jobs.add(currentJob);
			}
		}
		
		return jobs;
	}
	
	public void removeJob(PrintJob job) {
		if (job == null)
			return;
		
		printJobsByJobId.remove(job.getId());
	}
}