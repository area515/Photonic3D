package org.area515.resinprinter.printphoto;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.twodim.TwoDimensionalImageRenderer;
import org.area515.resinprinter.twodim.TwoDimensionalPlatformPrintFileProcessor;

public class ImagePrintFileProcessor extends TwoDimensionalPlatformPrintFileProcessor<Object,Object> {
	@Override
	public String[] getFileExtensions() {
		return new String[]{"gif", "jpg", "jpeg", "png"};
	}
	
	@Override
	public boolean acceptsFile(File processingFile) {
		//TODO: this could be smarter by loading the file instead of just checking the file type
		String name = processingFile.getName().toLowerCase();
		return name.endsWith("gif") || name.endsWith("jpg") || name.endsWith("jpeg") || name.endsWith("png");
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
		return "Image";
	}

	@Override
	public boolean isThreeDimensionalGeometryAvailable() {
		return false;
	}

	@Override
	public TwoDimensionalImageRenderer createRenderer(DataAid aid, AbstractPrintFileProcessor<?, ?> processor, Object imageIndexToBuild) {
		return new TwoDimensionalImageRenderer(aid, processor, imageIndexToBuild) {
			public BufferedImage loadImageFromFile(PrintJob job) throws JobManagerException {
				try {
					return ImageIO.read(job.getJobFile());
				} catch (IOException e) {
					throw new JobManagerException("Couldn't load image file:" + job.getJobFile(), e);
				}
			}
		};
	}
}
