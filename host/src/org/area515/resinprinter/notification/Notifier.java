package org.area515.resinprinter.notification;

import java.io.File;
import java.util.List;

import javax.websocket.server.ServerContainer;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.slice.StlError;

public interface Notifier {
	public enum NotificationEvent {
		OutOfInk,
		PrinterChanged,
		PrintJobChanged,
		GeometryError,
		FileUploadComplete
	}
	
	//Management methods for Notifier
	public void register(ServerContainer container) throws InappropriateDeviceException;
	public void stop();
	
	//Events 
	public void jobChanged(Printer printer, PrintJob job);
	public void printerOutOfMatter(Printer printer, PrintJob job);
	public void printerChanged(Printer printer);
	public void fileUploadComplete(File fileUploaded);
	public void geometryError(PrintJob job, List<StlError> error);
}
