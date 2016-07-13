package org.area515.resinprinter.gcode;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.printer.MachineConfig;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.util.IOUtilities;
import org.area515.util.IOUtilities.ParseState;
import org.area515.util.TemplateEngine;

import freemarker.template.TemplateException;

public abstract class GCodeControl {
	public static Logger logger = LogManager.getLogger();
	public int SUGGESTED_TIMEOUT_FOR_ONE_GCODE = 1000 * 60 * 2;//2 minutes
	private Pattern GCODE_RESPONSE_PATTERN = Pattern.compile("(?i)(?:(o?k|e?rror:|a?larm:)(.*)|<?([^>]*)>|\\[?([^]]*)\\])\r?\n");
	
    private Printer printer;
    private ReentrantLock gCodeLock = new ReentrantLock();
    private StringBuilder builder = new StringBuilder();
    private int parseLocation = 0;
    
    public GCodeControl(Printer printer) {
    	this.printer = printer;
    	//Don't do this it MUST be lazy loaded!
    	//this.port = printer.getSerialPort();
    }
	
    private Printer getPrinter() {
    	return printer;
    }
    
	private PrinterResponse readUntilOkOrStoppedPrinting(Printer printer) throws IOException {
		PrinterResponse line = null;
		StringBuilder responseBuilder = new StringBuilder();
		ParseState state = null;
		Matcher matcher = null;
		do {
			state = IOUtilities.readLine(printer, getPrinter().getPrinterFirmwareSerialPort(), builder, parseLocation, SUGGESTED_TIMEOUT_FOR_ONE_GCODE, IOUtilities.CPU_LIMITING_DELAY);
			parseLocation = state.parseLocation;
			if (state.currentLine != null) {
				if (line == null) {
					line = new PrinterResponse();
					line.setFullResponse(responseBuilder);
				}
				responseBuilder.append(state.currentLine);
				matcher = GCODE_RESPONSE_PATTERN.matcher(state.currentLine);
				line.setLastLineMatcher(matcher);
			}
			
			logger.info("lineRead: {}", state.currentLine);
		} while (matcher != null && !matcher.matches());
		return line;
	}
	
	private boolean isPausableError(Matcher matcher, PrintJob printJob) {
		if (matcher.group(1) == null || !matcher.group(1).toLowerCase().endsWith("rror:")) {
			return false;
		}
		
		String responseRegEx = printJob.getPrinter().getConfiguration().getMachineConfig().getPauseOnPrinterResponseRegEx();
		return responseRegEx != null && responseRegEx.trim().length() > 0 && matcher.group(2) != null && matcher.group(2).matches(responseRegEx);
	}
	
    public String sendGcodeAndRespectPrinter(PrintJob printJob, String cmd) throws IOException {
		gCodeLock.lock();
        try {
        	if (!cmd.endsWith("\n")) {
        		cmd += "\n";
        	}
        	
        	StringBuilder builder = new StringBuilder();
        	boolean mustAttempt = true;
        	for (int attempt = 0; mustAttempt; attempt++) {
	        	logger.info("Write {}: {}", attempt, cmd);
	        	getPrinter().getPrinterFirmwareSerialPort().write(cmd.getBytes());
	        	PrinterResponse response = readUntilOkOrStoppedPrinting(printer);
	        	if (response == null) {
	        		return "";//I think this should be null, but I'm preserving backwards compatibility
	        	}
	        	
	        	if (isPausableError(response.getLastLineMatcher(), printJob)) {
	        		attempt++;
	        		printJob.setErrorDescription(response.getLastLineMatcher().group(2));
	        		printer.setStatus(JobStatus.PausedWithWarning);
	        		NotificationManager.jobChanged(printer, printJob);
	        		
	        		//Allow the user to manipulate the printer while paused
	        		gCodeLock.unlock();
	        		try {
	        			mustAttempt = printer.waitForPauseIfRequired();
	        		} finally {
	        			gCodeLock.lock();
	        		}
	        	} else {
	        		mustAttempt = false;
	        	}
	        	
	        	builder.append(response.getFullResponse().toString());
        	}
        	
        	return builder.toString();
        } finally {
        	gCodeLock.unlock();
        }
    }
    
