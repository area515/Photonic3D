package org.area515.resinprinter.job;

import java.io.IOException;

import org.area515.resinprinter.serial.SerialManager;

/// <summary>
/// This class is the go-between for sending gcode commands to the Device interface
/// This is here so delegate functions can be bound not to a GUI control for control of the printer
/// </summary>
public class ManualControl
{
    private static ManualControl m_instance = null;

    // define some rates 
    private double m_rateXY;
    private double m_rateZ;
    private double m_rateE;
    private double m_distE;
    private double m_distZ; // how far in Z to move
    private double m_distXY; // the X/Y distance to move

    private ManualControl()
    {
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
//    public double EDist
//    {
//        get { return m_distE; }
//        set { m_distE = value; Save(); }
//    }
    
    
    public static ManualControl Instance()
    {
        if (m_instance == null)
        {
            m_instance = new ManualControl();
        }
        return m_instance;

    }
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


    void SendGcode(String cmd)
    {
        try
        {
        	SerialManager.Instance().send(cmd);
//            if (DevInterface.Connected == true)
//            {
//                DevInterface.SendCommandToDevice(cmd);
//            }
        }
        catch (Exception ex)
        {
        	System.out.println(ex);
//            DebugLogger.Instance().LogError(ex);
        }
    }

    void cmd_XHome()
    {
        SendGcode("G28 X0\r\n");
    }

    void cmd_YHome()
    {
        SendGcode("G28 Y0\r\n");
    }

    void cmd_ZHome()
    {
        SendGcode("G28 Z0\r\n");
    }

    void cmd_HomeAll()
    {
        SendGcode("G28\r\n");
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
            SendGcode("U\r\n");
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
            SendGcode("J\r\n");
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

            SendGcode("O\r\n");
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
            SendGcode("C\r\n");
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
                SendGcode("Y\r\n");
            }
            if (dist == 1.0)
            { // medium reverse
                SendGcode("U\r\n");
            }
            if (dist == 10.0)
            { // large reverse
                SendGcode("I\r\n");
            }
            if (dist < -.024 && dist > -.026)
            { // small forward
                SendGcode("H\r\n");
            }
            if (dist == -1.0)
            {  // medium forward
                SendGcode("J\r\n");
            }
            if (dist == -10.0)
            {  // large forward
                SendGcode("K\r\n");
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
    public void cmdMotorsOn() throws IOException, InterruptedException
    {
        //string gcode = "M17\r\n";
        String gcode = "E\r\n";
//        UVDLPApp.Instance().m_deviceinterface.SendCommandToDevice(gcode);
        SerialManager.Instance().send(gcode);
    }

    public void cmdMotorsOff() throws IOException, InterruptedException
    {
        //string gcode = "M18\r\n";
        String gcode = "D\r\n";
//        UVDLPApp.Instance().m_deviceinterface.SendCommandToDevice(gcode);
        SerialManager.Instance().send(gcode);
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