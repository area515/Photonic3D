package org.area515.resinprinter.projector;

import gnu.io.CommPortIdentifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.area515.resinprinter.printer.MachineConfig.ComPortSettings;
import org.area515.resinprinter.serial.JSSCCommPort;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.test.HardwareCompatibilityTestSuite;

public class ProjectorOutput {
	public static void main(String[] args) throws Exception {
		BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Projector Output Test.");
		int index = 0;
		List<ProjectorModel> models = HostProperties.Instance().getAutodetectProjectors();
		for (ProjectorModel model : models) {
			System.out.println(index++ + ". " + model.getName());
		}
		System.out.println("Type the number of the projector that you would like to test (then press enter):");
		HexCodeBasedProjector projector = (HexCodeBasedProjector)models.get(Integer.parseInt(inputStream.readLine()));
		
		index = 0;
		List<CommPortIdentifier> ports = new ArrayList<CommPortIdentifier>(Collections.list(CommPortIdentifier.getPortIdentifiers()));
		for (CommPortIdentifier port : ports) {
			System.out.println(index++ + ". " + port.getName());
		}
		System.out.println("Type the number of the Serial Port that you would like to test (then press enter):");
		CommPortIdentifier serialPort = (CommPortIdentifier)ports.get(Integer.parseInt(inputStream.readLine()));

		index = 0;
		for (long speed : HardwareCompatibilityTestSuite.COMMON_SPEEDS) {
			System.out.println(index++ + ". " + speed);
		}
		System.out.println("Type the number of the speed that you would like to test (then press enter):");
		long speed = HardwareCompatibilityTestSuite.COMMON_SPEEDS[Integer.parseInt(inputStream.readLine())];

		System.out.println("0. On Hex");
		System.out.println("1. Off Hex");
		System.out.println("2. Detection Hex");
		System.out.println("Type the number of the hexcode that you would like to test (then press enter):");
		int hex = Integer.parseInt(inputStream.readLine());

		ComPortSettings newComPortSettings = new ComPortSettings();
		newComPortSettings.setSpeed(speed);
		newComPortSettings.setDatabits(8);
		newComPortSettings.setParity("NONE");
		newComPortSettings.setStopbits("1");
		newComPortSettings.setPortName(serialPort.getName());

		SerialCommunicationsPort port = new JSSCCommPort();
		port.open("ProjectorOutputTest", SerialManager.TIME_OUT, newComPortSettings);
		switch (hex) {
		case 0:
			System.out.println(projector.testCodeAgainstPattern(port, projector.getOnHex()));
			break;
		case 1:
			System.out.println(projector.testCodeAgainstPattern(port, projector.getOffHex()));
			break;
		case 2:
			System.out.println(projector.testCodeAgainstPattern(port, projector.getDetectionHex()));
			break;
		}
		port.close();
	}
}
