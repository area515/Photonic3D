package org.area515.resinprinter.job;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.area515.resinprinter.printer.Printer;

public class PrintJob {

	private AtomicInteger totalSlices = new AtomicInteger();
	private AtomicInteger currentSlice = new AtomicInteger();
	
	private UUID id = UUID.randomUUID();
	private File jobFile;
	private File gCodeFile;
	private Printer printer;
	
	public PrintJob(File jobFile) {
		this.jobFile = jobFile;
	}

	public UUID getId() {
		return id;
	}
	
	public File getJobFile() {
		return jobFile;
	}
	
	public File getGCodeFile(){
		return gCodeFile;
	}	
	public void setGCodeFile(File gCodeFile){
		this.gCodeFile = gCodeFile;
	}
	
	public int getTotalSlices(){
		return totalSlices.get();
	}
	
	public void setTotalSlices(int totalSlices){
		this.totalSlices.set(totalSlices);
	}
	public int getCurrentSlice(){
		return currentSlice.get();
	}
	
	public void setCurrentSlice(int currentSlice){
		this.currentSlice.set(currentSlice);
	}

	public void setPrinter(Printer printer) {
		this.printer = printer;
	}
	public Printer getPrinter() {
		return printer;
	}
	
	public String toString() {
		if (printer == null) {
			return jobFile.getName() + " (No Printer)";
		}
		return jobFile.getName() + " assigned to printer:" + printer;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jobFile == null) ? 0 : jobFile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrintJob other = (PrintJob) obj;
		if (jobFile == null) {
			if (other.jobFile != null)
				return false;
		} else if (!jobFile.equals(other.jobFile))
			return false;
		return true;
	}
}
