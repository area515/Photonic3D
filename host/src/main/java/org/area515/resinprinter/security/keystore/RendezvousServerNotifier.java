package org.area515.resinprinter.security.keystore;

import java.io.File;
import java.net.URI;
import java.util.List;

import javax.websocket.server.ServerContainer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.notification.Notifier;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.slice.StlError;

public class RendezvousServerNotifier implements Notifier {
    private static final Logger logger = LogManager.getLogger();

    private RendezvousServer server;

    //TODO: This class will get used once we want to notify things happening through websockets
	@Override
	public void register(URI startURI, ServerContainer container) throws InappropriateDeviceException {
		try {
			this.server = RendezvousServer.getServer(startURI);
		} catch (Exception e) {
			throw new InappropriateDeviceException("Couldn't connect to rendezvous server", e);
		}
	}
	
	@Override
	public void stop() {
		server.close();
	}

	@Override
	public void jobChanged(Printer printer, PrintJob job) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void printerChanged(Printer printer) {
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
		// Do nothing...
	}
}
