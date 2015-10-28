package org.area515.resinprinter.notification;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.websocket.server.ServerContainer;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.server.CwhEmailSettings;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.slice.StlError;
import org.area515.util.MailUtilities;

public class EmailOnCompletionNotifier implements Notifier {
	
	@Override
	public void register(ServerContainer container) throws InappropriateDeviceException {
	}

	@Override
	public void jobChanged(Printer printer, PrintJob job) {
		if (printer.getStatus() == JobStatus.Completed) {
			CwhEmailSettings settings = HostProperties.Instance().loadEmailSettings();
			
			Transport transport = null;
			try {
				transport = MailUtilities.openTransportFromSettings(settings);
				MailUtilities.executeSMTPSend (
						HostProperties.Instance().getDeviceName().replace(" ", "") + "@My3DPrinter", 
						settings.getNotificationEmailAddresses(),
						"Print Job Complete", 
						"Print job complete for job:" + job.getJobFile().getName() + " on printer:" + printer.getName(), 
						transport,
						(File[])null);
			} catch (MessagingException | IOException e) {
				e.printStackTrace();
			} finally {
				if (transport != null) {
					try {transport.close();} catch (MessagingException e) {}
				}
			}
		}
	}

	@Override
	public void printerChanged(Printer printer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void fileUploadComplete(File fileUploaded) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void geometryError(PrintJob job, List<StlError> error) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void printerOutOfMatter(Printer printer, PrintJob job) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void hostSettingsChanged() {
	}
}
