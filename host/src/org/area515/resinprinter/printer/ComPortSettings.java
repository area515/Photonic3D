package org.area515.resinprinter.printer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class ComPortSettings {
	@XmlElement(name="PortName")
	private String portName;
	@XmlElement(name="Speed")
	private long speed;		
	@XmlElement(name="Databits")
	private int databits;
	@XmlElement(name="Parity")
	private String parity;
	@XmlElement(name="Stopbits")
	private String stopbits;
	@XmlElement(name="Handshake")
	private String handshake;

	public ComPortSettings() {
	}
	
	public ComPortSettings(ComPortSettings settings) {
		this.portName = settings.portName;
		this.speed = settings.speed;
		this.databits = settings.databits;
		this.parity = settings.parity;
		this.stopbits = settings.stopbits;
		this.handshake = settings.handshake;
	}
	
	@XmlTransient
	public String getPortName() {
		return portName;
	}
	public void setPortName(String portName) {
		this.portName = portName;
	}
	
	@XmlTransient
	public long getSpeed() {
		return speed;
	}
	public void setSpeed(long speed) {
		this.speed = speed;
	}
	
	@XmlTransient
	public int getDatabits() {
		return databits;
	}
	public void setDatabits(int databits) {
		this.databits = databits;
	}
	
	@XmlTransient
	public String getParity() {
		return parity;
	}
	public void setParity(String parity) {
		this.parity = parity;
	}
	
	@XmlTransient
	public String getStopbits() {
		return stopbits;
	}
	public void setStopbits(String stopbits) {
		this.stopbits = stopbits;
	}
	
	@XmlTransient
	public String getHandshake() {
		return handshake;
	}
	public void setHandshake(String handshake) {
		this.handshake = handshake;
	}
	
	public String toString() {
		return "{Port:" + portName + " speed:" + speed + " databits:" + databits + " parity:" + parity + " stopbits:" + stopbits + " handshake:" + handshake + "}";
	}
}