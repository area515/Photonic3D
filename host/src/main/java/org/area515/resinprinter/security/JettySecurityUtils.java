package org.area515.resinprinter.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.server.HostInformation;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.util.security.KeystoreUtilities;
import org.area515.resinprinter.util.security.LdapUtils;
import org.area515.resinprinter.util.security.PhotonicUser;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

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

public class JettySecurityUtils {
    private static final Logger logger = LogManager.getLogger();

	
	public static LdapName buildFullyQualifiedDN(String cn, String dc) throws InvalidNameException {
		HostInformation info = HostProperties.Instance().loadHostInformation();
		List<Rdn> rdn = new ArrayList<Rdn>();
		rdn.add(new Rdn("ou", info.getManufacturer()));
		rdn.add(new Rdn("ou", info.getDeviceName()));
		if (cn != null) {
			rdn.add(new Rdn("cn", cn));
		}
		if (dc != null) {
			rdn.add(new Rdn("dc", dc));
		}
		return new LdapName(rdn);
	}
	
	public static void removeKeysForUser(UUID userId, File keyFile, String keystorePassword) throws IOException, GeneralSecurityException {
		FileInputStream outputStream = new FileInputStream(keyFile);
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(outputStream, keystorePassword.toCharArray());
		
		keyStore.deleteEntry(userId + "E");
		keyStore.deleteEntry(userId + "S");
		
		KeystoreUtilities.saveKeystore(keyFile, keyStore, keystorePassword);
	}
	
	public static Set<PhotonicUser> getAllUsers(File keyFile, String keystorePassword) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		FileInputStream outputStream = new FileInputStream(keyFile);
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(outputStream, keystorePassword.toCharArray());
		Set<PhotonicUser> users = new HashSet<>();
		
		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			X509Certificate cert = (X509Certificate)keyStore.getCertificate(alias);
			String[] userIdAndName = null;
			try {
				userIdAndName = LdapUtils.getUserIdAndName(cert.getSubjectDN().getName());
			} catch (InvalidNameException e) {
				logger.error("Couldn't parse name from keystore:" + cert.getSubjectDN().getName(), e);
				continue;
			}		

			if (userIdAndName[0] == null) {
				logger.error("No UID component found for:" + cert.getSubjectDN().getName());
				continue;
			}			
			if (userIdAndName[1] == null) {
				logger.error("No CN component found for:" + cert.getSubjectDN().getName());
				continue;
			}

			String userId = alias.substring(0, alias.length() - 1);
			if (!userIdAndName[0].equals(userId)) {
				logger.error("UID component:" + userIdAndName[0] + " can't be different than alias:" + userId);
				continue;
			}

			users.add(new PhotonicUser(userIdAndName[1], null, UUID.fromString(userIdAndName[0]), null, null, !keyStore.isKeyEntry(alias)));
		}

		return users;
	}
	
	public static void generateRSAKeypairAndPossiblyKeystore(LdapName fullyQualifiedDN, Date endDate, File keyFile, String keyPairAlias, String keypairPassword, String keystorePassword) throws IOException, GeneralSecurityException, InvalidNameException {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		if (keyFile.exists()) {
			FileInputStream outputStream = new FileInputStream(keyFile);
			keyStore.load(outputStream, keystorePassword.toCharArray());
		} else {
			keyStore.load(null, null);
		}
		
		PrivateKeyEntry data = KeystoreUtilities.generateCertAndKeyPair(fullyQualifiedDN, endDate);
		keyStore.setKeyEntry(keyPairAlias, data.getPrivateKey(), keypairPassword.toCharArray(), new Certificate[]{data.getCertificate()});
		
		KeystoreUtilities.saveKeystore(keyFile, keyStore, keystorePassword);
	}

	public static boolean isCertificateAndPrivateKeyAvailable(File keyFile, String privateAndCertKeyAlias, String privateKeyPassword, String keystorePassword) {
		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			FileInputStream inputStream = new FileInputStream(keyFile);
			keyStore.load(inputStream, keystorePassword.toCharArray());
			Key key = keyStore.getKey(privateAndCertKeyAlias, privateKeyPassword.toCharArray());
			Certificate cert = keyStore.getCertificate(privateAndCertKeyAlias);
			return key != null && cert != null;
		} catch (KeyStoreException e) {
			return false;
		} catch (FileNotFoundException e) {
			return false;
		} catch (NoSuchAlgorithmException e) {
			return false;
		} catch (CertificateException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (UnrecoverableKeyException e) {
			return false;
		}
	}
	
	public static void secureContext(String ipAddress, ServletContextHandler context, Server jettyServer) throws Exception {
    	if (HostProperties.Instance().getExternallyAccessableName() != null) {
    		ipAddress = HostProperties.Instance().getExternallyAccessableName();
    	}
    	
    	File keystoreFile = HostProperties.Instance().getSSLKeystoreFile();
    	if (keystoreFile.isDirectory()) {
    		throw new IllegalArgumentException("Keystore location:" + keystoreFile + " is a directory, not a file.");
    	}
    	
		File parent = keystoreFile.getAbsoluteFile().getParentFile();
		if (!parent.exists() && !parent.mkdirs()) {
    		throw new IllegalArgumentException("Couldn't create directory structure: " + parent + " (Do you have rights?)");
		}
		
		if (!isCertificateAndPrivateKeyAvailable(
				keystoreFile, 
				ipAddress, 
				HostProperties.Instance().getSSLKeypairPassword(), 
				HostProperties.Instance().getSSLKeystorePassword())) {
			
    		generateRSAKeypairAndPossiblyKeystore(buildFullyQualifiedDN(null, ipAddress),
    				new Calendar.Builder().set(Calendar.YEAR, Calendar.getInstance()
    						.get(Calendar.YEAR) + 5).set(Calendar.MONTH, 2)
    						.set(Calendar.DAY_OF_MONTH, 1)
    						.build().getTime(),
    				keystoreFile, 
    				ipAddress, 
    				HostProperties.Instance().getSSLKeypairPassword(), 
    				HostProperties.Instance().getSSLKeystorePassword());
		}
    	
    	SslContextFactory sslContextFactory = new SslContextFactory();
    	sslContextFactory.setValidateCerts(false);
    	sslContextFactory.setKeyStorePath(keystoreFile + "");
    	sslContextFactory.setKeyStorePassword(HostProperties.Instance().getSSLKeystorePassword());
    	sslContextFactory.setKeyManagerPassword(HostProperties.Instance().getSSLKeypairPassword());
    	sslContextFactory.setCertAlias(ipAddress);
    	
        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setSecureScheme("https");
        httpsConfig.setSecurePort(HostProperties.Instance().getPrinterHostPort());
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
        
        ServerConnector https = new ServerConnector(
        		jettyServer, 
        		new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfig));
        https.setPort(HostProperties.Instance().getPrinterHostPort());//TODO: Why do we have to set this again!!!
            
        jettyServer.setConnectors(new Connector[]{https});
	}
}
