package org.area515.resinprinter.server;

public class Skin {
	private String name;
	private String[] welcomeFiles;
	private String resourceBase;
	private boolean active;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String[] getWelcomeFiles() {
		return welcomeFiles;
	}
	public void setWelcomeFiles(String[] welcomeFiles) {
		this.welcomeFiles = welcomeFiles;
	}
	public String getResourceBase() {
		return resourceBase;
	}
	public void setResourceBase(String resourceBase) {
		this.resourceBase = resourceBase;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
}
