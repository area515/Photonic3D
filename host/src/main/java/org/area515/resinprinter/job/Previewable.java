package org.area515.resinprinter.job;

import java.awt.image.BufferedImage;

import java.io.*;


import javax.script.ScriptException;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.exception.SlicerException;
import org.area515.resinprinter.exception.NoPrinterFoundException;


public interface Previewable {
	public BufferedImage previewSlice(Customizer customizer, File jobFile) throws NoPrinterFoundException, SlicerException, IOException, InappropriateDeviceException, ScriptException ;
}