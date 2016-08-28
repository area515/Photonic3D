package org.area515.resinprinter.projector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.printer.ComPortSettings;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.server.HostProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HexCodeBasedProjector implements ProjectorModel {
    private static final Logger logger = LogManager.getLogger();
	public static final int PROJECTOR_TIMEOUT = 500;
	
	public static enum Conversion {
		BigEndian,
		LittleEndian,
		ASCII
	}
	
	public static class HexCommand {
		private String script;
		private byte[] hex;
		
		public HexCommand(String script) {
			this.script = script;
		}
		
		public HexCommand(byte[] hex) {
			this.hex = hex;
		}
		
		public byte[] buildHex() {
			if (hex != null) {
				return hex;
			}
			
			ScriptEngine engine = HostProperties.Instance().getSharedScriptEngine();
			try {
				Object value = engine.eval(script);
				if (value == null) {
					return null;
				}
				if (value instanceof byte[]) {
					return (byte[])value;
				}
				return value.toString().getBytes();
			} catch (ScriptException e) {
				logger.error("Script Error", e);
				return null;
			}
		}
		
		public String toString() {
			return script != null?script:DatatypeConverter.printHexBinary(hex);
		}
		
		public static List<HexCommand> parseHexCommands(String commandString) {
			List<HexCommand> commands = new ArrayList<HexCommand>();
			Pattern hexAndScript = Pattern.compile("([0-9a-fA-F]+)?(s*\\(.*\\)\\s*\\(\\s*\\))?");
			Matcher matcher = hexAndScript.matcher(commandString);
			while (matcher.find()) {
				if (matcher.group(1) != null) {
					commands.add(new HexCommand(DatatypeConverter.parseHexBinary(matcher.group(1))));
				}
				if (matcher.group(2) != null) {
					commands.add(new HexCommand(matcher.group(2)));
				}
			}
			
			if (matcher.end() != commandString.length()) {
				logger.error("Partial command ignored:{} in full commandString:{}", commandString.substring(matcher.end()), commandString);
			}
			
			return commands;
		}
		
		public static String formatHexCommands(List<HexCommand> commands) {
			if (commands == null || commands.isEmpty()) {
				return null;
			}
			
			StringBuilder builder = new StringBuilder();
			for (HexCommand command : commands) {
				if (command.hex != null) {
					builder.append(DatatypeConverter.printHexBinary(command.hex));
				}
				if (command.script != null) {
					builder.append(command.script);
				}
			}
			
			return builder.toString();
		}
	}
	
	@JsonIgnore
	private byte[] onHex;
	@JsonIgnore
	private byte[] offHex;
	@JsonIgnore
	private List<HexCommand> detectionHex;
	@JsonIgnore
	private Pattern detectionResponsePattern;
	@JsonIgnore
	private List<HexCommand> bulbHoursHex;
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
		if (detectionHex == null) {
			return null;
		}
		
		return HexCommand.formatHexCommands(detectionHex);
	}
	public void setDetectionHex(String detectionHex) {
		if (detectionHex == null) {
			this.detectionHex = null;
			return;
		}		
		
		this.detectionHex = HexCommand.parseHexCommands(detectionHex);
	}

	@JsonProperty
	public String getDetectionResponseRegex() {
		if (detectionResponsePattern == null) {
			return null;
		}
		
		return detectionResponsePattern.pattern();
	}
	public void setDetectionResponseRegex(String detectionResponsePattern) {
		if (detectionResponsePattern == null) {
			this.detectionResponsePattern = null;
			return;
		}
		
		this.detectionResponsePattern = Pattern.compile(detectionResponsePattern);
	}
	
	@JsonProperty
	public ComPortSettings getDefaultComPortSettings() {
		return comPortSettings;
	}
	public void setDefaultComPortSettings(ComPortSettings comPortSettings) {
		this.comPortSettings = comPortSettings;
	}
	
	@JsonProperty
	public String getBulbHoursHex() {
		if (bulbHoursHex == null) {
			return null;
		}
		
		return HexCommand.formatHexCommands(bulbHoursHex);
	}
	public void setBulbHoursHex(String bulbHoursHex) {
		if (bulbHoursHex == null) {
			this.bulbHoursHex = null;
			return;
		}
		
		this.bulbHoursHex = HexCommand.parseHexCommands(bulbHoursHex);
	}

	@JsonProperty
	public String getBulbHoursResponseRegex() {
		if (bulbHoursResponsePattern == null) {
			return null;
		}
		
		return bulbHoursResponsePattern.pattern();
	}
	public void setBulbHoursResponseRegex(String bulbHoursResponsePattern) {
		if (bulbHoursResponsePattern == null) {
			this.bulbHoursResponsePattern = null;
			return;                         
		}
		
		this.bulbHoursResponsePattern = Pattern.compile(bulbHoursResponsePattern);
	}

	@JsonProperty
	public Conversion getBulbHoursConversion() {
		return bulbHoursConversion;
	}
	public void setBulbHoursConversion(Conversion bulbHoursConversion) {
		this.bulbHoursConversion = bulbHoursConversion;
	}

	public String findString(SerialCommunicationsPort port, List<HexCommand> writeHex, Pattern responsePattern) {
		StringBuilder builder = new StringBuilder();
		try {
			for (HexCommand command : writeHex) {
				byte[] data = command.buildHex();
				if (data != null) {
					port.write(data);
				}
			}
			
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
		if (detectionHex == null || detectionResponsePattern == null) {
			return false;
		}
		
		return findString(port, detectionHex, detectionResponsePattern) != null;
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
		if (bulbResponse == null) {
			logger.info("Projector didn't return a recognized bulbHoursResponse.");
			return null;
		}
		
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
		case ASCII :
			return Integer.parseInt(bulbResponse);
		}
	}
}
