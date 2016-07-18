package org.area515.resinprinter.security.keystore;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.naming.InvalidNameException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.security.Friend;
import org.area515.resinprinter.security.UserManagementException;
import org.area515.resinprinter.security.keystore.RendezvousClient.UserConnection;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebSocket()
public class IncomingHttpTunnel {
    private static final Logger logger = LogManager.getLogger();

	private Session session;
	private RendezvousClient server;
	private Map<Long, ResponseWaiter> waiters = new ConcurrentHashMap<>();
	
	public class ResponseWaiter {
		private CountDownLatch latch = new CountDownLatch(1);
		private int offset;
		private int length;
		private byte[] content;
		
		private ResponseWaiter() {
		}
		
		public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
			return latch.await(timeout, unit);
		}
		
		public void setResponse(byte[] content, int offset, int length) {
			this.content = content;
			this.offset = offset;
			this.length = length;
			latch.countDown();
		}
		
		public ByteBuffer getByteBuffer() {
			return ByteBuffer.wrap(content, offset, length);
		}
	}
	
	public IncomingHttpTunnel(RendezvousClient server, URI rendezvousServerWebSocketAddress) throws Exception {
		this.server = server;
        WebSocketClient client = new WebSocketClient();
        client.start();
        
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        session = client.connect(this, rendezvousServerWebSocketAddress, request).get();
	}
	
    public void sendCertificateTrustExchange(PhotonicCrypto fromLocalCrypto, UUID toRemote) throws CertificateEncodingException, JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		Message message = fromLocalCrypto.buildCertificateTrustMessage(toRemote);
		session.getRemote().sendBytes(ByteBuffer.wrap(mapper.writeValueAsBytes(message)));
		//TODO: Someday we should wait for an accept friend response from the remote.
    }
    
    public UserConnection buildConnection(UUID first, UUID second) throws UserManagementException, JsonProcessingException, IOException, CertificateExpiredException, CertificateNotYetValidException, InvalidKeyException, InvalidNameException, NoSuchAlgorithmException, SignatureException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		UserConnection connection = server.getConnection(first, second);
		if (connection == null) {
			connection = server.createOutgoingConnection(first, second);
	    	Message message = connection.getCrypto().buildKeyExchange();
	    	session.getRemote().sendBytes(ByteBuffer.wrap(mapper.writeValueAsBytes(message)));
			//TODO: Someday we should wait for a key receipt message
		}
		
		return connection;
    }
    
    public ResponseWaiter sendMessage(UUID fromLocal, UUID toRemote, String url, byte[] body, long timeoutValue, TimeUnit timeoutUnit) throws InvalidKeyException, InvalidNameException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, JsonProcessingException, IOException, InterruptedException, TimeoutException, UserManagementException, CertificateExpiredException, CertificateNotYetValidException, NoSuchAlgorithmException, SignatureException, NoSuchPaddingException {
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
    	UserConnection connection = buildConnection(fromLocal, toRemote);
    	Long requestNumber = new Random().nextLong();
		String requestNumberString = requestNumber + "\n";
		
		url += "\n";
		ByteBuffer buffer = ByteBuffer.allocate(requestNumberString.length() + url.length() + body.length);
		buffer.put(requestNumberString.getBytes());
		buffer.put(url.getBytes());
		Message outMessage = connection.getCrypto().buildEncryptedMessage(buffer);
		session.getRemote().sendBytes(ByteBuffer.wrap(mapper.writeValueAsBytes(outMessage)));
		ResponseWaiter waiter = new ResponseWaiter();
		waiters.put(requestNumber, waiter);
		return waiter;
    }
    	
    //All of the following is for HTTP tunneling
	//TODO: @OnWebSocketError is missing
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
    	//TODO: Attempt another connection
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
    }
    
    @OnWebSocketMessage
    public void onMessage(byte buf[], int offset, int length) {
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		Message inMessage;
		try {
			inMessage = mapper.readValue(buf, Message.class);
		} catch (IOException e) {
			logger.error(e);
			server.unauthenticatedMessage(null);
			return;
		}

    	//UserConnection connection = getConnection(inMessage.getFrom(), inMessage.getTo());//Don't call this because we need to login if a connection doesn't already exist.
    	UserConnection connection = server.getConnection(inMessage.getFrom(), inMessage.getTo());
		if (connection == null) {
			
			//first check if this is a trust request!
			Friend newPotentialFriend = null;
			try {
				newPotentialFriend = PhotonicCrypto.checkCertificateTrustExchange(inMessage);
				if (newPotentialFriend != null) {
					server.addToFriendRequestList(newPotentialFriend);
					return;
				}
			} catch (InvalidNameException | CertificateException | RuntimeException e) {
				logger.error(e);
				server.unauthenticatedMessage(inMessage);
				return;
			}
			
			//If not a trust request, we need to attempt a login first
			try {
				connection = server.attemptLoginWithRemoteId(inMessage.getFrom(), inMessage.getTo());
			} catch (UserManagementException e) {
				logger.error(e);
				server.unauthenticatedMessage(inMessage);
				return;
			}
			
			//TODO: We don't have a "remote accepted friend request protocol"
		}

		//Next check to see if the remote is sending us a key exchange message
		byte[] content = null;
		try {
			content = connection.getCrypto().getData(inMessage);
			if (content == null) {
				logger.info("AES key exchange occurred between:" + inMessage.getFrom() + " and:" + inMessage.getTo());
				//TODO: We should send a key receipt message after this.
				return;
			}
		} catch (CertificateExpiredException | CertificateNotYetValidException | InvalidKeyException
				| NoSuchAlgorithmException | SignatureException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | InvalidAlgorithmParameterException e) {
			logger.error(e);
			server.unauthenticatedMessage(inMessage);
			return;
		}
		
		//I don't think we have to call validate on Identity, because http execution will do it for us.
		String relativeURL = null;		
		int start = 0;
		int lastStart = 0;
		String requestOrResponse = null;
		for (start = 0; start < content.length; start++) {
			if (content[start] == 10) {
				if (requestOrResponse == null) {
					requestOrResponse = new String(content, 0, start);
					
					//Check to see if this was a response first
					ResponseWaiter waiter = waiters.remove(Long.parseLong(requestOrResponse));
					if (waiter != null) {
						waiter.setResponse(content, start + 1, content.length - start - 1);
						return;
					}

					lastStart = start;
				} else if (relativeURL == null) {
					relativeURL = new String(content, lastStart + 1, start - lastStart - 1);
					break;
				}
			}
		}
		
		//Add 1 to start in order to get past the \n
		start++;
		requestOrResponse += "\n";
		try {
			byte[] response = server.executeProxiedRequestFromRemote(connection, relativeURL, start == content.length?null:content, start, content.length - start);
			ByteBuffer buffer = ByteBuffer.allocate(requestOrResponse.length() + response.length);
			buffer.put(requestOrResponse.getBytes());
			buffer.put(response);
			Message outMessage = connection.getCrypto().buildEncryptedMessage(buffer);
			session.getRemote().sendBytes(ByteBuffer.wrap(mapper.writeValueAsBytes(outMessage)));
		} catch (IOException | InvalidNameException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			logger.error(e);
		}
    }
    
    public void close() {
    	session.close();
    }
}
