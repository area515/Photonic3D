package org.area515.resinprinter.security.keystore;

import java.security.InvalidKeyException;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class PhotonicCrypto {
	private PrivateKeyEntry decryptor;
	private PrivateKeyEntry signer;
	private X509Certificate encryptor;
	private X509Certificate verifier;
	
	private PhotonicCrypto remoteCrypto;
	private Cipher conversationCipher;
	
	public PhotonicCrypto(Entry decryptor, Entry signer) throws CertificateExpiredException, CertificateNotYetValidException {
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
	}
	
	public void setRemoteCrypto(PhotonicCrypto remoteCrypto) {
		this.remoteCrypto = remoteCrypto;
	}
	
	public boolean isAsymetricEncryption(String algorithm) {
		return algorithm.equalsIgnoreCase("rsa");
	}
	
	/*public byte[] getData(Message message) throws NoSuchAlgorithmException, CertificateExpiredException, CertificateNotYetValidException, InvalidKeyException, SignatureException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		byte[] signature = message.getSignature();
		if (signature != null) {
			verifier.checkValidity(new Date());
			Signature sig = Signature.getInstance(verifier.getSigAlgName());
			sig.initVerify(remoteCrypto.verifier);
			sig.update(message.getFrom().toString().getBytes());
			sig.update(message.getTo().toString().getBytes());
			if (message.getEncryptionAlgorithm() != null) {
				sig.update(message.getEncryptionAlgorithm().getBytes());
			}
			sig.update(message.getData());
			if (sig.verify(message.getSignature())) {
				throw new SignatureException("Signature of sender couldn't be verified");
			}
		}
		
		if (message.getEncryptionAlgorithm() == null) {
			return message.getData();
		}
		
		if (isAsymetricEncryption(message.getEncryptionAlgorithm())) {
			Cipher decrypt=Cipher.getInstance(message.getEncryptionAlgorithm());
			decrypt.init(Cipher.DECRYPT_MODE, decryptor.getPrivateKey());
			byte[] data = decrypt.doFinal(message.getData());
			//TODO: figure out how this works...
	        IvParameterSpec iv = new IvParameterSpec(initVector);
	        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
	        conversationCipher = Cipher.getInstance(message.getEncryptionAlgorithm());//TODO: When creating... "AES/CBC/PKCS5PADDING"
	        return null;
		}
		
        conversationCipher.init(Cipher.DECRYPT_MODE, key, iv);
        return conversationCipher.doFinal(message.getData());
	}*/
}