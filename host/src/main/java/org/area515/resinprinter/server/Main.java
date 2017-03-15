package org.area515.resinprinter.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.server.ServerContainer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.plugin.FeatureManager;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.security.JettySecurityUtils;
import org.area515.resinprinter.security.UserManagementFeature;
import org.area515.resinprinter.services.PrinterService;
import org.area515.resinprinter.stream.ProgressiveDownloadServlet;
import org.area515.resinprinter.util.security.PhotonicUser;
import org.area515.util.RedirectRegexRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

/*
 * References:
 * http://news-anand.blogspot.com/2012/05/today-i-am-going-tell-you-how-to-create.html
 * http://alvinalexander.com/java/jwarehouse/jetty-6.1.9/etc/realm.properties.shtml
 * http://www.eclipse.org/jetty/documentation/9.1.4.v20140401/embedded-examples.html#embedded-secured-hello-handler
 */

public class Main {
    private static final Logger logger = LogManager.getLogger();
    
    public static final String AUTHENTICATION_SCHEME = Constraint.__BASIC_AUTH;
	public static ScheduledExecutorService GLOBAL_EXECUTOR = new ScheduledThreadPoolExecutor(3, new ThreadFactory() {
		private AtomicInteger threads = new AtomicInteger();
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "PrintJobProcessorThread-" + threads.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		}
	});
	
	public static void setupAuthentication(ServletContextHandler context, UserManagementFeature loginService) {
        //All below is user based security
        Constraint constraint = new Constraint();
        constraint.setName(AUTHENTICATION_SCHEME);
        constraint.setRoles(new String[]{PhotonicUser.FULL_RIGHTS, PhotonicUser.LOGIN});//Allows a login
        constraint.setAuthenticate(true);
     
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec("/*");
        
        /*HashLoginService loginService = new HashLoginService();
        loginService.putUser(
        		HostProperties.Instance().getClientUsername(), 
        		Credential.getCredential(HostProperties.Instance().getClientPassword()), new String[] { HostProperties.FULL_RIGHTS});*/
        loginService.setName(HostProperties.Instance().getSecurityRealmName());
        //loginService.start();
        //OAuthLoginService OAuth2 AuthenticatorFactory ServletSecurityAnnotationHandler
        //http://stackoverflow.com/questions/24591782/resteasy-support-for-jax-rs-rolesallowed
        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setLoginService(loginService);
        //FormAuthenticator d;
        //csh.setAuthenticator(authenticator);
        //csh.setAuthenticatorFactory(null);change above from BASIC to FORM and change this to a FormAuthenticator
        csh.setConstraintMappings( new ConstraintMapping[]{ mapping } );
        
        context.setInitParameter("resteasy.role.based.security", String.valueOf(true));
     	context.setSecurityHandler(csh);
	}
	
	public static void main(String[] args) throws Exception {
		logger.info("=================================================================");
		logger.info("=================================================================");
		logger.info("Photonic3D started");
		logger.info("=================================================================");
		logger.info("=================================================================");

		int port = HostProperties.Instance().getPrinterHostPort();
		/*
		 * Sequence
		 * Setup ResourceHandler for html files
		 * Setup ServletContextHandler for rest services
		 * Add serviceContext, resource_handler, and DetaultHandler to the server (must be in that order)
		 * Initialize application
		 * Start server
		 */

		final Server server = new Server(port);
		 
		/*
		 * Setup ResourceHandler for html files
		 */
		
		 // Create the ResourceHandler. It is the object that will handle the request for a given file. It is
        // a Jetty Handler object so it is suitable for chaining with other handlers as you will see in other examples.
        ResourceHandler resource_handler = new ResourceHandler();
        // Configure the ResourceHandler. Setting the resource base indicates where the files should be served out of.
        // In this example it is the current directory but it can be configured to anything that the jvm has access to.
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[]{ "index.html" });
        resource_handler.setResourceBase(HostProperties.Instance().getHostGUIDir());
        
	    //Angular is pretty messed up when it comes to link rewriting: https://github.com/angular/angular.js/issues/4608
	    //I can't believe we need to server side changes to fix this!!! https://github.com/angular-ui/ui-router/wiki/Frequently-Asked-Questions#how-to-configure-your-server-to-work-with-html5mode
	    //I suppose for Jetty we'll create a 404 redirector! Massive pain and stupidity...
        //Rewrite anything that ends with "/page" to "/#page"
        //RedirectRegexRule rewritePagesRule = new RedirectRegexRule();
        RedirectRegexRule rewritePagesRule = new RedirectRegexRule();
        rewritePagesRule.setRegex("/(.*[pP]age.*)");
        rewritePagesRule.setReplacement("/#$1?$Q");
        RewriteHandler rewritePagesHandler = new RewriteHandler();
        rewritePagesHandler.addRule(rewritePagesRule);
        
        //Setup ServletContextHandler for rest services
        ServletContextHandler serviceContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		serviceContext.setContextPath("/services");
		ServletHolder servicesHolder = new ServletHolder(new HttpServletDispatcher());
		servicesHolder.setInitParameter("javax.ws.rs.Application", ApplicationConfig.class.getName());
		serviceContext.addServlet(servicesHolder, "/*");
		
        // For Raspberry Pi video
        ServletContextHandler videoContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        videoContext.setContextPath("/video");
		videoContext.addServlet(ProgressiveDownloadServlet.class, "/*");		
		
		// Add the ResourceHandler to the server.
		HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { videoContext, serviceContext, resource_handler, rewritePagesHandler, new DefaultHandler() });
        server.setHandler(handlers);

        String externallyAccessableIP = HostProperties.Instance().getExternallyAccessableName();
		if (externallyAccessableIP == null) {
			//Don't do this: 127.0.0.1 on Linux!
			//getSetup().externallyAccessableIP = InetAddress.getLocalHost().getHostAddress();
			
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface currentInterface = interfaces.nextElement();
				if (currentInterface.isLoopback() || currentInterface.isVirtual()) {
					continue;
				}
				
				Enumeration<InetAddress> addresses = currentInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (address.getAddress().length == 4) {//Make sure it's ipv4
						externallyAccessableIP = address.getHostAddress();
					}
				}
			}
		}
		
		//Determine if we are going to use SSL
		if (HostProperties.Instance().isUseSSL()) {
			JettySecurityUtils.secureContext(externallyAccessableIP, serviceContext, server);
		}
		
		//Determine if we are going to use user authentication
		if (HostProperties.Instance().isUseAuthentication()) {
			setupAuthentication(serviceContext, FeatureManager.getUserManagementFeature());
		}
		
		URI startURI = new URI("http" + (HostProperties.Instance().isUseSSL()?"s://":"://") + externallyAccessableIP + ":" + port);
		ServerContainer container = WebSocketServerContainerInitializer.configureContext(serviceContext);
		NotificationManager.start(startURI, container);
		
		//Start server before we start broadcasting!
		try {
			server.start();
		} catch (Exception e) {
			logger.error("FATAL Error starting Photonic3D http server", e);
			return;
		}
		   
		//Start broadcasting server
		FeatureManager.start(startURI);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					FeatureManager.shutdown();
				} catch (Exception e) {
					logger.error("Error shutting down BroadcastManager", e);
				}
				try {
					NotificationManager.shutdown();
				} catch (Exception e) {
					logger.error("Error shutting down NotificationManager", e);
				}
				try {
					server.stop();
				} catch (Exception e) {
					logger.error("Error stopping Photonic3D http server", e);
				} finally {
					server.destroy();
					logger.info("Shutdown Complete");
				}
			}
		});
		
		//Startup all printers that should be autostarted
		List<PrinterConfiguration> configurations = HostProperties.Instance().getPrinterConfigurations();
		for (PrinterConfiguration configuration : configurations) {
			if (configuration.isAutoStart()) {
				PrinterService.INSTANCE.startPrinter(configuration.getName());
			}
		}

		//At this point we can safely say that a startup is officially complete.
		HostProperties.Instance().hostStartupComplete();
		
		//Wait in the Main method until we are shutdown by the OS
		try {
			server.join();
		} catch (Exception e) {
			logger.error("Error waiting for the OS to shut us down", e);
		}
	}
}