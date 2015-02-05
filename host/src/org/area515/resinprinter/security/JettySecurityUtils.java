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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.area515.resinprinter.server.HostProperties;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
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
	public static X509Certificate generateX509Certificate(String dn, KeyPair pair, Date to, String algorithm) throws GeneralSecurityException, IOException {
		PrivateKey privkey = pair.getPrivate();
		X509CertInfo info = new X509CertInfo();
		Date from = new Date();
		CertificateValidity interval = new CertificateValidity(from, to);
		BigInteger sn = new BigInteger(64, SecureRandom.getInstance("SHA1PRNG"));
		X500Name owner = new X500Name(dn);

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
		
		//AlgorithmId algo = new AlgorithmId(AlgorithmId.DH_oid);
		AlgorithmId algo = new AlgorithmId(AlgorithmId.sha256WithRSAEncryption_oid);
		info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

		// Sign the cert to identify the algorithm that's used.
		X509CertImpl cert = new X509CertImpl(info);
		cert.sign(privkey, algorithm);

		// Update the algorith, and resign.
		algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
		info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
		cert = new X509CertImpl(info);
		cert.sign(privkey, algorithm);
		return cert;
	}

	public static KeyStore generateRSAKeypairAndKeystore(String fullyQualifiedDN, Date endDate, String keystoreLocation, String keyPairAlias, String keypairPassword, String keystorePassword) throws IOException, GeneralSecurityException {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, null);
		
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		keyGen.initialize(2048, random);
		
		KeyPair keyPair = keyGen.generateKeyPair();
		X509Certificate cert = generateX509Certificate(fullyQualifiedDN, keyPair, endDate, "SHA1withRSA");
		keyStore.setKeyEntry(keyPairAlias, keyPair.getPrivate(), keypairPassword.toCharArray(), new Certificate[]{cert});
		
		File keyFile = new File(keystoreLocation);
		FileOutputStream outputStream = new FileOutputStream(keyFile);
		try {
			keyStore.store(outputStream, keystorePassword.toCharArray());
		} finally {
			try {
				outputStream.close();
			} catch (IOException e) {}
		}
		
		return keyStore;
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
    	
    	File keystoreFile = HostProperties.Instance().getKeystoreFile();
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
				HostProperties.Instance().getKeypairPassword(), 
				HostProperties.Instance().getKeystorePassword())) {
    		generateRSAKeypairAndKeystore(
    				"cn=" + ipAddress + ",ou=" + HostProperties.Instance().getDeviceName() + ",ou=" + HostProperties.Instance().getManufacturer(),
    				new Date(new Date().getYear() + 5, 1, 1),
    				keystoreFile + "", 
    				ipAddress, 
    				HostProperties.Instance().getKeypairPassword(), 
    				HostProperties.Instance().getKeystorePassword());
		}
    	
    	SslContextFactory sslContextFactory = new SslContextFactory();
    	sslContextFactory.setValidateCerts(false);
    	sslContextFactory.setKeyStorePath(keystoreFile + "");
    	sslContextFactory.setKeyStorePassword(HostProperties.Instance().getKeystorePassword());
    	sslContextFactory.setKeyManagerPassword(HostProperties.Instance().getKeypairPassword());
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

        //All below is user based security
        Constraint constraint = new Constraint();
        constraint.setName( Constraint.__BASIC_AUTH );
        constraint.setRoles( new String[]{ HostProperties.FULL_RIGHTS } );
        constraint.setAuthenticate( true );
     
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint( constraint );
        mapping.setPathSpec( "/*" );
        
        HashLoginService loginService = new HashLoginService();
        loginService.putUser(
        		HostProperties.Instance().getClientUsername(), 
        		Credential.getCredential(HostProperties.Instance().getClientPassword()), new String[] { HostProperties.FULL_RIGHTS});
        loginService.setName(HostProperties.Instance().getSecurityRealmName());
        loginService.start();
        
        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setLoginService(loginService);
        csh.setConstraintMappings( new ConstraintMapping[]{ mapping } );
     
     	context.setSecurityHandler(csh);
	}
}
