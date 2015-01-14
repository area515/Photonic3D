package org.area515.resinprinter.printer;

import gnu.io.SerialPort;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;

import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.gcode.GCodeControl;
import org.area515.resinprinter.job.JobStatus;


public class Printer {
	private PrinterConfiguration configuration;
	
	//For Display
	private Graphics2D graphics;
	private GraphicsConfiguration graphicsConfiguration;
	private BufferedImage blankImage;
	private JFrame frame;
	private Rectangle screenSize;

	//For Serial Port
	private SerialPort serialPort;
	private InputStream input;
	private OutputStream output;
	
	//For Job Status
	private volatile JobStatus status;
	private ReentrantLock statusLock = new ReentrantLock();
	private Condition jobContinued = statusLock.newCondition();
	
	//GCode
	private GCodeControl gCodeControl;
	private ReentrantLock gCodeLock = new ReentrantLock();
	
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
	
	public void setSerialPort(SerialPort serialPort) throws IOException {
		this.serialPort = serialPort;
		
		// open the streams
		input = serialPort.getInputStream();
		output = serialPort.getOutputStream();
		
		//Read the welcome mat
		try {
			System.out.println("Firmware Welcome chitchat:" + getGCodeControl().readWelcomeChitChat());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//synchronized allows us to send manual commands to the printer
	public String readLine(boolean returnIfPrintNotInProgress) throws IOException {
		gCodeLock.lock();
		try {
			StringBuilder builder = new StringBuilder();
			
			int value = -1;
			do {
				value = input.read();
				if (value > -1) {
					builder.append((char)value);
				}
			} while ((value > -1 && value != '\n') || (returnIfPrintNotInProgress && value == -1 && isPrintInProgress()));
			if (builder.length() == 0) {
				return null;
			}
			return builder.toString();
		} finally {
			gCodeLock.unlock();
		}
	}
	
	//synchronized allows us to send manual commands to the printer
	public String sendGCodeAndWaitForResponseForever(String gcode) throws IOException {
		gCodeLock.lock();
		try {
			output.write(gcode.getBytes());
			return readLine(false);
		} finally {
			gCodeLock.unlock();
		}
	}
	
	public String sendGCodeAndWaitForResponseOnlyWhilePrintIsInProgress(String gcode) throws IOException {
		gCodeLock.lock();
		try {
			output.write(gcode.getBytes());
			return readLine(true);
		} finally {
			gCodeLock.unlock();
		}
	}
	
	public void setGraphicsData(JFrame frame, GraphicsConfiguration graphicsConfiguration) {
		this.frame = frame;
		this.graphics = (Graphics2D)frame.getGraphics();
		this.graphicsConfiguration = graphicsConfiguration;
		this.screenSize = graphicsConfiguration.getBounds();
		getConfiguration().setOSMonitorID(graphicsConfiguration.getDevice().getIDstring());
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

	public String toString() {
		return getName() + "(SerialPort:" + serialPort + ", Display:" + graphicsConfiguration.getDevice().getIDstring() + ")";
	}
	
	public void close() {
		//jobFile.deleteOnExit();
		if (input != null) {
			try {input.close();} catch (IOException e) {}
		}		
		if (output != null) {
			try {output.close();} catch (IOException e) {}
		}
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
