package org.area515.resinprinter.notification;

import javax.websocket.server.ServerContainer;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;

public interface Notifier {
	public void register(ServerContainer container) throws InappropriateDeviceException;
	public void jobChanged(Printer printer, PrintJob job);
	public void printerChanged(Printer printer);
	public void stop();
}
