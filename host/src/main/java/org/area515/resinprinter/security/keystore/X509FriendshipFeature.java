package org.area515.resinprinter.security.keystore;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.InvalidNameException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.security.FriendshipFeature;
import org.area515.resinprinter.security.UserManagementException;
import org.area515.resinprinter.util.security.Friend;
import org.area515.resinprinter.util.security.PhotonicCrypto;
import org.area515.resinprinter.util.security.PhotonicUser;

public class X509FriendshipFeature implements FriendshipFeature {
    private static final Logger logger = LogManager.getLogger();	
	private RendezvousClient server;
    private Map<UUID, Friend> remotesAskingToBeFriends = new ConcurrentHashMap<>();
    private Map<UUID, List<PhotonicUser>> localsAskingToBeFriends = new ConcurrentHashMap<>();
    
    public X509FriendshipFeature() {
    }
    
    X509FriendshipFeature(RendezvousClient server) {
    	this.server = server;
    }
    
	@Override
	public List<Friend> getFriendRequests() throws UserManagementException {
		return new ArrayList<Friend>(remotesAskingToBeFriends.values());
	}

	@Override
	public String getName() {
		return PhotonicCrypto.FEATURE_NAME;
	}

	void addRemoteFriendRequest(Friend friend) {
		remotesAskingToBeFriends.put(friend.getUser().getUserId(), friend);
		NotificationManager.remoteMessageReceived("Friend request from:" + friend.getUser());
	}
	
	void remoteAcceptedFriendRequest(UUID local, PhotonicUser remote) {
		List<PhotonicUser> myOutGoingFriendRequests = localsAskingToBeFriends.get(local);
		if (myOutGoingFriendRequests == null) {
			return;
		}
		if (myOutGoingFriendRequests.remove(local)) {
			NotificationManager.remoteMessageReceived("Your friend request was accepted from:" + remote.getName());
		}
	}
	
	@Override
	public void start(URI startURI, String settings) throws Exception {
		this.server = RendezvousClient.getServer(startURI);
	}
	
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void acceptFriendRequest(Friend newFriend) {
		server.sendAcceptFriendResponse(newFriend);
		remotesAskingToBeFriends.remove(newFriend.getUser().getUserId());
	}

	@Override
	public Friend sendFriendRequest(PhotonicUser me, PhotonicUser myNewFriend) throws UserManagementException {
		List<PhotonicUser> myOutGoingFriendRequests = localsAskingToBeFriends.get(me.getUserId());
		if (myOutGoingFriendRequests == null) {
			myOutGoingFriendRequests = new ArrayList<PhotonicUser>();
			localsAskingToBeFriends.put(me.getUserId(), myOutGoingFriendRequests);
		}
		myOutGoingFriendRequests.add(myNewFriend);
		try {
			return server.sendFriendRequest(me.getUserId(), myNewFriend.getUserId());
		} catch (InvalidNameException | IOException | GeneralSecurityException e) {
			throw new UserManagementException("Couldn't send friend request to:" + myNewFriend.getName(), e);
		}
	}
}
