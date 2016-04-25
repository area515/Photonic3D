package org.area515.resinprinter.network;

import java.util.ArrayList;
import java.util.List;

public class WirelessEncryption {
	private EncryptionClass encryptionClass;
	private List<WirelessCipher> groupCipher = new ArrayList<WirelessCipher>();
	private List<WirelessCipher> pairwiseCipher = new ArrayList<WirelessCipher>();
	
	public WirelessEncryption(){}

	public EncryptionClass getEncryptionClass() {
		return encryptionClass;
	}
	public void setEncryptionClass(EncryptionClass encryptionClass) {
		this.encryptionClass = encryptionClass;
	}

	public List<WirelessCipher> getGroupCipher() {
		return groupCipher;
	}
	public void setGroupCipher(List<WirelessCipher> groupCipher) {
		this.groupCipher = groupCipher;
	}

	public List<WirelessCipher> getPairwiseCipher() {
		return pairwiseCipher;
	}
	public void setPairwiseCipher(List<WirelessCipher> pairwiseCipher) {
		this.pairwiseCipher = pairwiseCipher;
	}
}