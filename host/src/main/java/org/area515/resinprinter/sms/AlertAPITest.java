package org.area515.resinprinter.sms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AlertAPITest {
	public static class HttpClientUtils {

	    private final static Map<String, HttpClientUtils> instances = new HashMap<>();

	    private final ThreadSafeClientConnManager threadSafeClientConnManager;

	    private final int connectionTimeout;
	    private final int soTimeout;

	    private HttpClientUtils(int connectionTimeout, int soTimeout) {
	        this.connectionTimeout = connectionTimeout;
	        this.soTimeout = soTimeout;

	        this.threadSafeClientConnManager = new ThreadSafeClientConnManager();
	        this.threadSafeClientConnManager.setDefaultMaxPerRoute(200);
	        this.threadSafeClientConnManager.setMaxTotal(200);
	    }

	    /**
	     * Return an existing or instantiate a new HttpClient factory instance with explicitly specified connection and read timeout values
	     *
	     * @param connectionTimeout the timeout value in milliseconds to use when establishing a new http socket
	     * @param soTimeout the timeout value in milliseconds to wait for a http response before closing the socket
	     *
	     * @return HttpClientUtils an instance of the HttpClient factory primed with the requested timeout values
	     */
	    public static HttpClientUtils getInstance(int connectionTimeout, int soTimeout) {
	        String key = "c-" + connectionTimeout + "-so-" + soTimeout;
	        HttpClientUtils instance = instances.get(key);
	        if (instance == null) {
	            instance = new HttpClientUtils(connectionTimeout, soTimeout);
	            instances.put(key, instance);
	        }
	        return instance;
	    }

	    /**
	     * Instantiate a new HttpClient instance that uses the timeout values associated with this factory instance
	     *
	     * @return HttpClient a new HttpClient instance
	     */
	    public HttpClient getNewHttpClient() {
	        HttpParams httpClientParams = new BasicHttpParams();
	        HttpProtocolParams.setUserAgent(httpClientParams, "nexmo-java/2.0.0-prerelease");
	        HttpProtocolParams.setContentCharset(httpClientParams, "UTF-8");
	        HttpProtocolParams.setHttpElementCharset(httpClientParams, "UTF-8");
	        HttpConnectionParams.setConnectionTimeout(httpClientParams, this.connectionTimeout);
	        HttpConnectionParams.setSoTimeout(httpClientParams, this.soTimeout);
	        HttpConnectionParams.setStaleCheckingEnabled(httpClientParams, true);
	        HttpConnectionParams.setTcpNoDelay(httpClientParams, true);

	        return new DefaultHttpClient(this.threadSafeClientConnManager, httpClientParams);
	    }
	}
	
	public static class ShortCode {
		@JsonProperty("api_key")
		private String api_key;
		@JsonProperty("api_secret")
		private String api_secret;
		@JsonProperty("to")
		private String to;
		@JsonProperty("template")
		private int template;
		
		//This is a custom parameter for your template
		@JsonProperty("body")
		private String body;
	}
	
	public static class ResponseMessage {
		@JsonProperty("status")
		private String status;
		@JsonProperty("message-id")
		private String messageId;
		@JsonProperty("to")
		private String to;
		@JsonProperty("client-ref")
		private String clientRef;
		@JsonProperty("remaining-balance")
		private String remainingBalance;
		@JsonProperty("message-price")
		private String messagePrice;
		@JsonProperty("network")
		private String network;
		@JsonProperty("error-message")
		private String errorMessage;
	}
	
	public static class ShortCodeResponse {
		@JsonProperty("message-count")
		private String messageCount;
		@JsonProperty("messages")
		private List<ResponseMessage> messages;
	}
	
	public static void main(String[] args) throws Exception {
		ShortCode code = new ShortCode();
		//TODO: Setup short code request here...
		
		HttpClientUtils utils = HttpClientUtils.getInstance(5000, 30000);
		HttpClient client = utils.getNewHttpClient();
        HttpPost httpPost = new HttpPost("https://rest.nexmo.com/sc/us/alert/json");
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        ObjectMapper mapper = new ObjectMapper();
        httpPost.setEntity(new StringEntity(mapper.writeValueAsString(code)));
        HttpResponse httpResponse = client.execute(httpPost);
        ShortCodeResponse response = mapper.readValue(httpResponse.getEntity().getContent(), ShortCodeResponse.class);
        System.out.println(response);
	}
}
