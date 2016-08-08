package org.area515.resinprinter.util.security;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

public class LdapUtils {
	
	public static String[] getUserIdAndName(String fullyQualifiedDN) throws InvalidNameException {
		LdapName ldapName = new LdapName(fullyQualifiedDN);
		String[] names = new String[3];
		for (Rdn rdn : ldapName.getRdns()) {
			if (rdn.getType().equalsIgnoreCase("cn")) {
				names[1] = rdn.getValue() + "";
			} else if (rdn.getType().equalsIgnoreCase("uid")) {
				names[0] = rdn.getValue() + "";
			} else if (rdn.getType().equalsIgnoreCase("mail")) {
				names[2] = rdn.getValue() + "";
			}
		}
		
		return names;
	}

}
