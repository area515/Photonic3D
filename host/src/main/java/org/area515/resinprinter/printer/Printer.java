package org.area515.resinprinter.printer;

import java.awt.Cursor;
import java.awt.Dimension;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.display.GraphicsOutputInterface;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.display.PrinterDisplayFrame;
import org.area515.resinprinter.display.dispmanx.RaspberryPiMainLCDScreen;
import org.area515.resinprinter.gcode.GCodeControl;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.projector.ProjectorModel;
import org.area515.resinprinter.serial.SerialCommunicationsPort;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Printer {
    private static final Logger logger = LogManager.getLogger();
	private PrinterConfiguration configuration;
	
	//For Display
	private GraphicsOutputInterface refreshFrame;
	private boolean started;
	private boolean shutterOpen;
	private Integer bulbHours;
	private String displayDeviceID;
	private long currentSlicePauseTime;
	
	//For Serial Ports
	private SerialCommunicationsPort printerFirmwareSerialPort;
	private SerialCommunicationsPort projectorSerialPort;
	
	//For Job Status
	private volatile JobStatus status = JobStatus.Ready;
	private ReentrantLock statusLock = new ReentrantLock();
	private Condition jobContinued = statusLock.newCondition();
	
	//GCode
	private GCodeControl gCodeControl;

	//Projector model
	private ProjectorModel projectorModel;
	
	public static enum DisplayState {
		Calibration,
		Grid,
		Blank,
		CurrentSlice
	}
	
	//For jaxb/json
	@SuppressWarnings("unused")
	private Printer() {}
	
	public Printer(PrinterConfiguration configuration) throws InappropriateDeviceException {
		this.configuration = configuration;
		
		try {
			@SuppressWarnings("unchecked")
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
		return status != null && status.isPrintInProgress();
	}
	@JsonIgnore
	public void setPrintInProgress(boolean printInProgress) {
	}
	
	@XmlTransient
	@JsonProperty
	public boolean isPrintPaused() {
		return status != null && getStatus().isPaused();
	}
	@JsonIgnore
	public void setPrintPaused(boolean printInProgress) {
	}
	
	@XmlTransient
	@JsonIgnore
	public boolean isPrintActive() {
		return status != null && status.isPrintActive();
	}

	@XmlTransient
	@JsonProperty
	public boolean isStarted() {
		return started;
	}
	public void setStarted(boolean started) {
		this.started = started;
	}
	
	@XmlTransient
	@JsonProperty
	public JobStatus getStatus() {
		return status;
	}
	
	@JsonIgnore
	public void setStatus(JobStatus status) {
		statusLock.lock();
		try {
			if (this.status != null && this.status.isPaused()) {
				jobContinued.signalAll();
			}
			logger.info("Moving from status:" + this.status + " to status:" + status);
			this.status = status;
			if (!status.isPrintInProgress()) {
				refreshFrame.resetSliceCount();
			}
		} finally {
			statusLock.unlock();
		}
	}
	
	public boolean waitForPauseIfRequired() {
		statusLock.lock();
		try {
			//Very important that this check is performed
			if (this.status != null && !this.status.isPaused()) {
				return isPrintActive();
			}
			logger.info("Print has been paused.");
			long startPause = System.currentTimeMillis();
			jobContinued.await();
			currentSlicePauseTime += System.currentTimeMillis() - startPause;
			logger.info("Print has resumed.");
			return isPrintActive();
		} catch (InterruptedException e) {
			logger.error("Normal if os is shutting us down", e);
			return isPrintActive();
		} finally {
			statusLock.unlock();
		}
	}
	
	public JobStatus togglePause() {
		statusLock.lock();
		try {
			if (this.status != null && this.status.isPaused()) {
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
	
	public void setGraphicsData(final GraphicsDevice device) {
		this.displayDeviceID = device.getIDstring();
		
		if (device instanceof GraphicsOutputInterface) {
			this.refreshFrame = (GraphicsOutputInterface)device;
		} else if (device.getIDstring().equalsIgnoreCase(DisplayManager.SIMULATED_DISPLAY)) {
			PrinterDisplayFrame refreshFrame = new PrinterDisplayFrame();
			refreshFrame.setTitle("Printer Simulation");
			refreshFrame.setVisible(true);
			refreshFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			refreshFrame.setMinimumSize(new Dimension(500, 500));
			this.refreshFrame = refreshFrame;
		} else  {
			PrinterDisplayFrame refreshFrame = new PrinterDisplayFrame(device.getDefaultConfiguration());
			refreshFrame.setAlwaysOnTop(true);
			refreshFrame.setUndecorated(true);
			refreshFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			Dimension dim = device.getDefaultConfiguration().getBounds().getSize();
			refreshFrame.setMinimumSize(dim);
			refreshFrame.setSize(dim);
			refreshFrame.setVisible(true);
			if (device.isFullScreenSupported()) {
				device.setFullScreenWindow(refreshFrame);//TODO: Does projector not support full screen
			}
			//This can only be done with a real graphics device since it would reassign the printer Simulation
			//OLD getConfiguration().getMachineConfig().setOSMonitorID(device.getDefaultConfiguration().getDevice().getIDstring());
			getConfiguration().getMachineConfig().setOSMonitorID(device.getIDstring());
			
			// hide mouse in full screen
			Toolkit toolkit = Toolkit.getDefaultToolkit();
		    Point hotSpot = new Point(0,0);
		    BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT); 
		    Cursor invisibleCursor = toolkit.createCustomCursor(cursorImage, hotSpot, "InvisibleCursor");        
		    refreshFrame.setCursor(invisibleCursor);
		    this.refreshFrame = refreshFrame;
		}

		Rectangle screenSize = refreshFrame.getBoundry();
		getConfiguration().getMachineConfig().getMonitorDriverConfig().setDLP_X_Res(screenSize.width);
		getConfiguration().getMachineConfig().getMonitorDriverConfig().setDLP_Y_Res(screenSize.height);
	}
	
	public String getDisplayDeviceID() {
		return displayDeviceID;
	}

	public void showBlankImage() {	
		refreshFrame.showBlankImage();
	}
	
	public void showCalibrationImage(int xPixels, int yPixels) {
		refreshFrame.showCalibrationImage(xPixels, yPixels);
	}
	
	public void showGridImage(int pixels) {
		refreshFrame.showGridImage(pixels);
	}
	
	public void showImage(BufferedImage image) {
		refreshFrame.showImage(image);
	}
	
	public boolean isDisplayBusy() {
		return refreshFrame.isDisplayBusy();
	}

	@JsonIgnore
	@XmlTransient
	public boolean isProjectorPowerControlSupported() {
		return projectorModel != null;
	}
	
	@JsonIgnore
	@XmlTransient
	public void setProjectorModel(ProjectorModel projectorModel) {
		this.projectorModel = projectorModel;
	}
	@JsonIgnore
	@XmlTransient
	
	public ProjectorModel getProjectorModel() {
		return projectorModel;
	}
	
	public void setProjectorPowerStatus(boolean powerOn) throws IOException {
		if (projectorModel == null) {
			throw new IOException("Projector model couldn't be detected");
		}
		
		if (projectorSerialPort == null) {
			throw new IOException("Serial port not available for projector.");
		}
		
		projectorModel.setPowerState(powerOn, projectorSerialPort);
	}

	public PrinterConfiguration getConfiguration() {
		return configuration;
	}
	public void setConfiguration(PrinterConfiguration configuration) {
		this.configuration = configuration;
	}

	public boolean isShutterOpen() {
		return shutterOpen;
	}
	public void setShutterOpen(boolean shutterOpen) {
		this.shutterOpen = shutterOpen;
	}

	@JsonIgnore
	public Integer getBulbHours() {
		if (bulbHours == null && projectorModel != null) {
			try {
				bulbHours = projectorModel.getBulbHours(projectorSerialPort);
			} catch (IOException e) {
				logger.error("Failed communicating with projector for bulb hours", e);
			}
		}
		
		return bulbHours;
	}
	public void setBulbHours(Integer bulbHours) {
		this.bulbHours = bulbHours;
	}
	
	public Integer getCachedBulbHours() {
		return bulbHours;
	}
	public void setCachedBulbHours(Integer bulbHours) {
		this.bulbHours = bulbHours;
	}
	
	public long getCurrentSlicePauseTime() {
		return currentSlicePauseTime;
	}
	public void setCurrentSlicePauseTime(long currentSlicePauseTime) {
		this.currentSlicePauseTime = currentSlicePauseTime;
	}
	
	@JsonIgnore
	public GCodeControl getGCodeControl() {
		return gCodeControl;
	}
	
	public void setPrinterFirmwareSerialPort(SerialCommunicationsPort printerFirmwareSerialPort) {
		this.printerFirmwareSerialPort = printerFirmwareSerialPort;
		logger.info("Firmware serial port set to:" + printerFirmwareSerialPort);
		
		//Read the welcome mat if it's not null
		if (printerFirmwareSerialPort != null) {
			try {
				logger.info("Firmware Welcome chitchat:" + getGCodeControl().readWelcomeChitChat());
			} catch (IOException e) {
				logger.error("Error while reading welcome chitchat", e);
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
		return getName() + "(printerFirmwareSerialPort:" + printerFirmwareSerialPort + ", projectorSerialPort:" + projectorSerialPort + " Display:" + displayDeviceID + ")";
	}
	
	public void close() {
		if (printerFirmwareSerialPort != null) {
			printerFirmwareSerialPort.close();
		}
		if (refreshFrame != null) {
			refreshFrame.dispose();
		}
		bulbHours = null;
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
