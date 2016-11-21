package org.area515.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.serial.SerialCommunicationsPort;

import com.google.common.io.ByteStreams;

public class IOUtilities {
    private static final Logger logger = LogManager.getLogger();
	public static int CPU_LIMITING_DELAY = 100;
	public static int NATIVE_COMMAND_TIMEOUT = 10000;
	
	public static enum SearchStyle {
		RepeatUntilMatch,
		RepeatWhileMatching,
		RepeatUntilMatchWithNullGroup
	}
	
	public static class ParseState {
		public int parseLocation;
		public String currentLine;
		public boolean timeout;
	}
	
	public static class ParseAction {
		public String[] command;
		public String waitForRegEx;
		public SearchStyle searchStyle;
		public String parseReturnValue;
		
		public ParseAction(String[] command, String waitForRegEx, SearchStyle searchStyle) {
			this.command = command;
			this.waitForRegEx = waitForRegEx;
			this.searchStyle = searchStyle;
		}
		public ParseAction(String[] command, String waitForRegEx, String parseReturnValue, SearchStyle searchStyle) {
			this.command = command;
			this.waitForRegEx = waitForRegEx;
			this.searchStyle = searchStyle;
			this.parseReturnValue = parseReturnValue;
		}
		
		public String toString() {
			return "Execute:" + command[0] + " " + searchStyle + " for: " + waitForRegEx + " then parse return with:" + parseReturnValue;
		}
	}
	
	public static ZipEntry zipFile(File fileToZip, ZipOutputStream zipOutputStream) {
		ZipEntry entry = new ZipEntry(fileToZip.getName());
		InputStream inStream = null;
		try {
			zipOutputStream.putNextEntry(entry);
			inStream = new BufferedInputStream(new FileInputStream(fileToZip));
			ByteStreams.copy(inStream, zipOutputStream);
			inStream.close();
			return entry;
		} catch (IOException e) {
			logger.error("Problem ziping file:" + fileToZip, e);
			return null;
		} finally {
			IOUtils.closeQuietly(inStream);
		}
	}
	
	public static ZipEntry zipStream(String name, InputStream inStream, ZipOutputStream output) {
		ZipEntry entry = new ZipEntry(name);
		try {
			output.putNextEntry(entry);
			ByteStreams.copy(inStream, output);
			inStream.close();
			return entry;
		} catch (IOException e) {
			logger.error("Problem ziping file:" + name, e);
			return null;
		} finally {
			IOUtils.closeQuietly(inStream);
		}
	}
	
