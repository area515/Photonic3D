package org.area515.resinprinter.printer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name="PrinterConfiguration")
public class PrinterConfiguration {
	private String name;
	@XmlElement(name="MachineConfigurationName")
	private String machineConfigName;
	@XmlElement(name="SlicingProfileName")
	private String slicingProfileName;
	@XmlElement(name="AutoStart")
	private boolean autoStart;
	
	private MachineConfig machineConfig;
	private SlicingProfile slicingProfile;

	public PrinterConfiguration() {
	}
	
	public PrinterConfiguration(String machineConfigName, String slicingProfileName, boolean autoStart) {
		 this.machineConfigName = machineConfigName;
		 this.slicingProfileName = slicingProfileName;
		 this.autoStart = autoStart;
	}
	
	public String getMachineConfigName() {
		return machineConfigName;
	}

	public String getSlicingProfileName() {
		return slicingProfileName;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public MachineConfig getMachineConfig() {
		return machineConfig;
	}
	public void setMachineConfig(MachineConfig machineConfig) {
		this.machineConfig = machineConfig;
	}
	
	public SlicingProfile getSlicingProfile() {
		return slicingProfile;
	}	
	public void setSlicingProfile(SlicingProfile slicingProfile) {
		this.slicingProfile = slicingProfile;
	}
	
	@XmlTransient
	public boolean isAutoStart() {
		return autoStart;
	}
	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
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
		PrinterConfiguration other = (PrinterConfiguration) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
