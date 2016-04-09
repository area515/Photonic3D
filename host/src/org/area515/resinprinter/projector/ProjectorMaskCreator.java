package org.area515.resinprinter.projector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import org.area515.resinprinter.slice.PrinterTools;
import org.area515.resinprinter.slice.SliceBrowser;

public class ProjectorMaskCreator extends JComponent {
	private PrinterTools tools;
	private JPanel showcasePanel;
	private DefaultBoundedRangeModel opacityLevelModel;
	private DefaultBoundedRangeModel bulbSizeModel;	
	private int focusX;
	private int focusY;
	private int centerX;
	private int centerY;

	private class ShowcaseUpdaterModel extends DefaultBoundedRangeModel {
		public ShowcaseUpdaterModel(int value, int extent, int min, int max) {
			super(value, extent, min, max);
		}
	
		@Override
		public void setValue(int n) {
			super.setValue(n);
			showcasePanel.repaint();
		}
	}

	public ProjectorMaskCreator(PrinterTools tools) {
		this.tools = tools;
		focusX = (int)tools.getBuildPlatformX() / 2;
		focusY = (int)tools.getBuildPlatformY() / 2;
		centerX = (int)tools.getBuildPlatformX() / 2;
		centerY = (int)tools.getBuildPlatformY() / 2;
			opacityLevelModel = new ShowcaseUpdaterModel(20, 0, 0, 100);
			bulbSizeModel = new ShowcaseUpdaterModel(200, 0, 0, 2048);
			
			showcasePanel = new ProjectorMaskCreatorPanel();
			showcasePanel.addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseDragged(MouseEvent e) {
					if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) {
						focusX = e.getX();
						focusY = e.getY();
						showcasePanel.repaint();
					}
				}
			});
			
			showcasePanel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					centerX = e.getX();
					centerY = e.getY();
					showcasePanel.repaint();
				}
			});
			
			JSlider transparencySlider = new JSlider(SwingConstants.VERTICAL);
			transparencySlider.setModel(opacityLevelModel);
			
			JSlider bulbSizeSlider = new JSlider(SwingConstants.VERTICAL);
			bulbSizeSlider.setModel(bulbSizeModel);
			
			setLayout(new BorderLayout());
			add(showcasePanel, BorderLayout.CENTER);
			add(transparencySlider, BorderLayout.WEST);
			add(bulbSizeSlider, BorderLayout.EAST);
	  }

	public void applyProjectorMask(Graphics2D g2) {
		g2.setPaintMode();
		Rectangle r = this.getBounds();//g2.getDeviceConfiguration().getBounds();
		
		Point2D bulbCenter = new Point2D.Double(centerX, centerY);
		Point2D bulbFocus = new Point2D.Double(focusX > 0?focusX:(r.width / 2), focusY > 0?focusY:(r.height / 2));
		float[] fractions = getFractions(bulbSizeModel.getValue(), 0, 1);
		Color[] colors = getColors(fractions, (float)opacityLevelModel.getValue()/(float)opacityLevelModel.getMaximum(), 0);
		final RadialGradientPaint paint = new RadialGradientPaint(
				bulbCenter, 
				bulbSizeModel.getValue(), 
				bulbFocus, 
				fractions, 
				colors, 
				CycleMethod.NO_CYCLE);
		g2.setPaint(paint);
		g2.fillRect(r.x, r.y, r.width, r.height);
		
		System.out.println("Bulb Mask Properties");
		System.out.println("====================");
		System.out.println("var bulbCenter = new Packages.java.awt.geom.Point2D.Double($buildPlatformXPixels * " + ((float)centerX / (float)r.width) + ", $buildPlatformYPixels * " + ((float)centerY / (float)r.height) + ")");
		System.out.println("var bulbFocus = new Packages.java.awt.geom.Point2D.Double($buildPlatformXPixels * " + (bulbFocus.getX() / r.width) + ", $buildPlatformYPixels * " + (bulbFocus.getY() / r.height) + ")");
		System.out.println("var colors = [new Packages.java.awt.Color(0.0, 0.0, 0.0, " + ((float)opacityLevelModel.getValue()/(float)opacityLevelModel.getMaximum()) + "), new Packages.java.awt.Color(0.0, 0.0, 0.0, 0.0)];");
		System.out.println("var fractions = [0.0, 1.0];");
		System.out.println("var totalSizeOfGradient = $buildPlatformXPixels > $buildPlatformYPixels?$buildPlatformXPixels:$buildPlatformYPixels;");
		int totalSizeOfGradient = r.width > r.height? r.width: r.height;
		System.out.println("new Packages.java.awt.RadialGradientPaint(bulbCenter, totalSizeOfGradient * " + ((float)bulbSizeModel.getValue()/(float)totalSizeOfGradient) + ", bulbFocus, fractions, colors, java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE);");
		System.out.println("====================");
	}
	
	private class ProjectorMaskCreatorPanel extends JPanel {
		private static final long serialVersionUID = 5363505068904537189L;

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			applyProjectorMask((Graphics2D)g);
			tools.drawBuildPlatform((Graphics2D)g);
		}
	}

	private float[] getFractions(int count, float start, float end) {
		/*float incrementAmount = (end - start) / (float)count;
		float fractions[] = new float[count];
		for (int t = 0; t < count; t++) {
			fractions[t] = start + incrementAmount * t;
		}
		return fractions;
		*/
		return new float[]{0, 1};
	}
	
	private Color[] getColors(float[] fractions, float start, float stop) {
		/*Color colors[] = new Color[fractions.length];
		float colorRange = stop - start;
		float atanDivergencePoint = (float)Math.PI / 2;
		for (int t = 0; t < fractions.length; t++) {
			colors[t] = new Color(0, 0, 0, (float)(Math.atan(fractions[t] * atanDivergencePoint)) * colorRange + start);
		}
		return colors;*/
		return new Color[]{new Color(0, 0, 0, (float)opacityLevelModel.getValue()/(float)opacityLevelModel.getMaximum()), new Color(0, 0, 0, 0)};
	}
}
