package org.area515.resinprinter.projector;

import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

import org.area515.resinprinter.printer.MachineConfig.ComPortSettings;
import org.area515.resinprinter.serial.JSSCCommPort;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.test.HardwareCompatibilityTestSuite;
import org.junit.Assert;
import org.junit.Test;

public class DetectProjector {
	@Test
	public void noErrorsInAutodetectProjectors() {
		System.out.println("Projector json parse test.");
		HostProperties.Instance().getAutodetectProjectors();
	}
	
	@Test
	public void noErrorsDetectingProjector() {
		System.out.println("Projector detection test.");

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
				
				System.out.println("Port:" + currentIdentifier.getName() + " Baud:" + speed);
				
				SerialCommunicationsPort port = new JSSCCommPort();
				ProjectorModel model = SerialManager.Instance().getProjectorModel(port, newComPortSettings);
				if (model != null) {
					hasFound = true;
				}
				System.out.println("  JSSCCommPort projector detection:" + model);
				
				/*port = new RXTXEventBasedCommPort();
				System.out.println("  RXTXEventBasedCommPort projector detection:" + SerialManager.Instance().getProjectorModel(port, newComPortSettings, false));
				
				port = new RXTXSynchronousReadBasedCommPort();
				System.out.println("  RXTXSynchronousReadBasedCommPort projector detection:" + SerialManager.Instance().getProjectorModel(port, newComPortSettings, false));*/
			}
		}
		Assert.assertTrue(hasFound);
	}
}
