package org.area515.resinprinter.services;

import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;

@SwaggerDefinition(
        info = @Info(
                description = "This API is currently provisional and will change. "
                		+ "Many of the future changes will be centered around getting the API more standardized. "
                		+ "For example, several methods have the non-standard get(or other parameter names) in the service path, there are capitalization inconsistancies and response object/error inconsistencies"
                		+ "<ul>"
                		+ " <li><a href=\"#/machine\">machine:</a> This service allows you to perform operations unique to the Host itself. "
                		+ "The category of operations include serial port enumeration, wifi management, support diagnostics, "
                		+ "display enumeration, slicing profile enumeration, printable file type enumeration, machine configuration enumeration, "
                		+ "network enumeration, staging automatic updates, supported font enumeration and font uploads.</li>"
						+ " <li><a href=\"#/printers\">printers:</a> This service allows you to manage all printers in Phontonic 3D.</li>"
						+ " <li><a href=\"#/settings\">settings:</a> This service allows a client to manage all of the global settings in Photonic 3D. "
						+ "It is designed as a light wrapper for HostProperties that are defined in <b class=\"code\">config.properties</b></li>"
						+ " <li><a href=\"#/printJobs\">printJobs:</a> This service allows you to view the progress of all currently printing jobs and all printjobs that have already been attempted.</li>"
						+ " <li><a href=\"#/printables\">printables:</a> This service allows you to manage all printable files in Phontonic3d."
						+ " <li><a href=\"#/media\">media:</a> This service performs all media based activities related to imaging, live streaming and video. "
						+ "Currently progressive download of video is performed in the <b class=\"code\">org.area515.resinprinter.stream.ProgressiveDownloadServlet</b>. "
						+ "Eventually those capabilities will be moved to this service once a permanent oauth model has been put into place.</li>"
						+ " <li><a href=\"#/remote\">remote:</a> This service allows you to securely execute any restful method on a remote instance of Photonic3d by prefixing the following path to the front of the call you'd like to remotely execute: "
						+ "<b class=\"code\">remote/execute/{userIdOfRemoteUser}/</b>. "
						+ "The full security prerequisites are detailed in the restful method descriptions below.</b>"
						+ "</ul>",
				contact = @Contact(name = "Wes Gilster", email = "wesgilster@gmail.com", url = "http://www.photonic3d.com"),
                version = "",
                title = "Photonic 3D Rest API"
        ),
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS})
public class SwaggerMetadata {
	public static final String TODO = "TODO: Need to document response model.";
	public static final String SUCCESS = "Success";
	public static final String MACHINE_RESPONSE = "This method returns a MachineResponse which is comprised of three things:"
			+ "<ul>"
			+ "	<li>command - This names the command that was just sent to the server, it is designed to be suitable for a UI to show this as a part of a window title.</li>"
			+ "	<li>response - This method returns 'true' if the command being attempted was successful and 'false' if the command wasn't able to be performed.</li>"
			+ "	<li>message - If the command that was sent to the host was unsuccessful(the response was false), then this is the error message that is designed to be shown to the user. "
			+ "Conversely, if the command was successful, this will contain a bit of helpful information that the user might be interested in seeing.</li>"
			+ "</ul>";
	public static final String UNEXPECTED_ERROR = "If the server experienced an unexpected error that the GUI should not expect to recover.";
	public static final String USER_UNDERSTANDABLE_ERROR = "The request was unsuccessful and the user of the application should be expected to understand the reason for failure.";
	public static final String NOTIFICATION_MANAGER_SUFFIX = ", a notification will be sent to the NotificationManager. The NotificationManager will then notify all registered implementations of <b class=\"code\">org.area515.resinprinter.notification.Notifier</b>. Implemenations of Notifier are currently Email and WebSockets.";
	public static final String DIAGNOSTIC_DUMP_PREFIX = "Perform a diagnostic dump that contains system properties, log4j properties, startup logs, cwh logs, configuration properties, stacktrace, xml representation of printers and photonic3d log";
	public static final String PRINT_FILE_PROCESSOR = "PrintFileProcessors are registered in the <b class=\"code\">config.properties</b> under the <b class=\"code\">notify.[classname.of.PrintFileProcessor.implementation]=true</b>";
	public static final String RESOURCE_NOT_FOUND = "The reqest was unsuccessful because the resource was not found";
}
