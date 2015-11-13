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
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.slice.StlError;
import org.area515.util.JacksonEncoder;

@ServerEndpoint(value="/hostNotification", encoders={JacksonEncoder.class})
public class WebSocketHostNotifier implements Notifier {
	private static ConcurrentHashMap<String, Session> sessionsBySessionId = new ConcurrentHashMap<String, Session>();
	
	@OnOpen
	public void onOpen(Session session) {
		sessionsBySessionId.putIfAbsent(session.getId(), session);
	}
	
	@OnClose
	public void onClose(Session session) {
		sessionsBySessionId.remove(session.getId());
	}

	@OnError
	public void onError(Session session, Throwable cause) {
		sessionsBySessionId.remove(session.getId());
	}
	
	@Override
	public void register(ServerContainer container) throws InappropriateDeviceException {
		try {
			container.addEndpoint(WebSocketHostNotifier.class);
		} catch (DeploymentException e) {
			throw new InappropriateDeviceException("Couldn't deploy", e);
		}
	}

	@Override
	public void jobChanged(Printer printer, PrintJob job) {
		//Not for the host
	}

	@Override
	public void printerChanged(Printer printer) {
		//Not for the host
	}

	@Override
	public void stop() {
		for (Session currentSession : sessionsBySessionId.values()) {
			try {
				currentSession.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "The printer host has been asked to shut down now!"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void fileUploadComplete(File fileUploaded) {
		//Not for the host
	}

	@Override
	public void geometryError(PrintJob job, List<StlError> error) {
		//Not for the host
	}

	@Override
	public void printerOutOfMatter(Printer printer, PrintJob job) {
		//Not for the host
	}

	@Override
	public void hostSettingsChanged() {
		for (Session currentSession : sessionsBySessionId.values()) {
			currentSession.getAsyncRemote().sendObject(new HostEvent("HostSettingsChanged", NotificationEvent.SettingsChanged));
		}
	}
	@Override
	public void sendPingMessage(String message) {
		for (Session currentSession : sessionsBySessionId.values()) {
			currentSession.getAsyncRemote().sendObject(new HostEvent(message, NotificationEvent.Ping));
		}
	}
}
