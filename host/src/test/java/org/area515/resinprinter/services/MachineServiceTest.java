package org.area515.resinprinter.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.area515.resinprinter.printer.MachineConfig;
import org.area515.resinprinter.printer.Named;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.printer.SlicingProfile.InkConfig;
import org.area515.resinprinter.server.ApplicationConfig;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

public class MachineServiceTest {
	public static boolean isFound(List<? extends Named> named, String name) {
    	boolean fileFound = false;
    	for (Named currentConfig : named) {
    		if (currentConfig.getName().equals(name)) {
    			fileFound = true;
    			break;
    		}
    	}
    	
    	return fileFound;
	}
	
	public Printer readConfiguration() throws JsonParseException, JsonMappingException, IOException {
		JacksonJaxbJsonProvider provider = ApplicationConfig.buildJacksonJaxbJsonProvider();
		Object printer = provider.readFrom(Object.class, Printer.class, null, MediaType.APPLICATION_JSON_TYPE, null, MachineServiceTest.class.getResourceAsStream("jsonToXMLTest.json"));
		return (Printer)printer;
	}
    
    private void assertPrinterProperties(PrinterConfiguration config) {
    	SlicingProfile slicingProfile = config.getSlicingProfile();
    	InkConfig inkConfig = slicingProfile.getSelectedInkConfig();
    	Map<?,?> settings = slicingProfile.getInkConfigs().iterator().next().getPrintMaterialDetectorSettings().getSettings();
    	
    	Assert.assertEquals("Firm Amber 50 Microns", inkConfig.getName());
    	Assert.assertEquals(2, settings.size());
    	Assert.assertEquals("AnotherValue", settings.get("AnotherSetting"));
    	Assert.assertEquals("GoodValue", settings.get("GoodSetting"));
    	Assert.assertEquals(10L, slicingProfile.getInkConfigs().size());
    	Assert.assertEquals(2L, (long)slicingProfile.getSelectedInkConfigIndex());
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
    
    @Test
    public void roundTripJSONToObjectToXMLToObject() throws JsonParseException, JsonMappingException, IOException, JAXBException {
    	Printer printer = readConfiguration();
    	PrinterConfiguration configuration = printer.getConfiguration();
    	assertPrinterProperties(configuration);
    	
    	ByteArrayOutputStream output = new ByteArrayOutputStream();
    	JAXBContext jaxbContext = JAXBContext.newInstance(PrinterConfiguration.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		jaxbMarshaller.marshal(configuration, output);
		
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		configuration = (PrinterConfiguration)jaxbUnmarshaller.unmarshal(new ByteArrayInputStream(output.toByteArray()));
    	assertPrinterProperties(configuration);
    }
}
