package org.area515.resinprinter.util.security;

import java.util.UUID;

/**
 * Uses Encrypt-then-MAC theory of crypto.
 * 
 * Asym
 * signature = signatureAlgorithm(from + to + encryptionAlgorithm + encryptionAlgorithm(data(base64(iv) + '\n' + base64(symkey)))
 * 
 * @author wgilster
 */
public class Message {
	private UUID from;
	private UUID to;
	private byte[] signature;
	private String encryptionAlgorithm;
	private Integer ivOffset;
	private byte[] data;
	
	public Integer getIvOffset() {
		return ivOffset;
	}
	public void setIvOffset(Integer ivOffset) {
		this.ivOffset = ivOffset;
	}
	public UUID getFrom() {
		return from;
	}
	public void setFrom(UUID from) {
		this.from = from;
	}
	public UUID getTo() {
		return to;
	}
	public void setTo(UUID to) {
		this.to = to;
	}
	public byte[] getSignature() {
		return signature;
	}
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}
	public String getEncryptionAlgorithm() {
		return encryptionAlgorithm;
	}
	public void setEncryptionAlgorithm(String encryptionAlgorithm) {
		this.encryptionAlgorithm = encryptionAlgorithm;
	}
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
}