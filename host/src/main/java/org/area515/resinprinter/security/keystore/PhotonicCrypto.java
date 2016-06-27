package org.area515.resinprinter.security.keystore;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.InvalidNameException;

import org.area515.resinprinter.security.JettySecurityUtils;
import org.area515.resinprinter.server.HostProperties;

public class PhotonicCrypto {
	private static final int IV_LENGTH = 16;
	private static final int AES_KEY_SIZE = 16;
	
	//Local User
	private PrivateKeyEntry decryptor;
	private PrivateKeyEntry signer;
	private X509Certificate encryptor;
	private X509Certificate verifier;
	private UUID localUserId;
	
	//Remote User
	private PhotonicCrypto remoteCrypto;
	
	//ConversationState
	private Cipher conversationCipher;
	private SecretKeySpec symKey;
	private int currentOffset;
	private SecureRandom random;
	private boolean allowInsecureCommunication;
	
	public PhotonicCrypto(Entry decryptor, Entry signer, boolean allowInsecureCommunication) throws CertificateExpiredException, CertificateNotYetValidException, CertificateEncodingException, InvalidNameException {
		this.allowInsecureCommunication = allowInsecureCommunication;
		if (decryptor instanceof PrivateKeyEntry) {
			this.decryptor = (PrivateKeyEntry)decryptor;
			this.encryptor = (X509Certificate)((PrivateKeyEntry)decryptor).getCertificate();
		} else if (decryptor instanceof TrustedCertificateEntry) {
			this.encryptor = (X509Certificate)((TrustedCertificateEntry) decryptor).getTrustedCertificate();
		}
		encryptor.checkValidity();

		if (signer instanceof PrivateKeyEntry) {
			this.signer = (PrivateKeyEntry)signer;
			this.verifier = (X509Certificate)((PrivateKeyEntry)signer).getCertificate();
		} else if (signer instanceof TrustedCertificateEntry) {
			this.verifier = (X509Certificate)((TrustedCertificateEntry) signer).getTrustedCertificate();
		}
		verifier.checkValidity();
		validateUIDOfVerifier();
	}
	
	public X509Certificate[] getCertificates() {
		return new X509Certificate[]{verifier, encryptor};
	}

	private void validateUIDOfVerifier() throws InvalidNameException, CertificateEncodingException {
		String[] userIdAndName = JettySecurityUtils.getUserIdAndName(verifier.getSubjectDN().getName());
		if (!UUID.fromString(userIdAndName[0]).equals(UUID.nameUUIDFromBytes(verifier.getPublicKey().getEncoded()))) {
			throw new InvalidNameException("Uid of subject on certifiate didn't match expected uuid of public key.");
		}
		localUserId = UUID.fromString(userIdAndName[0]);
	}
	
	public UUID getLocalUserId() {
		return localUserId;
	}
	
	public void setRemoteCrypto(PhotonicCrypto remoteCrypto) {
		this.remoteCrypto = remoteCrypto;
	}
	
	public boolean isAsymetricEncryption(String algorithm) {
		return algorithm != null && algorithm.equalsIgnoreCase("rsa");
	}
	
	public byte[] getData(Message message) throws NoSuchAlgorithmException, CertificateExpiredException, CertificateNotYetValidException, InvalidKeyException, SignatureException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
		//If there is a signature, check it!
		byte[] signature = message.getSignature();
		if (signature != null) {
			remoteCrypto.verifier.checkValidity(new Date());
			Signature sig = Signature.getInstance(remoteCrypto.verifier.getSigAlgName());
			sig.initVerify(remoteCrypto.verifier);
			sig.update(message.getFrom().toString().getBytes());
			sig.update(message.getTo().toString().getBytes());
			if (message.getEncryptionAlgorithm() != null) {
				sig.update(message.getEncryptionAlgorithm().getBytes());
			}
			sig.update(message.getData());
			if (!sig.verify(message.getSignature())) {
				throw new SignatureException("Signature of sender couldn't be verified");
			}
		} else if (!allowInsecureCommunication) {
			if (isAsymetricEncryption(message.getEncryptionAlgorithm())) {
				throw new InvalidAlgorithmParameterException("Only signed messages can be used with this Crypto.");
			}
		}
		
		//No encryption was specified
		if (message.getEncryptionAlgorithm() == null) {
			if (!allowInsecureCommunication) {
				throw new InvalidAlgorithmParameterException("Only encrypted messages can be used with this Crypto.");
			}
			
			return message.getData();
		}
		
		//This must be a key exchange message
		if (isAsymetricEncryption(message.getEncryptionAlgorithm())) {
			Cipher decrypt=Cipher.getInstance(message.getEncryptionAlgorithm());
			encryptor.checkValidity(new Date());
			decrypt.init(Cipher.DECRYPT_MODE, decryptor.getPrivateKey());
			String ivAndKey = new String(decrypt.doFinal(message.getData()));
			int ivSep = ivAndKey.indexOf("\n");
			if (ivSep > 0) {
				random = new SecureRandom(Base64.getDecoder().decode(ivAndKey.substring(0, ivSep)));
				currentOffset = 0;
			}
			if (ivSep < ivAndKey.length()) {
		        symKey = new SecretKeySpec(Base64.getDecoder().decode(ivAndKey.substring(ivSep + 1)), "AES");
			}
	        
	        conversationCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
	        return null;
		}
		
