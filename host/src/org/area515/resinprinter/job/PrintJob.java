package org.area515.resinprinter.job;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.SlicingProfile.InkConfig;

public class PrintJob {
	private volatile int totalSlices = 0;
	private volatile int currentSlice = 0;
	private volatile long currentSliceTime = 0;
	private volatile long averageSliceTime = 0;
	private volatile long startTime = 0;
	private volatile double totalCost = 0;
	private volatile double currentSliceCost = 0;
	private volatile PrintFileProcessor<?> printFileProcessor;
	
	//Overridables
	private volatile boolean overrideExposureTime;
	private volatile int exposureTime = 0;
	private volatile boolean overrideZLiftSpeed;
	private volatile double zLiftSpeed;
	private volatile boolean overrideZLiftDistance;
	private volatile double zLiftDistance;

	private UUID id = UUID.randomUUID();
	private File jobFile;
	private Printer printer;
	private Future<JobStatus> futureJobStatus;
	private CountDownLatch futureJobStatusAssigned = new CountDownLatch(1);
	
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
	
	
	
	public JobStatus getJobStatus() {
		//If the futureJobStatus is done, we will certainly have the last status that will never be changed.
		if (futureJobStatus != null && (futureJobStatus.isDone() || futureJobStatus.isCancelled())) {
			try {
				return futureJobStatus.get();
			} catch (InterruptedException | ExecutionException e) {
			}
		}

		Printer localPrinter = printer;
		
		if (localPrinter != null) {
			return localPrinter.getStatus();
		}
		
		//TODO: Why are we doing this?
		return JobStatus.Failed;
	}
	public void setJobStatus() {
		//do nothing.  This is just for JSON
	}
	
	public void setFutureJobStatus(Future<JobStatus> futureJobStatus) {
		this.futureJobStatus = futureJobStatus;
		futureJobStatusAssigned.countDown();
	}
	
	public String getErrorDescription() {
		try {
			futureJobStatusAssigned.await();
		} catch (InterruptedException e1) {

		}
		
		if (futureJobStatus.isDone() || futureJobStatus.isCancelled()) {
			String errorDescription = null;
			try {
				return "Job Status:" + futureJobStatus.get();
			} catch (Throwable e) {
				while (e.getCause() != null) {
					e = e.getCause();
				}
				
				errorDescription = e.getMessage();
			}
			
			if (!"".equals(errorDescription)) {
				return errorDescription;
			}
			
			return "Job Failed. Check server logs for exact problem";
		}
		
		return null;
	}
	public void setErrorDescription(String errorDescription) {
		//do nothing.  This is just for JSON
	}

	public PrintFileProcessor<?> getPrintFileProcessor() {
		return printFileProcessor;
	}
	public void setPrintFileProcessor(PrintFileProcessor<?> printFileProcessor) {
		this.printFileProcessor = printFileProcessor;
	}
	

	public void stopOverridingZLiftDistance() {
		overrideZLiftDistance = false;
	}
	public void overrideZLiftDistance(double zLiftDistance) throws InappropriateDeviceException {
		if (printer == null) {
			throw new InappropriateDeviceException("This print job:" + jobFile.getName() + " doesn't have a printer assigned.");
		}
		
		try {
			overrideZLiftDistance = true;
			this.zLiftDistance = zLiftDistance;
			printer.getGCodeControl().executeGCodeWithTemplating(this, printer.getConfiguration().getSlicingProfile().getZLiftDistanceGCode());
		} catch (InappropriateDeviceException e) {
			throw e;
		}
	}
	public boolean isZLiftDistanceOverriden() {
		return overrideZLiftDistance;
	}
	public double getZLiftDistance() {
		return zLiftDistance;
	}
	public void setZLiftDistance(double zLiftDistance) {
		this.zLiftDistance = zLiftDistance;
	}

	
	public void stopOverridingZLiftSpeed() {
		overrideZLiftSpeed = false;
	}
	public void overrideZLiftSpeed(double zLiftSpeed) throws InappropriateDeviceException {
		if (printer == null) {
			throw new InappropriateDeviceException("This print job:" + jobFile.getName() + " doesn't have a printer assigned.");
		}
		
		try {
			this.overrideZLiftSpeed = true;
			this.zLiftSpeed = zLiftSpeed;
			printer.getGCodeControl().executeGCodeWithTemplating(this, printer.getConfiguration().getSlicingProfile().getZLiftSpeedGCode());
		} catch (InappropriateDeviceException e) {
			throw e;
		}
	}
	public boolean isZLiftSpeedOverriden() {
		return overrideZLiftSpeed;
	}
	public double getZLiftSpeed() {
		return zLiftSpeed;
	}
	public void setZLiftSpeed(double zLiftSpeed) {
		this.zLiftSpeed = zLiftSpeed;
	}

	
	public void stopOverridingExposureTime() {
		this.overrideExposureTime = false;
	}
	public void overrideExposureTime(int exposureTime) {
		this.exposureTime = exposureTime;
		this.overrideExposureTime = true;
	}
	public boolean isExposureTimeOverriden() {
		return overrideExposureTime;
	}
	public int getExposureTime() {
		return exposureTime;
	}
	public void setExposureTime(int exposureTime) {
		this.exposureTime = exposureTime;
	}

	
	public long getAverageSliceTime() {
		return averageSliceTime;
	}
	public void setAverageSliceTime(long averageSliceTime) {
		this.averageSliceTime = averageSliceTime;
	}

	public double getTotalCost() {
		return totalCost;
	}
	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
	}

	public double getCurrentSliceCost() {
		return currentSliceCost;
	}
	public void setCurrentSliceCost(double currentSliceCost) {
		this.currentSliceCost = currentSliceCost;
	}

	public void addNewSlice(long sliceTime, double buildAreaInMM) {
		InkConfig inkConfig = getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig();
		averageSliceTime = ((averageSliceTime * currentSlice) + sliceTime) / (currentSlice + 1);
		currentSliceTime = sliceTime;
		currentSlice++;
		if (buildAreaInMM > 0) {
			double buildVolume = buildAreaInMM * inkConfig.getSliceHeight();
			currentSliceCost = (buildVolume / 1000000) * inkConfig.getResinPriceL();
		}
		
		totalCost += currentSliceCost;
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
