package org.area515.resinprinter.job;

import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.slice.ZSlicer;

public class STLDataAid extends DataAid {
	public ZSlicer slicer;
	
	public STLDataAid(PrintJob printJob) throws JobManagerException {
		super(printJob);
	}
}
