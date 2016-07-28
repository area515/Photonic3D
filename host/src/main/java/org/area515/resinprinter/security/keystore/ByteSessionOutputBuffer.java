package org.area515.resinprinter.security.keystore;

import java.io.IOException;

import org.apache.http.io.HttpTransportMetrics;
import org.apache.http.io.SessionOutputBuffer;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.CharArrayBuffer;

public class ByteSessionOutputBuffer implements SessionOutputBuffer {
	private ByteArrayBuffer byteBuffer = new ByteArrayBuffer(64000);
	
	public byte[] getByteArray() {
		return byteBuffer.buffer();
	}
	
	public int getLength() {
		return byteBuffer.length();
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		throw new IllegalArgumentException("Not implemented");
	}

	@Override
	public void write(byte[] b) throws IOException {
		throw new IllegalArgumentException("Not implemented");
	}

	@Override
	public void write(int b) throws IOException {
		throw new IllegalArgumentException("Not implemented");
	}

	@Override
	public void writeLine(String s) throws IOException {
		throw new IllegalArgumentException("Not implemented");
	}

	@Override
	public void writeLine(CharArrayBuffer buffer) throws IOException {
		this.byteBuffer.append(buffer, 0, buffer.length());
	}

	@Override
	public void flush() throws IOException {
		throw new IllegalArgumentException("Not implemented");
	}

	@Override
	public HttpTransportMetrics getMetrics() {
		return null;
	}
}
