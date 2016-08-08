package org.area515.resinprinter.security;

import java.util.Set;
import java.util.UUID;

import org.area515.resinprinter.plugin.Feature;
import org.area515.resinprinter.util.security.Friend;
import org.area515.resinprinter.util.security.PhotonicUser;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;

public interface UserManagementFeature<T, C> extends LoginService, Feature {
	public UserIdentity loginRemote(PhotonicUser user) throws UserManagementException;
	public UserIdentity getLoggedInIdentity(PhotonicUser user);
    public PhotonicUser update(PhotonicUser user) throws UserManagementException;
    public void remove(PhotonicUser user) throws UserManagementException;
    public Set<PhotonicUser> getUsers();
    public PhotonicUser getUser(UUID uuid);
    public void setName(String realmName);
    public C trustNewFriend(Friend friend) throws UserManagementException;
}
