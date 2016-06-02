package org.area515.resinprinter.projector;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.server.HostProperties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class HexCodeBasedProjectorTesting {
    private static final Logger logger = LogManager.getLogger();
    private List<ProjectorModel> projectors;
        
    @Before
    public void loadPrinters() {
    	projectors = HostProperties.Instance().getAutodetectProjectors();
    }
    
    @Test
	public void noJSONErrorsAndJavaScriptAutodetectProjectors() {
    	//SerialCommunicationsPort port = Mockito.mock(SerialCommunicationsPort.class, Mockito.RETURNS_MOCKS);
    	boolean atLeastOneProjectorHasDefaultComPortSettings = false;
		logger.info("Projector json parse and javascript eval test.");
		
		Assert.assertTrue("There must be at least 1 projector found!", projectors.size() > 0);
    	for (ProjectorModel model : projectors) {
    		if (model.getDefaultComPortSettings() != null) {
    			atLeastOneProjectorHasDefaultComPortSettings = true;
    		}
    		HexCodeBasedProjector projector = (HexCodeBasedProjector)model;
    		//projector.autodetect(port);//Causes nasty GC bugs on some JVMs
    	}
		Assert.assertTrue("There must be at least 1 projector that has default com port settings", atLeastOneProjectorHasDefaultComPortSettings);
    }
    
    @Test
    public void testPrinterWithLittleEndianBulbConversion() throws IOException {
    	long expectedHours = 268;//=0x0C 0x01
    	byte[] bulbHoursBytes = new byte[]{0x05, 0x14, 0x00, 0x06, 0x00, 0x00, 0x00, 0x0C, 0x01, 0x00, 0x00, 0x27};
		SerialCommunicationsPort serial = org.mockito.Mockito.mock(SerialCommunicationsPort.class);
		Mockito.when(serial.read())
			.thenReturn(bulbHoursBytes)
			.thenThrow(new IllegalArgumentException("The read method should never have been called this time."));
		
    	for (ProjectorModel model : projectors) {
    		if (model.getName().contains("pjd7820hd")) {
    			Assert.assertEquals(expectedHours, model.getBulbHours(serial).longValue());
    		}
    	}
    }
}
