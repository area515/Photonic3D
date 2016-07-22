package org.area515.resinprinter.security.keystore;

import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.security.Friend;
import org.area515.resinprinter.security.PhotonicUser;
import org.eclipse.jetty.util.security.Credential;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "java.security.*", "javax.crypto.*"})
public class RendezvousExchange {
	public class WaitForTwoRemoteMessages implements Answer<Object> {
		private CountDownLatch latch = new CountDownLatch(2);
		
		@Override
		public Object answer(InvocationOnMock invocation) throws Throwable {
			if (invocation.getMethod().getName().equals("remoteMessageReceived")) {
				latch.countDown();
			}
			
			return null;
		}
		
		public void awaitForMessages() throws InterruptedException {
			latch.await(15, TimeUnit.SECONDS);
		}
	}
	
	@Test
	@PrepareForTest(NotificationManager.class)
	public void messageExchange() throws Exception {
		WaitForTwoRemoteMessages waiter = new WaitForTwoRemoteMessages();
		PowerMockito.mockStatic(NotificationManager.class, waiter);
		
		String password = "test";
		int port = 2222;
		int httpPort = 3333;
		String testMessage = "This is my test message";
		
		RendezvousPipe pipe = new RendezvousPipe(port);
		TestServer httpServer = new TestServer(httpPort, testMessage);
		
		File user1Keystore = new File("user1.keystore");
		File user2Keystore = new File("user2.keystore");
		
		user1Keystore.delete();
		user2Keystore.delete();
		
		String username1 = "User1";
		String username2 = "User2";
		
		KeystoreLoginService service1 = new KeystoreLoginService(user1Keystore, password, false);
		KeystoreLoginService service2 = new KeystoreLoginService(user2Keystore, password, false);
		
		PhotonicUser insertUser1 = new PhotonicUser(username1, password, null, username1 + "@stuff.com", new String[]{PhotonicUser.FULL_RIGHTS});
		PhotonicUser insertUser2 = new PhotonicUser(username2, password, null, username2 + "@stuff.com", new String[]{PhotonicUser.FULL_RIGHTS});
		
		PhotonicUser user1 = service1.update(insertUser1);
		PhotonicUser user2 = service2.update(insertUser2);
		
		Assert.assertNotNull(service1.login(username1, Credential.getCredential(password), null));
		Assert.assertNotNull(service2.login(username2, Credential.getCredential(password), null));
		
		RendezvousClient server1 = new RendezvousClient(user1Keystore, password, false, new URI("ws://127.0.0.1:" + port + "/httpTunnel"), new URI("http://127.0.0.1:" + httpPort), service1);
		RendezvousClient server2 = new RendezvousClient(user2Keystore, password, false, new URI("ws://127.0.0.1:" + port + "/httpTunnel"), new URI("http://127.0.0.1:" + httpPort), service2);
		
		X509FriendshipFeature friendship1 = new X509FriendshipFeature(server1);
		X509FriendshipFeature friendship2 = new X509FriendshipFeature(server2);
		
		server1.setX509FriendshipFeature(friendship1);
		server2.setX509FriendshipFeature(friendship2);
		
		server1.sendFriendRequest(user1.getUserId(), user2.getUserId());
		server2.sendFriendRequest(user2.getUserId(), user1.getUserId());
		
		waiter.awaitForMessages();
		
		List<Friend> friendRequests1 = friendship1.getFriendRequests();
		List<Friend> friendRequests2 = friendship2.getFriendRequests();
		
		Friend newFriendFor1 = friendRequests1.get(0);
		Friend newFriendFor2 = friendRequests2.get(0);
		
		friendship1.acceptFriendRequest(newFriendFor1);
		friendship2.acceptFriendRequest(newFriendFor2);
		
		service1.trustNewFriend(newFriendFor1);
		service2.trustNewFriend(newFriendFor2);
		
		Assert.assertTrue(friendship1.getFriendRequests().isEmpty());
		Assert.assertTrue(friendship2.getFriendRequests().isEmpty());
		
		ByteBuffer bufferFrom1To2 = server1.sendRequestToRemote(user1.getUserId(), user2.getUserId(), "services/printers/list", new byte[]{}, 20, TimeUnit.SECONDS);
		ByteBuffer bufferFrom2To1 = server2.sendRequestToRemote(user2.getUserId(), user1.getUserId(), "services/printers/list", new byte[]{}, 20, TimeUnit.SECONDS);
		
		Assert.assertEquals(testMessage, new String(bufferFrom1To2.array(), bufferFrom1To2.position(), bufferFrom1To2.limit() - bufferFrom1To2.position()));
		Assert.assertEquals(testMessage, new String(bufferFrom2To1.array(), bufferFrom2To1.position(), bufferFrom2To1.limit() - bufferFrom2To1.position()));
		
		pipe.close();
		httpServer.close();
	}
}

