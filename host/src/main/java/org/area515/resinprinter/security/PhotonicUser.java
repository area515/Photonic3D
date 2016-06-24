package org.area515.resinprinter.security;

import java.security.Principal;
import java.util.UUID;

public class PhotonicUser implements Principal {
	private UUID userId;			//Globally unique
	private String simpleUserName;	//Unique in a single photonic installation
	
	public PhotonicUser(String simpleUserName, UUID userId) {
		this.simpleUserName = simpleUserName;
		this.userId = userId;
	}
	
	public UUID getUserId() {
		return userId;
	}
	
	@Override
	public String getName() {
		return simpleUserName;
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
