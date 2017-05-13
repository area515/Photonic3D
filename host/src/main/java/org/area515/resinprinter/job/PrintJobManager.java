package org.area515.resinprinter.job;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.job.render.StubPrintFileProcessor;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;

public class PrintJobManager {
	private static final Logger logger = LogManager.getLogger();
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
	
	public List<PrintJob> getPrintJobs() {
		return new ArrayList<PrintJob>(printJobsByJobId.values());
	}
	
	public PrintJob createJob(File job, final Printer printer, Customizer customizer) throws JobManagerException, AlreadyAssignedException  {
		final PrintJob newJob = new PrintJob(job);
		PrintJob otherJob = printJobsByJobId.putIfAbsent(newJob.getId(), newJob);

		//This could never happen.
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

		if (customizer != null) {
			newJob.setCustomizer(customizer);
			logger.info(newJob.getCustomizer());
		}
		
		//TODO: These should be set by customizer
		newJob.setCurrentSlice(0);
		newJob.setTotalSlices(0);

		CompletableFuture<JobStatus> futureJobStatus = null;
		try {
			PrinterManager.Instance().assignPrinter(newJob, printer);
			PrintJobSupplier worker = new PrintJobSupplier(newJob, printer);
			futureJobStatus = CompletableFuture.supplyAsync(worker, Main.GLOBAL_EXECUTOR);
			newJob.setPrintFileProcessor(worker.getPrintFileProcessor());
			newJob.initializePrintJob(futureJobStatus);
		} catch (AlreadyAssignedException e) {
			printJobsByJobId.remove(newJob.getId());
			throw e;
		} finally { 
			//Trigger all job completion tasks after job is complete
			futureJobStatus.whenComplete((status, exeption) -> {
				try {
					//You can't change the state of the job after this point. Too many processes are waiting for the future jobStatus for a final outcome of the job
					if (status != null) {
						printer.setStatus(status);
						logger.info("Job Success:{}", Thread.currentThread().getName());
						NotificationManager.jobChanged(printer, newJob);
					} else {
						logger.error("Job Failed:" + Thread.currentThread().getName(), exeption);
						printer.setStatus(JobStatus.Failed);
						NotificationManager.jobChanged(printer, newJob);
					}
				} finally {
					newJob.setElapsedTime(System.currentTimeMillis() - newJob.getStartTime());
					
					//Don't need to close the printer or dissassociate the serial and display devices
					printer.showBlankImage();
					if (HostProperties.Instance().isRemoveJobOnCompletion()) {
						PrintJobManager.Instance().removeJob(newJob);
					}
					
					//If we don't do this, the next print will carry the last pause along with it and make falsify the slicing time.
					printer.setCurrentSlicePauseTime(0);
					PrinterManager.Instance().removeAssignment(newJob);
					newJob.setPrintFileProcessor(new StubPrintFileProcessor<>(newJob.getPrintFileProcessor()));
					
					//Set the jobstatus back on the printer to Ready
					printer.setStatus(JobStatus.Ready);
					logger.info("Job Ended:{}", Thread.currentThread().getName());
				}
			});
		}
		return newJob;		
	}
	
	public PrintJob getJob(UUID jobId) {
		return printJobsByJobId.get(jobId);
	}
	
	public PrintJob getPrintJobByPrinterName(String printerName) {
		for (PrintJob job : printJobsByJobId.values()) {
			if (job.getPrinter() != null && printerName.equals(job.getPrinter().getName())) {
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
	
	public boolean removeJob(PrintJob job) {
		if (job == null)
			return false;
		
		job = printJobsByJobId.get(job.getId());
		Printer printer = job.getPrinter();
		if (printer != null && printer.isPrintActive()) {
			throw new IllegalArgumentException("Can't remove job while it's being printed.");
		}
		
		return printJobsByJobId.remove(job.getId()) != null;
	}
}