package org.area515.resinprinter.slice;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.area515.resinprinter.slice.StlError.ErrorType;
import org.area515.resinprinter.stl.Face3d;
import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Shape3d;
import org.area515.resinprinter.stl.Triangle3d;

public class SliceBrowser extends JSplitPane {
	private PrinterTools tools;
	
	private int firstSlice = 1;
	//95 CornerBracket_2.stl
	//C:\\Users\\wgilster\\Desktop\\fdhgg.stl
	//78;//"C:\\Users\\wgilster\\Documents\\ArduinoMegaEnclosure.stl";
	//321;//"C:\\Users\\wgilster\\Documents\\ArduinoMegaEnclosureTop.stl"; good
	//781;//"C:\\Users\\wgilster\\Documents\\Fat_Guy_Statue.stl"; good
	//"C:\\Users\\wgilster\\git\\Creation-Workshop-Host\\host\\src\\test\\resources\\org\\area515\\resinprinter\\slice\\CornerBracket_2.stl"
	
	private String firstFile = "C:\\Users\\wgilster\\Documents\\fdhgg.stl";//1,200,670
//	private String firstFile = "C:\\Users\\wgilster\\Documents\\NonManifoldBox.stl";//-19
//	private String firstFile = "C:\\Users\\wgilster\\Documents\\Fat_Guy_Statue.stl";
//	private String firstFile = "C:\\Users\\wgilster\\AppData\\Local\\Temp\\uploaddir\\CornerBracket_2.stl";//95
//	private String firstFile = "C:\\Users\\wgilster\\Documents\\ArduinoMegaEnclosureBottom.stl"; 54
//	private String firstFile = "C:\\Users\\wgilster\\Documents\\ArduinoMegaEnclosure.stl";//78, 54
	/*
C:\Users\wgilster\Documents\Olaf_set3_whole.stl
C:\Users\wgilster\Documents\Fat_Guy_Statue.stl
C:\Users\wgilster\Documents\ArduinoMegaEnclosureTop.stl
C:\Users\wgilster\Documents\ArduinoMegaEnclosureBottom.stl
"C:\\Users\\wgilster\\Documents\\ArduinoMega.stl",
"C:\\Users\\wgilster\\Documents\\Olaf_set3_whole.stl",
//http://www.thingiverse.com/download:888699

	 */
	
	//C:\\Users\\wgilster\\AppData\\Local\\Temp\\uploaddir\\CornerBracket_2.stl
	//0,10,30,140,148,205
	
 	private boolean useRender = false;
	private JTextField loadStlText = new JTextField(30);
	private ZSlicer slicer;
	private DefaultBoundedRangeModel zSliceModel = new DefaultBoundedRangeModel();
	private LineSliceModel lineSliceModel = new LineSliceModel();
	private SliceBrowserSelectionListener sliceBrowserListener = new SliceBrowserSelectionListener();
	private JScrollBar zSliceBar = new JScrollBar(JScrollBar.VERTICAL);
	private JTree sliceTree;
	private int mmPerStlUnit = 1;
	private double pixelsPerMMX = 5;
	private double pixelsPerMMY = 5;
	private double sliceResolution = 0.1;
	private JPanel browserPanel;
	private List<Integer> watchYs = null;
	
