package org.area515.resinprinter.http;

import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.conn.DefaultHttpResponseParser;
import org.area515.resinprinter.security.keystore.ByteSessionInputBuffer;
import org.junit.Assert;
import org.junit.Test;

public class TestByteSession {
	@Test
	public void testSession() throws Exception {
		String entity = "Entity data goes here";
		String githubResponse = "HTTP/1.1 200 OK\r\n"
				+ "Server: GitHub.com\r\n"
				+ "Date: Tue, 26 Jul 2016 00:59:10 GMT\r\n"
				+ "Content-Type: text/html; charset=utf-8\r\n"
				+ "Transfer-Encoding: chunked\r\n"
				+ "Status: 200 OK\r\n"
				+ "Cache-Control: no-cache\r\n"
				+ "Vary: X-PJAX\r\n"
				+ "X-UA-Compatible: IE=Edge,chrome=1\r\n"
				+ "X-Runtime: 0.238573\r\n"
				+ "Content-Security-Policy: default-src 'none'; base-uri 'self'; block-all-mixed-content; child-src render.githubusercontent.com; connect-src 'self' uploads.github.com status.github.com api.github.com www.google-analytics.com github-cloud.s3.amazonaws.com wss://live.github.com; font-src assets-cdn.github.com; form-action 'self' github.com gist.github.com; frame-ancestors 'none'; frame-src render.githubusercontent.com; img-src 'self' data: assets-cdn.github.com identicons.github.com www.google-analytics.com collector.githubapp.com *.gravatar.com *.wp.com *.githubusercontent.com; media-src 'none'; object-src assets-cdn.github.com; plugin-types application/x-shockwave-flash; script-src assets-cdn.github.com; style-src 'unsafe-inline' assets-cdn.github.com\r\n"
				+ "Strict-Transport-Security: max-age=31536000; includeSubdomains; preload\r\n"
				+ "Public-Key-Pins: max-age=5184000; pin-sha256=\"W=\"; pin-sha256=\"R=\"; pin-sha256=\"k=\"; pin-sha256=\"K=\"; pin-sha256=\"I=\"; pin-sha256=\"i=\"; pin-sha256=\"L=\"; includeSubDomains\r\n"
				+ "X-Content-Type-Options: nosniff\r\n"
				+ "X-Frame-Options: deny\r\n"
				+ "X-XSS-Protection: 1; mode=block\r\n"
				+ "Vary: Accept-Encoding\r\n"
				+ "X-Served-By: b\r\n"
				+ "Content-Encoding: gzip\r\n"
				+ "\r\n"
				+ entity;
		byte[] responseBytes = githubResponse.getBytes();
		byte[] entityBytes = entity.getBytes();
		ByteSessionInputBuffer buffer = new ByteSessionInputBuffer(responseBytes, 0, responseBytes.length);
		DefaultHttpResponseParser parser = new DefaultHttpResponseParser(buffer);
		HttpResponse response = parser.parse();
		response.setEntity(buffer.getRemainingEntity());
		byte[] data = new byte[entity.length()];
		response.getEntity().getContent().read(data);
		Assert.assertArrayEquals(entityBytes, data);
	}
}
