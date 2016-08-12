package org.area515.resinprinter.util.security;

import java.nio.ByteBuffer;
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
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.InvalidNameException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sun.security.x509.X509CertImpl;

public class PhotonicCrypto {
    private static final Logger logger = LogManager.getLogger();

    private static final int IV_LENGTH = 16;
	private static final int AES_KEY_SIZE = 16;
	private static final String PUBLIC_CERT_REQUEST = "X509CertsBase64NewLine";
	private static final String BAD_UUID = "Uid of subject on certifiate didn't match expected uuid of public key.";
	private static final PhotonicSecurityProvider SHA1PRNG_PROVIDER = new PhotonicSecurityProvider();
	public static final String FEATURE_NAME = "Make friends through X509";
	
	//Local User
	private PrivateKeyEntry decryptor;
	private PrivateKeyEntry signer;
	private X509Certificate encryptor;
	private X509Certificate verifier;
	private UUID localUserId;
	
	//Remote User
	private PhotonicCrypto remoteCrypto;
	
	//ConversationState
	private ThreadLocal<Cipher> conversationCypher = new ThreadLocal<Cipher>();
	private Boolean lockSync = new Boolean(true);//TODO: synchronize around key changes if we are going to stream video. TLS probably never even does a key change...
	private SecretKeySpec symKey;
	private boolean allowInsecureCommunication;
	private boolean isKeyCreator;
	private InitializationVectorControl keyCreatorIV;
	private InitializationVectorControl keyReceiverIV;
	
	public static class InitializationVectorControl {
		private Map<Integer, byte[]> ivs = new HashMap<>();
		private SecureRandom random;
		private int offset;
		
		public InitializationVectorControl(byte[] seed) throws NoSuchAlgorithmException {
			this.random = SecureRandom.getInstance("SHA1PRNG", SHA1PRNG_PROVIDER);
			this.random.setSeed(seed);
			this.offset = 0;
		}
		
		public byte[] getNextAvailableIV(int findIv) {
			byte[] ivBytes = null;
			for (; offset < findIv; offset++) {
				ivBytes = new byte[IV_LENGTH];
				random.nextBytes(ivBytes);
				ivs.put(offset + 1, ivBytes);
			}

			return ivs.remove(findIv);
		}
		
		public int getNextAvailableIV(byte[] ivBytes) {
			offset++;
			random.nextBytes(ivBytes);
			return offset;
		}
		
		public String toString() {
			return "offset:" + offset + " ivs:" + ivs.keySet();
		}
	}
	
