package org.area515.resinprinter.services;

import java.util.ArrayList;
import java.util.List;

public class NetInterface {
	private List<WirelessNetwork> wirelessNetworks = new ArrayList<WirelessNetwork>();
	private String name;
	
	public static class WirelessNetwork {
		private String ssid;
		
		public String getSsid() {
			return ssid;
		}
		public void setSsid(String ssid) {
			this.ssid = ssid;
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
