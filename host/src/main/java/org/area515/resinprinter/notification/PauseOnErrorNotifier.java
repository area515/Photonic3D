package org.area515.resinprinter.notification;

import java.io.File;
import java.net.URI;
import java.util.List;

import javax.websocket.server.ServerContainer;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.slice.StlError;

public class PauseOnErrorNotifier implements Notifier {
	@Override
	public void register(URI uri, ServerContainer container) throws InappropriateDeviceException {
	}

	@Override
	public void stop() {
	}

	@Override
	public void jobChanged(Printer printer, PrintJob job) {
	}

	@Override
	public void printerChanged(Printer printer) {
	}

	@Override
	public void fileUploadComplete(File fileUploaded) {
	}

	@Override
	public void geometryError(PrintJob job, List<StlError> error) {
		job.setErrorDescription("Your 3D file has improper geometry.");
		job.getPrinter().setStatus(JobStatus.PausedWithWarning);
	}

	@Override
	public void hostSettingsChanged() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendPingMessage(String message) {
		// TODO Auto-generated method stub

	}

	@Override
	public Long getTimeOfLastClientPing() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remoteMessageReceived(String message) {
		// TODO Auto-generated method stub
		
	}

}
