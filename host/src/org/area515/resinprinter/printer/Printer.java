package org.area515.resinprinter.printer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;
import javax.xml.bind.annotation.XmlTransient;

import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.gcode.GCodeControl;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.projector.ProjectorModel;
import org.area515.resinprinter.serial.SerialCommunicationsPort;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Printer {
	private PrinterConfiguration configuration;
	
	//For Display
	private Frame refreshFrame;
	private DisplayState displayState = DisplayState.Blank;
	private int calibrationSquareSize;
	private BufferedImage displayImage;
	private boolean started;
	private String displayDeviceID;
	
	//For Serial Ports
	private SerialCommunicationsPort printerFirmwareSerialPort;
	private SerialCommunicationsPort projectorSerialPort;
	
	//For Job Status
	private volatile JobStatus status;
	private ReentrantLock statusLock = new ReentrantLock();
	private Condition jobContinued = statusLock.newCondition();
	
	//GCode
	private GCodeControl gCodeControl;

	//Projector model
	private ProjectorModel projectorModel;
	
	public static enum DisplayState {
		Calibration,
		Blank,
		CurrentSlice
	}
	
	//For jaxb/json
	private Printer() {}
	
	public Printer(PrinterConfiguration configuration) throws InappropriateDeviceException {
		this.configuration = configuration;
		
		try {
			Class<GCodeControl> gCodeClass = (Class<GCodeControl>)Class.forName("org.area515.resinprinter.gcode." + configuration.getMachineConfig().getMotorsDriverConfig().getDriverType() + "GCodeControl");
			gCodeControl = (GCodeControl)gCodeClass.getConstructors()[0].newInstance(this);
		} catch (ClassNotFoundException e) {
			throw new InappropriateDeviceException("Couldn't find GCode controller for:" + configuration.getMachineConfig().getMotorsDriverConfig().getDriverType(), e);
		} catch (SecurityException e) {
			throw new InappropriateDeviceException("No permission to create class for:" + configuration.getMachineConfig().getMotorsDriverConfig().getDriverType(), e);
		} catch (Exception e) {
			throw new InappropriateDeviceException("Couldn't create instance for:" + configuration.getMachineConfig().getMotorsDriverConfig().getDriverType(), e);
		}
	}
	
	@JsonIgnore
	public String getName() {
		return configuration.getName();
	}
	
	@XmlTransient
	@JsonProperty
	public boolean isPrintInProgress() {
		return status == JobStatus.Paused || status == JobStatus.Printing || this.status == JobStatus.PausedOutOfPrintMaterial;
	}
	public void setPrintInProgress(boolean progress) {
		//Ignore anyone trying to set this property, this is for json only!!
	}
	
	@XmlTransient
	@JsonProperty
	public boolean isStarted() {
		return started;
	}
	public void setStarted(boolean started) {
		this.started = started;
	}
	
	public JobStatus getStatus() {
		return status;
	}
	
	public void setStatus(JobStatus status) {
		statusLock.lock();
		try {
			if (this.status == JobStatus.Paused || this.status == JobStatus.PausedOutOfPrintMaterial) {
				jobContinued.signalAll();
			}
			
			this.status = status;
		} finally {
			statusLock.unlock();
		}
	}
	
	public boolean waitForPauseIfRequired() {
		statusLock.lock();
		try {
			//Very important that this check is performed
			if (this.status != JobStatus.Paused && this.status != JobStatus.PausedOutOfPrintMaterial) {
				return isPrintInProgress();
			}
			System.out.println("Print has been paused.");
			jobContinued.await();
			System.out.println("Print has resumed.");
			return isPrintInProgress();
		} catch (InterruptedException e) {
			e.printStackTrace();//Normal if os is shutting us down
			return isPrintInProgress();
		} finally {
			statusLock.unlock();
		}
	}
	
	public JobStatus togglePause() {
		statusLock.lock();
		try {
			if (this.status == JobStatus.Paused || this.status == JobStatus.PausedOutOfPrintMaterial) {
				setStatus(JobStatus.Printing);
				return this.status;
			}
			
			if (this.status == JobStatus.Printing) {
				setStatus(JobStatus.Paused);
			}

			return this.status;
		} finally {
			statusLock.unlock();
		}
	}
	
	public void setGraphicsData(GraphicsDevice device) {
		refreshFrame = new JFrame() {
			private static final long serialVersionUID = 5024551291098098753L;

			@Override
			public void paint(Graphics g) {
				//super.paint(g);
				
				Rectangle screenSize = refreshFrame.getGraphicsConfiguration().getBounds();
				Graphics2D g2 = (Graphics2D)g;
				switch (displayState) {
				case Blank :
					g2.setBackground(Color.black);
					g2.clearRect(0, 0, screenSize.width, screenSize.height);
					return;
				case Calibration :
					g2.setBackground(Color.black);
					g2.clearRect(0, 0, screenSize.width, screenSize.height);
					g2.setColor(Color.RED);
					for (int x = 0; x < screenSize.width; x += calibrationSquareSize) {
						g2.drawLine(x, 0, x, screenSize.height);
					}
					
					for (int y = 0; y < screenSize.height; y += calibrationSquareSize) {
						g2.drawLine(0, y, screenSize.width, y);
					}
					return;
				case CurrentSlice :
					g2.drawImage(displayImage, null, screenSize.width / 2 - displayImage.getWidth() / 2, screenSize.height / 2 - displayImage.getHeight() / 2);
					return;
				}
			}
		};

		if (device.getIDstring().equalsIgnoreCase(DisplayManager.SIMULATED_DISPLAY)) {
			refreshFrame.setTitle("Printer Simulation");
			refreshFrame.setVisible(true);
			refreshFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			refreshFrame.setMinimumSize(new Dimension(500, 500));
			
		} else {
			refreshFrame.setUndecorated(true);
			device.setFullScreenWindow(refreshFrame);
			//This can only be done with a real graphics device since it would reassign the printer Simulation
			//OLD getConfiguration().getMachineConfig().setOSMonitorID(device.getDefaultConfiguration().getDevice().getIDstring());
			getConfiguration().getMachineConfig().setOSMonitorID(device.getIDstring());
			
			// hide mouse in full screen
			Toolkit toolkit = Toolkit.getDefaultToolkit();
		    Point hotSpot = new Point(0,0);
		    BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT); 
		    Cursor invisibleCursor = toolkit.createCustomCursor(cursorImage, hotSpot, "InvisibleCursor");        
		    refreshFrame.setCursor(invisibleCursor);
		}

		this.displayDeviceID = device.getIDstring();
		Rectangle screenSize = refreshFrame.getGraphicsConfiguration().getBounds();
		getConfiguration().getMachineConfig().getMonitorDriverConfig().setDLP_X_Res(screenSize.width);
		getConfiguration().getMachineConfig().getMonitorDriverConfig().setDLP_Y_Res(screenSize.height);
	}
	
	public String getDisplayDeviceID() {
		return displayDeviceID;
	}

	public void showBlankImage() {
		displayState = DisplayState.Blank;		
		refreshFrame.repaint();
	}
	
	public void showCalibrationImage(int pixels) {
		displayState = DisplayState.Calibration;
		calibrationSquareSize = pixels;
		refreshFrame.repaint();
	}
	
	public void showImage(BufferedImage image) {
		displayState = DisplayState.CurrentSlice;		
		displayImage = image;
		refreshFrame.repaint();
	}

	public void setProjectorModel(ProjectorModel projectorModel) {
		this.projectorModel = projectorModel;
	}
	public void setProjectorPowerStatus(boolean powerOn) throws IOException {
		if (projectorModel == null) {
			throw new IOException("Projector model couldn't be detected");
		}
		
		if (projectorSerialPort == null) {
			throw new IOException("Serial port not available for projector.");
		}
		
		projectorModel.setProjectorState(powerOn, projectorSerialPort);
	}
	
	public PrinterConfiguration getConfiguration() {
		return configuration;
	}
	public void setConfiguration(PrinterConfiguration configuration) {
		this.configuration = configuration;
	}

	@JsonIgnore
	public GCodeControl getGCodeControl() {
		return gCodeControl;
	}
	
	public void setPrinterFirmwareSerialPort(SerialCommunicationsPort printerFirmwareSerialPort) {
		this.printerFirmwareSerialPort = printerFirmwareSerialPort;
		
		//Read the welcome mat if it's not null
		if (printerFirmwareSerialPort != null) {
			try {
				System.out.println("Firmware Welcome chitchat:" + getGCodeControl().readWelcomeChitChat());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	@JsonIgnore
	public SerialCommunicationsPort getPrinterFirmwareSerialPort() {
		return printerFirmwareSerialPort;
	}
	
	public void setProjectorSerialPort(SerialCommunicationsPort projectorSerialPort) {
		this.projectorSerialPort = projectorSerialPort;
	}
	@JsonIgnore
	public SerialCommunicationsPort getProjectorSerialPort() {
		return projectorSerialPort;
	}

	public String toString() {
		return getName() + "(SerialPort:" + printerFirmwareSerialPort + ", Display:" + displayDeviceID + ")";
	}
	
	public void close() {
		if (printerFirmwareSerialPort != null) {
			printerFirmwareSerialPort.close();
		}
		if (refreshFrame != null) {
			refreshFrame.dispose();
		}
		started = false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((configuration == null) ? 0 : configuration.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Printer other = (Printer) obj;
		if (configuration == null) {
			if (other.configuration != null)
				return false;
		} else if (!configuration.equals(other.configuration))
			return false;
		return true;
	}
}
