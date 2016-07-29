package org.area515.resinprinter.security.keystore;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.io.HttpTransportMetrics;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.util.CharArrayBuffer;

public class ByteSessionInputBuffer implements SessionInputBuffer {
	private byte[] content;
	private int offset;
	private int lastBytePlus1;
	
	public ByteSessionInputBuffer(byte[] content, int offset, int length) {
		this.content = content;
		this.offset = offset;
		this.lastBytePlus1 = offset + length;
	}
	
	public HttpEntity getRemainingEntity() {
		return new ByteArrayEntity(content, offset, lastBytePlus1 - offset);
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		throw new IllegalArgumentException("Not implemented");
	}

	@Override
	public int read(byte[] b) throws IOException {
		throw new IllegalArgumentException("Not implemented");
	}

	@Override
	public int read() throws IOException {
		throw new IllegalArgumentException("Not implemented");
	}

	@Override
	public int readLine(CharArrayBuffer buffer) throws IOException {
		int start = offset;
		int bytesRead = 0;
		for (int current = offset; offset < lastBytePlus1 && content[offset] != 10; offset++) {
		}
		if (content[offset-1] == 13) {
			bytesRead = offset - start - 1;
		} else {
			bytesRead = offset - start;
		}
		
		buffer.append(content, start, bytesRead);
		if (offset < lastBytePlus1 && content[offset] == 10) {
			offset++;
		}
		return bytesRead;
	}

	@Override
	public String readLine() throws IOException {
		throw new IllegalArgumentException("Not implemented");
	}

	@Override
	public boolean isDataAvailable(int timeout) throws IOException {
		return offset < lastBytePlus1;
	}

	@Override
	public HttpTransportMetrics getMetrics() {
		return null;
	}
}
