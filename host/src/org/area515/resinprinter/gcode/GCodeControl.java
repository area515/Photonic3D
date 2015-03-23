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
import org.area515.util.TemplateEngine;

import freemarker.template.TemplateException;

public abstract class GCodeControl {
    private Printer printer;
    private SerialCommunicationsPort port;
    private ReentrantLock gCodeLock = new ReentrantLock();
    
    public GCodeControl(Printer printer) {
    	this.printer = printer;
    	//Don't do this it MUST be lazy loaded!
    	//this.port = printer.getSerialPort();
    }
	
    private SerialCommunicationsPort getSerialPort() {
    	if (port == null) {
    		port = printer.getSerialPort();
    	}
    	
    	return port;
    }
    public String sendGcode(String cmd) {
		gCodeLock.lock();
        try {
        	if (!cmd.endsWith("\n")) {
        		cmd += "\n";
        	}
        	
        	getSerialPort().write(cmd);
        	return getSerialPort().readUntilOkOrStoppedPrinting(null);
        } catch (IOException ex) {
        	ex.printStackTrace();
        	return "IO Problem!";
        } finally {
        	gCodeLock.unlock();
        }
    }
    
    public String sendGcodeReturnIfPrinterStops(String cmd) throws IOException {
		gCodeLock.lock();
        try {
        	if (!cmd.endsWith("\n")) {
        		cmd += "\n";
        	}
        	
        	getSerialPort().write(cmd);
        	return getSerialPort().readUntilOkOrStoppedPrinting(printer);
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
    	return executeSetAbsolutePositioning();
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