	public static List<String[]> communicateWithNativeCommand(List<ParseAction> parseActions, String eolRegEx, boolean removeNewLineChar, String friendlyErrorMessage, String... arguments) {
		if (parseActions == null || parseActions.size() == 0)
			return null;
		
		StringBuilder builder = new StringBuilder();
		int parseLocation = 0;
		List<String[]> returnList = new ArrayList<String[]>();
		Process listSSIDProcess = null;
		try {
			InputStream inputStream = null;
			OutputStream outputStream = null;
			boolean firstIteration = true;
			for (ParseAction parseAction : parseActions) {
				String[] replacedCommands = new String[parseAction.command.length];
				for (int t = 0; t < parseAction.command.length; t++) {
					replacedCommands[t] = MessageFormat.format(parseAction.command[t], (Object[])arguments);
				}

				if (firstIteration) {
					listSSIDProcess = Runtime.getRuntime().exec(replacedCommands);
					inputStream = listSSIDProcess.getInputStream();
					outputStream = listSSIDProcess.getOutputStream();
					firstIteration = false;
				} else {
					outputStream.write(replacedCommands[0].getBytes());
					outputStream.flush();
				}
				
				boolean matcherGroupHasValue = false;
				Matcher matcher = null;
				Pattern pattern = Pattern.compile(parseAction.waitForRegEx);
				Pattern returnValuePattern = parseAction.parseReturnValue != null?Pattern.compile(parseAction.parseReturnValue):null;
				do {
					ParseState state = IOUtilities.readLine(inputStream, builder, eolRegEx, parseLocation, NATIVE_COMMAND_TIMEOUT, CPU_LIMITING_DELAY);
					if (state.currentLine == null) {
						return returnList;
					}
					
					if (removeNewLineChar && state.currentLine.endsWith("\n")) {
						state.currentLine = state.currentLine.substring(0, state.currentLine.length() - 1);
					}
					if (returnValuePattern != null) {
						matcher = returnValuePattern.matcher(state.currentLine);
						if (matcher.matches() && matcher.groupCount() > 0) {
							for (int group = 1; group <= matcher.groupCount(); group++) {
								String[] dest = new String[arguments.length + 1];
								System.arraycopy(arguments, 0, dest, 0, arguments.length);
								dest[arguments.length] = matcher.group(group);
								arguments = dest;
							}
							if (parseAction.searchStyle == SearchStyle.RepeatUntilMatch) {
								returnValuePattern = null;//Reset this to null so that we use our regular line parser after this point...
							}
						}
					}
					
					matcher = pattern.matcher(state.currentLine);
					if (matcher.matches() && matcher.groupCount() > 0) {
						String[] groups = new String[matcher.groupCount()];
						for (int group = 1; group <= matcher.groupCount(); group++) {
							groups[group - 1] = matcher.group(group);
							matcherGroupHasValue = groups[group -1] == null;
						}
						returnList.add(groups);
					}
					
					if (!matcher.matches() && parseAction.searchStyle == SearchStyle.RepeatUntilMatch) {
						logger.debug("UNMATCHED OUTPUT:" + state.currentLine);
					}
				} while ((parseAction.searchStyle == SearchStyle.RepeatUntilMatch && !matcher.matches()) ||
						  (parseAction.searchStyle == SearchStyle.RepeatWhileMatching && matcher.matches()) ||
						  (parseAction.searchStyle == SearchStyle.RepeatUntilMatchWithNullGroup && (matcherGroupHasValue || !matcher.matches())));
			}
			
			return returnList;
		} catch (IOException e) {
			if (friendlyErrorMessage == null) {
				logger.error("Error masked due to the friendly error message not being set", e);
				return null;
			}
			
			throw new RuntimeException(friendlyErrorMessage, e);
		} finally {
			//Don't leave straggling processes
			if (listSSIDProcess != null) {
				listSSIDProcess.destroy();
			}
		}
	}
	
	public static String readWithTimeout(SerialCommunicationsPort currentIdentifier, int timeoutMillis, int cpuLimitingDelay) throws IOException, InterruptedException {
		StringBuilder builder = new StringBuilder();
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < timeoutMillis) {
			byte[] data = currentIdentifier.read();
			if (data == null) {
				Thread.sleep(cpuLimitingDelay);
			} else {
				builder.append(new String(data));
				start = System.currentTimeMillis();
			}
		}
		
