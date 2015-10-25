package org.area515.resinprinter.gcode;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.MachineConfig;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.util.IOUtilities;
import org.area515.util.IOUtilities.ParseState;
import org.area515.util.TemplateEngine;

import freemarker.template.TemplateException;

public abstract class GCodeControl {
	public int SUGGESTED_TIMEOUT_FOR_ONE_GCODE = 1000 * 60 * 2;//2 minutes
	
    private Printer printer;
    private ReentrantLock gCodeLock = new ReentrantLock();
    private StringBuilder builder = new StringBuilder();
    private int parseLocation = 0;
    
    public GCodeControl(Printer printer) {
    	this.printer = printer;
    	//Don't do this it MUST be lazy loaded!
    	//this.port = printer.getSerialPort();
    }
	
    private SerialCommunicationsPort getSerialPort() {
    	return printer.getPrinterFirmwareSerialPort();
    }
	
	private String readUntilOkOrStoppedPrinting(Printer printer) throws IOException {
		StringBuilder responseBuilder = new StringBuilder();
		ParseState state = null;
		do {
			state = IOUtilities.readLine(printer, getSerialPort(), builder, parseLocation, SUGGESTED_TIMEOUT_FOR_ONE_GCODE, IOUtilities.CPU_LIMITING_DELAY);
			parseLocation = state.parseLocation;
			
			if (state.currentLine != null) {
				responseBuilder.append(state.currentLine);
			}
			
			System.out.println("lineRead:" + state.currentLine);
		} while (state.currentLine != null && !state.currentLine.matches("(?is:ok.*)"));
		
		return responseBuilder.toString();
	}

    public String sendGcodeReturnIfPrinterStops(String cmd) throws IOException {
		gCodeLock.lock();
        try {
        	if (!cmd.endsWith("\n")) {
        		cmd += "\n";
        	}
        	
        	getSerialPort().write(cmd.getBytes());
        	return readUntilOkOrStoppedPrinting(printer);
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
        	
        	getSerialPort().write(cmd.getBytes());
        	return readUntilOkOrStoppedPrinting(null);
        } catch (IOException ex) {
        	ex.printStackTrace();
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
			builder.append(IOUtilities.readWithTimeout(getSerialPort(), SerialManager.TIME_OUT, SerialManager.CPU_LIMITING_DELAY));
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
    
    public void executeGCodeWithTemplating(PrintJob printJob, String gcodes) throws InappropriateDeviceException {
		Pattern gCodePattern = Pattern.compile("\\s*([^;]+)\\s*;?.*", Pattern.CASE_INSENSITIVE);
		try {
			if (gcodes == null || gcodes.trim().isEmpty()) {
				throw new InappropriateDeviceException(MachineConfig.NOT_CAPABLE);
			}
			
			for (String gcode : gcodes.split("[\r]?\n")) {
				gcode = TemplateEngine.buildData(printJob, printer, gcode);
				Matcher matcher = gCodePattern.matcher(gcode);
				if (matcher.matches()) {
					sendGcode(matcher.group(1));
				}
			}
			
		} catch (IOException | TemplateException e) {
			throw new InappropriateDeviceException(MachineConfig.NOT_CAPABLE, e);
		}
    }
}
