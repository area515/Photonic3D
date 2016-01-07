package org.area515.resinprinter.job;

public enum JobStatus {
	Unpacking,
	Printing,
	Failed,
	Completed,
	Cancelled,
	Cancelling,
	Deleted,
	Paused,
	PausedOutOfPrintMaterial,
	PausedWithWarning,
	Ready;
	
	public boolean isPrintInProgress() {
		return this == JobStatus.Paused || this == JobStatus.Printing || this == JobStatus.PausedOutOfPrintMaterial || this == JobStatus.PausedWithWarning || this == JobStatus.Cancelling;
	}
	
	public boolean isPrintActive() {
		return this == JobStatus.Paused || this == JobStatus.Printing || this == JobStatus.PausedOutOfPrintMaterial || this == JobStatus.PausedWithWarning;
	}
	
	public boolean isPaused() {
		return this == JobStatus.Paused || this == JobStatus.PausedOutOfPrintMaterial || this == JobStatus.PausedWithWarning;
	}
}
