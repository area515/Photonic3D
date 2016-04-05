package org.area515.resinprinter.projector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.area515.resinprinter.serial.JSSCCommPort;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.server.HostProperties;

public class ProjectorOutput {
    public static String convertHexString(String data) {
		Pattern pattern = Pattern.compile("0[xX]([1234567890a-fA-F]{2})\\s*");
		StringBuilder builder = new StringBuilder();
		Matcher matcher = pattern.matcher(data);
		while (matcher.find()) {
			builder.append(matcher.group(1));
		}
		return builder.toString();
    }

    public static String getCustomString(BufferedReader reader) throws IOException {
		System.out.println("Enter custom hex string in the form \"0x00 0x00 0x00\" (then press enter):");
		String data = reader.readLine();
		return convertHexString(data);
	}
	
	public static String testCodeAgainstPattern(SerialCommunicationsPort port, String hexCode, Pattern pattern) throws IOException {
		System.out.println("Writing:" + hexCode);
		port.write(DatatypeConverter.parseHexBinary(hexCode));
		long start = System.currentTimeMillis();
		StringBuilder builder = new StringBuilder();
		while (true) {
			byte[] response = port.read();
			if (response != null) {
				builder.append(new String(response));
				
				if (pattern != null) {
					Matcher matcher = pattern.matcher(builder.toString());
					if (matcher.matches()) {
						StringBuilder returnBuilder = new StringBuilder();
						returnBuilder.append("Match:");
						
						for (int t = 0; t <= matcher.groupCount(); t++) {
							returnBuilder.append("\nGroup ");
							returnBuilder.append(t);
							returnBuilder.append(" Hex:");
							if (matcher.group(t) == null) {
								returnBuilder.append("null");
							} else {
								returnBuilder.append(DatatypeConverter.printHexBinary(matcher.group(t).getBytes()));
							}
							returnBuilder.append("\nGroup ");
							returnBuilder.append(t);
							returnBuilder.append(" ASCII:");
							returnBuilder.append(matcher.group(t));
						}
						returnBuilder.append("\nAgainst:");
						returnBuilder.append(pattern.pattern());
						return returnBuilder.toString();
					}
				}
			}
			
			if (System.currentTimeMillis() - start >= HexCodeBasedProjector.PROJECTOR_TIMEOUT) {
				return "No Match\n"
						+ " Hex:" + DatatypeConverter.printHexBinary(builder.toString().getBytes()) + "\n"
						+ " ASCII:" + builder.toString().getBytes() + "\n"
						+ "Against:" + (pattern != null?pattern.pattern():null);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		//System.out.println(getCustomString(reader));
		System.out.println("Projector Output Test.");
		int portIndex = 0;
		String[] identifiers = SerialManager.Instance().getPortNames();
		for (String port : identifiers) {
			System.out.println(portIndex++ + ". " + port);
		}
		System.out.println("Type the number of the Serial Port that you would like to test (then press enter):");
		portIndex = Integer.parseInt(reader.readLine());
		
		int index = 0;
		List<ProjectorModel> models = HostProperties.Instance().getAutodetectProjectors();
		for (ProjectorModel model : models) {
			System.out.println(index++ + ". " + model.getName());
		}
		System.out.println("Type the number of the projector that you would like to test (then press enter):");
		HexCodeBasedProjector projector = (HexCodeBasedProjector)models.get(Integer.parseInt(reader.readLine()));
		projector.getDefaultComPortSettings().setPortName(identifiers[portIndex]);
		
		SerialCommunicationsPort port = new JSSCCommPort();
		port.open("ProjectorOutputTest", SerialManager.TIME_OUT, projector.getDefaultComPortSettings());

		while (true) {
			System.out.println("0. On Hex");
			System.out.println("1. Off Hex");
			System.out.println("2. Detection Hex");
			System.out.println("3. Bulb hours Hex");
			System.out.println("4. Custom (0x00 0x00...)");
			
			System.out.println("Type the number of the hexcode that you would like to test (then press enter):");
			int hex = Integer.parseInt(reader.readLine());
			
			switch (hex) {
			case 0:
				System.out.println(testCodeAgainstPattern(port, projector.getOnHex(), null));
				break;
			case 1:
				System.out.println(testCodeAgainstPattern(port, projector.getOffHex(), null));
				break;
			case 2:
				System.out.println(testCodeAgainstPattern(port, projector.getDetectionHex(), Pattern.compile(projector.getDetectionResponseRegex())));
				break;
			case 3:
				System.out.println(testCodeAgainstPattern(port, projector.getBulbHoursHex(), Pattern.compile(projector.getBulbHoursResponseRegex())));
				break;
			case 4:
				System.out.println(testCodeAgainstPattern(port, getCustomString(reader), null));
				break;
			}
		}
	}
}
