package org.area515.resinprinter.util.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.UUID;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public class KeystoreUtilities {
	public static void saveKeystore(File keyFile, KeyStore keyStore, String keystorePassword) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		FileOutputStream outputStream = new FileOutputStream(keyFile);
		try {
			keyStore.store(outputStream, keystorePassword.toCharArray());
		} finally {
			try {
				outputStream.close();
			} catch (IOException e) {}
		}
	}

	private static X509Certificate generateX509Certificate(LdapName dn, KeyPair pair, Date to, String algorithm) throws GeneralSecurityException, IOException {
		PrivateKey privkey = pair.getPrivate();
		X509CertInfo info = new X509CertInfo();
		Date from = new Date();
		CertificateValidity interval = new CertificateValidity(from, to);
		BigInteger sn = new BigInteger(64, SecureRandom.getInstance("SHA1PRNG"));
		X500Name owner = new X500Name(dn.toString());

		info.set(X509CertInfo.VALIDITY, interval);
		info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
		try {
			info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
			info.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner));
		} catch (CertificateException e) {
			info.set(X509CertInfo.SUBJECT, owner);
			info.set(X509CertInfo.ISSUER, owner);
		}
		info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
		info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
		//info.set(X509CertInfo.DN_NAME, dn);

		//TODO: Technically I should be specifying key usage requirements to prevent key confusion attacks
		/*CertificateExtensions ext = new CertificateExtensions();
        ext.set(sun.security.x509.SubjectKeyIdentifierExtension.NAME,
                new SubjectKeyIdentifierExtension(
                new KeyIdentifier(pair.getPublic()).getIdentifier()));
        info.set(X509CertInfo.EXTENSIONS, ext);*/
		//1.3.6.1.5.5.7.3.2 client auth
		//1.3.6.1.5.5.7.3.1 server auth
		//1.2.840.113549.1.1.X RSA Encryption
		//1.2.840.113549.2.X Signing Algorithms
		//AlgorithmId algo = new AlgorithmId(AlgorithmId.DH_oid);
		AlgorithmId algo = new AlgorithmId(AlgorithmId.sha256WithRSAEncryption_oid);
		info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

		// Sign the cert to identify the algorithm that's used.
		X509CertImpl cert = new X509CertImpl(info);
		cert.sign(privkey, algorithm);
		
		// Update the algorithm, and resign.
		algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
		info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
		cert = new X509CertImpl(info);
		cert.sign(privkey, algorithm);
		return cert;
	}
	
	public static PrivateKeyEntry generateCertAndKeyPair(LdapName fullyQualifiedDN, Date endDate) throws GeneralSecurityException, IOException, InvalidNameException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		keyGen.initialize(2048, random);
		
		KeyPair keyPair = keyGen.generateKeyPair();
		String[] userIdAndName = LdapUtils.getUserIdAndName(fullyQualifiedDN.toString());
		if (userIdAndName[0] != null) {
			throw new InvalidNameException("The uid component of the ldapname cannot be set in the fullQualifiedDN");
		}
		
		fullyQualifiedDN.add(new Rdn("uid", UUID.nameUUIDFromBytes(keyPair.getPublic().getEncoded()).toString()));
		X509Certificate cert = generateX509Certificate(fullyQualifiedDN, keyPair, endDate, "SHA256withRSA");
		return new PrivateKeyEntry(keyPair.getPrivate(), new Certificate[]{cert});
	}

	public static PhotonicCrypto generateRSAKeypairsForUserAndPossiblyKeystore(LdapName fullyQualifiedUserDN, Date endDate, File keyFile, String keypairPassword, String keystorePassword, boolean allowInsecureCommunications) throws IOException, GeneralSecurityException, InvalidNameException {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		if (keyFile.exists()) {
			FileInputStream outputStream = new FileInputStream(keyFile);
			keyStore.load(outputStream, keystorePassword.toCharArray());
		} else {
			keyStore.load(null, null);
		}
		
		LdapName newUserDN = new LdapName(fullyQualifiedUserDN.getRdns());
		PrivateKeyEntry signatureData = generateCertAndKeyPair(newUserDN, endDate);
		String[] userIdAndName = LdapUtils.getUserIdAndName(newUserDN.toString());
		keyStore.setKeyEntry(userIdAndName[0] + "S", signatureData.getPrivateKey(), keypairPassword.toCharArray(), new Certificate[]{signatureData.getCertificate()});
		PrivateKeyEntry encryptionData = generateCertAndKeyPair(fullyQualifiedUserDN, endDate);
		keyStore.setKeyEntry(userIdAndName[0] + "E", encryptionData.getPrivateKey(), keypairPassword.toCharArray(), new Certificate[]{encryptionData.getCertificate()});
		
		saveKeystore(keyFile, keyStore, keystorePassword);
		
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
		
		saveKeystore(keyFile, keyStore, keystorePassword);
		
		return new PhotonicCrypto(keyStore.getEntry(signerName, null), keyStore.getEntry(encryptorName, null), allowInsecureCommunications);
	}
}
