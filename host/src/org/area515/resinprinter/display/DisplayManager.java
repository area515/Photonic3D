package org.area515.resinprinter.display;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class DisplayManager {

	private static DisplayManager m_instance = null;
	private static BufferedImage blank = null;
	public static DisplayManager Instance() {
		if (m_instance == null) {
			m_instance = new DisplayManager();
		}
		return m_instance;
	}
	
	private DisplayManager(){
		setup();
	}
	
	GraphicsEnvironment ge;
	GraphicsConfiguration gc;
	GraphicsDevice device;
	JFrame window;
	Graphics2D graphics;
	
	private void setup(){
	 ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
	gc = ge.getDefaultScreenDevice()
				.getDefaultConfiguration();
		
	device = ge.getDefaultScreenDevice();
	window = new JFrame();
		System.out.println(device.isFullScreenSupported());
		window.setUndecorated(true);
		device.setFullScreenWindow(window);
		
	graphics = (Graphics2D) window.getGraphics();	
	blank = new BufferedImage(1024, 768,
		    BufferedImage.TYPE_BYTE_BINARY);
	
	// hide mouse in full screen
	Toolkit toolkit = Toolkit.getDefaultToolkit();
    Point hotSpot = new Point(0,0);
    BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT); 
    Cursor invisibleCursor = toolkit.createCustomCursor(cursorImage, hotSpot, "InvisibleCursor");        
    window.setCursor(invisibleCursor);
//    setCursor(invisibleCursor);
	}

	public void Show(BufferedImage bImage){
	
		graphics.drawImage(bImage,null,0,0);
	}
	
	public void ShowBlank(){
		graphics.drawImage(blank,null,0,0);
	}
	
	public void Close(){
		window.dispose();
		graphics.dispose();
	}
}