		//It must be a data message
		if (random == null) {
			throw new InvalidKeyException("You need to perform a key exchange with this crypto before you use it");
		}
		IvParameterSpec iv = null;
		for (; currentOffset < message.getIvOffset(); currentOffset++) {
			byte[] ivBytes = new byte[IV_LENGTH];
			random.nextBytes(ivBytes);
			iv = new IvParameterSpec(ivBytes);
		}
		
		if (iv == null) {
			throw new InvalidAlgorithmParameterException("Old iv offset specified. Replay attack?");
		}
		
    	conversationCipher.init(Cipher.DECRYPT_MODE, symKey, iv);
    	return conversationCipher.doFinal(message.getData());
	}
	
	public Message buildKeyExchange() throws InvalidNameException, CertificateExpiredException, CertificateNotYetValidException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		if (signer == null) {
			throw new SignatureException("This crypto is not capable of signing messages.");
		}
		Message keyMessage = new Message();
		String[] userIdAndName = JettySecurityUtils.getUserIdAndName(remoteCrypto.verifier.getSubjectDN().getName());
		keyMessage.setTo(UUID.fromString(userIdAndName[0]));
		userIdAndName = JettySecurityUtils.getUserIdAndName(((X509Certificate)signer.getCertificate()).getSubjectDN().getName());
		keyMessage.setFrom(UUID.fromString(userIdAndName[0]));
		keyMessage.setEncryptionAlgorithm("RSA");
		
		remoteCrypto.encryptor.checkValidity(new Date());
		SecureRandom throwAwayRandom = new SecureRandom();
		byte[] ivBytes = new byte[IV_LENGTH];
		throwAwayRandom.nextBytes(ivBytes);
		byte[] keyBytes = new byte[AES_KEY_SIZE];
		throwAwayRandom.nextBytes(keyBytes);
		Cipher encrypt=Cipher.getInstance("RSA");
		encrypt.init(Cipher.ENCRYPT_MODE, remoteCrypto.encryptor.getPublicKey());
		encrypt.update(Base64.getEncoder().encode(ivBytes));
		encrypt.update(new byte[]{10});
		byte[] ivAndKey = encrypt.doFinal(Base64.getEncoder().encode(keyBytes));
		keyMessage.setData(ivAndKey);
    
		verifier.checkValidity(new Date());
		Signature sig = Signature.getInstance(verifier.getSigAlgName());
		sig.initSign(signer.getPrivateKey());
		sig.update(keyMessage.getFrom().toString().getBytes());
		sig.update(keyMessage.getTo().toString().getBytes());
		sig.update(keyMessage.getEncryptionAlgorithm().getBytes());
		sig.update(keyMessage.getData());
		keyMessage.setSignature(sig.sign());
		
		currentOffset = 0;
		this.random = new SecureRandom(ivBytes);
	    symKey = new SecretKeySpec(keyBytes, "AES");
    	conversationCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		return keyMessage;
	}
	
	public Message buildEncryptedMessage(byte[] data) throws InvalidNameException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		if (random == null) {
			throw new InvalidKeyException("You need to perform a key exchange with this crypto before you use it");
		}
		
		Message keyMessage = new Message();
		String[] userIdAndName = JettySecurityUtils.getUserIdAndName(remoteCrypto.verifier.getSubjectDN().getName());
		keyMessage.setTo(UUID.fromString(userIdAndName[0]));
		userIdAndName = JettySecurityUtils.getUserIdAndName(((X509Certificate)signer.getCertificate()).getSubjectDN().getName());
		keyMessage.setFrom(UUID.fromString(userIdAndName[0]));

		currentOffset++;
		byte[] ivBytes = new byte[IV_LENGTH];
		random.nextBytes(ivBytes);
		IvParameterSpec iv = new IvParameterSpec(ivBytes);
		keyMessage.setEncryptionAlgorithm("AES/CBC/PKCS5PADDING");
		keyMessage.setIvOffset(currentOffset);
		
    	conversationCipher.init(Cipher.ENCRYPT_MODE, symKey, iv);
    	keyMessage.setData(data);
		keyMessage.setData(conversationCipher.doFinal(data));
    	return keyMessage;
	}
	
	public Message buildMessage(byte[] data) throws InvalidNameException, InvalidAlgorithmParameterException {
		if (!allowInsecureCommunication) {
			throw new InvalidAlgorithmParameterException("Only encrypted messages can be used with this Crypto.");
		}
		
		Message keyMessage = new Message();
		String[] userIdAndName = JettySecurityUtils.getUserIdAndName(remoteCrypto.verifier.getSubjectDN().getName());
		keyMessage.setTo(UUID.fromString(userIdAndName[0]));
		userIdAndName = JettySecurityUtils.getUserIdAndName(((X509Certificate)signer.getCertificate()).getSubjectDN().getName());
		keyMessage.setFrom(UUID.fromString(userIdAndName[0]));
		keyMessage.setData(data);
		return keyMessage;
	}
}