package org.area515.resinprinter.printer;

import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Collections;

import org.area515.resinprinter.printer.MachineConfig.ComPortSettings;
import org.area515.resinprinter.serial.JSSCCommPort;
import org.area515.resinprinter.serial.RXTXEventBasedCommPort;
import org.area515.resinprinter.serial.RXTXSynchronousReadBasedCommPort;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.junit.Test;

public class DetectFirmware {
	@Test
	public void noErrorsDetectingFirmware() {
		System.out.println("Firmware detection test");

		ComPortSettings newComPortSettings = new ComPortSettings();
		newComPortSettings.setSpeed(115200);
		newComPortSettings.setDatabits(8);
		newComPortSettings.setParity("NONE");
		newComPortSettings.setStopbits("1");
		
		ArrayList<CommPortIdentifier> identifiers = new ArrayList<CommPortIdentifier>(Collections.list(CommPortIdentifier.getPortIdentifiers()));
		for (CommPortIdentifier currentIdentifier : identifiers) {
			newComPortSettings.setPortName(currentIdentifier.getName());
			
			System.out.println("Port:" + currentIdentifier.getName());
			
			SerialCommunicationsPort port = new JSSCCommPort();
			System.out.println("  JSSCCommPort firmware detection:" + SerialManager.Instance().is3dFirmware(port, newComPortSettings));
			
			port = new RXTXEventBasedCommPort();
			System.out.println("  RXTXEventBasedCommPort firmware detection:" + SerialManager.Instance().is3dFirmware(port, newComPortSettings));
			
			port = new RXTXSynchronousReadBasedCommPort();
			System.out.println("  RXTXSynchronousReadBasedCommPort firmware detection:" + SerialManager.Instance().is3dFirmware(port, newComPortSettings));
		}
	}
}
