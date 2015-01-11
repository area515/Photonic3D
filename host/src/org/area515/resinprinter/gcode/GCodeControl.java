package org.area515.resinprinter.gcode;

import java.io.IOException;

import org.area515.resinprinter.printer.Printer;

public abstract class GCodeControl {
    private Printer printer;
    
    public GCodeControl(Printer printer) {
    	this.printer = printer;
    }
	
    String sendGcode(String cmd) {
        try {
        	return printer.sendGCodeAndWaitForResponseForever(cmd);
        } catch (InterruptedException ex) {
        	ex.printStackTrace();
        	return "Interrupted!";
        }
    }
    
    /**
     * Unfortunately the welcome mat isn't like gcode responses. Instead, the reads seem to go on forever without an indication of 
     * when they are going to stop. I'm hoping our read timeout is long enough to cover the time that it takes to dump the welcome mat.
     * 
     * @return
     * @throws IOException
     */
    public String readWelcome() throws IOException {
    	StringBuilder builder = new StringBuilder();
    	String currentLine = null;
    	while ((currentLine = printer.readLine(false)) != null) {
    		if (currentLine != null) {
        		builder.append(currentLine);
    		}
    	}
    	
    	return builder.toString();
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
}
