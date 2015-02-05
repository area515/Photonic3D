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
import org.eclipse.jetty.websocket.jsr356.server.AnnotatedServerEndpointConfig;

@ServerEndpoint("/printjobnotification/{printJobName}")
public class WebSocketPrintJobNotifier implements Notifier {
	private static ConcurrentHashMap<String, ConcurrentHashMap<String, Session>> sessionsByPrintJobName = new ConcurrentHashMap<String, ConcurrentHashMap<String, Session>>();
	
	public WebSocketPrintJobNotifier() {
		super();
	}

	@OnOpen
	public void onOpen(Session session, @PathParam("printJobName") String printerName) {
		ConcurrentHashMap<String, Session> sessionsBySessionId = new ConcurrentHashMap<String, Session>();
		sessionsBySessionId.put(session.getId(), session);
		/*ConcurrentHashMap<String, Session> otherSessionsBySessionId = sessionsByPrintJobName.putIfAbsent(printerName, sessionsBySessionId);
		if (otherSessionsBySessionId != null) {
			otherSessionsBySessionId.put(session.getId(), session);
		}*/
	}
	
	@OnClose
	public void onClose(Session session, @PathParam("printJobName") String printerName) {
		ConcurrentHashMap<String, Session> sessionsBySessionId = new ConcurrentHashMap<String, Session>();
		sessionsBySessionId.put(session.getId(), session);
		/*ConcurrentHashMap<String, Session> otherSessionsBySessionId = sessionsByPrintJobName.get(printerName);
		if (otherSessionsBySessionId != null) {
			otherSessionsBySessionId.remove(session);
		}*/
	}
	
	@Override
	public void register(ServerContainer container) throws InappropriateDeviceException {
		try {
			//container.addEndpoint(WebSocketPrintJobNotifier.class);
	        container.addEndpoint(new AnnotatedServerEndpointConfig(
	        		                WebSocketPrintJobNotifier.class,
	        		                WebSocketPrintJobNotifier.class.getAnnotation( ServerEndpoint.class ) 
	        		            ) {
	        		                @Override
	        		                public Configurator getConfigurator() {
	        		                    return super.getConfigurator();
	        		                }
	        		            }
	        		        );

		} catch (DeploymentException e) {
			throw new InappropriateDeviceException("Couldn't deploy", e);
		}
	}

	@Override
	public void jobChanged(PrintJob job) {
		ConcurrentHashMap<String, Session> sessionsBySessionId = sessionsByPrintJobName.get(job.getJobFile().getName());
		for (Session currentSession : sessionsBySessionId.values()) {
			currentSession.getAsyncRemote().sendObject(job);
		}
	}

	@Override
	public void printerChanged(Printer printer) {
	}
	
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
}
