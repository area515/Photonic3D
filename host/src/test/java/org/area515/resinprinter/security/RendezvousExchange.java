package org.area515.resinprinter.security;

import java.io.File;
import java.util.Base64;

import javax.websocket.server.ServerContainer;

import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.security.keystore.KeystoreLoginService;
import org.area515.resinprinter.security.keystore.KeystoreUtilities;
import org.area515.resinprinter.security.keystore.PhotonicCrypto;
import org.junit.Test;
import org.mockito.Mockito;

public class RendezvousExchange {
	private static PhotonicCrypto crypto1;
	private static PhotonicCrypto crypto2;
	private static PhotonicCrypto uninitializedCrypto;
	private static File user1Keystore;
	private static File user2Keystore;
	private static KeystoreLoginService service1;
	private static KeystoreLoginService service2;
	private static PhotonicUser user1;
	private static PhotonicUser user2;

	@Test
	public void messageExchange() {
	}
}

