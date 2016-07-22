package org.area515.resinprinter.security.keystore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.naming.InvalidNameException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.plugin.FeatureManager;
import org.area515.resinprinter.security.Friend;
import org.area515.resinprinter.security.PhotonicUser;
import org.area515.resinprinter.security.UserManagementException;
import org.area515.resinprinter.security.UserManagementFeature;
import org.area515.resinprinter.security.keystore.IncomingHttpTunnel.ResponseWaiter;
import org.area515.resinprinter.server.HostProperties;

import com.fasterxml.jackson.core.JsonProcessingException;

public class RendezvousClient {
    private static final Logger logger = LogManager.getLogger();
    
    private Map<Conversation, UserConnection> openConversations = new ConcurrentHashMap<>();
	private static RendezvousClient defaultServer = null;
	private HttpClientBuilder clientBuilder;
	private IncomingHttpTunnel incoming;
	private URI schemaHostPort;
	private File keyFile;
	private String keystorePassword;
	private boolean allowInsecureCommunications;
	private UserManagementFeature userManagement;
	private X509FriendshipFeature friendshipFeature;
	
	private static class Conversation {
		private UUID first;
		private UUID second;
		
		private Conversation(UUID first, UUID second) {
	    	if (first.compareTo(second) > 0) {
	    		UUID other = first;
	    		first = second;
	    		second = other;
	    	}
	    	this.first = first;
	    	this.second = second;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((first == null) ? 0 : first.hashCode());
			result = prime * result + ((second == null) ? 0 : second.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Conversation other = (Conversation) obj;
			if (first == null) {
				if (other.first != null)
					return false;
			} else if (!first.equals(other.first))
				return false;
			if (second == null) {
				if (other.second != null)
					return false;
			} else if (!second.equals(other.second))
				return false;
			return true;
		}
	}
	
	RendezvousClient(
			File keyFile, 
			String keystorePassword, 
			boolean allowInsecureCommunications, 
			URI rendezvousServerWebSocketAddress, 
			URI schemaHostPort, 
			UserManagementFeature userManagement) throws Exception {
		this.clientBuilder = HttpClientBuilder.create();
		this.schemaHostPort = schemaHostPort;
		this.keyFile = keyFile;
		this.keystorePassword = keystorePassword;
		this.allowInsecureCommunications = allowInsecureCommunications;
		this.userManagement = userManagement;
		//TODO: this server won't scale until we make a whole bunch of these, but we need to beware of how we route the waiting responses to the proper http tunnel
        incoming = new IncomingHttpTunnel(this, rendezvousServerWebSocketAddress);
        //TODO: we need another session for the purpose of sending/receiving WebSocketNotifications
	}
	
	void setX509FriendshipFeature(X509FriendshipFeature feature) {
		this.friendshipFeature = feature;
	}
	
	public synchronized static RendezvousClient getServer(URI schemaHostPort) throws Exception {
		if (defaultServer == null) {
			HostProperties properties = HostProperties.Instance();
			File keyFile = properties.getUserKeystoreFile();
			String keystorePassword = properties.getUserKeystorePassword();
			
			URI rendezvousServerWebSocketAddress = new URI(properties.loadProperty("rendezvousServerURI"));
			defaultServer = new RendezvousClient(
					keyFile, 
					keystorePassword, 
					false, 
					rendezvousServerWebSocketAddress, 
					schemaHostPort, 
					FeatureManager.getUserManagementFeature());
			defaultServer.setX509FriendshipFeature((X509FriendshipFeature)FeatureManager.getFriendshipFeatures().get(X509FriendshipFeature.FEATURE_NAME));
		}
		
		return defaultServer;
	}
	
	public class UserConnection {
		private PhotonicUser localUser;
		private PhotonicUser remoteUser;
		private PhotonicCrypto crypto;
		private CloseableHttpClient localClient;
		
		public UserConnection(PhotonicUser localUser, PhotonicUser remoteUser, PhotonicCrypto crypto) {
			this.localUser = localUser;
			this.remoteUser = remoteUser;
			this.crypto = crypto;
			//TODO: setup auth in clientBuilder
			this.localClient = clientBuilder.build();
		}

		public PhotonicUser getRemoteUser() {
			return remoteUser;
		}

		public PhotonicCrypto getCrypto() {
			return crypto;
		}

		public CloseableHttpClient getLocalClient() {
			return localClient;
		}
	}
	
	public UserConnection getConnection(UUID request, UUID response) {
		return openConversations.get(new Conversation(request, response));
	}
	
	public ByteBuffer sendRequestToRemote(UUID from, UUID to, String url, byte[] bodyOfRequest, long timeout, TimeUnit timeUnit) throws TimeoutException, NoSuchPaddingException, SignatureException, InvalidKeyException, CertificateExpiredException, CertificateNotYetValidException, InvalidNameException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, JsonProcessingException, NoSuchAlgorithmException, IOException, InterruptedException, TimeoutException, UserManagementException {
		ResponseWaiter waiter = incoming.sendMessage(from, to, url, bodyOfRequest, timeout, timeUnit);
		if (!waiter.await(timeout, timeUnit)) {
			throw new TimeoutException("Timed out waiting for response");
		}
		
		return waiter.getByteBuffer();
	}
	
