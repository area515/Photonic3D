package org.area515.resinprinter.job;

import gnu.io.SerialPort;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;

public class PrintJob {
	private Graphics2D graphics;
	private GraphicsConfiguration graphicsConfiguration;
	private File jobFile;
	private BufferedImage blankImage;
	private JFrame frame;
	private Rectangle screenSize;
	private SerialPort serialPort;
	private BufferedReader input;
	private OutputStream output;
	private AtomicInteger totalSlices = new AtomicInteger();
	private AtomicInteger currentSlice = new AtomicInteger();
	private File gCodeFile;
	private volatile JobStatus status;
	private UUID id = UUID.randomUUID();
	private boolean manual;
	private ReentrantLock lock = new ReentrantLock();
	private Condition jobContinued = lock.newCondition();
	
	public PrintJob() {
		this.manual = true;
	}
	
	public PrintJob(File jobFile) {
		this.jobFile = jobFile;
		this.manual = false;
	}
	
	public void setGraphicsData(JFrame frame, GraphicsConfiguration graphicsConfiguration) {
		this.frame = frame;
		this.graphics = (Graphics2D)frame.getGraphics();
		this.graphicsConfiguration = graphicsConfiguration;
		this.screenSize = graphicsConfiguration.getBounds();
	}

	public boolean isManual() {
		return manual;
	}
	
	public UUID getId() {
		return id;
	}
	
	public boolean isPrintInProgress() {
		return status == JobStatus.Paused || status == JobStatus.Printing;
	}
	
	public JobStatus getStatus() {
		return status;
	}
	public void setStatus(JobStatus status) {
		lock.lock();
		try {
			if (this.status == JobStatus.Paused) {
				jobContinued.signalAll();
			}
			
			this.status = status;
		} finally {
			lock.unlock();
		}
	}
	
	public JobStatus waitForPauseIfRequired() {
		lock.lock();
		try {
			//Very important that this check is performed
			if (this.status != JobStatus.Paused) {
				return this.status;
			}
			
			jobContinued.await();
			return this.status;
		} catch (InterruptedException e) {
			e.printStackTrace();//Normal if os is shutting us down
			return this.status;
		} finally {
			lock.unlock();
		}
	}
	
	public JobStatus togglePause() {
		lock.lock();
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
			lock.unlock();
		}
	}
	
	public File getJobFile() {
		return jobFile;
	}
	
	public File getGCodeFile(){
		return gCodeFile;
	}	
	public void setGCodeFile(File gCodeFile){
		this.gCodeFile = gCodeFile;
	}
	
	public int getTotalSlices(){
		return totalSlices.get();
	}
	
	public void setTotalSlices(int totalSlices){
		this.totalSlices.set(totalSlices);
	}
	public int getCurrentSlice(){
		return currentSlice.get();
	}
	
	public void setCurrentSlice(int currentSlice){
		this.currentSlice.set(currentSlice);
	}

	public void setSerialPort(SerialPort serialPort) throws IOException {
		this.serialPort = serialPort;
		
		// open the streams
		input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
		output = serialPort.getOutputStream();
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
	
	public void close() {
		//jobFile.deleteOnExit();
		
		if (graphics != null) {
			graphics.dispose();
		}
		if (frame != null) {
			frame.dispose();
		}
		if (input != null) {
			try {input.close();} catch (IOException e) {}
		}		
		if (output != null) {
			try {output.close();} catch (IOException e) {}
		}
		if (serialPort != null) {
			serialPort.close();
		}
	}
	
	public void sendAndWaitForResponse(String gcode) throws InterruptedException {
		try {
			output.write(gcode.getBytes());
			
			int value = input.read();
			while ((value == -1 || value != '\n') && status == JobStatus.Printing) {
				value = input.read();
			}
		} catch (IOException e) {
			throw new InterruptedException("IO problem reading from device");
		}
	}
	
	public String toString() {
		return jobFile.getName() + " assigned to:" + graphicsConfiguration.getDevice().getIDstring();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jobFile == null) ? 0 : jobFile.hashCode());
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
		PrintJob other = (PrintJob) obj;
		if (jobFile == null) {
			if (other.jobFile != null)
				return false;
		} else if (!jobFile.equals(other.jobFile))
			return false;
		return true;
	}
}
