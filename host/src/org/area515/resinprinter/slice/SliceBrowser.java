package org.area515.resinprinter.slice;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.util.List;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import org.area515.resinprinter.stl.Face3d;
import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Shape3d;

public class SliceBrowser extends JFrame{
	private int firstSlice = 0;//781;//bad frame in big guy
	private String firstFile = "C:\\Users\\wgilster\\Documents\\ArduinoMegaEnclosureTop.stl";
	/*
	 * 
C:\Users\wgilster\Documents\Olaf_set3_whole.stl
C:\Users\wgilster\Documents\Fat_Guy_Statue.stl
C:\Users\wgilster\Documents\ArduinoMegaEnclosureTop.stl
C:\Users\wgilster\Documents\ArduinoMegaEnclosureBottom.stl

"C:\\Users\\wgilster\\Documents\\ArduinoMega.stl",
"C:\\Users\\wgilster\\Documents\\Olaf_set3_whole.stl",

	 */
	
	
 	private boolean useRender = false;
	private JTextField loadStlText = new JTextField(30);
	private ZSlicer slicer;
	private DefaultBoundedRangeModel sliceModel = new DefaultBoundedRangeModel();
	private JScrollBar sliceBar = new JScrollBar(JScrollBar.VERTICAL);
	
	private int mmPerStlUnit = 1;//We need to scale the whole stl large enough to have high enough precision before the decimal point
	private double pixelsPerMMX = 5;
	private double pixelsPerMMY = 5;
	private double sliceResolution = 0.1;
	private double buildPlatformX = 1024;
	private double buildPlatformY = 500;
	
	//This is for the ProjectorShowcase
	private int focusX = (int)buildPlatformX / 2;
	private int focusY = (int)buildPlatformY / 2;
	private int centerX = (int)buildPlatformX / 2;
	private int centerY = (int)buildPlatformY / 2;
	private JPanel showcasePanel;
	private DefaultBoundedRangeModel opacityLevelModel;
	private DefaultBoundedRangeModel bulbSizeModel;
	
	private void drawBuildPlatform(Graphics2D g2) {
		g2.setColor(Color.black);
		g2.drawRect(1, 1, (int)buildPlatformX, (int)buildPlatformY);
	}
	
	private float[] getFractions(int count, float start, float end) {
		float incrementAmount = (end - start) / (float)count;
		float fractions[] = new float[count];
		for (int t = 0; t < count; t++) {
			fractions[t] = start + incrementAmount * t;
		}
		//return new float[]{0, 1};
		return fractions;
	}
	
	private Color[] getColors(float[] fractions, float start, float stop) {
		Color colors[] = new Color[fractions.length];
		float colorRange = stop - start;
		float atanDivergencePoint = (float)Math.PI / 2;
		for (int t = 0; t < fractions.length; t++) {
			colors[t] = new Color(0, 0, 0, (float)(Math.atan(fractions[t] * atanDivergencePoint)) * colorRange + start);
		}
		//return new Color[]{new Color(0, 0, 0, (float)opacityLevelModel.getValue()/(float)opacityLevelModel.getMaximum()), new Color(0, 0, 0, 0)};
		return colors;
	}
	
	private void applyProjectorMask(Graphics2D g2) {
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
	}
	
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
	
