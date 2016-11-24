package org.area515.resinprinter.services;

import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.area515.resinprinter.printer.MachineConfig;
import org.area515.resinprinter.printer.Named;
import org.area515.resinprinter.printer.SlicingProfile;
import org.junit.Assert;
import org.junit.Test;

public class MachineServiceTest {
	public boolean isFound(List<? extends Named> named, String name) {
    	boolean fileFound = false;
    	for (Named currentConfig : named) {
    		if (currentConfig.getName().equals(name)) {
    			fileFound = true;
    			break;
    		}
    	}
    	
    	return fileFound;
	}
	
    @Test
    public void createListDeleteNewMachineConfig() throws JAXBException {
    	String configName = UUID.randomUUID() + "";
    	MachineConfig config = new MachineConfig();
    	config.setName(configName);
    	MachineService.INSTANCE.saveMachineConfiguration(config);
    	Assert.assertTrue(isFound(MachineService.INSTANCE.getMachineConfigurations(), configName));
    	MachineService.INSTANCE.deleteMachineConfiguration(configName);
    	Assert.assertFalse(isFound(MachineService.INSTANCE.getMachineConfigurations(), configName));
    }
    
    @Test
    public void createListDeleteNewSlicingProfileConfig() throws JAXBException {
    	String configName = UUID.randomUUID() + "";
    	SlicingProfile config = new SlicingProfile();
    	config.setName(configName);
    	MachineService.INSTANCE.saveSlicingProfile(config);
    	Assert.assertTrue(isFound(MachineService.INSTANCE.getSlicingProfiles(), configName));
    	MachineService.INSTANCE.deleteSlicingProfile(configName);
    	Assert.assertFalse(isFound(MachineService.INSTANCE.getSlicingProfiles(), configName));
    }
}
