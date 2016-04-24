package org.area515.resinprinter.notification;

import org.area515.resinprinter.notification.Notifier.NotificationEvent;

public class HostEvent {
	private String message;
	private NotificationEvent event;
	
	public HostEvent(String message, NotificationEvent event) {
		this.message = message;
		this.event = event;
	}

	public NotificationEvent getNotificationEvent() {
		return event;
	}
	public void setNotificationEvent(NotificationEvent event) {
		this.event = event;
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