	//TODO: maybe we should do this instead: private class SliceBrowserSelectionModel extends DefaultTreeSelectionModel {
	private class SliceBrowserSelectionListener implements TreeSelectionListener {
		private List<Line3d> selectedLines = new ArrayList<Line3d>();

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			TreePath nodes[] = e.getPaths();
			for (int t = 0; t < nodes.length; t++) {
				SliceBrowserTreeNode treeNode = (SliceBrowserTreeNode)nodes[t].getLastPathComponent();
				if (treeNode.getUserObject() instanceof Line3d) {
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
		
		public List<Triangle3d> getSelectedTriangles() {
			List<Triangle3d> triangles = new ArrayList<Triangle3d>();
			for (Line3d line : selectedLines) {
				triangles.add((Triangle3d)line.getOriginatingFace());
			}
			
			return triangles;
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
	
	private class ToolTipRenderer extends DefaultTreeCellRenderer {
		public Component getTreeCellRendererComponent(
		                        JTree tree,
		                        Object value,
		                        boolean sel,
		                        boolean expanded,
		                        boolean leaf,
		                        int row,
		                        boolean hasFocus) {
		     super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		     if (leaf) {
		    	 Object newValue = ((SliceBrowserTreeNode)value).getUserObject();
		    	 if (newValue instanceof Line3d) {
		    		 setToolTipText(((Line3d)newValue).getNormal() + "");
		    	 }
		     } else {
		    	 setToolTipText(null);
		     }
		     
		     return this;
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
					 List<List<Line3d>> coloredLines = slicer.colorizePolygons(null, null);
					 int t = 0;
					 for (List<Line3d> loops : coloredLines) {
						 SliceBrowserTreeNode parent = new SliceBrowserTreeNode("Slice:" + slicer.getZIndex() + " #" + t++ + " :(" + loops.size() + ")");
						 rootNode.add(parent);
						 for (Line3d line : loops) {
							 SliceBrowserTreeNode lineNode = new SliceBrowserTreeNode(line);
							 parent.add(lineNode);
							 Face3d face3d = line.getOriginatingFace();
							 
							 if (face3d instanceof Triangle3d) {
								 lineNode.add(new SliceBrowserTreeNode(slicer.translateTriangle((Triangle3d)face3d)));
							 }
						 }
					 }
					 nodeChanged(root);
					 reload();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
			
			if (mouseLabel != null) {
				mouseLabel.setText("Z:" + slicer.getZIndex() + " Area:" + slicer.getBuildArea());
			}
			browserPanel.repaint();
		}
	}
	
	private JTree getSliceTree() {
		if (sliceTree == null) {
			sliceTree = new JTree(lineSliceModel);
			sliceTree.setExpandsSelectedPaths(true);
			sliceTree.addTreeSelectionListener(sliceBrowserListener);
			sliceTree.setCellRenderer(new ToolTipRenderer());
			ToolTipManager.sharedInstance().registerComponent(sliceTree);
		}

		return sliceTree;
	}
	
	private void loadStl(Integer firstSlice) {
		 ZSlicer newSlicer = new ZSlicer(
			 mmPerStlUnit,
			 pixelsPerMMX,
			 pixelsPerMMY,
			 sliceResolution,
			 0d,
			 false,
			 false, 
			 new CloseOffMend());
		try {
			newSlicer.loadFile(new FileInputStream(new File(loadStlText.getText())), tools.getBuildPlatformX(), tools.getBuildPlatformY());
			slicer = newSlicer;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "File not found? " + loadStlText.getText());
			return;
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Error loading stl file: " + loadStlText.getText() + ": " + e.getMessage());
			return;
		}
		zSliceModel.setMaximum(slicer.getZMaxIndex());
		zSliceModel.setMinimum(slicer.getZMinIndex());
		if (firstSlice == null) {
			firstSlice = slicer.getZMinIndex();
		}
		
		firstSlice = Math.min(Math.max(firstSlice, slicer.getZMinIndex()), slicer.getZMaxIndex());
		zSliceModel.setValue(firstSlice);
		slicer.setZIndex(firstSlice);
	}
	
	public void runWatch(int z, JLabel mouseLabel) {
		slicer.setZIndex(z);
		System.out.println("Testing Z:" + z);
		slicer.colorizePolygons(sliceBrowserListener.getSelectedTriangles(), watchYs);
		for (StlError error : slicer.getStlErrors()) {
			if (error.getType() == ErrorType.NonManifold) {
				System.out.println(error);
			}
		}
		zSliceModel.setValue(z);
		lineSliceModel.refreshGui(mouseLabel, false);
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
					slicer.paintSlice((Graphics2D) g, true);
					try {
						tools.getProjectorMaskCreator().applyProjectorMask((Graphics2D) g);
					} catch (Exception e) {
						//Since this is already initialized, this can't happen
					}
				} else {
					slicer.debugPaintSlice((Graphics2D) g);

					sliceBrowserListener.drawSelectedLines(g);

					tools.drawBuildPlatform((Graphics2D) g);
				}
			}
		};

