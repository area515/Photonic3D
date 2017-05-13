package org.area515.resinprinter.notification;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.slice.StlError;
import org.area515.util.JacksonEncoder;
import org.area515.util.PrintJobJacksonDecoder;

@ServerEndpoint(value="/printJobNotification/{printJobName}", encoders={JacksonEncoder.class}, decoders={PrintJobJacksonDecoder.class})
public class WebSocketPrintJobNotifier implements Notifier {
    private static final Logger logger = LogManager.getLogger();
	private static ConcurrentHashMap<String, ConcurrentHashMap<String, Session>> sessionsByPrintJobName = new ConcurrentHashMap<String, ConcurrentHashMap<String, Session>>();
	
	public WebSocketPrintJobNotifier() {
		super();
	}
	
	@OnError
	public void onError(Session session, Throwable cause) {
		for (ConcurrentHashMap<String, Session> sessions : sessionsByPrintJobName.values()) {
			sessions.remove(session.getId());
		}
	}
	
	@OnOpen
	public void onOpen(Session session, @PathParam("printJobName") String printJobName) {
		ConcurrentHashMap<String, Session> sessionsBySessionId = new ConcurrentHashMap<String, Session>();
		sessionsBySessionId.put(session.getId(), session);
		ConcurrentHashMap<String, Session> otherSessionsBySessionId = sessionsByPrintJobName.putIfAbsent(printJobName, sessionsBySessionId);
		if (otherSessionsBySessionId != null) {
			otherSessionsBySessionId.put(session.getId(), session);
		}
	}
	
	@OnClose
	public void onClose(Session session, @PathParam("printJobName") String printJobName) {
		ConcurrentHashMap<String, Session> otherSessionsBySessionId = sessionsByPrintJobName.get(printJobName);
		if (otherSessionsBySessionId != null) {
			otherSessionsBySessionId.remove(session.getId());
		}
	}
	
	@Override
	public void register(URI uri, ServerContainer container) throws InappropriateDeviceException {
		try {
			container.addEndpoint(WebSocketPrintJobNotifier.class);
		} catch (DeploymentException e) {
			throw new InappropriateDeviceException("Couldn't deploy", e);
		}
	}

	@Override
	public void jobChanged(Printer printer, PrintJob job) {
		ConcurrentHashMap<String, Session> sessionsBySessionId = sessionsByPrintJobName.get(job.getJobFile().getName());
		if (sessionsBySessionId == null) {
			return;
		}
		
		for (Session currentSession : sessionsBySessionId.values()) {
			try {
				currentSession.getAsyncRemote().sendObject(new PrintJobEvent(job, NotificationEvent.PrintJobChanged));
			} catch (Exception e) {
				logger.error("Error sending event to websocket:" + currentSession.getId(), e);
			}
		}
	}

	@Override
	public void printerChanged(Printer printer) {
		//Not for print jobs
	}
	
	@Override
	public void stop() {
		for (ConcurrentHashMap<String, Session> sessions : sessionsByPrintJobName.values()) {
			for (Session currentSession : sessions.values()) {
				try {
					currentSession.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "The printer host has been asked to shut down now!"));
				} catch (IOException e) {
					logger.error("Error sending event to websocket:" + currentSession.getId(), e);
				}
			}
		}
	}

	@Override
	public void fileUploadComplete(File fileUploaded) {
		ConcurrentHashMap<String, Session> sessionsBySessionId = sessionsByPrintJobName.get(fileUploaded.getName());
		if (sessionsBySessionId == null) {
			return;
		}
		
		for (Session currentSession : sessionsBySessionId.values()) {
			try {
				//This just mocks up a printJob it's not a real print job, it's just something we can notify our clients with.
				PrintJob job = new PrintJob(fileUploaded);
				job.initializePrintJob(CompletableFuture.completedFuture(JobStatus.Ready));
				currentSession.getAsyncRemote().sendObject(new PrintJobEvent(job, NotificationEvent.FileUploadComplete));
			} catch (Exception e) {
				logger.error("Error sending event to websocket:" + currentSession.getId(), e);
			}
		}
	}
	
	@Override
	public void geometryError(PrintJob job, List<StlError> errors) {
		ConcurrentHashMap<String, Session> sessionsBySessionId = sessionsByPrintJobName.get(job.getJobFile().getName());
		if (sessionsBySessionId == null) {
			return;
		}
		
		for (Session currentSession : sessionsBySessionId.values()) {
			try {
				currentSession.getAsyncRemote().sendObject(new PrintJobEvent(job, NotificationEvent.GeometryError, errors));
			} catch (Exception e) {
				logger.error("Error sending event to websocket:" + currentSession.getId(), e);
			}
		}
	}

	@Override
	public void hostSettingsChanged() {
		//Not for print jobs
	}

	@Override
	public void sendPingMessage(String message) {
		//Not for print jobs
	}
	@Override
	public Long getTimeOfLastClientPing() {
		//Not for print jobs
		return null;
	}

	@Override
	public void remoteMessageReceived(String message) {
		// TODO Auto-generated method stub
		
	}
}
