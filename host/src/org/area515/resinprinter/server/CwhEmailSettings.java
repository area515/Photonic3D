package org.area515.resinprinter.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.area515.util.MailUtilities.EmailSettings;

public class CwhEmailSettings extends EmailSettings {
	private List<String> notificationEmailAddresses = new ArrayList<String>();
	private List<String> serviceEmailAddresses = new ArrayList<String>();
	 
	private CwhEmailSettings() {
		super();
	}
	
	public CwhEmailSettings(
			String smtpServer, 
			int smtpPort, 
			String userName, 
			String password, 
			String notificationEmailAddresses, 
			String serviceEmailAddresses,
			boolean useTLS) {
		super(smtpServer, smtpPort, userName, password, useTLS);
		this.notificationEmailAddresses = Arrays.asList(notificationEmailAddresses.split("[;,]"));
		this.serviceEmailAddresses = Arrays.asList(serviceEmailAddresses.split("[;,]"));
	}
	
	public List<String> getNotificationEmailAddresses() {
		return notificationEmailAddresses;
	}
	public void setNotificationEmailAddresses(List<String> notificationEmailAddresses) {
		this.notificationEmailAddresses = notificationEmailAddresses;
	}

	public List<String> getServiceEmailAddresses() {
		return serviceEmailAddresses;
	}
	public void setServiceEmailAddresses(List<String> serviceEmailAddresses) {
		this.serviceEmailAddresses = serviceEmailAddresses;
	}
}
