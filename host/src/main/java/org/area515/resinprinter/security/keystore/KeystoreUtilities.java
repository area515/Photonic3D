package org.area515.resinprinter.security.keystore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.UUID;

import javax.naming.ldap.LdapName;

import org.area515.resinprinter.security.JettySecurityUtils;
import org.area515.resinprinter.security.PhotonicUser;

public class KeystoreUtilities {
	public static PhotonicCrypto generateRSAKeypairsForUserAndPossiblyKeystore(LdapName fullyQualifiedUserDN, UUID userId, Date endDate, File keyFile, String keypairPassword, String keystorePassword) throws IOException, GeneralSecurityException {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		if (keyFile.exists()) {
			FileInputStream outputStream = new FileInputStream(keyFile);
			keyStore.load(outputStream, keystorePassword.toCharArray());
		} else {
			keyStore.load(null, null);
		}
		
		PrivateKeyEntry encryptionData = JettySecurityUtils.generateCertAndKeyPair(fullyQualifiedUserDN, endDate);
		keyStore.setKeyEntry(userId + "E", encryptionData.getPrivateKey(), keypairPassword.toCharArray(), new Certificate[]{encryptionData.getCertificate()});
		PrivateKeyEntry signatureData = JettySecurityUtils.generateCertAndKeyPair(fullyQualifiedUserDN, endDate);
		keyStore.setKeyEntry(userId + "S", encryptionData.getPrivateKey(), keypairPassword.toCharArray(), new Certificate[]{signatureData.getCertificate()});
		
		JettySecurityUtils.saveKeystore(keyFile, keyStore, keystorePassword);
		
		return new PhotonicCrypto(encryptionData, signatureData);
	}

	public static PhotonicCrypto getPhotonicCrypto(PhotonicUser user, File keyFile, String keypairPassword, String keystorePassword) throws IOException, GeneralSecurityException {
		FileInputStream outputStream = new FileInputStream(keyFile);
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(outputStream, keystorePassword.toCharArray());
		
		return new PhotonicCrypto(
				keyStore.getEntry(user.getUserId() + "E", keypairPassword != null?new PasswordProtection(keypairPassword.toCharArray()):null), 
				keyStore.getEntry(user.getUserId() + "S", keypairPassword != null?new PasswordProtection(keypairPassword.toCharArray()):null));
	}
}
