package org.area515.resinprinter.display;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.HeadlessException;

import javax.swing.JFrame;

public class SimulatedDisplay extends GraphicsDeviceOutputInterface {
	public static final String NAME = "Simulated display";
	public int displayIndex;
	
	public SimulatedDisplay() {
		super(NAME, null);
	}

	@Override
	public GraphicsDevice getGraphicsDevice() {
		try {
			return DisplayManager.Instance().getGraphicsEnvironment().getScreenDevices()[0];
		} catch (HeadlessException | InappropriateDeviceException e) {
			throw new IllegalArgumentException("Couldn't get screen devices from display manager.", e);
		}
	}

	@Override
	public String buildIDString() {
		return NAME + ":" + displayIndex++;
	}

	@Override
	public GraphicsOutputInterface initializeDisplay(String displayId) {
		PrinterDisplayFrame refreshFrame = new PrinterDisplayFrame(displayId);
		refreshFrame.setTitle(displayId);
		refreshFrame.setVisible(true);
		refreshFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		refreshFrame.setMinimumSize(new Dimension(500, 500));
		return refreshFrame;
	}
}
