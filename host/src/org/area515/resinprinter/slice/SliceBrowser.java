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
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;

import org.area515.resinprinter.stl.Face3d;
import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Shape3d;
import org.eclipse.jetty.io.SelectorManager.SelectableEndPoint;

public class SliceBrowser extends JFrame {
	private int firstSlice = 54;
	//78;//"C:\\Users\\wgilster\\Documents\\ArduinoMegaEnclosure.stl";
	//321;//"C:\\Users\\wgilster\\Documents\\ArduinoMegaEnclosureTop.stl"; good
	//781;//"C:\\Users\\wgilster\\Documents\\Fat_Guy_Statue.stl"; good
	
	private String firstFile = "C:\\Users\\wgilster\\Documents\\ArduinoMegaEnclosureBottom.stl";
//	private String firstFile = "C:\\Users\\wgilster\\Documents\\ArduinoMegaEnclosure.stl";//78 & 54
	/*
C:\Users\wgilster\Documents\Olaf_set3_whole.stl
C:\Users\wgilster\Documents\Fat_Guy_Statue.stl
C:\Users\wgilster\Documents\ArduinoMegaEnclosureTop.stl
C:\Users\wgilster\Documents\ArduinoMegaEnclosureBottom.stl
"C:\\Users\\wgilster\\Documents\\ArduinoMega.stl",
"C:\\Users\\wgilster\\Documents\\Olaf_set3_whole.stl",
//http://www.thingiverse.com/download:888699
	 */
	
	
 	private boolean useRender = false;
	private JTextField loadStlText = new JTextField(30);
	private ZSlicer slicer;
	private DefaultBoundedRangeModel zSliceModel = new DefaultBoundedRangeModel();
	private LineSliceModel lineSliceModel = new LineSliceModel();
	private SliceBrowserSelectionListener sliceBrowserListener = new SliceBrowserSelectionListener();
	private JScrollBar zSliceBar = new JScrollBar(JScrollBar.VERTICAL);
	private JSplitPane mainSplitter;
	private JTree sliceTree;
	private int mmPerStlUnit = 1;
	private double pixelsPerMMX = 5;
	private double pixelsPerMMY = 5;
	private double sliceResolution = 0.1;
	private double buildPlatformX = 1024;
	private double buildPlatformY = 500;
	private JPanel browserPanel;
	
	//This is for the ProjectorShowcase
	private int focusX = (int)buildPlatformX / 2;
	private int focusY = (int)buildPlatformY / 2;
	private int centerX = (int)buildPlatformX / 2;
	private int centerY = (int)buildPlatformY / 2;
	private JPanel showcasePanel;
	private DefaultBoundedRangeModel opacityLevelModel;
	private DefaultBoundedRangeModel bulbSizeModel;
	
	//TODO: maybe we should do this instead: private class SliceBrowserSelectionModel extends DefaultTreeSelectionModel {
	private class SliceBrowserSelectionListener implements TreeSelectionListener {
		private List<Line3d> selectedLines = new ArrayList<Line3d>();

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			TreePath nodes[] = e.getPaths();
			for (int t = 0; t < nodes.length; t++) {
				SliceBrowserTreeNode treeNode = (SliceBrowserTreeNode)nodes[t].getLastPathComponent();
				if (treeNode.isLeaf() && treeNode.getUserObject() instanceof Line3d) {
					if (e.isAddedPath(t)) {
						selectedLines.add((Line3d)treeNode.getUserObject());
					} else {
						selectedLines.remove((Line3d)treeNode.getUserObject());
					}
				}
			}
			
			lineSliceModel.refreshGui(null, false);
		}
		
		public void drawSelectedLines(Graphics g) {
    		if (selectedLines != null) {
    			g.setColor(Color.BLUE);
    			//g.setStroke(new BasicStroke(10));
    			for (Line3d line : selectedLines) {
    				g.drawLine((int)line.getPointOne().x, (int)line.getPointOne().y, (int)line.getPointTwo().x, (int)line.getPointTwo().y);
    			}
    		}
		}

