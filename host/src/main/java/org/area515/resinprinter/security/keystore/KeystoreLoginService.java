package org.area515.resinprinter.security.keystore;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.naming.InvalidNameException;
import javax.security.auth.Subject;
import javax.servlet.ServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.security.JettySecurityUtils;
import org.area515.resinprinter.security.PhotonicUser;
import org.area515.resinprinter.security.UserManagementException;
import org.area515.resinprinter.security.UserManagementFeature;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.services.UserService;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;

public class KeystoreLoginService implements UserManagementFeature {
    private static final Logger logger = LogManager.getLogger();
    
	private String realmName;
	private IdentityService identityService = new DefaultIdentityService();
	private Map<PhotonicUser, DefaultUserIdentity> loggedInUsers = new HashMap<>();
	private Map<String, Set<PhotonicUser>> knownUsers = new HashMap<>();
	
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
			return null;
		}
		
		PhotonicUser userFound = null;
		for (PhotonicUser user : users) {
			File keyFile = HostProperties.Instance().getUserKeystoreFile();
			String keystorePassword = HostProperties.Instance().getUserKeystorePassword();
			if (JettySecurityUtils.isCertificateAndPrivateKeyAvailable(keyFile, user.getUserId() + "E", credentials + "", keystorePassword)) {
				if (userFound != null) {
					logger.error("There are at least two users that have the same username id1:" + user.getUserId() + " and id2:" + userFound.getUserId());
					return null;
				}
				userFound = user;
			}
		}

		if (userFound == null) {
			return null;
		}
		
		//TODO: stop hardcoding roles
		DefaultUserIdentity identity = buildUserIdentity(userFound, new String[] {UserService.FULL_RIGHTS});
		loggedInUsers.put(userFound, identity);
		return identity;
	}

	//TODO: stop hardcoding rights
	private DefaultUserIdentity buildUserIdentity(PhotonicUser user, String[] roles) {
		return new DefaultUserIdentity(new Subject(), user, roles);
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
		File keyFile = HostProperties.Instance().getUserKeystoreFile();
		String keystorePassword = HostProperties.Instance().getUserKeystorePassword();
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public PhotonicUser update(String userName, Credential credential, String[] roleArray) throws UserManagementException {
		File keyFile = HostProperties.Instance().getUserKeystoreFile();
		String keystorePassword = HostProperties.Instance().getUserKeystorePassword();
		Set<PhotonicUser> users = knownUsers.get(userName);
		if (users == null) {
			users = new HashSet<>();
			knownUsers.put(userName, users);
		}
		
		PhotonicUser user = new PhotonicUser(userName, UUID.randomUUID());
		
		Date validTo = new Calendar.Builder()
								.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR) + 50)
								.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH))
								.set(Calendar.DAY_OF_MONTH, 1)
							.build().getTime();
		try {
			//TODO: Need to add roles here:
			KeystoreUtilities.generateRSAKeypairsForUserAndPossiblyKeystore(JettySecurityUtils.buildFullyQualifiedDN(userName), user.getUserId(), validTo, keyFile, credential.toString(), keystorePassword);
			users.add(user);
			DefaultUserIdentity identity = loggedInUsers.get(user);
			if (identity != null) {
				loggedInUsers.put(user, buildUserIdentity(user, roleArray));
			}
			return user;
		} catch (InvalidNameException e) {
			throw new UserManagementException("Invalid username", e);
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
		File keyFile = HostProperties.Instance().getUserKeystoreFile();
		String keystorePassword = HostProperties.Instance().getUserKeystorePassword();
		
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
		} catch (IOException | GeneralSecurityException e) {
			throw new UserManagementException("Couldn't remove user", e);
		}
	}

	public void createRemoteUser(PhotonicCrypto crypto, String[] roleArray) throws UserManagementException {
		//TODO: This allows you to install remote users
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
}
