package org.area515.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;

//TODO: this shouldn't be a static util should be Singleton
public class MailUtilities {
	//private static EmailSettings mailSettings = null;
	public static final String SMTP_USE_TLS = "mail.smtp.starttls.enable";
	public static final String SMTP_HOST = "mail.smtp.host";
	
	private static Session session = null;

	public static class EmailSettings {
		private String smtpServer;
		private int smtpPort;
		private String userName;
		private String password;
		private boolean useTLS;
		
		//TODO: Support these settings eventually...
		//props.put("mail.smtp.socketFactory.port", "465");
		//props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
		//props.put("mail.smtp.auth", "true");
		//props.put("mail.smtp.port", "465");
		//props.put("mail.smtp.auth", "true");

		protected EmailSettings() {}
		
		public EmailSettings(String smtpServer, int smtpPort, String userName, String password, boolean useTLS) {
			this.smtpServer = smtpServer;
			this.smtpPort = smtpPort;
			this.userName = userName;
			this.password = password;
			this.useTLS = useTLS;
		}

		public int getSmtpPort() {
			return smtpPort;
		}
		public void setSmtpPort(int smtpPort) {
			this.smtpPort = smtpPort;
		}
		
		public String getSmtpServer() {
			return smtpServer;
		}
		public void setSmtpServer(String smtpServer) {
			this.smtpServer = smtpServer;
		}

		public String getUserName() {
			return userName;
		}
		public void setUserName(String userName) {
			this.userName = userName;
		}

		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}

		public boolean isUseTLS() {
			return useTLS;
		}
		public void setUseTLS(boolean useTLS) {
			this.useTLS = useTLS;
		}
	}

	public static void executeSMTPSend(
			String fromEmailAddress, 
			List<String> toEmailAddresses, 
			String subject,
			String body,
			Transport transport, 
			File... fileAttachments) throws MessagingException, UnsupportedEncodingException, IOException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmailAddress));
        message.setRecipients(RecipientType.TO, buildAddresses(toEmailAddresses.toArray(new String[toEmailAddresses.size()])));
        message.setSubject(subject);
        
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body);
        Multipart multiPart = new MimeMultipart();
        multiPart.addBodyPart(textPart);
       
        if (fileAttachments != null) {
        	for (File attachment : fileAttachments) {
	            MimeBodyPart attachmentPart = new MimeBodyPart();
				attachmentPart.attachFile(attachment.getCanonicalFile());
		        multiPart.addBodyPart(attachmentPart);
        	}
        }
        
        message.setContent(multiPart);
        message.setSentDate(new Date());
        transport.sendMessage(message, message.getAllRecipients());
	}
	
	public static Transport openTransportFromSettings(EmailSettings mailSettings) throws MessagingException {
		String username = mailSettings.getUserName();
		String smtpServer = mailSettings.getSmtpServer();
		String password = mailSettings.getPassword();
		Integer smtpPort = mailSettings.getSmtpPort();
		
		Properties mailProperties = new Properties();
		if (username == null || username.equals("")) {
			//This is for unauthenticated communication
			mailProperties.setProperty(SMTP_HOST, smtpServer);
		}
		if (mailSettings.isUseTLS()) {
			mailProperties.setProperty("mail.smtp.starttls.enable", "true");
		}
        session = Session.getInstance(mailProperties);
        Transport transport = session.getTransport("smtp");

        if (username == null || username.equals("")) {
        	transport.connect();
        } else if (smtpPort == null || smtpPort < 0) {
	        transport.connect(smtpServer, username, password);
        } else {
	        transport.connect(smtpServer, smtpPort, username, password);
        }
        
        return transport;
	}
	
	public static Address[] buildAddresses(String addresses[]) throws AddressException, UnsupportedEncodingException  {
		Address[] newAddresses = new Address[addresses.length];
		for (int t = 0; t < addresses.length; t++) {
			newAddresses[t] = new InternetAddress(addresses[t]);
		}
		
		return newAddresses;
	}
}
