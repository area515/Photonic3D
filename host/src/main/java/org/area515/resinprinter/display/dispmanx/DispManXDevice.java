package org.area515.resinprinter.display.dispmanx;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.GraphicsOutputInterface;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.util.Log4jUtil;

import com.wgilster.dispmanx.DISPMANX_MODEINFO_T;
import com.wgilster.dispmanx.DispManX;
import com.wgilster.dispmanx.SCREEN;
import com.wgilster.dispmanx.VC_RECT_T;
import com.wgilster.dispmanx.window.Display;
import com.wgilster.dispmanx.window.NativeMemoryBackedBufferedImage;
import com.wgilster.dispmanx.window.Resource;
import com.wgilster.dispmanx.window.Sprite;

public class DispManXDevice implements GraphicsOutputInterface {
	private static final String IMAGE_TIMER = "Image Realize";
	private static final String CALIBRATION_TIMER = "Calibration Realize";
	private static final String GRID_TIMER = "Grid Realize";
    private static final Logger logger = LogManager.getLogger();
    private int BLANK_LAYER = Integer.MAX_VALUE - 10;
    private String displayName;
    private SCREEN screen;
    private Display display = null;
    private DISPMANX_MODEINFO_T displaySettings;
    private ReentrantLock displayLock = new ReentrantLock(true);
    private Resource blankScreenResource;
    private Sprite blankScreenSprite;
    private Resource displayImageResource;
    private Sprite displayedImageSprite;
    private NativeMemoryBackedBufferedImage calGridImage;
    
    public DispManXDevice(String displayName, SCREEN screen) throws InappropriateDeviceException {
		this.displayName = displayName;
		this.screen = screen;
		
		//Call a harmless method to ensure that Dispmanx lib is initialized
        VC_RECT_T.ByReference sourceRect = new VC_RECT_T.ByReference();
        DispManX.INSTANCE.vc_dispmanx_rect_set(sourceRect, 0, 0, 0, 0);
	}
    
	@Override
	public void dispose() {
		if (display == null) {
			logger.warn("Display already closed. Nothing to dispose");
			return;
		}
		
    	displayLock.lock();
    	try {
    		if (display != null) {
    			removeNonBlankElementsFromScreen();
    			blankScreenResource.release();
    			display.removeNow(blankScreenSprite);
		    	display.close();
		    	display = null;
    		}
    	} finally {
    		displayLock.unlock();
    	}
	}
    
	@Override
	public void showBlankImage() {
		removeNonBlankElementsFromScreen();
	}

	private void removeNonBlankElementsFromScreen() {
		logger.info("Screen cleanup started");
    	displayLock.lock();
    	try {
    		if (displayedImageSprite != null) {
    			display.removeNow(displayedImageSprite);
    		}
    		if (displayImageResource != null) {
    			displayImageResource.release();
    		}
    		displayedImageSprite = null;
    		displayImageResource = null;
    	} finally {
    		displayLock.unlock();
    	}
	}
	
	@Override
	public void showCalibrationImage(int xPixels, int yPixels) {
		logger.debug("Calibration assigned:{}", () -> Log4jUtil.startTimer(CALIBRATION_TIMER));
		if (calGridImage == null) {
			calGridImage = (NativeMemoryBackedBufferedImage)buildBufferedImage(displaySettings.width, displaySettings.height);
		}
		Graphics2D graphics = (Graphics2D)calGridImage.createGraphics();
		GraphicsOutputInterface.showCalibration(graphics, new Rectangle(displaySettings.width, displaySettings.height), xPixels, yPixels);
		graphics.dispose();
		showImage(calGridImage, true);		
		logger.debug("Calibration realized:{}", () -> Log4jUtil.completeTimer(CALIBRATION_TIMER));
	}
	
	@Override
	public void showGridImage(int pixels) {
		logger.debug("Grid assigned:{}", () -> Log4jUtil.startTimer(GRID_TIMER));
		if (calGridImage == null) {
			calGridImage = (NativeMemoryBackedBufferedImage)buildBufferedImage(displaySettings.width, displaySettings.height);
		}
		Graphics2D graphics = (Graphics2D)calGridImage.createGraphics();
		GraphicsOutputInterface.showGrid(graphics, new Rectangle(displaySettings.width, displaySettings.height), pixels);
		graphics.dispose();
		showImage(calGridImage, true);		
		logger.debug("Grid realized:{}", () -> Log4jUtil.completeTimer(GRID_TIMER));
	}
	
	@Override
	//This method will not stack images. It will only remove the previous layer and display the next
	//If the previous layer doesn't exist, it will simply display the next layer
	public void showImage(BufferedImage image, boolean performFullUpdate) {
		logger.debug("Image assigned:{}", () -> Log4jUtil.startTimer(IMAGE_TIMER));
    	displayLock.lock();
    	try {
    		NativeMemoryBackedBufferedImage nativeImage = null;
			if (image instanceof NativeMemoryBackedBufferedImage) {
				nativeImage = (NativeMemoryBackedBufferedImage)image;
			} else {
				nativeImage = (NativeMemoryBackedBufferedImage)buildBufferedImage(image.getWidth(), image.getHeight());
				nativeImage.getGraphics().drawImage(image, 0, 0, null);
			}
    		int x = 0;
    		int y = 0;
    		if (nativeImage.getWidth() > displaySettings.width) {
    			x = 0;
    		} else {
    			x = (displaySettings.width - nativeImage.getWidth()) / 2;
    		}
    		if (nativeImage.getHeight() > displaySettings.height) {
    			y = 0;
    		} else {
    			y = (displaySettings.height - nativeImage.getHeight()) / 2;
    		}
			Resource newResource = display.createResource(nativeImage);
    		if (displayImageResource != null && displayedImageSprite != null) {
    			displayedImageSprite = display.animate(displayedImageSprite, newResource, x, y, BLANK_LAYER+1);
    			displayImageResource.release();
    			displayImageResource = newResource;
    		} else {
    			displayedImageSprite = display.showNow(newResource, x, y, BLANK_LAYER+1);
    			displayImageResource = newResource;
    		}
    	} finally {
    		displayLock.unlock();
    	}
		logger.debug("Image realized:{}", () -> Log4jUtil.completeTimer(IMAGE_TIMER));
	}
	
	@Override
	public void resetSliceCount() {
		//Since this isn't used for debugging we don't do anything
	}

	@Override
	public Rectangle getBoundary() {
		return new Rectangle(displaySettings.width, displaySettings.height);
	}

	@Override
	public boolean isDisplayBusy() {
		return displayLock.isLocked();
	}

	@Override
	public String getIDstring() {
		return displayName;
	}

	@Override
	public String buildIDString() {
		return displayName;
	}

	@Override
	public GraphicsOutputInterface initializeDisplay(String displayId, PrinterConfiguration configuration) {
    	if (display != null) {
			logger.warn("Display already inititalized. Nothing to initialize");
    		return this;
    	}

    	displayLock.lock();
    	try {
    		if (display == null) {
    			display = new Display(screen);
    		}
    		displaySettings = display.getScreenInfo();
    		blankScreenResource = display.createResource((NativeMemoryBackedBufferedImage)buildBufferedImage(displaySettings.width, displaySettings.height));
    		blankScreenSprite = display.showNow(blankScreenResource, 0, 0, BLANK_LAYER);
    	} finally {
    		displayLock.unlock();
    	}
		return this;
	}

	@Override
	public BufferedImage buildBufferedImage(int x, int y) {
		return NativeMemoryBackedBufferedImage.newInstance(x,  y, true);
	}
}
