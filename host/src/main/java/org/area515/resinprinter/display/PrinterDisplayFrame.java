package org.area515.resinprinter.display;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.printer.Printer.DisplayState;
import org.area515.util.Log4jTimer;

public class PrinterDisplayFrame extends JFrame implements GraphicsOutputInterface {
	private static final long serialVersionUID = 5024551291098098753L;
	private static final Logger logger = LogManager.getLogger();
	
	private String IMAGE_REALIZE_TIMER = "Image Realize:";
	private DisplayState displayState;
	private int gridSquareSize;
	private int calibrationX;
	private int calibrationY;
	private BufferedImage displayImage;
	private int sliceNumber;
	private boolean isSimulatedDisplay;
	
	public PrinterDisplayFrame() throws HeadlessException {
		super();
		this.isSimulatedDisplay = true;
		getRootPane().setBackground(Color.black);
		getContentPane().setBackground(Color.black);
		IMAGE_REALIZE_TIMER += hashCode();
	}

	public PrinterDisplayFrame(GraphicsConfiguration gc) {
		super(gc);
		this.isSimulatedDisplay = false;
		getRootPane().setBackground(Color.black);
		getContentPane().setBackground(Color.black);
		IMAGE_REALIZE_TIMER += hashCode();
	}

	public DisplayState getDisplayState() {
		return displayState;
	}
	public void setDisplayState(DisplayState displayState) {
		this.displayState = displayState;
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		//we need to add this method back in because some UV light engines require it.
		super.paint(g);
		
		Rectangle screenSize = getGraphicsConfiguration().getBounds();
		switch (displayState) {
		case Blank :
			g2.setBackground(Color.black);
			g2.clearRect(0, 0, screenSize.width, screenSize.height);
			logger.debug("Blank realized:{}", () -> Log4jTimer.completeGlobalTimer(IMAGE_REALIZE_TIMER));
			return;
		case Grid :
			GraphicsOutputInterface.showGrid(g2, screenSize, gridSquareSize);
			logger.debug("Grid realized:{}", () -> Log4jTimer.completeGlobalTimer(IMAGE_REALIZE_TIMER));
			return;
		case Calibration :
			GraphicsOutputInterface.showCalibration(g2, screenSize, calibrationX, calibrationY);
			logger.debug("Calibration realized:{}", () -> Log4jTimer.completeGlobalTimer(IMAGE_REALIZE_TIMER));
			return;
		case CurrentSlice :
			g2.drawImage(displayImage, null, screenSize.width / 2 - displayImage.getWidth() / 2, screenSize.height / 2 - displayImage.getHeight() / 2);
			if (isSimulatedDisplay) {
				g2.setColor(Color.RED);
				g2.setFont(getFont());
				g2.drawString("Slice:" + sliceNumber, getInsets().left, getInsets().top + g2.getFontMetrics().getHeight());
			}
			logger.debug("Image realized:{}", () -> Log4jTimer.completeGlobalTimer(IMAGE_REALIZE_TIMER));
			return;
		}
		
	}
	
	public void resetSliceCount() {
		sliceNumber = 0;
	}
	
	public void showBlankImage() {
		logger.debug("Blank assigned:{}", () -> Log4jTimer.startGlobalTimer(IMAGE_REALIZE_TIMER));
		setDisplayState(DisplayState.Blank);	
		repaint();
	}
	
	public void showCalibrationImage(int xPixels, int yPixels) {
		logger.debug("Calibration assigned:{}", () -> Log4jTimer.startGlobalTimer(IMAGE_REALIZE_TIMER));
		setDisplayState(DisplayState.Calibration);
		calibrationX = xPixels;
		calibrationY = yPixels;
		repaint();
	}
	
	public void showGridImage(int pixels) {
		logger.debug("Grid assigned:{}", () -> Log4jTimer.startGlobalTimer(IMAGE_REALIZE_TIMER));
		setDisplayState(DisplayState.Grid);
		gridSquareSize = pixels;
		repaint();
	}
	
	public void showImage(BufferedImage image) {
		logger.debug("Image assigned:{}", () -> Log4jTimer.startGlobalTimer(IMAGE_REALIZE_TIMER));
		sliceNumber++;
		setDisplayState(DisplayState.CurrentSlice);	
		displayImage = image;
		repaint();
	}

	@Override
	public Rectangle getBoundry() {
		return getGraphicsConfiguration().getBounds();
	}

	@Override
	public boolean isDisplayBusy() {
		return false;
	}
}
