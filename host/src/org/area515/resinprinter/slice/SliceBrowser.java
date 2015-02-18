package org.area515.resinprinter.slice;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

public class SliceBrowser {
	  public static void main(String[] args) throws Exception {
		 int z = 384;//600;//0, 384, 387, 388, 548, 691, 709;
		 int precisionScaler = 100000;//We need to scale the whole stl large enough to have high enough precision before the decimal point
		 double pixelsPerMMX = 10;
		 double pixelsPerMMY = 10;
		 double imageOffsetX = 35 * pixelsPerMMX;
		 double imageOffsetY = 25 * pixelsPerMMY;
		 double sliceResolution = 0.1;
		 
		 final ZSlicer slicer = new ZSlicer(
				 //"C:\\Users\\wgilster\\Documents\\ArduinoMega.stl
				 "C:\\Users\\wgilster\\Documents\\Olaf_set3_whole.stl", 
				 precisionScaler, 
				 pixelsPerMMX, 
				 pixelsPerMMY, 
				 imageOffsetX, 
				 imageOffsetY, 
				 sliceResolution);
		 slicer.setZ(z);
		 slicer.loadFile();
		 
			JFrame window = new JFrame();
			window.setLayout(new BorderLayout());
			final JPanel panel = new JPanel() {
			    public void paintComponent(Graphics g) {
			    	super.paintComponent(g);
			    	slicer.debugPaintSlice((Graphics2D)g);
			    }
			};
	
			final JButton colorize = new JButton("Colorize:" + slicer.getZ());
			JScrollBar bar = new JScrollBar(JScrollBar.VERTICAL, slicer.getZ(), 0, slicer.getZMin(), slicer.getZMax());
			bar.addAdjustmentListener(new AdjustmentListener(){
				@Override
				public void adjustmentValueChanged(AdjustmentEvent e) {
					slicer.setZ(e.getValue());
					colorize.setText("Colorize:" + e.getValue());
					panel.repaint();
				}
			});
			
			colorize.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					slicer.colorizePolygons();
					panel.repaint();
				}
			});

			
			window.add(bar, BorderLayout.EAST);
			window.add(panel, BorderLayout.CENTER);
			window.add(colorize, BorderLayout.SOUTH);
			
			window.setTitle("Printer Simulation");
			window.setVisible(true);
			//window.setExtendedState(JFrame.MAXIMIZED_BOTH);
			window.setMinimumSize(new Dimension(500, 500));
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	  }
}

