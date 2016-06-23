package org.area515.resinprinter.security;

import java.util.Set;
import java.util.UUID;

import org.area515.resinprinter.plugin.Feature;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.util.security.Credential;

public interface UserManagementFeature extends LoginService, Feature {
    public PhotonicUser update(String userName, Credential credential, String[] roleArray) throws UserManagementException;
    public void remove(PhotonicUser user) throws UserManagementException;
    public Set<PhotonicUser> getUsers();
    public PhotonicUser getUser(UUID uuid);
    public void setName(String realmName);
}
