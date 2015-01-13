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
        } catch (Exception ex) {
        	ex.printStackTrace();
        	return "Interrupted!";
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
    	StringBuilder builder = new StringBuilder();
    	String currentLine = null;
    	while ((currentLine = printer.readLine(false)) != null) {
    		if (currentLine != null) {
        		builder.append(currentLine);
    		}
    	}
    	
    	String g90Response = null;
    	int g90ResponsesSent = 0;
    	while (g90Response == null || !g90Response.matches("[Oo][Kk].*")) {
    		if (g90Response != null) {
    			builder.append(g90Response);
    		}
    		
    		//This sets the device into absolute positioning mode which is the default anyway
	    	g90Response = sendGcode("G90\r\n");
	    	g90ResponsesSent++;
    	}
    	
    	//We start at 1 because we should have already gotten one good response in the above loop
    	for (int t = 1; t < g90ResponsesSent; t++) {
    		printer.readLine(false);
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
