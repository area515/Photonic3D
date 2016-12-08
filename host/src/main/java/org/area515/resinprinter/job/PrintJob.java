package org.area515.resinprinter.job;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlTransient;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.SlicingProfile.InkConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PrintJob {
	private volatile int totalSlices = 0;
	private volatile int currentSlice = 0;
	private volatile long currentSliceTime = 0;
	private volatile long averageSliceTime = 0;
	private volatile long startTime = 0;
	private volatile long elapsedTime = 0;
	private volatile double totalCost = 0;
	private volatile double currentSliceCost = 0;
	private volatile PrintFileProcessor<?,?> printFileProcessor;
	private volatile String errorDescription;
	
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
	private Map<String, CompiledScript> scriptsByName = new HashMap<>();

	private Customizer customizer;

	public PrintJob(File jobFile) {
		this.jobFile = jobFile;
	}

	public UUID getId() {
		return id;
	}
	
	@JsonIgnore
	public File getJobFile() {
		return jobFile;
	}
	
	public String getJobName() {
		if (jobFile == null) {
			return null;
		}
		
		return jobFile.getName();
	}
	
	public long getElapsedTime() {
		return elapsedTime;
	}
	public void setElapsedTime(long elapsedTime) {
		this.elapsedTime = elapsedTime;
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

	public void setCustomizer(Customizer customizer) {
		this.customizer = customizer;
	}

	public Customizer getCustomizer() {
		return customizer;
	}
	
	@XmlTransient
	@JsonProperty
	public boolean isPrintInProgress() {
		return getStatus().isPrintInProgress();
	}
	
	@XmlTransient
	@JsonProperty
	public boolean isPrintPaused() {
		return getStatus().isPaused();
	}

	public JobStatus getStatus() {
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
	
	public void initializePrintJob(Future<JobStatus> futureJobStatus) {
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
				JobStatus checkStatus = futureJobStatus.get();
				if (checkStatus == JobStatus.Failed) {
					return "Check server logs for exact problem.";
				}
			} catch (Throwable e) {
				while (e.getCause() != null) {
					e = e.getCause();
				}
				
				errorDescription = e.getMessage();
			}
			
			if (!"".equals(errorDescription)) {
				return errorDescription;
			}
		}
		
		return errorDescription;
	}
	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}
	
	public PrintFileProcessor<?,?> getPrintFileProcessor() {
		return printFileProcessor;
	}
	public void setPrintFileProcessor(PrintFileProcessor<?,?> printFileProcessor) {
		this.printFileProcessor = printFileProcessor;
	}
	

	public void stopOverridingZLiftDistance() {
		overrideZLiftDistance = false;
	}
	public void overrideZLiftDistance(double zLiftDistance) throws InappropriateDeviceException {
		if (printer == null) {
			throw new InappropriateDeviceException("This print job:" + getJobName() + " doesn't have a printer assigned.");
		}
		
		try {
			overrideZLiftDistance = true;
			this.zLiftDistance = zLiftDistance;
			printer.getGCodeControl().executeGCodeWithTemplating(this, printer.getConfiguration().getSlicingProfile().getZLiftDistanceGCode(), true);
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

	public CompiledScript buildCompiledScript(String scriptName, String script, ScriptEngine engine) throws ScriptException {
		CompiledScript compiledScript = scriptsByName.get(scriptName);
		if (engine instanceof Compilable && compiledScript == null) {
			compiledScript = ((Compilable)engine).compile(script);
			scriptsByName.put(scriptName, compiledScript);
		}
		
		return compiledScript;
	}
	
	public void stopOverridingZLiftSpeed() {
		overrideZLiftSpeed = false;
	}
	public void overrideZLiftSpeed(double zLiftSpeed) throws InappropriateDeviceException {
		if (printer == null) {
			throw new InappropriateDeviceException("This print job:" + getJobName() + " doesn't have a printer assigned.");
		}
		
		try {
			this.overrideZLiftSpeed = true;
			this.zLiftSpeed = zLiftSpeed;
			printer.getGCodeControl().executeGCodeWithTemplating(this, printer.getConfiguration().getSlicingProfile().getZLiftSpeedGCode(), true);
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
	
	public void addNewSlice(long sliceTime, Double buildAreaInMM) {
		sliceTime -= getPrinter().getCurrentSlicePauseTime();
		getPrinter().setCurrentSlicePauseTime(0);
		InkConfig inkConfig = getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig();
		averageSliceTime = ((averageSliceTime * currentSlice) + sliceTime) / (currentSlice + 1);
		elapsedTime = System.currentTimeMillis() - startTime;
		
		currentSliceTime = sliceTime;
		currentSlice++;
		
		if (buildAreaInMM != null && buildAreaInMM > 0) {
			double buildVolume = buildAreaInMM * inkConfig.getSliceHeight();
			currentSliceCost = (buildVolume / 1000000) * inkConfig.getResinPriceL();
		}
		
		totalCost += currentSliceCost;
	}
	
	public String toString() {
		if (printer == null) {
			return getJobName() + " (No Printer)";
		}
		return getJobName() + " assigned to printer:" + printer;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
