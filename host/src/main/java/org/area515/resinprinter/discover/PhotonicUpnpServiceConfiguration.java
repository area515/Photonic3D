package org.area515.resinprinter.discover;

import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.model.Namespace;

public class PhotonicUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {
	public PhotonicUpnpServiceConfiguration(int streamListenPort) {
		super(streamListenPort);
	}

	@Override
	protected Namespace createNamespace() {
		return new PhotonicNamespace();
	}
}
