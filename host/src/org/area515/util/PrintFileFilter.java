package org.area515.util;

import java.io.File;
import java.io.FileFilter;

import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.server.HostProperties;

public class PrintFileFilter implements FileFilter {
	public static final PrintFileFilter INSTANCE = new PrintFileFilter();
	
    public boolean accept(File pathname) {
		for (PrintFileProcessor<?> currentProcessor : HostProperties.Instance().getPrintFileProcessors()) {
			if (currentProcessor.acceptsFile(pathname)) {
				return true;
			}
		}
		
		return false;
    }
}