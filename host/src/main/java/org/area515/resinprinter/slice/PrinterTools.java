package org.area515.resinprinter.slice;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.area515.resinprinter.inkdetection.visual.ImageDetectionPanel;
import org.area515.resinprinter.projector.ProjectorMaskCreator;

public class PrinterTools extends JFrame {
	private SliceBrowser sliceBrowser;
	private ImageDetectionPanel imageDetectionPanel;
	private ProjectorMaskCreator projectorMaskCreatorPanel;
	private double buildPlatformX = 1024;
	private double buildPlatformY = 500;

	public void drawBuildPlatform(Graphics2D g2) {
		g2.setColor(Color.black);
		g2.drawRect(1, 1, (int)buildPlatformX, (int)buildPlatformY);
	}
	
	public PrinterTools() throws Exception {
		JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
		tabs.addTab("Slice Browser", getSliceBrowser());
		tabs.addTab("Projector Mask Creator", getProjectorMaskCreator());
		tabs.addTab("Image Detector", getImageDetector());
		add(tabs);

		setTitle("Printer Simulation");
		// window.setExtendedState(JFrame.MAXIMIZED_BOTH);
		setMinimumSize(new Dimension(1000, 500));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	}
	
	public double getBuildPlatformX() {
		return buildPlatformX;
	}

	public double getBuildPlatformY() {
		return buildPlatformY;
	}
	
	public ImageDetectionPanel getImageDetector() throws Exception {
		if (imageDetectionPanel == null) {
			imageDetectionPanel = new ImageDetectionPanel();
		}

		return imageDetectionPanel;
	}

	public ProjectorMaskCreator getProjectorMaskCreator() throws Exception {
		if (projectorMaskCreatorPanel == null) {
			projectorMaskCreatorPanel = new ProjectorMaskCreator(this);
		}

		return projectorMaskCreatorPanel;
	}

	public SliceBrowser getSliceBrowser() throws Exception {
		if (sliceBrowser == null) {
			sliceBrowser = new SliceBrowser(this);
		}
		
		return sliceBrowser;
	}
	
	public static void main(String[] args) throws Exception {
		PrinterTools browser = new PrinterTools();
		browser.setVisible(true);
		// browser.getColors(browser.getFractions(1600, .0f, 1f), .2f, .0f);
	}
}