	private class ProjectorMaskCreatorPanel extends JPanel {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			applyProjectorMask((Graphics2D)g);
			drawBuildPlatform((Graphics2D)g);
		}
	}
	
	private void loadStl(Integer firstSlice) {
		 ZSlicer newSlicer = new ZSlicer(
			 loadStlText.getText(),
			 mmPerStlUnit,
			 pixelsPerMMX,
			 pixelsPerMMY,
			 sliceResolution,
			 true);
		try {
			newSlicer.loadFile(buildPlatformX, buildPlatformY);
			slicer = newSlicer;
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "File not found:" + loadStlText.getText());
			return;
		}
		sliceModel.setMaximum(slicer.getZMax());
		sliceModel.setMinimum(slicer.getZMin());
		if (firstSlice == null) {
			firstSlice = slicer.getZMin();
		}
		
		firstSlice = Math.min(Math.max(firstSlice, slicer.getZMin()), slicer.getZMax());
		sliceModel.setValue(firstSlice);
		slicer.setZ(firstSlice);
	}
	
	  public JComponent getSliceBrowser() throws Exception {
		  if (firstFile != null) {
				loadStlText.setText(firstFile);
				loadStl(firstSlice);
		  }
			 
			 final JButton loadStlButton = new JButton("Load");
			 
				final JLabel mouseLabel = new JLabel("X:" + " Y:" + "Z:");
				final JPanel window = new JPanel();
				window.setLayout(new BorderLayout());
				final JPanel panel = new JPanel() {
				    public void paintComponent(Graphics g) {
				    	super.paintComponent(g);
				    	if (slicer == null) {
				    		return;
				    	}
				    	
				    	if (useRender) {
				    		slicer.paintSlice((Graphics2D)g);
				    		applyProjectorMask((Graphics2D)g);
				    	} else {
				    		slicer.debugPaintSlice((Graphics2D)g);
							drawBuildPlatform((Graphics2D)g);
				    	}
				    }
				};
				
				panel.addMouseMotionListener(new MouseAdapter() {
					@Override
					public void mouseMoved(MouseEvent e) {
						if (slicer == null) {
							return;
						}
						mouseLabel.setText("X:" + e.getX() + " Y:" + e.getY() + " Z:" + slicer.getZ() + " Area:" + slicer.getBuildArea());
					}
				});
				panel.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						List<Shape3d> shapes = slicer.getTrianglesAt(e.getX(), e.getY());
						for (Shape3d shape : shapes) {
							Line3d line = (Line3d)shape;
							Face3d face = line.getOriginatingFace();
							System.out.println("line: x1:" + slicer.translateX(line.getPointOne().x) + 
													",y1:" + slicer.translateY(line.getPointOne().y) + 
													" x2:" + slicer.translateY(line.getPointTwo().x) + 
													",y2:" + slicer.translateY(line.getPointTwo().y) );
							System.out.println("stltriangle: "+ face + " Hash:" + face.hashCode());
						}
					}
				});
				
				final JPanel bottomPanel = new JPanel(new FlowLayout());
				final JButton colorize = new JButton("Alpha Colorize");
				final JButton render = new JButton("Render");
				sliceBar.setModel(sliceModel);
				sliceBar.addAdjustmentListener(new AdjustmentListener() {
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
						try {
							slicer.colorizePolygons();
						} catch (Throwable t) {
							t.printStackTrace();
						}
						mouseLabel.setText("Z:" + slicer.getZ() + " Area:" + slicer.getBuildArea());
						panel.repaint();
					}
				});
				
				render.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						useRender = true;
						try {
							slicer.colorizePolygons();
						} catch (Throwable t) {
							t.printStackTrace();
						}
						mouseLabel.setText("Z:" + slicer.getZ() + " Area:" + slicer.getBuildArea());
						panel.repaint();
					}
				});
				
				loadStlButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						loadStl(null);
						panel.repaint();
					}
				});
				
				bottomPanel.add(mouseLabel);
				bottomPanel.add(colorize);
				bottomPanel.add(render);
				bottomPanel.add(loadStlText);
				bottomPanel.add(loadStlButton);
				window.add(sliceBar, BorderLayout.EAST);
				window.add(panel, BorderLayout.CENTER);
				window.add(bottomPanel, BorderLayout.SOUTH);

				return window;
	  }
	  
	  public JComponent getProjectorMaskCreator() {
			opacityLevelModel = new ShowcaseUpdaterModel(20, 0, 0, 100);
			bulbSizeModel = new ShowcaseUpdaterModel(200, 0, 0, 2048);
			
			final JPanel masterPanel = new JPanel();
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
			
			masterPanel.setLayout(new BorderLayout());
			masterPanel.add(showcasePanel, BorderLayout.CENTER);
			masterPanel.add(transparencySlider, BorderLayout.WEST);
			masterPanel.add(bulbSizeSlider, BorderLayout.EAST);
			return masterPanel;
	  }
	  
	  public SliceBrowser() throws Exception {
		  JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
		  tabs.addTab("Slice Browser", getSliceBrowser());
		  tabs.addTab("Projector Mask Creator", getProjectorMaskCreator());
		  add(tabs);
		  
		  setTitle("Printer Simulation");
//window.setExtendedState(JFrame.MAXIMIZED_BOTH);
		  setMinimumSize(new Dimension(500, 500));
		  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	  }
	  
	  public static void main(String[] args) throws Exception {
		  SliceBrowser browser = new SliceBrowser();
		  browser.setVisible(true);
	  }
}

