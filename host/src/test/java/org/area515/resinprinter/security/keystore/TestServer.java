package org.area515.resinprinter.security.keystore;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.area515.resinprinter.security.UserManagementFeature;
import org.area515.resinprinter.server.Main;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class TestServer extends HttpServlet {
	private Server server;
	private String testMessage;
	
	public TestServer(int testPort, String testMessage, UserManagementFeature loginService) throws Exception {
		this.testMessage = testMessage;
	    server = new Server(testPort);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
 
        context.addServlet(new ServletHolder(this),"/*");
        
        Main.setupAuthentication(context, loginService);
        
        server.start();
	}

	public void close() throws Exception {
		server.stop();
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		OutputStream stream = resp.getOutputStream();
		stream.write(testMessage.getBytes());
		stream.close();
	}
}