		return builder.toString();
	}
	
	public static int readWithTimeout(InputStream is, byte[] b, int timeoutMillis, int cpuLimitingDelay) throws IOException, InterruptedException  {
		int bufferOffset = 0;
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < timeoutMillis && bufferOffset < b.length) {
			int bytesAvailable = is.available();
			int readLength = java.lang.Math.min(bytesAvailable,b.length-bufferOffset);
		    int readResult = is.read(b, bufferOffset, readLength);
		    if (readResult == -1) {
		    	break;
		    }
		    
		    bufferOffset += readResult;
			if (bytesAvailable == 0) {
				Thread.sleep(cpuLimitingDelay);
			} else {
				start = System.currentTimeMillis();
			}
		}
		return bufferOffset;
	}
	
	public static ParseState readLine(Printer printer, SerialCommunicationsPort serialPort, StringBuilder builder, int parseLocation, int timeoutMillis, int cpuLimitingDelay) throws IOException {
		long startTime = System.currentTimeMillis();
		boolean workPerformed = false;
		while (true) {
			byte[] newBuffer = serialPort.read();
			if (newBuffer != null) {
				builder.append(new String(newBuffer));
			}
			
			if (builder.length() > 0) {
				workPerformed = true;
				for (; parseLocation < builder.length(); parseLocation++) {
					if (builder.charAt(parseLocation) == '\n') {
						ParseState state = new ParseState();
						state.currentLine = builder.substring(0, parseLocation + 1);
						state.parseLocation = 0;
						builder.delete(0, parseLocation + 1);
						return state;
					}
				}
			}
			
			if (System.currentTimeMillis() - startTime > timeoutMillis) {
				ParseState state = new ParseState();
				state.currentLine = null;
				state.parseLocation = parseLocation;
				state.timeout = true;
				return state;
			}
			
			if (printer != null && !printer.isPrintActive()) {
				ParseState state = new ParseState();
				state.currentLine = null;
				state.parseLocation = parseLocation;
				return state;
			}
			
			if (!workPerformed) {
				try {
					Thread.sleep(cpuLimitingDelay);
				} catch (InterruptedException e) {
					ParseState state = new ParseState();
					state.currentLine = null;
					state.parseLocation = parseLocation;
					return state;
				}
			}
		}
	}	
	
	public static ParseState readLine(InputStream stream, StringBuilder builder, String eolRegex, int parseLocation, int timeoutMillis, int cpuLimitingDelay) throws IOException {
		long startTime = System.currentTimeMillis();
		boolean workPerformed = false;
		while (true) {
			if (stream.available() > 0) {
				byte[] b = new byte[1024];
				int bytesAvailable = stream.available();
				int readLength = java.lang.Math.min(bytesAvailable, b.length);
				int bytesRead = stream.read(b, 0, readLength);
				
				if (bytesRead > 0) {
					builder.append(new String(b, 0, bytesRead));
				}
			}
			
			if (builder.length() > 0) {
				workPerformed = true;
				Pattern eolPattern = Pattern.compile(eolRegex);
				Matcher matcher = eolPattern.matcher(builder.toString());
				if (matcher.find(parseLocation)) {
					ParseState state = new ParseState();
					state.currentLine = builder.substring(0, matcher.end());
					state.parseLocation = 0;
					builder.delete(0, matcher.end());
					return state;
				} else {
					parseLocation = builder.length();
				}
			}
			
			if (System.currentTimeMillis() - startTime > timeoutMillis) {
				ParseState state = new ParseState();
				state.currentLine = null;
				state.parseLocation = parseLocation;
				return state;
			}
			
			if (!workPerformed) {
				try {
					Thread.sleep(cpuLimitingDelay);
				} catch (InterruptedException e) {
					ParseState state = new ParseState();
					state.currentLine = null;
					state.parseLocation = parseLocation;
					return state;
				}
			}
		}
	}

	public static String[] executeNativeCommand(String[] commands, String friendlyErrorMessage, String... arguments) throws RuntimeException {
		if (commands == null || commands.length == 0)
			return new String[]{};
		
		Process listSSIDProcess = null;
		try {
			String[] replacedCommands = new String[commands.length];
			for (int t = 0; t < commands.length; t++) {
				replacedCommands[t] = MessageFormat.format(commands[t], (Object[])arguments);
			}
			listSSIDProcess = Runtime.getRuntime().exec(replacedCommands);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			IOUtils.copy(listSSIDProcess.getInputStream(), output);
			return new String(output.toString()).split("\r?\n");
		} catch (IOException e) {
			if (friendlyErrorMessage == null) {
				logger.error("Error masked due to the friendly error message not being set", e);
				return new String[]{};
			}
			
			throw new RuntimeException(friendlyErrorMessage, e);
		} finally {
			if (listSSIDProcess != null) {
				listSSIDProcess.destroy();
			}
		}
	}
}
