package org.area515.resinprinter.notification;

import java.util.List;

import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.notification.Notifier.NotificationEvent;
import org.area515.resinprinter.slice.StlError;

public class PrintJobEvent {
	private PrintJob printJob;
	private NotificationEvent event;
	private List<StlError> errors;
	
	public PrintJobEvent(PrintJob printJob, NotificationEvent event) {
		this.printJob = printJob;
		this.event = event;
	}
	
	public PrintJobEvent(PrintJob printJob, NotificationEvent event, List<StlError> errors) {
		this.printJob = printJob;
		this.event = event;
		this.errors = errors;
	}

	public NotificationEvent getNotificationEvent() {
		return event;
	}
	public void setNotificationEvent(NotificationEvent event) {
		this.event = event;
	}

	public PrintJob getPrintJob() {
		return printJob;
	}
	public void setPrintJob(PrintJob printJob) {
		this.printJob = printJob;
	}

	public List<StlError> getErrors() {
		return errors;
	}
	public void setErrors(List<StlError> errors) {
		this.errors = errors;
	}
}
