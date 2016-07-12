package org.area515.resinprinter.security.keystore;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.naming.InvalidNameException;
import javax.security.auth.Subject;
import javax.servlet.ServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.security.Friend;
import org.area515.resinprinter.security.JettySecurityUtils;
import org.area515.resinprinter.security.PhotonicUser;
import org.area515.resinprinter.security.UserManagementException;
import org.area515.resinprinter.security.UserManagementFeature;
import org.area515.resinprinter.server.HostProperties;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.server.UserIdentity;

import sun.security.x509.X509CertImpl;

public class KeystoreLoginService implements UserManagementFeature<String[], PhotonicCrypto> {
    private static final Logger logger = LogManager.getLogger();	
    
    private File keyFile;
    private String keystorePassword;
	private String realmName;
	private IdentityService identityService = new DefaultIdentityService();
	private Map<PhotonicUser, CryptoUserIdentity> loggedInUsers = new HashMap<>();
	private Map<String, Set<PhotonicUser>> knownUsers = new HashMap<>();//TODO: for faster access this needs to be a user/crypto combo
	private boolean allowInsecureCommunications = false;
	
	public KeystoreLoginService() {
		keyFile = HostProperties.Instance().getUserKeystoreFile();
		keystorePassword = HostProperties.Instance().getUserKeystorePassword();
		//Probably not a good idea
		//TODO: allowInsecureCommunications = HostProperties.Instance().allowInsecureCommunications();
	}
	
	public KeystoreLoginService(File userskeystore, String keystorePassword, boolean allowInsecureCommunications) {
		this.keyFile = userskeystore;
		this.keystorePassword = keystorePassword;
		this.allowInsecureCommunications = allowInsecureCommunications;
	}
	
	public void setName(String realmName) {
		this.realmName = realmName;
	}
	
	@Override
	public String getName() {
		return realmName;
	}

	@Override
	public UserIdentity login(String username, Object credentials, ServletRequest request) {
		Set<PhotonicUser> users = knownUsers.get(username);
		
		if (users == null) {
			logger.error("No known users. It isn't possible to login to Photonic3d");
			return null;
		}
		
		PhotonicUser userFound = null;
		PhotonicCrypto crypto = null;
		for (PhotonicUser user : users) {
			try {
				crypto = KeystoreUtilities.getPhotonicCrypto(user, keyFile, credentials + "", keystorePassword, allowInsecureCommunications);
			} catch (InvalidNameException | IOException | GeneralSecurityException e) {
				logger.info("There are at least two users that have the same username id1:" + 
							user.getUserId() + " and id2:" + 
							userFound.getUserId() + 
							" (looking for userid with correct credentials)", e);
			}
			if (crypto != null) {
				if (userFound != null) {
					logger.error("There are at least two local users that have the same username id1:" + user.getUserId() + " and id2:" + userFound.getUserId());
					return null;
				}
				userFound = user;
			}
		}

		if (userFound == null) {
			logger.error("No known user with the name:" + username);
			return null;
		}

		loadRoles(userFound);
		CryptoUserIdentity identity = buildUserIdentity(userFound, crypto);
		loggedInUsers.put(userFound, identity);
		return identity;
	}

	private CryptoUserIdentity buildUserIdentity(PhotonicUser user, PhotonicCrypto crypto) {
		CryptoUserIdentity identity = new CryptoUserIdentity(crypto, new Subject(), user, user.getRoles());
		return identity;
	}
	
	@Override
	public boolean validate(UserIdentity user) {
		return loggedInUsers.containsKey(user.getUserPrincipal());
	}

	@Override
	public IdentityService getIdentityService() {
		return identityService;
	}

	@Override
	public void setIdentityService(IdentityService identityService) {
		this.identityService = identityService;
	}

	@Override
	public void logout(UserIdentity user) {
		loggedInUsers.remove(user.getUserPrincipal());
	}

	@Override
	public void start(URI uri) {
		if (!keyFile.exists()) {
			return;
		}
		try {
			for (PhotonicUser user : JettySecurityUtils.getAllUsers(keyFile, keystorePassword)) {
				Set<PhotonicUser> users = knownUsers.get(user.getName());
				if (users == null) {
					users = new HashSet<>();
					knownUsers.put(user.getName(), users);
				}
				
				users.add(user);
			}
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			logger.error(e);
		}
	}

	@Override
	public void stop() {
	}

