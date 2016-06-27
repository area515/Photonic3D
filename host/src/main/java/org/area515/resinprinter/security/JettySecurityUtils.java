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
import org.area515.resinprinter.plugin.FeatureManager;
import org.area515.resinprinter.server.HostInformation;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.services.UserService;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;
import sun.security.x509.SubjectKeyIdentifierExtension;
import sun.security.x509.KeyIdentifier;

public class JettySecurityUtils {
    private static final Logger logger = LogManager.getLogger();

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
		String[] userIdAndName = getUserIdAndName(fullyQualifiedDN.toString());
		if (userIdAndName[0] != null) {
			throw new InvalidNameException("The uid component of the ldapname cannot be set in the fullQualifiedDN");
		}
		
		fullyQualifiedDN.add(new Rdn("uid", UUID.nameUUIDFromBytes(keyPair.getPublic().getEncoded()).toString()));
		X509Certificate cert = generateX509Certificate(fullyQualifiedDN, keyPair, endDate, "SHA256withRSA");
		return new PrivateKeyEntry(keyPair.getPrivate(), new Certificate[]{cert});
	}
	
	public static LdapName buildFullyQualifiedDN(String cn) throws InvalidNameException {
		HostInformation info = HostProperties.Instance().loadHostInformation();
		List<Rdn> rdn = new ArrayList<Rdn>();
		rdn.add(new Rdn("ou", info.getManufacturer()));
		rdn.add(new Rdn("ou", info.getDeviceName()));
		rdn.add(new Rdn("cn", cn));
		return new LdapName(rdn);
	}
	
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
	
	public static void removeKeysForUser(UUID userId, File keyFile, String keystorePassword) throws IOException, GeneralSecurityException {
		FileInputStream outputStream = new FileInputStream(keyFile);
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(outputStream, keystorePassword.toCharArray());
		
		keyStore.deleteEntry(userId + "E");
		keyStore.deleteEntry(userId + "S");
		
		saveKeystore(keyFile, keyStore, keystorePassword);
	}
	
	public static String[] getUserIdAndName(String fullyQualifiedDN) throws InvalidNameException {
		LdapName ldapName = new LdapName(fullyQualifiedDN);
		String[] names = new String[2];
		for (Rdn rdn : ldapName.getRdns()) {
			if (rdn.getType().equalsIgnoreCase("cn")) {
				names[1] = rdn.getValue() + "";
			} else if (rdn.getType().equalsIgnoreCase("uid")) {
				names[0] = rdn.getValue() + "";
			}
		}
		
		return names;
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
				userIdAndName = getUserIdAndName(cert.getSubjectDN().getName());
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

			alias = alias.substring(0, alias.length() - 1);
			if (!userIdAndName[0].equals(alias)) {
				logger.error("UID component:" + userIdAndName[0] + " can't be different than alias:" + alias);
				continue;
			}
			users.add(new PhotonicUser(userIdAndName[1], UUID.fromString(userIdAndName[0])));
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
		
		PrivateKeyEntry data = generateCertAndKeyPair(fullyQualifiedDN, endDate);
		keyStore.setKeyEntry(keyPairAlias, data.getPrivateKey(), keypairPassword.toCharArray(), new Certificate[]{data.getCertificate()});
		
		saveKeystore(keyFile, keyStore, keystorePassword);
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
			
    		generateRSAKeypairAndPossiblyKeystore(buildFullyQualifiedDN(ipAddress),
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

        //All below is user based security
        Constraint constraint = new Constraint();
        constraint.setName( Constraint.__BASIC_AUTH );
        constraint.setRoles( new String[]{ UserService.FULL_RIGHTS } );//Ahhh what????
        constraint.setAuthenticate( true );
     
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint( constraint );
        mapping.setPathSpec( "/*" );
        
        UserManagementFeature loginService =  FeatureManager.getUserManagementFeature();
        /*HashLoginService loginService = new HashLoginService();
        loginService.putUser(
        		HostProperties.Instance().getClientUsername(), 
        		Credential.getCredential(HostProperties.Instance().getClientPassword()), new String[] { HostProperties.FULL_RIGHTS});*/
        loginService.setName(HostProperties.Instance().getSecurityRealmName());
        //loginService.start();
        //OAuthLoginService OAuth2 AuthenticatorFactory ServletSecurityAnnotationHandler
        //http://stackoverflow.com/questions/24591782/resteasy-support-for-jax-rs-rolesallowed
        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setLoginService(loginService);
        //FormAuthenticator d;
        //csh.setAuthenticator(authenticator);
        //csh.setAuthenticatorFactory(null);change above from BASIC to FORM and change this to a FormAuthenticator
        csh.setConstraintMappings( new ConstraintMapping[]{ mapping } );
        
        context.setInitParameter("resteasy.role.based.security", String.valueOf(true));
     	context.setSecurityHandler(csh);
	}
}
