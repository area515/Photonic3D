package org.area515.resinprinter.printer;

import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Collections;

import org.area515.resinprinter.printer.MachineConfig.ComPortSettings;
import org.area515.resinprinter.serial.JSSCCommPort;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.junit.Assert;
import org.junit.Test;

public class DetectFirmware {
	@Test
	public void noErrorsDetectingFirmware() {
		System.out.println("Firmware detection test.");

		ComPortSettings newComPortSettings = new ComPortSettings();
		long[] commonSpeeds = new long[]{9600, 115200};
		boolean hasFound = false;
		for (long speed : commonSpeeds) {
			newComPortSettings.setSpeed(speed);
			newComPortSettings.setDatabits(8);
			newComPortSettings.setParity("NONE");
			newComPortSettings.setStopbits("1");
			
			ArrayList<CommPortIdentifier> identifiers = new ArrayList<CommPortIdentifier>(Collections.list(CommPortIdentifier.getPortIdentifiers()));
			for (CommPortIdentifier currentIdentifier : identifiers) {
				newComPortSettings.setPortName(currentIdentifier.getName());
				
				System.out.println("Port:" + currentIdentifier.getName() + " Baud:" + speed);
				
				SerialCommunicationsPort port = new JSSCCommPort();
				Boolean lastValue = null;
				for (int t = 0; t < 10; t++) {
					boolean found = SerialManager.Instance().is3dFirmware(port, newComPortSettings);
					System.out.println("  " + t + ". JSSCCommPort firmware detection:" + found);
					if (lastValue == null) {
						lastValue = found;
					} else {
						Assert.assertEquals((boolean)lastValue, (boolean)found);
					}
					if (found) {
						hasFound = true;
					} else {
						break;
					}
				}
				/*port = new RXTXEventBasedCommPort();
				for (int t = 0; t < 10; t++) {
					System.out.println("  " + t + ". RXTXEventBasedCommPort firmware detection:" + SerialManager.Instance().is3dFirmware(port, newComPortSettings));
				}
				
				port = new RXTXSynchronousReadBasedCommPort();
				for (int t = 0; t < 10; t++) {
					System.out.println("  " + t + ". RXTXSynchronousReadBasedCommPort firmware detection:" + SerialManager.Instance().is3dFirmware(port, newComPortSettings));
				}*/
			}
		}
		
		Assert.assertTrue(hasFound);
	}
}
