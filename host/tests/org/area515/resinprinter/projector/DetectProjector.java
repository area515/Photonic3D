package org.area515.resinprinter.projector;

import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.printer.ComPortSettings;
import org.area515.resinprinter.serial.JSSCCommPort;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.test.HardwareCompatibilityTestSuite;
import org.junit.Assert;
import org.junit.Test;

public class DetectProjector {
    private static final Logger logger = LogManager.getLogger();

    @Test
	public void noErrorsInGetAutodetectProjectors() {
		logger.info("Projector json parse test.");
		HostProperties.Instance().getAutodetectProjectors();
	}
	
	@Test
	public void noErrorsDetectingProjector() {
		logger.info("Projector detection test.");

		boolean hasFound = false;
		for (long speed : HardwareCompatibilityTestSuite.COMMON_SPEEDS ) {
			ComPortSettings newComPortSettings = new ComPortSettings();
			newComPortSettings.setSpeed(speed);
			newComPortSettings.setDatabits(8);
			newComPortSettings.setParity("NONE");
			newComPortSettings.setStopbits("1");
			
			ArrayList<CommPortIdentifier> identifiers = new ArrayList<CommPortIdentifier>(Collections.list(CommPortIdentifier.getPortIdentifiers()));
			for (CommPortIdentifier currentIdentifier : identifiers) {
				newComPortSettings.setPortName(currentIdentifier.getName());
				
				logger.info("Port:{} Baud:{}", currentIdentifier.getName(), speed);
				
				SerialCommunicationsPort port = new JSSCCommPort();
				ProjectorModel model = SerialManager.Instance().getProjectorModel(port, newComPortSettings);
				if (model != null) {
					hasFound = true;
				}
				logger.info("  JSSCCommPort projector detection:{}", model);
				
				/*port = new RXTXEventBasedCommPort();
				logger.info("  RXTXEventBasedCommPort projector detection:{}", SerialManager.Instance().getProjectorModel(port, newComPortSettings, false));
				
				port = new RXTXSynchronousReadBasedCommPort();
				logger.info("  RXTXSynchronousReadBasedCommPort projector detection:{}", SerialManager.Instance().getProjectorModel(port, newComPortSettings, false));*/
			}
		}
		Assert.assertTrue(hasFound);
	}
}
