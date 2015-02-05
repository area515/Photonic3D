package org.area515.resinprinter.notification;

import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;

@ServerEndpoint("/printernotification/{printerName}")
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
	
	@Override
	public void register(ServerContainer container) throws InappropriateDeviceException {
		try {
			container.addEndpoint(WebSocketPrinterNotifier.class);
		} catch (DeploymentException e) {
			throw new InappropriateDeviceException("Couldn't deploy", e);
		}
	}

	@Override
	public void jobChanged(PrintJob job) {
	}

	@Override
	public void printerChanged(Printer printer) {
		ConcurrentHashMap<String, Session> sessionsBySessionId = sessionsByPrinterName.get(printer.getName());
		for (Session currentSession : sessionsBySessionId.values()) {
			currentSession.getAsyncRemote().sendObject(printer);
		}
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
}
