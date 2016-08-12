package org.area515.resinprinter.security.keystore;

import java.security.Principal;

import javax.security.auth.Subject;

import org.area515.resinprinter.util.security.PhotonicCrypto;
import org.eclipse.jetty.security.DefaultUserIdentity;

public class CryptoUserIdentity extends DefaultUserIdentity {
	private PhotonicCrypto crypto;
	
	public CryptoUserIdentity(PhotonicCrypto crypto, Subject subject, Principal userPrincipal, String[] roles) {
		super(subject, userPrincipal, roles);
		this.crypto = crypto;
	}

	public PhotonicCrypto getCrypto() {
		return crypto;
	}
}
