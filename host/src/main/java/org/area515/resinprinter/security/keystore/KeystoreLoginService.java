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
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.naming.InvalidNameException;
import javax.security.auth.Subject;
import javax.servlet.ServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.security.JettySecurityUtils;
import org.area515.resinprinter.security.UserManagementException;
import org.area515.resinprinter.security.UserManagementFeature;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.util.security.Friend;
import org.area515.resinprinter.util.security.KeystoreUtilities;
import org.area515.resinprinter.util.security.PhotonicCrypto;
import org.area515.resinprinter.util.security.PhotonicUser;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.server.UserIdentity;

import sun.security.x509.X509CertImpl;

public class KeystoreLoginService implements UserManagementFeature<String[], PhotonicCrypto> {
    private static final Logger logger = LogManager.getLogger();	
    private static final String PASSWORD_CHARS = "`1234567890-=~!@#$%^&*()_+qwertyuiop[]\\QWERTYUIOP{}|asdfghjkl;'ASDFGHJKL:\"zxcvbnm,./ZXCVBNM<>?";
    
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
	//TODO: Logout users after a certain period of inactivity?
	public UserIdentity login(String username, Object credentials, ServletRequest request) {
		Set<PhotonicUser> users = knownUsers.get(username);
		
		if (users == null) {
			logger.error("No known users. It isn't possible to login to Photonic3d with username: " + username);
			return null;
		}
		
		PhotonicUser userFound = null;
		PhotonicCrypto crypto = null;
		for (PhotonicUser user : users) {
			try {
				if (user.isRemote()) {
					if (user.getCredential() != null && user.getCredential().equals(credentials + "")) {
						crypto = KeystoreUtilities.getPhotonicCrypto(user, keyFile, null, keystorePassword, allowInsecureCommunications);
						userFound = user;
						break;
					}
				} else {
					crypto = KeystoreUtilities.getPhotonicCrypto(user, keyFile, credentials + "", keystorePassword, allowInsecureCommunications);
				}
			} catch (InvalidNameException | IOException | GeneralSecurityException e) {
				logger.info("Couldn't login with username:" + username + " userId:" + user.getUserId(), e);
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
		byte[] newPassword = new byte[20];
		if (user.isRemote()) {
			Random random = new Random();
			for (int t = 0; t < newPassword.length; t++) {
				newPassword[t] = (byte)PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length()));
			}
			
			user = new PhotonicUser(user.getName(), new String(newPassword), user.getUserId(), user.getEmail(), user.getRoles(), true);
			
		}
		
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
		//TODO: When a user is logged out we need to logout all of it's conversations in the RendezvousClient.
		//TODO: When we logout, we should shut off all PhotonicCrypto connections to all remote users that were talking to this UserIdentity
		//TODO: When we logout, we should make sure to remove all credentials from the credentialProvider
		loggedInUsers.remove(user.getUserPrincipal());
	}

	@Override
	public void start(URI uri, String settings) {
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
				
				loadRoles(user);
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
				crypto = KeystoreUtilities.getPhotonicCrypto(user, keyFile, user.isRemote()?null:user.getCredential(), keystorePassword, allowInsecureCommunications);
			} else if (!users.isEmpty()){
				throw new UserManagementException("Invalid username or password");
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
			user = new PhotonicUser(user.getName(), null, crypto.getLocalUserId(), user.getEmail(), user.getRoles(), user.isRemote());
			saveRoles(user);
			users.remove(user);
			users.add(user);
			DefaultUserIdentity identity = loggedInUsers.get(user);
			if (identity != null) {
				loggedInUsers.put(user, buildUserIdentity(user, crypto));
			}
			return user;
		} catch (InvalidNameException e) {
			throw new UserManagementException("Invalid username or password", e);
		} catch (IOException | GeneralSecurityException e) {
			throw new UserManagementException("Couldn't add or update user", e);
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
		if (user == null) {
			throw new UserManagementException("No user specified");
		}
		
		try {
			Set<PhotonicUser> users = knownUsers.get(user.getName());
			if (users == null) {
				throw new UserManagementException("Couldn't remove user");
			}
			JettySecurityUtils.removeKeysForUser(user.getUserId(), keyFile, keystorePassword);
			loggedInUsers.remove(user);
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
			logger.info("Keystore trusted remote:{}", user);
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
			crypto = KeystoreUtilities.getPhotonicCrypto(user, keyFile, user.isRemote()?null:user.getCredential(), keystorePassword, allowInsecureCommunications);
			CryptoUserIdentity identity = buildUserIdentity(user, crypto);
			Set<PhotonicUser> users = knownUsers.get(user.getName());
			for (PhotonicUser currentUser : users) {
				if (currentUser.getUserId().equals(user.getUserId())) {
					//These lines ensure that the session password is setup in the identity
					users.remove(user);
					users.add((PhotonicUser)identity.getUserPrincipal());
					logger.info("Keystore login from remote:{}", user);
					return identity;
				}
			}
			
			throw new UserManagementException("Couldn't validate identity of user");
		} catch (InvalidNameException | IOException | GeneralSecurityException e) {
			throw new UserManagementException("Couldn't validate identity of user", e);
		}
	}

	@Override
	public CryptoUserIdentity getLoggedInIdentity(PhotonicUser user) {
		return loggedInUsers.get(user);
	}
}
