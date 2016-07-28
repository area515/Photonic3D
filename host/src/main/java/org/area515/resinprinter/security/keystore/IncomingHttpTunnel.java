package org.area515.resinprinter.security.keystore;

import java.io.ByteArrayOutputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.naming.InvalidNameException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.conn.DefaultHttpResponseParser;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.DefaultHttpRequestWriter;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
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
    private static final Pattern HEADER_PATTERN = Pattern.compile("([^=]+)=(.*)");
    
	private Session session;
	private RendezvousClient server;
	//TODO: Unfortunately this isn't a good idea, we can't use this single socket asynchronously as it will eventually cause: Blocking message pending 10000 for BLOCKING
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
		
		public HttpResponse buildResponse() throws HttpException, IOException {
			ByteSessionInputBuffer buffer = new ByteSessionInputBuffer(content, offset, length);
			DefaultHttpResponseParser parser = new DefaultHttpResponseParser(buffer);
			HttpResponse response = parser.parse();
			response.setEntity(buffer.getRemainingEntity());
			return response;
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
    
    public ResponseWaiter sendMessage(UUID fromLocal, UUID toRemote, HttpRequest request, long timeoutValue, TimeUnit timeoutUnit) throws InvalidKeyException, HttpException, InvalidNameException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, JsonProcessingException, IOException, InterruptedException, TimeoutException, UserManagementException, CertificateExpiredException, CertificateNotYetValidException, NoSuchAlgorithmException, SignatureException, NoSuchPaddingException {
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
    	UserConnection connection = buildConnection(fromLocal, toRemote);
    	Long requestNumber = new Random().nextLong();
		String requestNumberString = requestNumber + "\n";
		
		ByteSessionOutputBuffer sessionBuffer = new ByteSessionOutputBuffer();
		DefaultHttpRequestWriter writer = new DefaultHttpRequestWriter(sessionBuffer);
		writer.write(request);
		
		ByteArrayOutputStream entityOutput = null;
		if (request instanceof HttpEntityEnclosingRequest) {
			entityOutput = new ByteArrayOutputStream();
			((HttpEntityEnclosingRequest)request).getEntity().writeTo(entityOutput);
		}
		
		byte[] requestBytes = sessionBuffer.getByteArray();
		ByteBuffer buffer = ByteBuffer.allocate(requestNumberString.length() + sessionBuffer.getLength() + (entityOutput == null?0:entityOutput.toByteArray().length));
		buffer.put(requestNumberString.getBytes());
		buffer.put(requestBytes, 0, sessionBuffer.getLength());
		if (entityOutput != null) {
			buffer.put(entityOutput.toByteArray());
		}
		
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
		String requestOrResponse = null;
		int start = 0;
		for (; content[start] != 10 && start < content.length; start++) {
		}
		
		requestOrResponse = new String(content, 0, start).trim();
		
		//Check to see if this was a response first
		ResponseWaiter waiter = waiters.remove(Long.parseLong(requestOrResponse));
		if (waiter != null) {
			waiter.setResponse(content, start + 1, content.length - start - 1);
			return;
		}

		//This message must be an http request
		ByteSessionInputBuffer sessionBuffer = new ByteSessionInputBuffer(content, start + 1, content.length - start - 1);
		DefaultHttpRequestParser parser = new DefaultHttpRequestParser(sessionBuffer);
		HttpRequest request;
		try {
			request = (HttpRequest)parser.parse();
		} catch (IOException | HttpException e) {
			logger.error(e);
			server.unauthenticatedMessage(inMessage);
			return;
		}
		//HttpUriRequest request = parseRequestLine(requestLine);
		//request.setHeaders(headers.toArray(new Header[headers.size()]));
		if (request instanceof BasicHttpEntityEnclosingRequest) {
			((BasicHttpEntityEnclosingRequest)request).setEntity(sessionBuffer.getRemainingEntity());
		}
		
		//Add 1 to start in order to get past the \n
		start++;
		requestOrResponse += "\n";
		try {
			byte[] response = server.executeProxiedRequestFromRemote(connection, request);
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