	public byte[] executeProxiedRequestFromRemote(UserConnection connection, String relativeURL, byte[] entity, int offset, int length) throws IOException {
		logger.info("Remote user {} making request for: {}", connection.getRemoteUser(), relativeURL);
		HttpUriRequest request;
		if (entity == null) {
			request = new HttpGet(schemaHostPort + relativeURL);
		} else {
			request = new HttpPost(schemaHostPort + relativeURL);
			((HttpPost)request).setEntity(new ByteArrayEntity(entity, offset, length));
		}
		HttpResponse httpResponse = connection.getLocalClient().execute(request);
		HttpEntity responseEntity = httpResponse.getEntity();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		InputStream inputStream = null;
		try {
			inputStream = responseEntity.getContent();
			IOUtils.copy(inputStream, outputStream);
			return outputStream.toByteArray();
		} finally {
			if (inputStream != null) {
				try {inputStream.close();} catch (IOException e) {}
			}
			EntityUtils.consume(responseEntity);
		}
	}

	public synchronized void close() {
		for (UserConnection connection : openConversations.values()) {
			try {connection.localClient.close();} catch (IOException e) {}
		}
		
		openConversations.clear();
		incoming.close();
	}
	
	public void unauthenticatedMessage(Message message) {
		//TODO: Potential DOS attack. Too many of these and we will shut down the web socket permanently, Rendezvous server shouldn't send us these!
		logger.info("Unauthenticated message from:{}", message == null?"":message.getFrom());
		return;
	}

	private PhotonicCrypto getLocalCrypto(UUID local, UUID remote) throws UserManagementException {
		CryptoUserIdentity localIdentity = (CryptoUserIdentity)userManagement.getLoggedInIdentity(new PhotonicUser(null, null, local, null, null));
		if (localIdentity == null) {
			throw new UserManagementException("A remote user attepted to login:" + remote + " but the local user:" + local + " wasn't logged in to accept the response.");
		}
		try {
			return new PhotonicCrypto(localIdentity.getCrypto());
		} catch (CertificateExpiredException | CertificateNotYetValidException | CertificateEncodingException | InvalidNameException e) {
			throw new UserManagementException("Couldn't clone local crypto for purpose of creating new conversation");
		}
	}
	
	public UserConnection createOutgoingConnection(UUID fromLocal, UUID toRemote) throws UserManagementException {
    	PhotonicUser fromLocalUser = userManagement.getUser(fromLocal);
    	PhotonicUser toRemoteUser = userManagement.getUser(toRemote);
    	PhotonicCrypto fromLocalCrypto = getLocalCrypto(fromLocal, toRemote);
    	PhotonicCrypto toRemoteCrypto;
		try {
			toRemoteCrypto = KeystoreUtilities.getPhotonicCrypto(toRemoteUser, keyFile, null, keystorePassword, allowInsecureCommunications);
		} catch (InvalidNameException | IOException | GeneralSecurityException e) {
			throw new UserManagementException("Couldn't create user:" + toRemoteUser, e);
		}
    	fromLocalCrypto.setRemoteCrypto(toRemoteCrypto);
    	UserConnection connection = new UserConnection(fromLocalUser, toRemoteUser, fromLocalCrypto);
    	openConversations.put(new Conversation(fromLocal, toRemote), connection);
    	return connection;
	}
	
    public UserConnection attemptLoginWithRemoteId(UUID fromRemote, UUID toLocal) throws UserManagementException {
    	PhotonicUser fromRemoteUser = userManagement.getUser(fromRemote);
    	PhotonicUser toLocalUser = userManagement.getUser(toLocal);
    	CryptoUserIdentity fromRemoteIdentity = (CryptoUserIdentity)userManagement.loginRemote(fromRemoteUser);
		
    	PhotonicCrypto toLocalCrypto = getLocalCrypto(toLocal, fromRemote);
    	toLocalCrypto.setRemoteCrypto(fromRemoteIdentity.getCrypto());
    	UserConnection connection = new UserConnection(toLocalUser, fromRemoteUser, toLocalCrypto);
    	openConversations.put(new Conversation(fromRemote, toLocal), connection);
    	return connection;
    }
    
    public void addToFriendRequestList(Friend friend) throws InvalidNameException {
    	friendshipFeature.addRemoteFriendRequest(friend);
    }
    
    public Friend sendFriendRequest(UUID fromLocal, UUID toRemote) throws JsonProcessingException, IOException, InvalidNameException, GeneralSecurityException {
    	CryptoUserIdentity localIdentity = (CryptoUserIdentity)userManagement.getLoggedInIdentity(new PhotonicUser(null, null, fromLocal, null, null));
    	if (localIdentity == null) {
    		throw new InvalidNameException("Unkown local user: " + fromLocal);
    	}
    	
    	incoming.sendCertificateTrustExchange(localIdentity.getCrypto(), toRemote);
    	//TODO: Wait for a future
    	return null;
    }
    
    public void sendAcceptFriendResponse(Friend friend) {
    	//TODO: We don't have a protocol for this yet...
    }
}
