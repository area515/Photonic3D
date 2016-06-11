package org.area515.util;

import java.io.File;
import java.io.FileFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.server.HostProperties;

public class PrintFileFilter implements FileFilter {
	public static final PrintFileFilter INSTANCE = new PrintFileFilter();
    private static final Logger logger = LogManager.getLogger();
	
    public boolean accept(File pathname) {
		for (PrintFileProcessor<?,?> currentProcessor : HostProperties.Instance().getPrintFileProcessors()) {
			try {
				if (currentProcessor.acceptsFile(pathname)) {
					return true;
				}
			} catch (Exception e) {
				logger.error("Processor:" + currentProcessor.getFriendlyName() + " failed on file:" + pathname + " continuing PrintFileProcessor check", e);
			}
		}
		
		return false;
    }
    
    public PrintFileProcessor<?,?> findAssociatedPrintProcessor(File pathname) {
 		for (PrintFileProcessor<?,?> currentProcessor : HostProperties.Instance().getPrintFileProcessors()) {
 			if (currentProcessor.acceptsFile(pathname)) {
 				return currentProcessor;
 			}
 		}
 		
 		return null;
     }
}