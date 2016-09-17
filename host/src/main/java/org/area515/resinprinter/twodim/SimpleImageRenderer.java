package org.area515.resinprinter.twodim;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.render.CurrentImageRenderer;

public class SimpleImageRenderer extends CurrentImageRenderer {
	public SimpleImageRenderer(DataAid aid, AbstractPrintFileProcessor<?, ?> processor, Object imageIndexToBuild) {
		super(aid, processor, imageIndexToBuild);
	}

	@Override
	public BufferedImage renderImage(BufferedImage image) throws JobManagerException {
		try {
			return ImageIO.read((File)imageIndexToBuild);
		} catch (IOException e) {
			throw new JobManagerException("Unable to read image:" + imageIndexToBuild, e);
		}
	}
}
