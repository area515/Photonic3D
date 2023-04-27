package org.area515.resinprinter.discover;

import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.model.Namespace;
import org.fourthline.cling.transport.impl.apache.StreamClientConfigurationImpl;
import org.fourthline.cling.transport.impl.apache.StreamClientImpl;
import org.fourthline.cling.transport.spi.StreamClient;

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
    public StreamClient createStreamClient() {
        return new StreamClientImpl(new StreamClientConfigurationImpl(getSyncProtocolExecutorService()));
    }
}
