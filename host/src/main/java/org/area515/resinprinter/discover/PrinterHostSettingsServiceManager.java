package org.area515.resinprinter.discover;

import java.net.URI;

import org.area515.resinprinter.discover.UPNPAdvertiser.UPNPSetup;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.meta.LocalService;

public class PrinterHostSettingsServiceManager<T> extends DefaultServiceManager<T> {
	private URI uri;
	private UPNPSetup setup;
	
	public PrinterHostSettingsServiceManager(UPNPSetup setup, URI uri, LocalService<T> service, Class<T> serviceClass) {
		super(service, serviceClass);
		this.uri = uri;
		this.setup = setup;
	}

	public URI getUri() {
		return uri;
	}

	public UPNPSetup getSetup() {
		return setup;
	}
}
