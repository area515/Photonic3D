package org.area515.resinprinter.util.security;

import java.security.Principal;
import java.util.UUID;

public class PhotonicUser implements Principal {
	private UUID userId;			//Globally unique
	private String simpleUserName;	//Unique in a single Photonic installation
	private String email;
	private String[] roles;
	private String credential;
	private boolean remote;
	
	public static final String LISTENER = "listener";
	public static final String REMOTE_EXECUTION = "remoteExecution";
	public static final String FULL_RIGHTS = "admin";
	public static final String LOGIN = "login";
	public static final String USER_ADMIN = "userAdmin";
	public static final String CHAT = "chat";

	private PhotonicUser() {
	}

	public PhotonicUser(String simpleUserName, String credential, UUID userId, String email, String[] roles, boolean remote) {
		this.simpleUserName = simpleUserName;
		this.userId = userId;
		this.email = email;
		this.roles = roles;
		this.credential = credential;
		this.remote = remote;
	}
	
	public void setRoles(String[] roles) {
		this.roles = roles;
	}
	public String[] getRoles() {
		return roles;
	}
	
	public String getCredential() {
		return credential;
	}
	public void setCredential(String credential) {
		this.credential = credential;
	}
	
	public UUID getUserId() {
		return userId;
	}
	public void getUserId(UUID userId) {
		this.userId = userId;
	}
	
	public String getEmail() {
		return email;
	}
	
	public Boolean isRemote() {
		return remote;
	}
	public void setRemote(boolean remote) {
		this.remote = remote;
	}
	
	@Override
	public String getName() {
		return simpleUserName;
	}
	public void setName(String simpleUserName) {
		this.simpleUserName = simpleUserName;
	}
	
	public String toString() {
		return simpleUserName + "(" + getUserId() + ")";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PhotonicUser other = (PhotonicUser) obj;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}
}