		public void clearChildren() {
			selectedLines.clear();
			getSliceTree().clearSelection();
		}
	}
	
	private class SliceBrowserTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 4514477175597266206L;

		public SliceBrowserTreeNode() {
			super("Image Root");
		}
		
		public SliceBrowserTreeNode(Object lines) {
			super(lines);
			/*setUserObject(lines);
			setAllowsChildren(true);*/
		}
	}
	
	private class LineSliceModel extends DefaultTreeModel {
		private static final long serialVersionUID = 179974567762541369L;
		
		public LineSliceModel() {
			super(new SliceBrowserTreeNode());
		}
		
		public void clearChildren() {
			DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)root;
			if (rootNode.getChildCount() > 0) {
				sliceBrowserListener.clearChildren();
				rootNode.removeAllChildren();
				nodeChanged(root);
				reload();
				browserPanel.repaint();
			}
		}
		
		public void refreshGui(JLabel mouseLabel, boolean refreshTreeNodes) {
			if (refreshTreeNodes) {
				DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)root;
				rootNode.removeAllChildren();
				try {
					 List<List<Line3d>> coloredLines = slicer.colorizePolygons();
					 int t = 0;
					 for (List<Line3d> loops : coloredLines) {
						 SliceBrowserTreeNode parent = new SliceBrowserTreeNode("Slice:" + slicer.getZ() + " #" + t++ + " :(" + loops.size() + ")");
						 rootNode.add(parent);
						 for (Line3d line : loops) {
							 parent.add(new SliceBrowserTreeNode(line));
						 }
					 }
					 nodeChanged(root);
					 reload();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
			
			if (mouseLabel != null) {
				mouseLabel.setText("Z:" + slicer.getZ() + " Area:" + slicer.getBuildArea());
			}
			browserPanel.repaint();
		}
	}
	
	private void drawBuildPlatform(Graphics2D g2) {
		g2.setColor(Color.black);
		g2.drawRect(1, 1, (int)buildPlatformX, (int)buildPlatformY);
	}
	
	private JTree getSliceTree() {
		if (sliceTree == null) {
			sliceTree = new JTree(lineSliceModel);
			sliceTree.setExpandsSelectedPaths(true);
			sliceTree.addTreeSelectionListener(sliceBrowserListener);
			//sliceTree.setSelectionModel(selectionModel);
		}

		return sliceTree;
	}
	
	private JSplitPane getMainSplitter() throws Exception {
		if (mainSplitter == null) {
			mainSplitter = new JSplitPane();
			mainSplitter.setTopComponent(getSliceBrowser());
			JScrollPane scroller = new JScrollPane(getSliceTree());
			mainSplitter.setBottomComponent(scroller);
		}
		
		return mainSplitter;
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
		private static final long serialVersionUID = 5363505068904537189L;

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			applyProjectorMask((Graphics2D)g);
			drawBuildPlatform((Graphics2D)g);
		}
	}
	
	private void loadStl(Integer firstSlice) {
		 ZSlicer newSlicer = new ZSlicer(
			 new File(loadStlText.getText()),
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
		zSliceModel.setMaximum(slicer.getZMax());
		zSliceModel.setMinimum(slicer.getZMin());
		if (firstSlice == null) {
			firstSlice = slicer.getZMin();
		}
		
		firstSlice = Math.min(Math.max(firstSlice, slicer.getZMin()), slicer.getZMax());
		zSliceModel.setValue(firstSlice);
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
				browserPanel = new JPanel() {
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
				    		
				    		sliceBrowserListener.drawSelectedLines(g);

							drawBuildPlatform((Graphics2D)g);
				    	}
				    }
				};
				
				browserPanel.addMouseMotionListener(new MouseAdapter() {
					@Override
					public void mouseMoved(MouseEvent e) {
						if (slicer == null) {
							return;
						}
						mouseLabel.setText("X:" + e.getX() + " Y:" + e.getY() + " Z:" + slicer.getZ() + " Area:" + slicer.getBuildArea());
					}
				});
				browserPanel.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						List<Shape3d> shapes = slicer.getTrianglesAt(e.getX(), e.getY());
						for (Shape3d shape : shapes) {
							Line3d line = (Line3d)shape;
							Face3d face = line.getOriginatingFace();
							
							System.out.println(slicer.translateLineToString(line));
							System.out.println("stltriangle: "+ face + " Hash:" + face.hashCode());
						}					
						
						List<TreePath> selectedPaths = new ArrayList<TreePath>();
						Enumeration depth = ((SliceBrowserTreeNode)lineSliceModel.getRoot()).depthFirstEnumeration();
						while (depth.hasMoreElements()) {
							SliceBrowserTreeNode currentNode = (SliceBrowserTreeNode)depth.nextElement();
							System.out.println(currentNode);
							for (Shape3d shape : shapes) {
								Line3d line = slicer.translateLine(((Line3d)shape));
								
								if (line.pointsEqual(currentNode.getUserObject())) {
									selectedPaths.add(new TreePath(currentNode.getPath()));
								}
							}
						}
						
						if (e.isShiftDown() || e.isControlDown()) {
							getSliceTree().addSelectionPaths(selectedPaths.toArray(new TreePath[selectedPaths.size()]));
						} else {
							getSliceTree().setSelectionPaths(selectedPaths.toArray(new TreePath[selectedPaths.size()]));
						}
					}
				});
				
				final JPanel bottomPanel = new JPanel(new FlowLayout());
				final JButton colorize = new JButton("Alpha Colorize");
				final JButton render = new JButton("Render");
				zSliceBar.setModel(zSliceModel);
				zSliceBar.addAdjustmentListener(new AdjustmentListener() {
					@Override
					public void adjustmentValueChanged(AdjustmentEvent e) {
						slicer.setZ(e.getValue());
						mouseLabel.setText("Z:" + slicer.getZ());
						useRender = false;
						sliceBrowserListener.clearChildren();
						lineSliceModel.clearChildren();
						lineSliceModel.refreshGui(mouseLabel, false);
					}
				});
				
				colorize.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						useRender = false;
						lineSliceModel.refreshGui(mouseLabel, true);
					}
				});
				
				render.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						useRender = true;
						lineSliceModel.refreshGui(mouseLabel, true);
					}
				});
				
				loadStlButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						loadStl(null);
						sliceBrowserListener.clearChildren();
						lineSliceModel.refreshGui(null, false);
					}
				});
				
				bottomPanel.add(mouseLabel);
				bottomPanel.add(colorize);
				bottomPanel.add(render);
				bottomPanel.add(loadStlText);
				bottomPanel.add(loadStlButton);
				window.add(zSliceBar, BorderLayout.EAST);
				window.add(browserPanel, BorderLayout.CENTER);
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
		  tabs.addTab("Slice Browser", getMainSplitter());
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