	public PhotonicCrypto(Entry signer, Entry decryptor, boolean allowInsecureCommunication) throws CertificateExpiredException, CertificateNotYetValidException, CertificateEncodingException, InvalidNameException {
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
	
	//This constructor duplicates the local crypto information so it can be used to communicate with another remote.
	public PhotonicCrypto(PhotonicCrypto crypto) throws CertificateExpiredException, CertificateNotYetValidException, CertificateEncodingException, InvalidNameException {
		this.allowInsecureCommunication = crypto.allowInsecureCommunication;
		this.decryptor = crypto.decryptor;
		this.encryptor = crypto.encryptor;
		encryptor.checkValidity();
		this.signer = crypto.signer;
		this.verifier = crypto.verifier;
		verifier.checkValidity();
		validateUIDOfVerifier();
	}
	
	public X509Certificate[] getCertificates() {
		return new X509Certificate[]{verifier, encryptor};
	}

	private void validateUIDOfVerifier() throws InvalidNameException, CertificateEncodingException {
		String[] userIdAndName = LdapUtils.getUserIdAndName(verifier.getSubjectDN().getName());
		if (!UUID.fromString(userIdAndName[0]).equals(UUID.nameUUIDFromBytes(verifier.getPublicKey().getEncoded()))) {
			throw new InvalidNameException(BAD_UUID);
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
	
	public Message buildCertificateTrustMessage(UUID toUser) throws CertificateEncodingException {
		Message message = new Message();
		message.setFrom(getLocalUserId());
		message.setTo(toUser);
		message.setEncryptionAlgorithm(PUBLIC_CERT_REQUEST);
		message.setData((Base64.getEncoder().encodeToString(verifier.getEncoded()) + "\n" + Base64.getEncoder().encodeToString(encryptor.getEncoded())).getBytes());
		return message;
	}
	
	public static Friend checkCertificateTrustExchange(Message message) throws CertificateException, InvalidNameException {
		if (!message.getEncryptionAlgorithm().equalsIgnoreCase(PUBLIC_CERT_REQUEST)) {
			return null;
		}
		
		String signCertAndEncryptCert = new String(message.getData());
		int newLineSep = signCertAndEncryptCert.indexOf("\n");
		String signBase64 = signCertAndEncryptCert.substring(0, newLineSep);
		String encryptBase64 = signCertAndEncryptCert.substring(newLineSep + 1);
		X509CertImpl sign = new X509CertImpl(Base64.getDecoder().decode(signBase64));
		X509CertImpl encrypt = new X509CertImpl(Base64.getDecoder().decode(encryptBase64));
		
    	String[] names = LdapUtils.getUserIdAndName(sign.getSubjectDN().getName());
    	if (!names[0].equals(message.getFrom().toString())) {
    		throw new InvalidNameException("User:" + names[1] + " sent a friend request with userId:" + names[0] + " on cert which doesn't match userId:" + message.getFrom() + " from which it came");
    	}
    	
    	if (!UUID.fromString(names[0]).equals(UUID.nameUUIDFromBytes(sign.getPublicKey().getEncoded()))) {
    		throw new InvalidNameException(BAD_UUID);
    	}
    	
    	Friend friend = new Friend();
    	friend.setUser(new PhotonicUser(names[1], null, UUID.fromString(names[0]), null, new String[]{PhotonicUser.LOGIN}, true));
    	friend.setFriendshipFeature(PhotonicCrypto.FEATURE_NAME);
    	friend.setTrustData(new String[]{signBase64, encryptBase64});
		return friend;
	}
	
	private Cipher getConversationCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
		Cipher cipher = conversationCypher.get();
		if (cipher == null) {
			cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			conversationCypher.set(cipher);
		}
		
		return cipher;
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
			int newLineSep = ivAndKey.indexOf("\n");
			synchronized (lockSync) {
				if (newLineSep > 0) {
					byte[] keyCreatorSeed = Base64.getDecoder().decode(ivAndKey.substring(0, newLineSep));
					byte[] keyReceiverSeed = Base64.getDecoder().decode(ivAndKey.substring(0, newLineSep));
					keyReceiverSeed[0]++;
					keyCreatorIV = new InitializationVectorControl(keyCreatorSeed);
					keyReceiverIV = new InitializationVectorControl(keyReceiverSeed);
				}
				if (newLineSep < ivAndKey.length()) {
			        symKey = new SecretKeySpec(Base64.getDecoder().decode(ivAndKey.substring(newLineSep + 1)), "AES");
				}
				isKeyCreator = false;
			}
	        
			getConversationCipher();
	        return null;
		}
		
		//TODO: At this point we treat the message.getEncryptionAlgorithm() as AES encryption. Should I check it?
		
		//It must be a data message
		if (keyCreatorIV == null || keyReceiverIV == null) {
			throw new InvalidKeyException("You need to perform a key exchange with this crypto before you use it");
		}

		byte[] ivBytes = null;
		synchronized (lockSync) {
			InitializationVectorControl properControl = isKeyCreator?keyReceiverIV:keyCreatorIV;
			ivBytes = properControl.getNextAvailableIV(message.getIvOffset());
			if (ivBytes == null) {
				throw new InvalidAlgorithmParameterException("Old iv offset specified. Replay attack?");
			}
		}
		
		IvParameterSpec iv = new IvParameterSpec(ivBytes);
		Cipher cipher = getConversationCipher();
		cipher.init(Cipher.DECRYPT_MODE, symKey, iv);
    	return cipher.doFinal(message.getData());
	}
	
	public boolean isDueForKeyExchange() {
		return symKey == null || keyCreatorIV == null || keyReceiverIV == null || keyCreatorIV.offset > 100 || keyReceiverIV.offset > 100;
	}
	
	public Message buildKeyExchange() throws InvalidNameException, CertificateExpiredException, CertificateNotYetValidException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		if (signer == null) {
			throw new SignatureException("This crypto is not capable of signing messages.");
		}
		Message keyMessage = new Message();
		String[] userIdAndName = LdapUtils.getUserIdAndName(remoteCrypto.verifier.getSubjectDN().getName());
		keyMessage.setTo(UUID.fromString(userIdAndName[0]));
		userIdAndName = LdapUtils.getUserIdAndName(((X509Certificate)signer.getCertificate()).getSubjectDN().getName());
		keyMessage.setFrom(UUID.fromString(userIdAndName[0]));
		keyMessage.setEncryptionAlgorithm("RSA");
		
		remoteCrypto.encryptor.checkValidity(new Date());
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(AES_KEY_SIZE * 8);
		byte[] keyCreatorSeed = keyGen.generateKey().getEncoded();
		byte[] keyBytes = keyGen.generateKey().getEncoded();
		Cipher encrypt=Cipher.getInstance("RSA");
		encrypt.init(Cipher.ENCRYPT_MODE, remoteCrypto.encryptor.getPublicKey());
		encrypt.update(Base64.getEncoder().encode(keyCreatorSeed));
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
		
		synchronized (lockSync) {
			byte[] keyReceiverSeed = new byte[keyCreatorSeed.length];
			System.arraycopy(keyCreatorSeed, 0, keyReceiverSeed, 0, keyCreatorSeed.length);
			keyReceiverSeed[0]++;

			keyCreatorIV = new InitializationVectorControl(keyCreatorSeed);
			keyReceiverIV = new InitializationVectorControl(keyReceiverSeed);

			isKeyCreator = true;
			symKey = new SecretKeySpec(keyBytes, "AES");
		}

    	getConversationCipher();
		return keyMessage;
	}
	
