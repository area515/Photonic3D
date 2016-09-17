package org.area515.resinprinter.security.keystore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@ServerEndpoint(value="/httpTunnel")
@WebSocket
public class RendezvousPipe {
	private static List<Session> sessions = new ArrayList<>();
	private Server server;
	
	public RendezvousPipe() {
	}
	
	public RendezvousPipe(int testPort) throws Exception {
	    server = new Server(testPort);
		WebSocketHandler wsHandler = new WebSocketHandler() {
			@Override
			public void configure(WebSocketServletFactory factory) {
			    factory.register(RendezvousPipe.class);
			}
		};
		server.setHandler(wsHandler);
		server.start();
	}
	
	public void close() throws Exception {
		server.stop();
	}
	
	@OnWebSocketConnect
	@OnOpen
	public void onOpen(Session session) {
		sessions.add(session);
	}

    @OnWebSocketMessage
    @OnMessage
    public void onMessage(Session session, byte buf[], int offset, int length) throws IOException {
    	for (Session otherSession : sessions) {
    		if (otherSession.getRemoteAddress().getPort() != session.getRemoteAddress().getPort()) {
    			otherSession.getRemote().sendBytes(ByteBuffer.wrap(buf, offset, length));
    		}
    	}
    }
}
