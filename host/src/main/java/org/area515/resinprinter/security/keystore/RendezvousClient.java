package org.area515.resinprinter.security.keystore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.naming.InvalidNameException;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.plugin.FeatureManager;
import org.area515.resinprinter.security.UserManagementException;
import org.area515.resinprinter.security.UserManagementFeature;
import org.area515.resinprinter.security.keystore.IncomingHttpTunnel.ResponseWaiter;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.util.security.Friend;
import org.area515.resinprinter.util.security.KeystoreUtilities;
import org.area515.resinprinter.util.security.Message;
import org.area515.resinprinter.util.security.PhotonicCrypto;
import org.area515.resinprinter.util.security.PhotonicUser;

import com.fasterxml.jackson.core.JsonProcessingException;

public class RendezvousClient {
    private static final Logger logger = LogManager.getLogger();
    private static final Pattern REQUEST_LINE_PATTERN = Pattern.compile("([^ ]+) ([^ ]+) (.+)");
    private ConcurrentHashMap<Conversation, UserConnection> conversations = new ConcurrentHashMap<>();
    
	private static RendezvousClient defaultServer = null;
	private HttpClientBuilder clientBuilder;
	private IncomingHttpTunnel incoming;
	private URI schemaHostPort;
	private File keyFile;
	private String keystorePassword;
	private boolean allowInsecureCommunications;
	private UserManagementFeature userManagement;
	private X509FriendshipFeature friendshipFeature;
	private CredentialsProvider credentialProvider = new BasicCredentialsProvider();
	
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
		this.clientBuilder.setTargetAuthenticationStrategy(TargetAuthenticationStrategy.INSTANCE);
		this.clientBuilder.setUserAgent("Photonic3dRemoteClient");
		this.clientBuilder.setDefaultCredentialsProvider(credentialProvider);
		
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
			defaultServer.setX509FriendshipFeature((X509FriendshipFeature)FeatureManager.getFriendshipFeatures().get(PhotonicCrypto.FEATURE_NAME));
		}
		
		return defaultServer;
	}
	
	//TODO: Need some synchronization around key exchanges maybe?
	public class UserConnection {
		private PhotonicUser localUser;
		private PhotonicUser remoteUser;
		private PhotonicCrypto crypto;
		private CloseableHttpClient localClient;
		
		//This should only be created when a successful connection has been authenticated
		public UserConnection(PhotonicUser localUser, PhotonicUser remoteUser, PhotonicCrypto crypto, URI uri) {
			this.localUser = localUser;
			this.remoteUser = remoteUser;
			this.crypto = crypto;
			this.localClient = clientBuilder.build();

			credentialProvider.setCredentials(
					new AuthScope(
							uri.getHost(), 
							uri.getPort(), 
							HostProperties.Instance().getSecurityRealmName(),
							Main.AUTHENTICATION_SCHEME), 
					new UsernamePasswordCredentials(remoteUser.getName(), remoteUser.getCredential()));
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
	
	public HttpResponse sendRequestToRemote(UUID from, UUID to, HttpRequest bodyOfRequest, long timeout, TimeUnit timeUnit) throws TimeoutException, NoSuchPaddingException, SignatureException, InvalidKeyException, CertificateExpiredException, CertificateNotYetValidException, InvalidNameException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, JsonProcessingException, NoSuchAlgorithmException, IOException, InterruptedException, TimeoutException, UserManagementException, HttpException {
		ResponseWaiter waiter = incoming.sendMessage(from, to, bodyOfRequest, timeout, timeUnit);
		if (!waiter.await(timeout, timeUnit)) {
			throw new TimeoutException("Timed out waiting for response");
		}
		
		return waiter.buildResponse();
	}
	
	private HttpUriRequest parseRequestLine(String requestLine) throws IOException {
		Matcher matcher = REQUEST_LINE_PATTERN.matcher(requestLine);
		if (!matcher.matches()) {
			throw new IOException("Requestline:" + requestLine + " doesn't match:" + REQUEST_LINE_PATTERN.pattern());
		}
		
		String method = matcher.group(1);
		String relativeURL = matcher.group(1);
		HttpUriRequest request = null;
		if (method.equals(HttpGet.METHOD_NAME)) {
			request = new HttpGet(schemaHostPort + relativeURL);
		} else if (method.equals(HttpDelete.METHOD_NAME)) {
			request = new HttpDelete(schemaHostPort + relativeURL);
		} else if (method.equals(HttpHead.METHOD_NAME)) {
			request = new HttpHead(schemaHostPort + relativeURL);
		} else if (method.equals(HttpOptions.METHOD_NAME)) {
			request = new HttpOptions(schemaHostPort + relativeURL);
		} else if (method.equals(HttpTrace.METHOD_NAME)) {
			request = new HttpTrace(schemaHostPort + relativeURL);
		} else if (method.equals(HttpPost.METHOD_NAME)) {
			request = new HttpPost(schemaHostPort + relativeURL);
		} else if (method.equals(HttpPut.METHOD_NAME)) {
			request = new HttpPut(schemaHostPort + relativeURL);
		} else if (method.equals(HttpPatch.METHOD_NAME)) {
			request = new HttpPatch(schemaHostPort + relativeURL);
		}
		
		return request;
	}
	
	public byte[] executeProxiedRequestFromRemote(UserConnection connection, HttpRequest request) throws IOException {
		logger.info("Remote user {} making request for: {}", connection.getRemoteUser(), request.getRequestLine());
		HttpUriRequest uriRequest;
		if (request instanceof HttpUriRequest) {
			uriRequest = (HttpUriRequest)request;
		} else {
			uriRequest = parseRequestLine(request.getRequestLine() + "");
			uriRequest.setHeaders(request.getAllHeaders());
			if (request instanceof BasicHttpEntityEnclosingRequest) {
				((HttpEntityEnclosingRequest)request).setEntity(((BasicHttpEntityEnclosingRequest)request).getEntity());
			}
		}
		HttpResponse httpResponse = connection.getLocalClient().execute(uriRequest);
		logger.info("Local response from web server: {} - {}", httpResponse.getStatusLine().getStatusCode(), httpResponse.getStatusLine().getReasonPhrase());
		HttpEntity responseEntity = httpResponse.getEntity();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		InputStream inputStream = null;
		try {
			inputStream = responseEntity.getContent();
			outputStream.write((httpResponse + "\r\n").getBytes());
			for (Header header : uriRequest.getAllHeaders()) {
				outputStream.write((header.getName() + "=" + header.getValue() + "\r\n").getBytes());
			}
			outputStream.write("\r\n".getBytes());
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
		for (UserConnection connection : conversations.values()) {
			try {connection.localClient.close();} catch (IOException e) {}
		}
		
		conversations.clear();
		incoming.close();
	}
	
	public void unauthenticatedMessage(Message message) {
		//TODO: Potential DOS attack. Too many of these and we will shut down the web socket permanently, Rendezvous server shouldn't send us these!
		logger.info("Unauthenticated message from:{}", message == null?"":message.getFrom());
		return;
	}

	private PhotonicCrypto getLocalCrypto(UUID local, UUID remote) throws UserManagementException {
		//CryptoUserIdentity localIdentity = (CryptoUserIdentity)userManagement.getLoggedInIdentity(new PhotonicUser(null, null, local, null, null, false));
		CryptoUserIdentity localIdentity = (CryptoUserIdentity)userManagement.getLoggedInIdentity(userManagement.getUser(local));
		if (localIdentity == null) {
			throw new UserManagementException("A remote user attepted to login:" + remote + " but the local user:" + local + " wasn't logged in to accept the response.");
		}
		try {
			return new PhotonicCrypto(localIdentity.getCrypto());
		} catch (CertificateExpiredException | CertificateNotYetValidException | CertificateEncodingException | InvalidNameException e) {
			throw new UserManagementException("Couldn't clone local crypto for purpose of creating new conversation");
		}
	}
	
	private UserConnection createOutgoingConnection(UUID fromLocal, UUID toRemote) throws UserManagementException {
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
    	UserConnection connection = new UserConnection(fromLocalUser, toRemoteUser, fromLocalCrypto, schemaHostPort);
    	UserConnection oldConnection = conversations.putIfAbsent(new Conversation(fromLocal, toRemote), connection);
    	if (oldConnection != null) {
    		connection = oldConnection;
    	}
    	logger.info("Connection made from:{} to:{}", fromLocal, toRemote);
    	return connection;
	}
	
	//TODO: Creating remote logins should probably be in a different method than building connections
	//TODO: this should only throw a UserManagementException not all of the rest of the exceptions
    public UserConnection buildConnection(UUID local, UUID remote, boolean remoteLoginRequired) throws UserManagementException, JsonProcessingException, IOException, CertificateExpiredException, CertificateNotYetValidException, InvalidKeyException, InvalidNameException, NoSuchAlgorithmException, SignatureException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		UserConnection connection = conversations.get(new Conversation(local, remote));
		if (connection == null) {
			if (remoteLoginRequired) {
				return attemptRemoteLogin(remote, local);
			}
			
			connection = createOutgoingConnection(local, remote);
		} else if (remoteLoginRequired && userManagement.getLoggedInIdentity(userManagement.getUser(remote)) == null) {
			return attemptRemoteLogin(remote, local);
		}
		
		return connection;
    }

    private UserConnection attemptRemoteLogin(UUID fromRemote, UUID toLocal) throws UserManagementException {
    	PhotonicUser fromRemoteUser = userManagement.getUser(fromRemote);
    	PhotonicUser toLocalUser = userManagement.getUser(toLocal);
    	CryptoUserIdentity fromRemoteIdentity = (CryptoUserIdentity)userManagement.loginRemote(fromRemoteUser);
		
    	PhotonicCrypto toLocalCrypto = getLocalCrypto(toLocal, fromRemote);
    	toLocalCrypto.setRemoteCrypto(fromRemoteIdentity.getCrypto());
    	UserConnection connection = new UserConnection(toLocalUser, (PhotonicUser)fromRemoteIdentity.getUserPrincipal(), toLocalCrypto, schemaHostPort);
    	UserConnection oldConnection = conversations.putIfAbsent(new Conversation(fromRemote, toLocal), connection);
    	if (oldConnection != null) {
    		connection = oldConnection;
    	}
    	logger.info("Connection(and remote login) made from:{} to:{}", fromRemote, toLocal);
    	return connection;
    }
    
    public void addToFriendRequestList(Friend friend) throws InvalidNameException {
    	friendshipFeature.addRemoteFriendRequest(friend);
    }
    
    public Friend sendFriendRequest(UUID fromLocal, UUID toRemote) throws JsonProcessingException, IOException, InvalidNameException, GeneralSecurityException {
    	CryptoUserIdentity localIdentity = (CryptoUserIdentity)userManagement.getLoggedInIdentity(new PhotonicUser(null, null, fromLocal, null, null, false));
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
    
    public String toString() {
    	return keyFile + "";
    }
}
