package org.area515.resinprinter.server;

public class HostInformation {
	private String deviceName;
	private String manufacturer;
	
	private HostInformation() {}
	
	public HostInformation(String deviceName, String manufacturer) {
		this.deviceName = deviceName;
		this.manufacturer = manufacturer;
	}

	public String getDeviceName() {
		return deviceName;
	}
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getManufacturer() {
		return manufacturer;
	}
	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}
}
