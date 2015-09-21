package org.area515.resinprinter.printer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlRootElement(name="MachineConfig")
public class MachineConfig {
	public static final String NOT_CAPABLE = "Your printer configuration isn't capable of this feature";
	
	public static class DisplayedControls {
	}
	
	public static class CorrectionMask {
	}

	public static class ComPortSettings {
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
	}
	
	public static class MotorsDriverConfig {
		@XmlElement(name="DriverType")
		private String driverType = "eGENERIC";
		@XmlElement(name="ComPortSettings")
		private ComPortSettings comPortSettings;
		
		@XmlTransient
		public ComPortSettings getComPortSettings() {
			return comPortSettings;
		}
		public void setComPortSettings(ComPortSettings comPortSettings) {
			this.comPortSettings = comPortSettings;
		}
		
		@XmlTransient
		public String getDriverType() {
			return driverType;
		}
		public void setDriverType(String driverType) {
			this.driverType = driverType;
		}
	}

	public static class MonitorDriverConfig {
		@XmlElement(name="DLP_X_Res")
		private double dLP_X_Res;
		@XmlElement(name="DLP_Y_Res")
		private double dLP_Y_Res;
		@XmlElement(name="MonitorID")
		private String monitorID;
		@XmlElement(name="OSMonitorID")
		private String osMonitorID;
		@XmlElement(name="DisplayCommEnabled")
		private boolean displayCommEnabled;
		@XmlElement(name="ComPortSettings")
		private ComPortSettings comPortSettings;
		@XmlElement(name="MonitorTop")
		private int monitorTop;
		@XmlElement(name="MonitorLeft")
		private int monitorLeft;
		@XmlElement(name="MonitorRight")
		private int monitorRight;
		@XmlElement(name="MonitorBottom")
		private int monitorBottom;
		@XmlElement(name="UseMask")
		private boolean useMask;
		
		@XmlTransient
		public double getDLP_X_Res() {
			return dLP_X_Res;
		}
		public void setDLP_X_Res(double dLP_X_Res) {
			this.dLP_X_Res = dLP_X_Res;
		}

		@XmlTransient
		public double getDLP_Y_Res() {
			return dLP_Y_Res;
		}
		public void setDLP_Y_Res(double dLP_Y_Res) {
			this.dLP_Y_Res = dLP_Y_Res;
		}
		
		@XmlTransient
		public ComPortSettings getComPortSettings() {
			return comPortSettings;
		}
		public void setComPortSettings(ComPortSettings comPortSettings) {
			this.comPortSettings = comPortSettings;
		}
	}
	
	@XmlAttribute(name="FileVersion")
	private int fileVersion;
	@XmlElement(name="PlatformXSize")
	private double platformXSize;
	@XmlElement(name="PlatformYSize")
	private double platformYSize;
	@XmlElement(name="PlatformZSize")
	private double platformZSize;
	@XmlElement(name="MaxXFeedRate")
	private int maxXFeedRate;
	@XmlElement(name="MaxYFeedRate")
	private int maxYFeedRate;
	@XmlElement(name="MaxZFeedRate")
	private int maxZFeedRate;
	@XmlElement(name="XRenderSize")
	private int xRenderSize;
	@XmlElement(name="YRenderSize")
	private int yRenderSize;
	@XmlElement(name="DisplayedControls")
	private DisplayedControls controls;
	@XmlElement(name="MachineType")
	private String machineType;
	@XmlElement(name="MultiMonType")
	private String multiMonType;
	@XmlElement(name="MotorsDriverConfig")
	private MotorsDriverConfig motorsDriverConfig;
	@XmlElement(name="MonitorDriverConfig")
	private MonitorDriverConfig monitorDriverConfig;
	private String name;

	@XmlTransient
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@XmlTransient
	public double getPlatformXSize() {
		return platformXSize;
	}
	public void setPlatformXSize(double platformXSize) {
		this.platformXSize = platformXSize;
	}

	@XmlTransient
	public double getPlatformYSize() {
		return platformYSize;
	}
	public void setPlatformYSize(double platformYSize) {
		this.platformYSize = platformYSize;
	}

	@XmlTransient
	public double getPlatformZSize() {
		return platformZSize;
	}
	public void setPlatformZSize(double platformZSize) {
		this.platformZSize = platformZSize;
	}

	@XmlTransient
	public int getxRenderSize() {
		return xRenderSize;
	}
	public void setxRenderSize(int xRenderSize) {
		this.xRenderSize = xRenderSize;
	}

	@XmlTransient
	public int getyRenderSize() {
		return yRenderSize;
	}
	public void setyRenderSize(int yRenderSize) {
		this.yRenderSize = yRenderSize;
	}
	
	@XmlTransient
	public MotorsDriverConfig getMotorsDriverConfig() {
		return motorsDriverConfig;
	}
	public void setMotorsDriverConfig(MotorsDriverConfig motorsDriverConfig) {
		this.motorsDriverConfig = motorsDriverConfig;
	}
	
	@XmlTransient
	public MonitorDriverConfig getMonitorDriverConfig() {
		return monitorDriverConfig;
	}
	public void setMonitorDriverConfig(MonitorDriverConfig monitorDriverConfig) {
		this.monitorDriverConfig = monitorDriverConfig;
	}
	
	@JsonIgnore
	public Integer getDisplayIndex() {
		if (monitorDriverConfig == null || monitorDriverConfig.monitorID == null) {
			return null;
		}
		Pattern displayPattern = Pattern.compile(".*?([\\d]{1,2})");
		Matcher matcher = displayPattern.matcher(monitorDriverConfig.monitorID);
		if (!matcher.matches()) {
			return 0;
		}
		
		return Integer.parseInt(matcher.group(1)) - 1;		
	}
	
	@XmlTransient
	public String getOSMonitorID() {
		return monitorDriverConfig.osMonitorID;
	}
	public void setOSMonitorID(String name) {
		Pattern displayPattern = Pattern.compile(".*?([\\d]{1,2})");
		Matcher matcher = displayPattern.matcher(name);
		if (matcher.matches()) {
			Integer index = Integer.parseInt(matcher.group(1)) + 1;
			monitorDriverConfig.monitorID = "DISPLAY" + index;
		}
		
		monitorDriverConfig.osMonitorID = name;
	}
	
	public String toString() {
		return name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MachineConfig other = (MachineConfig) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
