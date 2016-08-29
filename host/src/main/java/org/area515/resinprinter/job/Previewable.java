package org.area515.resinprinter.job;

import java.awt.image.BufferedImage;

import java.io.*;


import org.area515.resinprinter.exception.SliceHandlingException;
import org.area515.resinprinter.exception.NoPrinterFoundException;


public interface Previewable {
	public BufferedImage previewSlice(Customizer customizer, File jobFile, boolean projectImage) throws NoPrinterFoundException, SliceHandlingException;
}