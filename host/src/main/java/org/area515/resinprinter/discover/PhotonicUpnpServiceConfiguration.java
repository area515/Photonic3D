package org.area515.resinprinter.discover;

import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.model.Namespace;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;
import org.fourthline.cling.transport.spi.StreamServer;

public class PhotonicUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {
	public PhotonicUpnpServiceConfiguration(int streamListenPort) {
		super(streamListenPort);
	}

	@Override
	protected Namespace createNamespace() {
		return new PhotonicNamespace();
	}
	
    /*public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
    	
		return null;
    }*/
}
