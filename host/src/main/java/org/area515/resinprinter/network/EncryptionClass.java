package org.area515.resinprinter.network;

public enum EncryptionClass {
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