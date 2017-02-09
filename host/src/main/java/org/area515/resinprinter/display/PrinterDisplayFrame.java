package org.area515.resinprinter.display;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.printer.Printer.DisplayState;
import org.area515.util.Log4jTimer;

public class PrinterDisplayFrame extends JFrame {
	private static final long serialVersionUID = 5024551291098098753L;
	private static final String IMAGE_REALIZE_TIMER = "Image Realize";
	private static final Logger logger = LogManager.getLogger();
	
	private DisplayState displayState;
	private int gridSquareSize;
	private Point calibrationXY;
	private BufferedImage displayImage;
	private int sliceNumber;
	private boolean isSimulatedDisplay;
	
	public PrinterDisplayFrame() throws HeadlessException {
		super();
		this.isSimulatedDisplay = true;
		getRootPane().setBackground(Color.black);
		getContentPane().setBackground(Color.black);
	}

	public PrinterDisplayFrame(GraphicsConfiguration gc) {
		super(gc);
		this.isSimulatedDisplay = false;
		getRootPane().setBackground(Color.black);
		getContentPane().setBackground(Color.black);
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
			break;
		case Grid :
			g2.setBackground(Color.black);
			g2.clearRect(0, 0, screenSize.width, screenSize.height);
			g2.setColor(Color.RED);
			for (int x = 0; x < screenSize.width; x += gridSquareSize) {
				g2.drawLine(x, 0, x, screenSize.height);
			}
			
			for (int y = 0; y < screenSize.height; y += gridSquareSize) {
				g2.drawLine(0, y, screenSize.width, y);
			}
			logger.debug("Image realized:{}", () -> Log4jTimer.completeTimer("Image Realize"));
			break;
		case Calibration :
			g2.setBackground(Color.black);
			g2.clearRect(0, 0, screenSize.width, screenSize.height);
			g2.setColor(Color.RED);
			int startingX = screenSize.width / 2 - calibrationXY.x / 2;
			int startingY = screenSize.height / 2 - calibrationXY.y / 2;
			int halfLengthOfDimLines = 50;
			
			//X Dimension lines
			g2.drawLine(startingX                  , screenSize.height / 2 - halfLengthOfDimLines, startingX                  , screenSize.height / 2 + halfLengthOfDimLines);
			g2.drawLine(startingX + calibrationXY.x, screenSize.height / 2 - halfLengthOfDimLines, startingX + calibrationXY.x, screenSize.height / 2 + halfLengthOfDimLines);
			
			//Y Dimension lines
			g2.drawLine(screenSize.width / 2 - halfLengthOfDimLines, startingY                  , screenSize.width / 2 + halfLengthOfDimLines, startingY);
			g2.drawLine(screenSize.width / 2 - halfLengthOfDimLines, startingY + calibrationXY.y, screenSize.width / 2 + halfLengthOfDimLines, startingY + calibrationXY.y);
								
			//Vertical line of cross
			g2.drawLine(screenSize.width / 2, startingY, screenSize.width / 2, startingY + calibrationXY.y);

			//Horizontal line of cross
			g2.setStroke(new BasicStroke(5, 0, 0, 1.0f, new float[]{10, 10}, 2.0f));
			g2.drawLine(startingX, screenSize.height / 2, startingX + calibrationXY.x, screenSize.height / 2);
			break;
		case CurrentSlice :
			g2.drawImage(displayImage, null, screenSize.width / 2 - displayImage.getWidth() / 2, screenSize.height / 2 - displayImage.getHeight() / 2);
			if (isSimulatedDisplay) {
				g2.setColor(Color.RED);
				g2.setFont(getFont());
				g2.drawString("Slice:" + sliceNumber, getInsets().left, getInsets().top + g2.getFontMetrics().getHeight());
			}
			break;
		}
		
		logger.debug("Image realized:{}", () -> Log4jTimer.completeTimer(IMAGE_REALIZE_TIMER));
	}
	
	public void resetSliceCount() {
		sliceNumber = 0;
	}
	
	public void showBlankImage() {
		logger.debug("Blank assigned:{}", () -> Log4jTimer.startTimer(IMAGE_REALIZE_TIMER));
		setDisplayState(DisplayState.Blank);	
		repaint();
	}
	
	public void showCalibrationImage(int xPixels, int yPixels) {
		logger.debug("Calibration assigned:{}", () -> Log4jTimer.startTimer(IMAGE_REALIZE_TIMER));
		setDisplayState(DisplayState.Calibration);
		calibrationXY = new Point(xPixels, yPixels);
		repaint();
	}
	
	public void showGridImage(int pixels) {
		logger.debug("Grid assigned:{}", () -> Log4jTimer.startTimer(IMAGE_REALIZE_TIMER));
		setDisplayState(DisplayState.Grid);
		gridSquareSize = pixels;
		repaint();
	}
	
	public void showImage(BufferedImage image) {
		logger.debug("Image assigned:{}", () -> Log4jTimer.startTimer(IMAGE_REALIZE_TIMER));
		sliceNumber++;
		setDisplayState(DisplayState.CurrentSlice);	
		displayImage = image;
		repaint();
	}
}
