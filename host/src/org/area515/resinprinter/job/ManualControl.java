package org.area515.resinprinter.job;

import java.io.IOException;

import org.area515.resinprinter.serial.SerialManager;

/// <summary>
/// This class is the go-between for sending gcode commands to the Device interface
/// This is here so delegate functions can be bound not to a GUI control for control of the printer
/// </summary>
public class ManualControl {

    // define some rates 
    private double m_rateXY;
    private double m_rateZ;
    private double m_rateE;
    private double m_distE;
    private double m_distZ; // how far in Z to move
    private double m_distXY; // the X/Y distance to move
    
    private PrintJob printJob;
    
    public ManualControl(PrintJob printJob) {
    	this.printJob = printJob;
    	
        m_rateXY = 200;
        m_rateZ = 200;
        m_rateE = 10;
        m_distE = 20;
        m_distZ = 10;
        m_distXY = 10;
        
//        Load(); // load the current values
//        RegisterCallbacks();
    }

//    private DeviceInterface DevInterface
//    {
//        get { return UVDLPApp.Instance().m_deviceinterface; }
//    }

    public double getXYRate(){return m_rateXY;}
    public void setXYRate(double rateXY){this.m_rateXY = rateXY;}
    
//    public double XYRate
//    {
//        get { return m_rateXY; }
//        set { m_rateXY = value; Save(); }
//    }
    
    
    public double getXYDist(){return m_distXY;}
    public void setXYDist(double distXY){this.m_distXY = distXY;}
//    public double XYDist
//    {
//        get { return m_distXY; }
//        set { m_distXY = value; Save(); }
//    }
    
    
    public double getZRate(){return m_rateZ;}
    public void setZRate(double rateZ){this.m_rateZ=rateZ;}
//    public double ZRate
//    {
//        get { return m_rateZ; }
//        set { m_rateZ = value; Save(); }
//    }
    
    
    public double getZDist(){return m_distZ;}
    public void setZDist(double zDist){this.m_distZ = zDist;}
//    public double ZDist
//    {
//        get { return m_distZ; }
//        set { m_distZ = value; Save(); }
//    }
    
    
    public double getERate(){return m_rateE;}
    public void setERate(double rateE){this.m_rateE = rateE;}
//    public double ERate
//    {
//        get { return m_rateE; }
//        set { m_rateE = value; Save(); }
//    }
    
    public double getEDist(){return m_distE;}
    public void setEDist(double distE){this.m_distE = distE;}
    
//    public bool Load()
//    {
//        return false;
//    }
//    public bool Save()
//    {
//        return false;
//    }
//    void RegisterCallbacks()
//    {
//        CallbackHandler cb = UVDLPApp.Instance().m_callbackhandler;
//        cb.RegisterCallback("MCCmdSetZDist", SetZdist, typeof(double), "Set distanse (zdist) in mm for manual up/down movement");
//        cb.RegisterCallback("MCCmdSetZRate", SetZrate, typeof(double), "Set rate in mm/m for manual up/down movement");
//        cb.RegisterCallback("MCCmdSetXYRate", SetXYrate, typeof(double), "Set rate in mm/m for manual left/right/front/back movement");
//        cb.RegisterCallback("MCCmdMoveUp", cmdUp_Click, null, "Move print head up zdist amount");
//        cb.RegisterCallback("MCCmdMoveDown", cmdDown_Click, null, "Move print head down zdist amount");
//        cb.RegisterCallback("MCCmdMoveX", cmdMoveX, typeof(double), "Move the X-axis specified amount");
//        cb.RegisterCallback("MCCmdXHome", cmd_XHome, null, "Move the X-axis to the home position");
//        cb.RegisterCallback("MCCmdMoveY", cmdMoveY, typeof(double), "Move the Y-axis specified amount");
//        cb.RegisterCallback("MCCmdYHome", cmd_YHome, null, "Move the Y-axis to the home position");
//        cb.RegisterCallback("MCCmdMoveZ", cmdMoveZ, typeof(double), "Move the Z-axis specified amount");
//        cb.RegisterCallback("MCCmdExtrude", cmdMoveE, typeof(double), "Move the E-axis specified amount");
//        cb.RegisterCallback("MCCmdZHome", cmd_ZHome, null, "Move the Z-axis to the home position");
//        cb.RegisterCallback("MCCmdAllHome", cmd_HomeAll, null, "Move all axis to the home position");
//        cb.RegisterCallback("MCCmdMotorOn", cmdMotorsOn, null, "Turn motors ON");
//        cb.RegisterCallback("MCCmdMotorOff", cmdMotorsOff, null, "Turn motors OFF");
//        cb.RegisterRetCallback("MCCmdGetZRate", cmdGetZRate, null, typeof(double), "Get Z-axis movement rate");
//        cb.RegisterRetCallback("MCCmdGetXYRate", cmdGetXYRate, null, typeof(double), "Get XY-axis movement rate");
//        //cb.RegisterCallback("", , null, "");
//    }


