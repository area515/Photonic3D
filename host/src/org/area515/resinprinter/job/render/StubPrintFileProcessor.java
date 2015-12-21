package org.area515.resinprinter.job.render;

import java.awt.image.BufferedImage;
import java.io.File;

import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.job.PrintJob;

public class StubPrintFileProcessor<G> implements PrintFileProcessor<G> {
	private String[] fileExtensions;
	private String friendlyName;
	
	public StubPrintFileProcessor(PrintFileProcessor<G> processor) {
		this.fileExtensions = processor.getFileExtensions();
		this.friendlyName = processor.getFriendlyName();
	}
	
	@Override
	public String[] getFileExtensions() {
		return fileExtensions;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}

	@Override
	public boolean acceptsFile(File processingFile) {
		return false;
	}

	@Override
	public BufferedImage getCurrentImage(PrintJob printJob) {
		return null;
	}

	@Override
	public double getBuildAreaMM(PrintJob printJob) {
		return 0;
	}

	@Override
	public JobStatus processFile(PrintJob printJob) throws Exception {
		return printJob.getStatus();
	}

	@Override
	public void prepareEnvironment(File processingFile, PrintJob printJob) throws JobManagerException {
	}

	@Override
	public void cleanupEnvironment(File processingFile) throws JobManagerException {
	}

	@Override
	public G getGeometry(PrintJob printJob) throws JobManagerException {
		return null;
	}
}
