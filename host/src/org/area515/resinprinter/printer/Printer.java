package org.area515.resinprinter.printer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.gcode.GCodeControl;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.serial.SerialCommunicationsPort;

public class Printer {
	private PrinterConfiguration configuration;
	
	//For Display
	private Graphics2D graphics;
	private GraphicsConfiguration graphicsConfiguration;
	private BufferedImage blankImage;
	private int calibrationSquareSize;
	private BufferedImage calibrationImage;
	private JFrame frame;
	private Rectangle screenSize;

	//For Serial Port
	private SerialCommunicationsPort serialPort;
	
	//For Job Status
	private volatile JobStatus status;
	private ReentrantLock statusLock = new ReentrantLock();
	private Condition jobContinued = statusLock.newCondition();
	
	//GCode
	private GCodeControl gCodeControl;

	public Printer(PrinterConfiguration configuration) throws InappropriateDeviceException {
		this.configuration = configuration;
		
		try {
			Class gCodeClass = Class.forName("org.area515.resinprinter.gcode." + configuration.getMotorsDriverConfig().getDriverType() + "GCodeControl");
			gCodeControl = (GCodeControl)gCodeClass.getConstructors()[0].newInstance(this);
		} catch (ClassNotFoundException e) {
			throw new InappropriateDeviceException("Couldn't find GCode controller for:" + configuration.getMotorsDriverConfig().getDriverType(), e);
		} catch (SecurityException e) {
			throw new InappropriateDeviceException("No permission to create class for:" + configuration.getMotorsDriverConfig().getDriverType(), e);
		} catch (Exception e) {
			throw new InappropriateDeviceException("Couldn't create instance for:" + configuration.getMotorsDriverConfig().getDriverType(), e);
		}
	}
	
	public String getName() {
		return configuration.getName();
	}
	
	public boolean isPrintInProgress() {
		return status == JobStatus.Paused || status == JobStatus.Printing;
	}
	
	public JobStatus getStatus() {
		return status;
	}
	
	public void setStatus(JobStatus status) {
		statusLock.lock();
		try {
			if (this.status == JobStatus.Paused) {
				jobContinued.signalAll();
			}
			
			this.status = status;
		} finally {
			statusLock.unlock();
		}
	}
	
	public JobStatus waitForPauseIfRequired() {
		statusLock.lock();
		try {
			//Very important that this check is performed
			if (this.status != JobStatus.Paused) {
				return this.status;
			}
			System.out.println("Print has been paused.");
			jobContinued.await();
			System.out.println("Print has resumed.");
			return this.status;
		} catch (InterruptedException e) {
			e.printStackTrace();//Normal if os is shutting us down
			return this.status;
		} finally {
			statusLock.unlock();
		}
	}
	
	public JobStatus togglePause() {
		statusLock.lock();
		try {
			if (this.status == JobStatus.Paused) {
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
	
	public void setSerialPort(SerialCommunicationsPort serialPort) {
		this.serialPort = serialPort;
		
		//Read the welcome mat
		try {
			System.out.println("Firmware Welcome chitchat:" + getGCodeControl().readWelcomeChitChat());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void setGraphicsData(JFrame frame, GraphicsConfiguration graphicsConfiguration) {
		this.frame = frame;
		this.graphics = (Graphics2D)frame.getGraphics();
		this.graphicsConfiguration = graphicsConfiguration;
		this.screenSize = graphicsConfiguration.getBounds();
		getConfiguration().getMonitorDriverConfig().setDLP_X_Res(screenSize.width);
		getConfiguration().getMonitorDriverConfig().setDLP_Y_Res(screenSize.height);
	}
	
	public void showBlankImage() {
		if (blankImage == null) {
			blankImage = graphicsConfiguration.createCompatibleImage(screenSize.width, screenSize.height);
			Graphics2D imageGraphics = (Graphics2D)blankImage.getGraphics();
			imageGraphics.setBackground(Color.black);
			imageGraphics.setColor(Color.black);
			imageGraphics.drawRect(0, 0, screenSize.width, screenSize.height);
		}
		
		graphics.drawImage(blankImage, null, 0, 0);
	}
	
	public void showCalibrationImage(int pixels) {
		if (calibrationSquareSize != pixels || calibrationImage == null) {
			calibrationSquareSize = pixels;
			
			if (calibrationImage != null) {
				//calibrationImage.flush();
			}
			calibrationImage = graphicsConfiguration.createCompatibleImage(screenSize.width, screenSize.height);
			Graphics2D imageGraphics = (Graphics2D)calibrationImage.getGraphics();
			imageGraphics.setBackground(Color.BLACK);
			imageGraphics.setColor(Color.RED);
			for (int x = 0; x < screenSize.width; x += pixels) {
				imageGraphics.drawLine(x, 0, x, screenSize.height);
			}
			
			for (int y = 0; y < screenSize.height; y += pixels) {
				imageGraphics.drawLine(0, y, screenSize.width, y);
			}
		}
		
		graphics.drawImage(calibrationImage, null, 0, 0);
	}
	
	public void showImage(BufferedImage image) {
		//Center image on build platform
		graphics.drawImage(image, null, screenSize.width / 2 - image.getWidth() / 2, screenSize.height / 2 - image.getHeight() / 2);
	}

	public GraphicsDevice getGraphicsDevice() {
		if (graphicsConfiguration == null)
			return null;
		
		return graphicsConfiguration.getDevice();
	}
	
	public PrinterConfiguration getConfiguration() {
		return configuration;
	}
	public void setConfiguration(PrinterConfiguration configuration) {
		this.configuration = configuration;
	}

	public GCodeControl getGCodeControl() {
		return gCodeControl;
	}

	public SerialCommunicationsPort getSerialPort() {
		return serialPort;
	}
	
	public String toString() {
		return getName() + "(SerialPort:" + serialPort + ", Display:" + graphicsConfiguration.getDevice().getIDstring() + ")";
	}
	
	public void close() {
		//jobFile.deleteOnExit();
		if (serialPort != null) {
			serialPort.close();
		}
		if (graphics != null) {
			graphics.dispose();
		}
		if (frame != null) {
			frame.dispose();
		}
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
