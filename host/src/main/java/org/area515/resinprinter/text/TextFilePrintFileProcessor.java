package org.area515.resinprinter.text;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.script.ScriptException;

import org.area515.resinprinter.exception.SliceHandlingException;
import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.twodim.TwoDimensionalImageRenderer;
import org.area515.resinprinter.twodim.TwoDimensionalPlatformPrintFileProcessor;

public class TextFilePrintFileProcessor extends TwoDimensionalPlatformPrintFileProcessor<Object, Object> {
	@Override
	public String[] getFileExtensions() {
		return new String[]{"txt"};
	}
	
	@Override
	public boolean acceptsFile(File processingFile) {
		return processingFile.getName().toLowerCase().endsWith("txt");
	}
	
	@Override
	public void cleanupEnvironment(File processingFile) throws JobManagerException {
		//Nothing to cleanup everything is done in memory.
	}

	@Override
	public Object getGeometry(PrintJob printJob) throws JobManagerException {
		throw new JobManagerException("You can't get geometry from this type of file");
	}

	@Override
	public Object getErrors(PrintJob printJob) throws JobManagerException {
		throw new JobManagerException("You can't get error geometry from this type of file");
	}

	@Override
	public String getFriendlyName() {
		return "Simple Text";
	}

	@Override
	public boolean isThreeDimensionalGeometryAvailable() {
		return false;
	}

	@Override
	public TwoDimensionalImageRenderer createRenderer(DataAid aid, AbstractPrintFileProcessor<?, ?> processor, Object imageIndexToBuild) {
		return new TextImageRenderer(aid, this, Boolean.TRUE);
	}
}
