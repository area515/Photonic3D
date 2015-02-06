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
	private MailUtilities(){}
	private static Properties mailProperties = null;
	public static final String SMTP_USE_TLS = "mail.smtp.starttls.enable";
	public static final String SMTP_HOST = "mail.smtp.host";

	private static Session session = null;
	//props.put("mail.smtp.socketFactory.port", "465");
	//props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
	//props.put("mail.smtp.auth", "true");
	//props.put("mail.smtp.port", "465");
	//props.put("mail.smtp.auth", "true");

	
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
	
	public static Transport openTransportFromProperties() throws MessagingException {
		Properties mailProperties = MailUtilities.getMailProperties();

		String username = (String)mailProperties.remove("username");
		String password = (String)mailProperties.remove("password");
		String smtpServer = (String)mailProperties.remove("smtpServer");
		String port = (String)mailProperties.remove("smtpPort");
		Integer smtpPort = port != null? Integer.valueOf(port): null;
		
		return MailUtilities.openTransport(username, password, smtpServer, smtpPort);
	}
	
	public static Transport openTransport(
			String username, 
			String password, 
			String smtpServer, 
			Integer smtpPort) throws MessagingException {
		
		if (username == null || username.equals("")) {
			mailProperties.setProperty(SMTP_HOST, smtpServer);
		}
		
        session = Session.getInstance(mailProperties);
        Transport transport = session.getTransport("smtp");

        if (username == null || username.equals("")) {
        	transport.connect();
        } else if (smtpPort == null) {
	        transport.connect(smtpServer, username, password);
        } else {
	        transport.connect(smtpServer, smtpPort, username, password);
        }
        
        return transport;
	}
	
	public static Transport executeSMTPSend(
			String fromEmailAddress, 
			List<String> toEmailAddresses, 
			String username, 
			String password, 
			String smtpServer, 
			Integer smtpPort,
			String subject,
			String body,
			File... fileAttachments
			) throws MessagingException, UnsupportedEncodingException, IOException {

		Transport transport = openTransport(username, password, smtpServer, smtpPort);
		
		executeSMTPSend(fromEmailAddress, toEmailAddresses, subject, body, transport, fileAttachments);
		
		return transport;
	}
	
	public static void setMailProperties(Properties mailProperties) {
		MailUtilities.mailProperties = new Properties(mailProperties);
		MailUtilities.mailProperties.putAll(mailProperties);
	}
	
	public static Properties getMailProperties() {
		Properties properties = new Properties(mailProperties);
		properties.putAll(mailProperties);
		return properties;
	}
	
	public static Address[] buildAddresses(String addresses[]) throws AddressException, UnsupportedEncodingException  {
		Address[] newAddresses = new Address[addresses.length];
		for (int t = 0; t < addresses.length; t++) {
			newAddresses[t] = new InternetAddress(addresses[t]);
		}
		
		return newAddresses;
	}
}