		browserPanel.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (slicer == null) {
					return;
				}
				mouseLabel.setText("X:" + e.getX() + " Y:" + e.getY() + " Z:" + slicer.getZIndex() + " Area:"
						+ slicer.getBuildArea());
			}
		});
		browserPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				List<Shape3d> clickedShapes = slicer.getTrianglesAt(e.getX(), e.getY());
				for (Shape3d shape : clickedShapes) {
					Line3d line = (Line3d) shape;
					Face3d face = line.getOriginatingFace();

					System.out.println(slicer.translateLineToString(line));
					System.out.println("stltriangle: " + face + " Hash:" + face.hashCode());
				}
				
				if (watchYs == null) {
					watchYs = new ArrayList<Integer>();
				}
				System.out.println("Added y watch for:" + e.getY());
				watchYs.add(e.getY());
				
				//Right click to save point into json file for persistant testing
				if (SwingUtilities.isRightMouseButton(e)) {
					Map<FillFile, FillFile> points;
					try {
						points = SlicePointUtils.loadPoints();
						File currentFile = new File(loadStlText.getText());
						FillFile checkFile = new FillFile();
						checkFile.setFileName(currentFile.getName());
						checkFile.setPixelsPerMMX(pixelsPerMMX);
						checkFile.setPixelsPerMMY(pixelsPerMMY);
						checkFile.setPoints(new ArrayList<FillPoint>());
						checkFile.setStlScale(slicer.getStlScale());
						checkFile.setzSliceOffset(slicer.getzOffset());
						checkFile.setzSliceResolution(slicer.getSliceResolution());

						FillFile joinFile = points.get(checkFile);
						if (joinFile == null) {
							boolean foundFile = false;
							for (FillFile current : points.values()) {
								if (current.getFileName().equals(checkFile.getFileName())) {
									foundFile = true;
									break;
								}
							}
							if (!foundFile) {
								SlicePointUtils.copyFileToPackage(currentFile);
							}
							points.put(checkFile, checkFile);
							joinFile = checkFile;
						}
						FillPoint newPoint = new FillPoint();
						newPoint.setSliceNumber(slicer.getZIndex());
						newPoint.setY(e.getY());
						newPoint.setX(e.getX());
						joinFile.getPoints().add(newPoint);
						SlicePointUtils.savePoints(points);
					} catch (IOException | URISyntaxException e1) {
						e1.printStackTrace();
					}
				}

				List<TreePath> selectedPaths = new ArrayList<TreePath>();
				Enumeration depth = ((SliceBrowserTreeNode) lineSliceModel.getRoot()).depthFirstEnumeration();
				while (depth.hasMoreElements()) {
					SliceBrowserTreeNode currentNode = (SliceBrowserTreeNode) depth.nextElement();
					System.out.println(currentNode);
					for (Shape3d clickedShape : clickedShapes) {
						Line3d line = slicer.translateLine(((Line3d) clickedShape));

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

		GridBagLayout bottomLayout = new GridBagLayout();
		final JPanel bottomPanel = new JPanel(bottomLayout);
		final JButton colorize = new JButton("Alpha Colorize");
		final JButton render = new JButton("Render");
		zSliceBar.setModel(zSliceModel);
		zSliceBar.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				slicer.setZIndex(e.getValue());
				mouseLabel.setText("Z:" + slicer.getZIndex());
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

		/*final JButton findNextError = new JButton("Next Error");
		findNextError.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				 for (int z = slicer.getZ() + 1; z < slicer.getZMax(); z++) {
					 slicer.setZ(z);
					 System.out.println("Testing Z:" + z);
					 slicer.colorizePolygons();
					 if (slicer.getStlErrors().size() > 0) {
						 boolean hasBadError = false;
						 for (StlError error : slicer.getStlErrors()) {
							 if (error.getType() == ErrorType.NonManifold) {
								 System.out.println(error);
								 hasBadError = true;
							 }
						 }
						 if (hasBadError) {
							 slicer.setZ(z);
							 zSliceModel.setValue(z);
							 lineSliceModel.refreshGui(mouseLabel, false);
							 return;
						 }
					 }
				 }
			}
		});*/
		
		final JButton findNextTriangle = new JButton("FTONS");//Find triangles on next slice
		findNextTriangle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int z = slicer.getZIndex() + 1;
				slicer.setZIndex(z);
				runWatch(z, mouseLabel);
			}
		});
		
		final JButton findPreviousTriangle = new JButton("FTOPS");//Find triangles on previous slice
		findPreviousTriangle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int z = slicer.getZIndex() - 1;
				runWatch(z, mouseLabel);
			}
		});
		
		final JButton clearYWatches = new JButton("Clear Y watches");//Find triangles on previous slice
		clearYWatches.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				watchYs = null;
			}
		});

		final JButton runWatches = new JButton("Run watch");//Find triangles on previous slice
		runWatches.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				runWatch(slicer.getZIndex(), mouseLabel);
			}
		});

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridx = 0;
		cons.gridy = 0;
		bottomPanel.add(mouseLabel, cons);
		cons = new GridBagConstraints();
		cons.gridx = 1;
		cons.gridy = 0;	
		bottomPanel.add(colorize, cons);
		cons = new GridBagConstraints();
		cons.gridx = 2;
		cons.gridy = 0;		
		bottomPanel.add(render, cons);
		cons = new GridBagConstraints();
		cons.gridx = 3;
		cons.gridy = 0;
		cons.gridwidth = 2;
		bottomPanel.add(loadStlText, cons);
		cons = new GridBagConstraints();
		cons.gridx = 0;
		cons.gridy = 1;		
		bottomPanel.add(loadStlButton, cons);
		cons = new GridBagConstraints();
		cons.gridx = 1;
		cons.gridy = 1;		
		bottomPanel.add(findNextTriangle, cons);
		cons = new GridBagConstraints();
		cons.gridx = 2;
		cons.gridy = 1;		
		bottomPanel.add(findPreviousTriangle, cons);
		cons = new GridBagConstraints();
		cons.gridx = 3;
		cons.gridy = 1;		
		bottomPanel.add(clearYWatches, cons);
		cons = new GridBagConstraints();
		cons.gridx = 4;
		cons.gridy = 1;		
		bottomPanel.add(runWatches, cons);
		window.add(zSliceBar, BorderLayout.EAST);
		window.add(browserPanel, BorderLayout.CENTER);
		window.add(bottomPanel, BorderLayout.SOUTH);

		return window;
	}

	public SliceBrowser(PrinterTools tools) throws Exception {
		this.tools = tools;
		setTopComponent(getSliceBrowser());
		JScrollPane scroller = new JScrollPane(getSliceTree());
		setBottomComponent(scroller);
	}

	public static void main(String[] args) throws Exception {
		PrinterTools browser = new PrinterTools();
		browser.setVisible(true);
		// browser.getColors(browser.getFractions(1600, .0f, 1f), .2f, .0f);
	}
}