    private void sendGcode(String cmd) {
        try {
        	printJob.sendAndWaitForResponse(cmd);
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
    }

    void cmd_XHome()
    {
        sendGcode("G28 X0\r\n");
    }

    void cmd_YHome()
    {
        sendGcode("G28 Y0\r\n");
    }

    void cmd_ZHome()
    {
        sendGcode("G28 Z0\r\n");
    }

    void cmd_HomeAll()
    {
        sendGcode("G28\r\n");
    }


    void SetZdist(Object vars)
    {
        try
        {
            double dist = (double)vars;
            m_distZ = dist;
        }
        catch (Exception ex)
        {
        	System.out.println(ex);
//            DebugLogger.Instance().LogError(ex);
        }
    }

    void SetZrate(Object vars)
    {
        try
        {
            double rate = (double)vars;
            m_rateZ = rate;
        }
        catch (Exception ex)
        {
        	System.out.println(ex);
//            DebugLogger.Instance().LogError(ex);
        }
    }

    void SetXYrate(Object vars)
    {
        try
        {
            double rate = (double)vars;
            m_rateXY = rate;
        }
        catch (Exception ex)
        {
        	System.out.println(ex);
//            DebugLogger.Instance().LogError(ex);
        }
    }

    public void cmdUp_Click()
    {
        try
        {
            //double dist = double.Parse(txtdist.Text);
            //DevInterface.Move(m_distZ, m_rateZ); // (movecommand); MODIFIED //SO
            sendGcode("U\r\n");
        }
        catch (Exception ex)
        {
//            DebugLogger.Instance().LogRecord(ex.Message);
        }
    }
    /// <summary>
    /// Z Axis Down
    /// </summary>
    /// <param name="sender"></param>
    /// <param name="e"></param>
    public void cmdDown_Click()
    {
        try
        {
            //m_distZ *= -1.0;
            //DevInterface.Move(m_distZ * -1.0d, m_rateZ); // (movecommand);
            sendGcode("J\r\n");
        }
        catch (Exception ex)
        {
//            DebugLogger.Instance().LogRecord(ex.Message);
        }
    }

    public void cmdMoveX()
    {
        try
        {

            sendGcode("O\r\n");
            //double dist = (double)e;
            //DevInterface.MoveX(dist, m_rateXY); // (movecommand);
        }
        catch (Exception ex)
        {
//            DebugLogger.Instance().LogRecord(ex.Message);
        }
    }

    public void cmdMoveY()
    {
        try
        {
            sendGcode("C\r\n");
            //double dist = (double)e;
            //DevInterface.MoveY(dist, m_rateXY); // (movecommand);
        }
        catch (Exception ex)
        {
//            DebugLogger.Instance().LogRecord(ex.Message);
        }
    }

    public void cmdMoveZ(double dist)
    {
        try
        {
//            double dist = (double)e;
            if (dist > .024 && dist < .026)
            { // small reverse 
                sendGcode("Y\r\n");
            }
            if (dist == 1.0)
            { // medium reverse
                sendGcode("U\r\n");
            }
            if (dist == 10.0)
            { // large reverse
                sendGcode("I\r\n");
            }
            if (dist < -.024 && dist > -.026)
            { // small forward
                sendGcode("H\r\n");
            }
            if (dist == -1.0)
            {  // medium forward
                sendGcode("J\r\n");
            }
            if (dist == -10.0)
            {  // large forward
                sendGcode("K\r\n");
            }


            //DevInterface.Move(dist, m_rateZ); // (movecommand);
        }
        catch (Exception ex)
        {
//            DebugLogger.Instance().LogRecord(ex.Message);
        }
    }
//    private void cmdMoveE(object sender, object e)
//    {
//        try
//        {
//            double dist = (double)e;
//            DevInterface.MoveE(dist, m_rateE); // (movecommand);
//        }
//        catch (Exception ex)
//        {
//            DebugLogger.Instance().LogRecord(ex.Message);
//        }
//    }
    public void cmdMotorsOn()    {
    	sendGcode("E\r\n");
    }

    public void cmdMotorsOff()
    {
        sendGcode("D\r\n");
    }


    public double cmdGetZRate()
    {
        return m_rateZ;
    }

    public double cmdGetXYRate()
    {
        return m_rateXY;
    }
}