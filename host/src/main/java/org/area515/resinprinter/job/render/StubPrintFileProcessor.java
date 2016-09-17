package org.area515.resinprinter.job.render;

import java.awt.image.BufferedImage;
import java.io.File;

import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.job.PrintJob;

public class StubPrintFileProcessor<G,E> implements PrintFileProcessor<G,E> {
	private String[] fileExtensions;
	private String friendlyName;
	private boolean threeDimensionalGeometryAvailable;
	
	public StubPrintFileProcessor(PrintFileProcessor<G,E> processor) {
		this.fileExtensions = processor.getFileExtensions();
		this.friendlyName = processor.getFriendlyName();
		this.threeDimensionalGeometryAvailable = processor.isThreeDimensionalGeometryAvailable();
	}
	
	public StubPrintFileProcessor() {
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
	public Double getBuildAreaMM(PrintJob printJob) {
		return null;
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
		throw new JobManagerException("This job is no longer operational");
	}

	@Override
	public E getErrors(PrintJob printJob) throws JobManagerException {
		throw new JobManagerException("This job is no longer operational");
	}

	@Override
	public boolean isThreeDimensionalGeometryAvailable() {
		return threeDimensionalGeometryAvailable;
	}
}
