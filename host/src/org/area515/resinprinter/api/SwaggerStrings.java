package org.area515.resinprinter.api;

public class SwaggerStrings {
	public static final String SUCCESS = "Success";
	public static final String UNEXPECTED_ERROR = "If the server experienced an unexpected error that the GUI should not expect to recover.";
	public static final String USER_UNDERSTANDABLE_ERROR = "The request was unsuccessful and the user of the application should be expected to understand the reason for failure.";
	public static final String NOTIFICATION_MANAGER_SUFFIX = ", a notification will be sent to the NotificationManager. The NotificationManager will then notify all registered implementations of org.area515.resinprinter.notification.Notifier. Implemenations of Notifier are currently Email and WebSockets.";
	public static final String PRINT_FILE_PROCESSOR = "PrintFileProcessors are registered in the config.properties under the notify.[classname.of.PrintFileProcessor.implementation]=true";
	public static final String DIAGNOSTIC_DUMP_PREFIX = "Perform a diagnostic dump that contains system properties, log4j properties, startup logs, cwh logs, configuration properties, stacktrace, xml representation of printers and photonic3d log";
}
