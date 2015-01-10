package org.area515.resinprinter.gcode;

import org.area515.resinprinter.printer.Printer;

/// <summary>
/// This class is the go-between for sending gcode commands to the Device interface
/// This is here so delegate functions can be bound not to a GUI control for control of the printer
/// </summary>
public class SedgwickGCodeControl extends GCodeControl {
	public SedgwickGCodeControl(Printer printer) {
		super(printer);
	}

    public void cmdMoveX(double dist) {
    	sendGcode("O\r\n");
    }

    public void cmdMoveY(double dist) {
    	sendGcode("C\r\n");
    }

    public void cmdMoveZ(double dist) {
        if (dist > .024 && dist < .026) { // small reverse 
            sendGcode("Y\r\n");
        }
        if (dist == 1.0) { // medium reverse
            sendGcode("U\r\n");
        }
        if (dist == 10.0) { // large reverse
            sendGcode("I\r\n");
        }
        if (dist < -.024 && dist > -.026) { // small forward
            sendGcode("H\r\n");
        }
        if (dist == -1.0) {  // medium forward
            sendGcode("J\r\n");
        }
        if (dist == -10.0) {  // large forward
            sendGcode("K\r\n");
        }
    }

    public void cmdMotorsOn() {
    	sendGcode("E\r\n");
    }

    public void cmdMotorsOff(){
        sendGcode("D\r\n");
    }
}