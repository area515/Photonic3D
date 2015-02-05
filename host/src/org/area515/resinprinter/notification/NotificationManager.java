package org.area515.resinprinter.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.websocket.server.ServerContainer;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;

public class NotificationManager {
	private static List<Notifier> notifiers = null;
	
	public static void start(ServerContainer container) {
		if (notifiers != null) {
			return;
		}
		
		notifiers = new ArrayList<Notifier>();
		List<Class<Notifier>> notifyierClasses = HostProperties.Instance().getNotifiers();
		for (Class<Notifier> currentClass : notifyierClasses) {
			Notifier notifier;
			try {
				notifier = currentClass.newInstance();
				notifier.register(container);
				notifiers.add(notifier);
			} catch (InstantiationException | IllegalAccessException | InappropriateDeviceException e) {
				System.out.println("Couldn't start Notifier");
				e.printStackTrace();
			}
		}
	}
	
	public static Future<?> jobChanged(final PrintJob job) {
		return Main.GLOBAL_EXECUTOR.submit(new Runnable() {
			@Override
			public void run() {
				for (Notifier currentNotifier : notifiers) {
					currentNotifier.jobChanged(job);
				}
			}
		});
	}
	
	public static Future<?> printerChanged(final Printer printer) {
		return Main.GLOBAL_EXECUTOR.submit(new Runnable() {
			@Override
			public void run() {
				for (Notifier currentNotifier : notifiers) {
					currentNotifier.printerChanged(printer);
				}
			}
		});
	}
	
	public static void shutdown() {
		for (Notifier currentNotifier : notifiers) {
			currentNotifier.stop();
		}
	}
}