    public String sendGcode(String cmd) {
		gCodeLock.lock();
        try {
        	if (!cmd.endsWith("\n")) {
        		cmd += "\n";
        	}
        	
        	logger.info("Write: {}", cmd);
        	getPrinter().getPrinterFirmwareSerialPort().write(cmd.getBytes());
        	PrinterResponse response = readUntilOkOrStoppedPrinting(null);
        	if (response == null) {
        		return "";
        	}
        	
        	return response.getFullResponse().toString();
        } catch (IOException ex) {
        	logger.error("Couldn't send:" + cmd, ex);
        	return "IO Problem!";
        } finally {
        	gCodeLock.unlock();
        }
    }
    
    /**
     * Unfortunately this chitchat isn't like gcode responses. Instead, the reads seem to go on forever without an indication of 
     * when they are going to stop. 
     * 
     * @return
     * @throws IOException
     */
    public String readWelcomeChitChat() throws IOException {
		try {
			StringBuilder builder = new StringBuilder();
			builder.append(IOUtilities.readWithTimeout(getPrinter().getPrinterFirmwareSerialPort(), SerialManager.TIME_OUT, SerialManager.CPU_LIMITING_DELAY));
			builder.append(executeSetAbsolutePositioning());
			return builder.toString();
		} catch (InterruptedException e) {
			return null;
		}
    }
    public String executeSetAbsolutePositioning() {
    	return sendGcode("G91\r\n");
    }
    public String executeSetRelativePositioning() {
    	return sendGcode("G91\r\n");
    }
    public String executeMoveX(double dist) {
    	return sendGcode(String.format("G1 X%1.3f\r\n", dist));
    }
    public String executeMoveY(double dist) {
    	return sendGcode(String.format("G1 Y%1.3f\r\n", dist));
    }
    public String executeMoveZ(double dist) {
    	return sendGcode(String.format("G1 Z%1.3f\r\n", dist));
    }
    public String executeMotorsOn() {
    	return sendGcode("M17\r\n");
    }
    public String executeMotorsOff() {
    	return sendGcode("M18\r\n");
    }
    public String executeXHome() {
        return sendGcode("G28 X\r\n");
    }
    public String executeYHome() {
        return sendGcode("G28 Y\r\n");
    }
    public String executeZHome() {
        return sendGcode("G28 Z\r\n");
    }
    public String executeHomeAll() {
        return sendGcode("G28\r\n");
    }
    
    public String executeGCodeWithTemplating(PrintJob printJob, String gcodes) throws InappropriateDeviceException {
		Pattern gCodePattern = Pattern.compile("\\s*([^;]+)\\s*;?(.*)", Pattern.CASE_INSENSITIVE);
		try {
			if (gcodes == null || gcodes.trim().isEmpty()) {
				throw new InappropriateDeviceException(MachineConfig.NOT_CAPABLE);
			}
			
			StringBuilder buffer = new StringBuilder();
			gcodes = TemplateEngine.buildData(printJob, printJob.getPrinter(), gcodes);
			if (gcodes == null) {
				return null;
			}
			
			for (String gcode : gcodes.split("[\r]?\n")) {
				if (!printJob.getPrinter().isPrintActive()) {
					break;
				}
				
				if (gcode != null) {
					Matcher matcher = gCodePattern.matcher(gcode);
					if (matcher.matches()) {
						buffer.append(sendGcodeAndRespectPrinter(printJob, matcher.group(1)));
					}
					if (matcher.group(2) != null) {
						Pattern delayPattern = Pattern.compile("\\s*<\\s*Delay\\s*>\\s*(\\d+)\\s*", Pattern.CASE_INSENSITIVE);
						matcher = delayPattern.matcher(matcher.group(2));
						if (matcher.matches()) {
							try {
								int sleepTime = Integer.parseInt(matcher.group(1));
								logger.info("Sleep:{}", sleepTime);
								Thread.sleep(sleepTime);
								logger.info("Sleep complete");
							} catch (InterruptedException e) {
								logger.error("Interrupted while waiting for sleep to complete.", e);
							}

						}
					}
				}
			}
			
			return buffer.toString();
		} catch (IOException | TemplateException e) {
			throw new InappropriateDeviceException(MachineConfig.NOT_CAPABLE, e);
		}
    }
}
