package org.area515.resinprinter.util.security;


public class Friend<T> {
	private PhotonicUser user;
	private T trustData;
	private String friendshipFeature;
	
	public Friend() {
	}
	
	public String getFriendshipFeature() {
		return friendshipFeature;
	}
	public void setFriendshipFeature(String friendshipFeature) {
		this.friendshipFeature = friendshipFeature;
	}

	public PhotonicUser getUser() {
		return user;
	}
	public void setUser(PhotonicUser user) {
		this.user = user;
	}
	
	public T getTrustData() {
		return trustData;
	}
	public void setTrustData(T trustData) {
		this.trustData = trustData;
	}
}
