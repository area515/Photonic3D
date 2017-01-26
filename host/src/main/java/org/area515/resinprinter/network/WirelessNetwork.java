package org.area515.resinprinter.network;

import java.util.ArrayList;
import java.util.List;

public class WirelessNetwork {
	private String ssid;
	private boolean hidden;
	private List<WirelessEncryption> supportedWirelessEncryption = new ArrayList<WirelessEncryption>();
	private String parentInterfaceName;
	private String password;
	
	public WirelessNetwork() {}
	
	public String getSsid() {
		return ssid;
	}
	public void setSsid(String ssid) {
		this.ssid = ssid;
	}
	
	public boolean isHidden() {
		return hidden;
	}
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public List<WirelessEncryption> getSupportedWirelessEncryption() {
		return supportedWirelessEncryption;
	}
	public void setSupportedWirelessEncryption(List<WirelessEncryption> supportedWirelessEncryption) {
		this.supportedWirelessEncryption = supportedWirelessEncryption;
	}

	public String getParentInterfaceName() {
		return parentInterfaceName;
	}
	public void setParentInterfaceName(String parentInterfaceName) {
		this.parentInterfaceName = parentInterfaceName;
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
}
