package org.area515.resinprinter.gcode;

import java.io.IOException;

import org.area515.resinprinter.printer.Printer;

/// <summary>
/// This class is the go-between for sending gcode commands to the Device interface
/// This is here so delegate functions can be bound not to a GUI control for control of the printer
/// </summary>
public class SedgwickGCodeControl extends GCodeControl {
	public SedgwickGCodeControl(Printer printer) {
		super(printer);
	}

    public String executeMoveX(double dist) {
    	return sendGcode("O\r\n");
    }

    public String executeMoveY(double dist) {
    	return sendGcode("C\r\n");
    }

    public String executeMoveZ(double dist) {
        if (dist > .024 && dist < .026) { // small reverse 
            return sendGcode("Y\r\n");
        }
        if (dist == 10.0) { // large reverse
            return sendGcode("I\r\n");
        }
        if (dist < -.024 && dist > -.026) { // small forward
            return sendGcode("H\r\n");
        }
        if (dist == -1.0) {  // medium forward
            return sendGcode("J\r\n");
        }
        if (dist == -10.0) {  // large forward
            return sendGcode("K\r\n");
        }
        //if (dist == 1.0) { // medium reverse
            return sendGcode("U\r\n");
        //}
    }

    public String executeSetRelativePositioning() {
    	return "\r\n";
    }
    
    //Sedgwick doesn't have a welcome mat
    @Override
    public String readWelcomeChitChat() throws IOException {
    	return null;
    }
    
    public String executeMotorsOn() {
    	return sendGcode("E\r\n");
    }

    public String executeMotorsOff(){
        return sendGcode("D\r\n");
    }
}