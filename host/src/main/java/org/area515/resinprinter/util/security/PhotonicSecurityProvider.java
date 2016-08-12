package org.area515.resinprinter.util.security;

import java.security.Provider;

public class PhotonicSecurityProvider extends Provider {
	private static final long serialVersionUID = -3997778298991186486L;

	public PhotonicSecurityProvider() {
		super("Photonic3d", 0.01, "Provider designed to ensure all platforms have a consistant SHA1PRNG function.");
		put("SecureRandom.SHA1PRNG", SHA1PRNG.class.getName());
	}
}