	public Message buildEncryptedMessage(ByteBuffer buffer) throws InvalidNameException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException {
		if (keyCreatorIV == null || keyReceiverIV == null) {
			throw new InvalidKeyException("You need to perform a key exchange with this crypto before you use it");
		}
		
		Message keyMessage = new Message();
		String[] userIdAndName = LdapUtils.getUserIdAndName(remoteCrypto.verifier.getSubjectDN().getName());
		keyMessage.setTo(UUID.fromString(userIdAndName[0]));
		userIdAndName = LdapUtils.getUserIdAndName(((X509Certificate)signer.getCertificate()).getSubjectDN().getName());
		keyMessage.setFrom(UUID.fromString(userIdAndName[0]));

		byte[] ivBytes = new byte[IV_LENGTH];
		synchronized (lockSync) {
			InitializationVectorControl properControl = isKeyCreator?keyCreatorIV:keyReceiverIV;
			int currentOffset = properControl.getNextAvailableIV(ivBytes);
			keyMessage.setIvOffset(currentOffset);
		}
		
		IvParameterSpec iv = new IvParameterSpec(ivBytes);
		keyMessage.setEncryptionAlgorithm("AES/CBC/PKCS5PADDING");
		
		Cipher cipher = getConversationCipher();
		cipher.init(Cipher.ENCRYPT_MODE, symKey, iv);
    	keyMessage.setData(cipher.doFinal(buffer.array()));
    	return keyMessage;
	}
	
	public Message buildMessage(byte[] data) throws InvalidNameException, InvalidAlgorithmParameterException {
		if (!allowInsecureCommunication) {
			throw new InvalidAlgorithmParameterException("Only encrypted messages can be used with this Crypto.");
		}
		
		Message keyMessage = new Message();
		String[] userIdAndName = LdapUtils.getUserIdAndName(remoteCrypto.verifier.getSubjectDN().getName());
		keyMessage.setTo(UUID.fromString(userIdAndName[0]));
		userIdAndName = LdapUtils.getUserIdAndName(((X509Certificate)signer.getCertificate()).getSubjectDN().getName());
		keyMessage.setFrom(UUID.fromString(userIdAndName[0]));
		keyMessage.setData(data);
		return keyMessage;
	}
}