	@Override
	public PhotonicUser update(PhotonicUser user) throws UserManagementException {
		Set<PhotonicUser> users = knownUsers.get(user.getName());
		if (users == null) {
			users = new HashSet<>();
			knownUsers.put(user.getName(), users);
		}

		try {
			PhotonicCrypto crypto = null;
			if (user.getUserId() != null) {
				crypto = KeystoreUtilities.getPhotonicCrypto(user, keyFile, user.getCredential(), keystorePassword, allowInsecureCommunications);
			} else {
				Date validTo = new Calendar.Builder()
						.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR) + 50)
						.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH))
						.set(Calendar.DAY_OF_MONTH, 1)
					.build().getTime();
				crypto = KeystoreUtilities.generateRSAKeypairsForUserAndPossiblyKeystore(
						JettySecurityUtils.buildFullyQualifiedDN(user.getName(), null), 
						validTo, 
						keyFile, 
						user.getCredential(), 
						keystorePassword,
						allowInsecureCommunications);
			}

			user = new PhotonicUser(user.getName(), null, crypto.getLocalUserId(), user.getEmail(), user.getRoles());
			saveRoles(user);
			users.add(user);
			DefaultUserIdentity identity = loggedInUsers.get(user);
			if (identity != null) {
				loggedInUsers.put(user, buildUserIdentity(user, crypto));
			}
			return user;
		} catch (InvalidNameException e) {
			throw new UserManagementException("Invalid username or password", e);
		} catch (IOException | GeneralSecurityException e) {
			throw new UserManagementException("Couldn't create user", e);
		}
	}

	@Override
	public Set<PhotonicUser> getUsers() {
		HashSet<PhotonicUser> users = new HashSet<>();
		for (Set<PhotonicUser> usersPerName : knownUsers.values()) {
			users.addAll(usersPerName);
		}
		
		return users;
	}

	@Override
	public void remove(PhotonicUser user) throws UserManagementException {
		try {
			JettySecurityUtils.removeKeysForUser(user.getUserId(), keyFile, keystorePassword);
			loggedInUsers.remove(user);
			Set<PhotonicUser> users = knownUsers.get(user.getName());
			for (PhotonicUser currentUser : users) {
				if (currentUser.equals(user)) {
					users.remove(currentUser);
				}
			}
			if (users.isEmpty()) {
				knownUsers.remove(user.getName());
			}
			
			removeRoles(user);
		} catch (IOException | GeneralSecurityException e) {
			throw new UserManagementException("Couldn't remove user", e);
		}
	}

	@Override
	public PhotonicUser getUser(UUID userId) {
		for (Set<PhotonicUser> usersPerName : knownUsers.values()) {
			for (PhotonicUser currentUser : usersPerName) {
				if (currentUser.getUserId().equals(userId)) {
					return currentUser;
				}
			}
		}
		
		return null;
	}

	private void removeRoles(PhotonicUser user) {
		HostProperties.Instance().removeProperties("user." + user.getUserId() + "");
	}
	
	private void saveRoles(PhotonicUser user) {
		HostProperties.Instance().saveProperty("user." + user.getUserId(), Arrays.toString(user.getRoles()).replaceAll("[\\[\\]]", ""));	
	}
	
	private void loadRoles(PhotonicUser user) {
		user.setRoles(HostProperties.Instance().loadProperty("user." + user.getUserId()).split(",\\s*"));
	}
	
	@Override
	public PhotonicCrypto trustNewFriend(Friend friend) throws UserManagementException {
		try {
			String[] trustData = (String[])friend.getTrustData();
			PhotonicUser user = friend.getUser();
			X509Certificate signCert = new X509CertImpl(Base64.getDecoder().decode(trustData[0]));//Sign
			X509Certificate encryptCert = new X509CertImpl(Base64.getDecoder().decode(trustData[1]));//Encrypt
			PhotonicCrypto crypto = KeystoreUtilities.trustCertificate(user, keyFile, keystorePassword, signCert, encryptCert, allowInsecureCommunications);
			saveRoles(user);
			Set<PhotonicUser> users = knownUsers.get(user.getName());
			if (users == null) {
				users = new HashSet<>();
				knownUsers.put(user.getName(), users);
			}
			users.add(user);
			return crypto;
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | InvalidNameException
				| UnrecoverableEntryException | IOException e) {
			throw new UserManagementException("Couldn't trust user", e);
		}
	}

	@Override
	public CryptoUserIdentity loginRemote(PhotonicUser user) throws UserManagementException {
		PhotonicCrypto crypto;
		try {
			crypto = KeystoreUtilities.getPhotonicCrypto(user, keyFile, null, keystorePassword, allowInsecureCommunications);
			CryptoUserIdentity identity = buildUserIdentity(user, crypto);
			loggedInUsers.put(user, identity);
			return identity;
		} catch (InvalidNameException | IOException | GeneralSecurityException e) {
			throw new UserManagementException("Couldn't validate identity of user", e);
		}
	}
}
