package org.area515.resinprinter.job;

import java.awt.image.BufferedImage;

import org.area515.resinprinter.exception.SliceHandlingException;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;

public interface Previewable {
	public BufferedImage renderPreviewImage(DataAid dataAid) throws SliceHandlingException;
}