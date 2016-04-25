package org.area515.resinprinter.notification;

import org.area515.resinprinter.notification.Notifier.NotificationEvent;
import org.area515.resinprinter.printer.Printer;

public class PrinterEvent {
	private Printer printer;
	private NotificationEvent event;
	
	public PrinterEvent(Printer printer, NotificationEvent event) {
		this.printer = printer;
		this.event = event;
	}

	public NotificationEvent getNotificationEvent() {
		return event;
	}
	public void setNotificationEvent(NotificationEvent event) {
		this.event = event;
	}

	public Printer getPrinter() {
		return printer;
	}
	public void setPrinter(Printer printer) {
		this.printer = printer;
	}
}
