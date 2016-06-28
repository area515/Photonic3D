package org.area515.resinprinter.security;

import javax.websocket.server.ServerContainer;

import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.security.keystore.KeystoreLoginService;
import org.area515.resinprinter.services.UserService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class KeystoreSecurityTest {
	@Test
    public void createKeystoreAndAddUser() throws Exception {
		NotificationManager.start(Mockito.mock(ServerContainer.class));
		
		String testUsername = "<>!@#$%^&*()~` ,.;'[]-=?:\"{}|\\frank,cn=Wes";//Ensure they can't perform a name spoof
		
		KeystoreLoginService service = new KeystoreLoginService();
		Assert.assertNull(service.login(testUsername, testUsername, null));
		PhotonicUser user = service.update(testUsername, Credential.getCredential(testUsername), new String[]{UserService.FULL_RIGHTS});
		Assert.assertNotNull(user);
		Assert.assertEquals(user.getName(), testUsername);
		Assert.assertEquals(testUsername, user.getName());
		Assert.assertTrue(service.getUsers().contains(user));
		UserIdentity identity = service.login(testUsername, testUsername, null);
		Assert.assertTrue(service.validate(identity));
		service.logout(identity);
		Assert.assertFalse(service.validate(identity));
		identity = service.login(testUsername, testUsername, null);
		Assert.assertTrue(service.validate(identity));
		service.remove(user);
		Assert.assertFalse(service.validate(identity));
		Assert.assertNull(service.login(testUsername, testUsername, null));
		Assert.assertFalse(service.getUsers().contains(user));
	}
}
