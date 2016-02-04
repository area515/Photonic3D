package org.area515.resinprinter.projector;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.printer.ComPortSettings;
import org.area515.resinprinter.serial.SerialCommunicationsPort;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HexCodeBasedProjector implements ProjectorModel {
    private static final Logger logger = LogManager.getLogger();
	private static final int PROJECTOR_TIMEOUT = 5000;
	
	public static enum Conversion {
		BigEndian,
		LittleEndian
	}
	
	@JsonIgnore
	private byte[] onHex;
	@JsonIgnore
	private byte[] offHex;
	@JsonIgnore
	private byte[] detectionHex;
	@JsonIgnore
	private Pattern detectionResponsePattern;
	@JsonIgnore
	private byte[] bulbHoursHex;
	@JsonIgnore
	private Pattern bulbHoursResponsePattern;
	@JsonIgnore
	private Conversion bulbHoursConversion;
	@JsonIgnore
	private String name;
	@JsonIgnore
	private ComPortSettings comPortSettings;
	
	public HexCodeBasedProjector() {
	}

	@JsonProperty
	public String getOnHex() {
		return DatatypeConverter.printHexBinary(onHex);
	}
	public void setOnHex(String onHex) {
		this.onHex = DatatypeConverter.parseHexBinary(onHex);
	}

	@JsonProperty
	public String getOffHex() {
		return DatatypeConverter.printHexBinary(offHex);
	}
	public void setOffHex(String offHex) {
		this.offHex = DatatypeConverter.parseHexBinary(offHex);
	}
	
	@JsonProperty
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@JsonProperty
	public String getDetectionHex() {
		return DatatypeConverter.printHexBinary(detectionHex);
	}
	public void setDetectionHex(String detectionHex) {
		this.detectionHex = DatatypeConverter.parseHexBinary(detectionHex);
	}

	@JsonProperty
	public String getDetectionResponseRegex() {
		return detectionResponsePattern.pattern();
	}
	public void setDetectionResponseRegex(String detectionResponsePattern) {
		this.detectionResponsePattern = Pattern.compile(detectionResponsePattern);
	}
	
	@JsonProperty
	public ComPortSettings getComPortSettings() {
		return comPortSettings;
	}
	public void setComPortSettings(ComPortSettings comPortSettings) {
		this.comPortSettings = comPortSettings;
	}
	
	@JsonProperty
	public String getBulbHoursHex() {
		return DatatypeConverter.printHexBinary(bulbHoursHex);
	}
	public void setBulbHoursHex(String bulbHoursHex) {
		this.bulbHoursHex = DatatypeConverter.parseHexBinary(bulbHoursHex);
	}

	@JsonProperty
	public String geBulbHoursResponseRegex() {
		return bulbHoursResponsePattern.pattern();
	}
	public void setBulbHoursResponseRegex(String bulbHoursResponsePattern) {
		this.bulbHoursResponsePattern = Pattern.compile(bulbHoursResponsePattern);
	}

	@JsonProperty
	public Conversion getBulbHoursConversion() {
		return bulbHoursConversion;
	}
	public void setBulbHoursConversion(Conversion bulbHoursConversion) {
		this.bulbHoursConversion = bulbHoursConversion;
	}

	public String findString(SerialCommunicationsPort port, byte[] writeHex, Pattern responsePattern) {
		StringBuilder builder = new StringBuilder();
		try {
			port.write(writeHex);
			long start = System.currentTimeMillis();
			while (true) {
				byte[] response = port.read();
				if (response != null) {
					builder.append(new String(response));
					Matcher matcher = responsePattern.matcher(builder.toString());
					if (matcher.matches()) {
						return matcher.group(matcher.groupCount());
					}
				}
				
				if (System.currentTimeMillis() - start >= PROJECTOR_TIMEOUT) {
					logger.debug("Timeout after bytes read \"{}\"", DatatypeConverter.printHexBinary(builder.toString().getBytes()));
					return null;
				}
			}
		} catch (IOException e) {
			logger.error("Error after bytes read \"" + DatatypeConverter.printHexBinary(builder.toString().getBytes()) + "\"", e);
			return null;
		}
	}
	
	@Override
	public boolean autodetect(SerialCommunicationsPort port) {
		return findString(port, detectionHex, detectionResponsePattern) != null;
	}
	
	public String testCodeAgainstPattern(SerialCommunicationsPort port, String hexCode) throws IOException {
		logger.info("Writing:{}", hexCode);
		port.write(DatatypeConverter.parseHexBinary(hexCode));
		long start = System.currentTimeMillis();
		StringBuilder builder = new StringBuilder();
		while (true) {
			byte[] response = port.read();
			if (response != null) {
				builder.append(new String(response));
				
				if (detectionResponsePattern.matcher(builder.toString()).matches()) {
					return "Match:(" + DatatypeConverter.printHexBinary(builder.toString().getBytes()) + ") against: " + detectionResponsePattern.pattern();
				}
			}
			
			if (System.currentTimeMillis() - start >= PROJECTOR_TIMEOUT) {
				return "No Match:(" + DatatypeConverter.printHexBinary(builder.toString().getBytes()) + ") against: " + detectionResponsePattern.pattern();
			}
		}
	}

	@Override
	public void setPowerState(boolean state, SerialCommunicationsPort port) throws IOException {
		if (state) {
			port.write(onHex);
		} else {
			port.write(offHex);
		}
	}

	@Override
	public boolean getPowerState(SerialCommunicationsPort port) throws IOException {
		throw new IOException("This feature isn't implemented yet");
	}

	public String toString() {
		return name;
	}

	@Override
	public Integer getBulbHours(SerialCommunicationsPort port) throws IOException {
		if (bulbHoursHex == null || bulbHoursResponsePattern == null) {
			return null;
		}
		
		int hours = 0;
		byte[] bytes = null;
		String bulbResponse = findString(port, bulbHoursHex, bulbHoursResponsePattern);
		switch (bulbHoursConversion == null?Conversion.LittleEndian:bulbHoursConversion) {
		case BigEndian:
			bytes = bulbResponse.getBytes();
			for (int power = 0; power < bytes.length; power++) {
				hours += bytes[bytes.length - power - 1] << (8*power);
			}
			return hours;
		default :
		case LittleEndian :
			bytes = bulbResponse.getBytes();
			for (int power = 0; power < bytes.length; power++) {
				hours += bytes[power] << (8*power);
			}
			return hours;
		}
	}
}
