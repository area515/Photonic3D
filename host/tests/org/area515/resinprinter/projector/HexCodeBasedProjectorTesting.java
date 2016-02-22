package org.area515.resinprinter.projector;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.server.HostProperties;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class HexCodeBasedProjectorTesting {
    private static final Logger logger = LogManager.getLogger();
    
    @Test
	public void noJSONErrorsAndJavaScriptAutodetectProjectors() {
    	SerialCommunicationsPort port = Mockito.mock(SerialCommunicationsPort.class, Mockito.RETURNS_MOCKS);
    	boolean atLeastOneProjectorHasDefaultComPortSettings = false;
		logger.info("Projector json parse and javascript eval test.");
		List<ProjectorModel> models = HostProperties.Instance().getAutodetectProjectors();
		Assert.assertTrue("There must be at least 1 projector found!", models.size() > 0);
    	for (ProjectorModel model : models) {
    		if (model.getDefaultComPortSettings() != null) {
    			atLeastOneProjectorHasDefaultComPortSettings = true;
    		}
    		HexCodeBasedProjector projector = (HexCodeBasedProjector)model;
    		projector.autodetect(port);
    	}
		Assert.assertTrue("There must be at least 1 projector that has default com port settings", atLeastOneProjectorHasDefaultComPortSettings);
    }
}
