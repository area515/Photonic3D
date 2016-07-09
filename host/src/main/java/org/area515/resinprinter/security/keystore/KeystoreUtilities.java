package org.area515.resinprinter.security.keystore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.area515.resinprinter.security.JettySecurityUtils;
import org.area515.resinprinter.security.PhotonicUser;

public class KeystoreUtilities {
	public static PhotonicCrypto generateRSAKeypairsForUserAndPossiblyKeystore(LdapName fullyQualifiedUserDN, Date endDate, File keyFile, String keypairPassword, String keystorePassword, boolean allowInsecureCommunications) throws IOException, GeneralSecurityException, InvalidNameException {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		if (keyFile.exists()) {
			FileInputStream outputStream = new FileInputStream(keyFile);
			keyStore.load(outputStream, keystorePassword.toCharArray());
		} else {
			keyStore.load(null, null);
		}
		
		LdapName newUserDN = new LdapName(fullyQualifiedUserDN.getRdns());
		PrivateKeyEntry signatureData = JettySecurityUtils.generateCertAndKeyPair(newUserDN, endDate);
		String[] userIdAndName = JettySecurityUtils.getUserIdAndName(newUserDN.toString());
		keyStore.setKeyEntry(userIdAndName[0] + "S", signatureData.getPrivateKey(), keypairPassword.toCharArray(), new Certificate[]{signatureData.getCertificate()});
		PrivateKeyEntry encryptionData = JettySecurityUtils.generateCertAndKeyPair(fullyQualifiedUserDN, endDate);
		keyStore.setKeyEntry(userIdAndName[0] + "E", encryptionData.getPrivateKey(), keypairPassword.toCharArray(), new Certificate[]{encryptionData.getCertificate()});
		
		JettySecurityUtils.saveKeystore(keyFile, keyStore, keystorePassword);
		
		return new PhotonicCrypto(signatureData, encryptionData, allowInsecureCommunications);
	}

	public static PhotonicCrypto getPhotonicCrypto(PhotonicUser user, File keyFile, String keypairPassword, String keystorePassword, boolean allowInsecureCommunications) throws IOException, GeneralSecurityException, InvalidNameException {
		FileInputStream outputStream = new FileInputStream(keyFile);
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(outputStream, keystorePassword.toCharArray());
		
		return new PhotonicCrypto(
				keyStore.getEntry(user.getUserId() + "S", keypairPassword != null?new PasswordProtection(keypairPassword.toCharArray()):null), 
				keyStore.getEntry(user.getUserId() + "E", keypairPassword != null?new PasswordProtection(keypairPassword.toCharArray()):null), 
				allowInsecureCommunications);
	}
	
	public static PhotonicCrypto trustCertificate(PhotonicUser user, File keyFile, String keystorePassword, X509Certificate signingCert, X509Certificate encryptionCert, boolean allowInsecureCommunications) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InvalidNameException, UnrecoverableEntryException {
		String signerName = user.getUserId() + "S";
		String encryptorName = user.getUserId() + "E";
		FileInputStream outputStream = new FileInputStream(keyFile);
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(outputStream, keystorePassword.toCharArray());

		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String currentName = aliases.nextElement();
			if (currentName.equalsIgnoreCase(signerName) &&
				currentName.equalsIgnoreCase(encryptorName)) {
				throw new InvalidNameException("UserId:" + user.getUserId() + " already exists in keystore");
			}
		}
		
		keyStore.setCertificateEntry(signerName, signingCert);
		keyStore.setCertificateEntry(encryptorName, encryptionCert);
		
		JettySecurityUtils.saveKeystore(keyFile, keyStore, keystorePassword);
		
		return new PhotonicCrypto(keyStore.getEntry(signerName, null), keyStore.getEntry(encryptorName, null), allowInsecureCommunications);
	}
}
