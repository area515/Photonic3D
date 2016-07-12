package org.area515.resinprinter.security;

import java.util.Set;
import java.util.UUID;

import org.area515.resinprinter.plugin.Feature;
import org.area515.resinprinter.security.keystore.CryptoUserIdentity;
import org.eclipse.jetty.security.LoginService;

public interface UserManagementFeature<T, C> extends LoginService, Feature {
	public CryptoUserIdentity loginRemote(PhotonicUser user) throws UserManagementException;
    public PhotonicUser update(PhotonicUser user) throws UserManagementException;
    public void remove(PhotonicUser user) throws UserManagementException;
    public Set<PhotonicUser> getUsers();
    public PhotonicUser getUser(UUID uuid);
    public void setName(String realmName);
    public C trustNewFriend(Friend friend) throws UserManagementException;
}
