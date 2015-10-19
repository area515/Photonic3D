package org.area515.resinprinter.network;

import java.util.ArrayList;
import java.util.List;

public class NetInterface {
	private List<WirelessNetwork> wirelessNetworks = new ArrayList<WirelessNetwork>();
	private String name;
	
	public static class WirelessEncryption {
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
	
	public static enum WirelessCipher {
		CCMP,
		TKIP
	}
	
	public static enum EncryptionClass {
		WPA(300),
		WPA2(400),
		WEP(200),
		Open(100);
		private int priority;
		
		EncryptionClass(int priority) {
			this.priority = priority;
		}

		public int getPriority() {
			return priority;
		}
	}

	public static class WirelessNetwork {
		private String ssid;
		private List<WirelessEncryption> supportedWirelessEncryption = new ArrayList<WirelessEncryption>();
		private NetInterface parentInterface;

		public WirelessNetwork() {}
		
		public String getSsid() {
			return ssid;
		}
		public void setSsid(String ssid) {
			this.ssid = ssid;
		}
		
		public List<WirelessEncryption> getSupportedWirelessEncryption() {
			return supportedWirelessEncryption;
		}
		public void setSupportedWirelessEncryption(List<WirelessEncryption> supportedWirelessEncryption) {
			this.supportedWirelessEncryption = supportedWirelessEncryption;
		}

		public NetInterface getParentInterface() {
			return parentInterface;
		}
		public void setParentInterface(NetInterface parentInterface) {
			this.parentInterface = parentInterface;
		}
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public List<WirelessNetwork> getWirelessNetworks() {
		return wirelessNetworks;
	}
	public void setWirelessNetworks(List<WirelessNetwork> wirelessNetworks) {
		this.wirelessNetworks = wirelessNetworks;
	}
}
