package org.area515.resinprinter.slice;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

import org.area515.resinprinter.stl.Face3d;
import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Shape3d;

public class SliceBrowser {
	  public static boolean useRender = false;
	  
	  public static void main(String[] args) throws Exception {
		 int z = 933;//547;
		 final int precisionScaler = 100000;//We need to scale the whole stl large enough to have high enough precision before the decimal point
		 final double pixelsPerMMX = 5;
		 final double pixelsPerMMY = 5;
		 final double imageOffsetX = 30 * pixelsPerMMX;
		 final double imageOffsetY = 30 * pixelsPerMMY;
		 double sliceResolution = 0.1;
		 
		 final ZSlicer slicer = new ZSlicer(
				 //"C:\\Users\\wgilster\\Documents\\ArduinoMega.stl",
				 //"C:\\Users\\wgilster\\Documents\\Olaf_set3_whole.stl",
				 "C:\\Users\\wgilster\\Documents\\Fat_Guy_Statue.stl", 
				 precisionScaler, 
				 pixelsPerMMX, 
				 pixelsPerMMY, 
				 imageOffsetX, 
				 imageOffsetY, 
				 sliceResolution,
				 true);
		 slicer.setZ(z);
		 slicer.loadFile();
		 
			final JLabel mouseLabel = new JLabel("X:" + " Y:" + "Z:");
			JFrame window = new JFrame();
			window.setLayout(new BorderLayout());
			final JPanel panel = new JPanel() {
			    public void paintComponent(Graphics g) {
			    	super.paintComponent(g);
			    	
			    	if (useRender) {
			    		slicer.paintSlice((Graphics2D)g);
			    	} else {
			    		slicer.debugPaintSlice((Graphics2D)g);
			    	}
			    }
			};
			
			panel.addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					mouseLabel.setText("X:" + e.getX() + " Y:" + e.getY() + " Z:" + slicer.getZ());
				}
			});
			panel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					List<Shape3d> shapes = slicer.getTrianglesAt(e.getX(), e.getY());
					for (Shape3d shape : shapes) {
						Line3d line = (Line3d)shape;
						Face3d face = line.getOriginatingFace();
						System.out.println("line: x1:" + (line.getPointOne().x / precisionScaler * pixelsPerMMX + imageOffsetX) + 
												",y1:" + (line.getPointOne().y / precisionScaler * pixelsPerMMY + imageOffsetY) + 
												" x2:" + (line.getPointTwo().x / precisionScaler * pixelsPerMMX + imageOffsetX) + 
												",y2:" + (line.getPointTwo().y / precisionScaler * pixelsPerMMY + imageOffsetY) );
						System.out.println("stltriangle: "+ face + " Hash:" + face.hashCode());
					}
				}
			});
			
			final JPanel bottomPanel = new JPanel(new FlowLayout());
			final JButton colorize = new JButton("Alpha Colorize");
			final JButton render = new JButton("Render");
			JScrollBar bar = new JScrollBar(JScrollBar.VERTICAL, slicer.getZ() < slicer.getZMin()?slicer.getZMin():slicer.getZ(), 0, slicer.getZMin(), slicer.getZMax());
			bar.addAdjustmentListener(new AdjustmentListener() {
				@Override
				public void adjustmentValueChanged(AdjustmentEvent e) {
					slicer.setZ(e.getValue());
					mouseLabel.setText("Z:" + slicer.getZ());
					panel.repaint();
					useRender = false;
				}
			});
			
			colorize.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					useRender = false;
					slicer.colorizePolygons();
					panel.repaint();
				}
			});
			
			render.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					useRender = true;
					slicer.colorizePolygons();
					panel.repaint();
				}
			});
			bottomPanel.add(mouseLabel);
			bottomPanel.add(colorize);
			bottomPanel.add(render);

			window.add(bar, BorderLayout.EAST);
			window.add(panel, BorderLayout.CENTER);
			window.add(bottomPanel, BorderLayout.SOUTH);
			
			window.setTitle("Printer Simulation");
			window.setVisible(true);
			//window.setExtendedState(JFrame.MAXIMIZED_BOTH);
			window.setMinimumSize(new Dimension(500, 500));
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	  }
}

