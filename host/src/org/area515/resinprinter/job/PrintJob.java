package org.area515.resinprinter.job;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.area515.resinprinter.printer.Printer;

public class PrintJob {

	private volatile int totalSlices = 0;
	private volatile int currentSlice = 0;
	private volatile long currentSliceTime = 0;
	private volatile long startTime = 0;
	private volatile int exposureTime = 0;
	private volatile double zLiftSpeed = 0;
	private volatile double zLiftDistance = 0;
	private volatile boolean exposureTimeOverriden = false;
	
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
	
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public File getGCodeFile(){
		return gCodeFile;
	}	
	public void setGCodeFile(File gCodeFile){
		this.gCodeFile = gCodeFile;
	}
	
	public int getTotalSlices(){
		return totalSlices;
	}
	public void setTotalSlices(int totalSlices){
		this.totalSlices = totalSlices;
	}
	
	public int getCurrentSlice(){
		return currentSlice;
	}
	public void setCurrentSlice(int currentSlice){
		this.currentSlice = currentSlice;
	}

	public long getCurrentSliceTime(){
		return currentSliceTime;
	}
	public void setCurrentSliceTime(long currentSliceTime) {
		this.currentSliceTime = currentSliceTime;
	}
	
	public void setPrinter(Printer printer) {
		this.printer = printer;
	}
	public Printer getPrinter() {
		return printer;
	}
	
	public int getExposureTime() {
		return exposureTime;
	}
	public void setExposureTime(int exposureTime) {
		this.exposureTime = exposureTime;
	}
	
	public double getZLiftSpeed() {
		return zLiftSpeed;
	}
	public void setZLiftSpeed(double zLiftSpeed) {
		this.zLiftSpeed = zLiftSpeed;
	}
	
	public double getZLiftDistance() {
		return zLiftDistance;
	}
	public void setZLiftDistance(double zLiftDistance) {
		this.zLiftDistance = zLiftDistance;
	}
	
	public void overrideExposureTime(int exposureTime) {
		this.exposureTime = exposureTime;
		exposureTimeOverriden = true;
	}
	
	public boolean isExposureTimeOverriden() {
		return exposureTimeOverriden;
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
