package org.area515.resinprinter.job;

import java.awt.image.BufferedImage;
import java.io.File;

public interface PrintFileProcessor {
	public String[] getFileExtensions();
	public boolean acceptsFile(File printJob);
	public BufferedImage getCurrentImage(PrintJob printJob);
	public double getBuildAreaMM(PrintJob printJob);
	public JobStatus processFile(PrintJob printJob);
	public void prepareEnvironment(File processingFile) throws JobManagerException;
	public void cleanupEnvironment(File processingFile) throws JobManagerException;
}
