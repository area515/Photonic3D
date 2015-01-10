package org.area515.resinprinter.gcode;

import org.area515.resinprinter.printer.Printer;

public abstract class GCodeControl {
    private Printer printer;
    
    public GCodeControl(Printer printer) {
    	this.printer = printer;
    }
	
    void sendGcode(String cmd) {
        try {
        	printer.sendAndWaitForResponse(cmd);
        } catch (InterruptedException ex) {
        	ex.printStackTrace();
        }
    }

    public void cmdMoveX(double dist) {
    	sendGcode(String.format("G1 X%1.3f\r\n", dist));
    }
    public void cmdMoveY(double dist) {
    	sendGcode(String.format("G1 Y%1.3f\r\n", dist));
    }
    public void cmdMoveZ(double dist) {
    	sendGcode(String.format("G1 Z%1.3f\r\n", dist));
    }
    public void cmdMotorsOn() {
    	sendGcode("M17\r\n");
    }
    public void cmdMotorsOff() {
    	sendGcode("M18\r\n");
    }
    public void cmd_XHome() {
        sendGcode("G28 X\r\n");
    }
    public void cmd_YHome() {
        sendGcode("G28 Y\r\n");
    }
    public void cmd_ZHome() {
        sendGcode("G28 Z\r\n");
    }
    public void cmd_HomeAll() {
        sendGcode("G28\r\n");
    }
}
