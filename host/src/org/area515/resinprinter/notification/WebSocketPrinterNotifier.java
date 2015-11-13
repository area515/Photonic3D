package org.area515.resinprinter.notification;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.slice.StlError;
import org.area515.util.JacksonEncoder;
import org.area515.util.PrintJobJacksonDecoder;

@ServerEndpoint(value="/printerNotification/{printerName}", encoders={JacksonEncoder.class})
public class WebSocketPrinterNotifier implements Notifier {
	private static ConcurrentHashMap<String, ConcurrentHashMap<String, Session>> sessionsByPrinterName = new ConcurrentHashMap<String, ConcurrentHashMap<String, Session>>();
	
	@OnOpen
	public void onOpen(Session session, @PathParam("printerName") String printerName) {
		ConcurrentHashMap<String, Session> sessionsBySessionId = new ConcurrentHashMap<String, Session>();
		sessionsBySessionId.put(session.getId(), session);
		ConcurrentHashMap<String, Session> otherSessionsBySessionId = sessionsByPrinterName.putIfAbsent(printerName, sessionsBySessionId);
		if (otherSessionsBySessionId != null) {
			otherSessionsBySessionId.put(session.getId(), session);
		}
	}
	
	@OnClose
	public void onClose(Session session, @PathParam("printerName") String printerName) {
		ConcurrentHashMap<String, Session> sessionsBySessionId = new ConcurrentHashMap<String, Session>();
		sessionsBySessionId.put(session.getId(), session);
		ConcurrentHashMap<String, Session> otherSessionsBySessionId = sessionsByPrinterName.get(printerName);
		if (otherSessionsBySessionId != null) {
			otherSessionsBySessionId.remove(session);
		}
	}
	
	@OnError
	public void onError(Session session, Throwable cause) {
		for (ConcurrentHashMap<String, Session> sessions : sessionsByPrinterName.values()) {
			sessions.remove(session.getId());
		}
	}
	
	@Override
	public void register(ServerContainer container) throws InappropriateDeviceException {
		try {
			container.addEndpoint(WebSocketPrinterNotifier.class);
		} catch (DeploymentException e) {
			throw new InappropriateDeviceException("Couldn't deploy", e);
		}
	}

	@Override
	public void jobChanged(Printer printer, PrintJob job) {
		//Not for printers
	}

	@Override
	public void printerChanged(Printer printer) {
		ConcurrentHashMap<String, Session> sessionsBySessionId = sessionsByPrinterName.get(printer.getName());
		if (sessionsBySessionId == null) {
			return;
		}
		
		for (Session currentSession : sessionsBySessionId.values()) {
			try {
				currentSession.getAsyncRemote().sendObject(new PrinterEvent(printer, NotificationEvent.PrinterChanged));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		for (ConcurrentHashMap<String, Session> sessions : sessionsByPrinterName.values()) {
			for (Session currentSession : sessions.values()) {
				try {
					currentSession.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "The printer host has been asked to shut down now!"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void fileUploadComplete(File fileUploaded) {
		//Not for printers
	}

	@Override
	public void geometryError(PrintJob job, List<StlError> error) {
		//Not for printers
	}

	@Override
	public void printerOutOfMatter(Printer printer, PrintJob job) {
		ConcurrentHashMap<String, Session> sessionsBySessionId = sessionsByPrinterName.get(printer.getName());
		if (sessionsBySessionId == null) {
			return;
		}
		
		for (Session currentSession : sessionsBySessionId.values()) {
			try {
				currentSession.getAsyncRemote().sendObject(new PrinterEvent(printer, NotificationEvent.PrinterChanged));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void hostSettingsChanged() {
		//Not for printers
	}
	
	@Override
	public void sendPingMessage(String message) {
		//Not for printers
	}
}
