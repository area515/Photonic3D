package org.area515.resinprinter.inkdetection.visual;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.inkdetection.visual.GenericHoughDetection.HoughReference;

public class ImageDetectionPanel extends JSplitPane {
    private static final Logger logger = LogManager.getLogger();
	private static final long serialVersionUID = -7186056420774499676L;
	private ImagePanel imagePanel;
	private JTree shapeTree;
	private ShapeTreeModel shapeTreeModel = new ShapeTreeModel();
	private String firstFile = "C:\\Users\\wgilster\\git\\Creation-Workshop-Host\\host\\src\\test\\resources\\org\\area515\\resinprinter\\inkdetection\\visual\\ToughSituation.png";
	private JTextField loadImageText = new JTextField(30);
	private VisualPrintMaterialDetector printMaterialDetector = new VisualPrintMaterialDetector();
	private BufferedImage originalImage;
	private BufferedImage edgesImage;
	private BufferedImage shapesImage;
	
	private BufferedImage circleWatchesImage;
	private BufferedImage circleHoughSpaceImage;
	private BufferedImage circleHoughSpaceWatchImage;
	private BufferedImage lineWatchesImage;
	private BufferedImage lineHoughSpaceImage;
	private BufferedImage lineHoughSpaceWatchImage;
	
	private List<Line> doubleClickedLines = new ArrayList<>();
	private List<Circle> doubleClickedCircles = new ArrayList<>();
	private List<HoughReference> watchedCircles = new ArrayList<>();
	private List<HoughReference> watchedLines = new ArrayList<>();
	
	private class ShapeTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 4514477175597266206L;

		public ShapeTreeNode(String title) {
			super(title);
		}
		
		public ShapeTreeNode(Object lines) {
			super(lines);
		}
	}
	
	private class ShapeTreeModel extends DefaultTreeModel {
		private static final long serialVersionUID = 179974567762541369L;
		
		private ShapeTreeNode linesNode;
		private ShapeTreeNode circlesNode;
		
		public ShapeTreeModel() {
			super(new ShapeTreeNode("Shape Root"));
			ShapeTreeNode root = (ShapeTreeNode)getRoot();
			
			linesNode = new ShapeTreeNode("Lines");
			circlesNode = new ShapeTreeNode("Circles");
			root.add(linesNode);
			root.add(circlesNode);
		}
		
		public void clearShapes() {
			linesNode.removeAllChildren();
			circlesNode.removeAllChildren();
		}
		
		public void addLine(ShapeTreeNode node) {
			linesNode.add(node);
		}
		public void addCircle(ShapeTreeNode node) {
			circlesNode.add(node);
		}
		
		public boolean isLineNode(ShapeTreeNode node) {
			ShapeTreeNode parent = (ShapeTreeNode)node.getParent();
			if (parent == null) {
				return false;
			}
			
			return parent.equals(linesNode);
		}
		
		public boolean isCircleNode(ShapeTreeNode node) {
			ShapeTreeNode parent = (ShapeTreeNode)node.getParent();
			if (parent == null) {
				return false;
			}
			
			return parent.equals(circlesNode);
		}
	}
	
	private class ShapeBrowserSelectionListener implements TreeSelectionListener {
		@Override
		public void valueChanged(TreeSelectionEvent e) {
			TreePath nodes[] = e.getPaths();
			
			for (int t = 0; t < nodes.length; t++) {
				ShapeTreeNode treeNode = (ShapeTreeNode)nodes[t].getLastPathComponent();
				if (treeNode.isLeaf()) {
					if (e.isAddedPath(t)) {
						if (treeNode.getUserObject() instanceof Line) {
							watchedLines.add(((HoughShape)treeNode.getUserObject()).getHoughReference());
						}
						if (treeNode.getUserObject() instanceof Circle) {
							watchedCircles.add(((HoughShape)treeNode.getUserObject()).getHoughReference());
						}
					} else {
						if (treeNode.getUserObject() instanceof Line) {
							watchedLines.remove(((HoughShape)treeNode.getUserObject()).getHoughReference());
						}
						if (treeNode.getUserObject() instanceof Circle) {
							watchedCircles.remove(((HoughShape)treeNode.getUserObject()).getHoughReference());
						}
					}
				}
			}
		}
	}
	
	public class ImagePanel extends JPanel {
		private static final long serialVersionUID = -5040640134693481687L;

		public void paintComponent(Graphics g) {
	    	super.paintComponent(g);
	    	if (edgesImage != null) {
	    		g.drawImage(edgesImage, 0, 0, null);
		    	if (shapesImage != null) {
		    		g.drawImage(shapesImage, 0, 0, null);
		    	}
	    	}
	    	g.setColor(Color.YELLOW);
	    	for (Line line : doubleClickedLines) {
				g.drawLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
	    	}
	    	g.setColor(Color.GREEN);
	    	for (Circle circle : doubleClickedCircles) {
				g.drawOval(circle.getX() - circle.getRadius(), circle.getY() - circle.getRadius(), circle.getRadius() * 2, circle.getRadius() * 2);
	    	}
	    	if (originalImage != null) {
	    		g.drawImage(originalImage, originalImage.getWidth(), 0, null);
	    		if (shapesImage != null) {
	    			g.drawImage(shapesImage, shapesImage.getWidth(), 0, null);
	    		}
	    	}
	    	
	    	if (lineWatchesImage != null) {
	    		//g.drawImage(lineWatchesImage, 0, 0, null);
	    		g.drawImage(lineWatchesImage, lineWatchesImage.getWidth(), 0, null);
	    	}
	    	if (lineHoughSpaceImage != null) {
	    		g.drawImage(lineHoughSpaceImage, 0, originalImage.getHeight(), null);
	    	}
	    	if (lineHoughSpaceWatchImage != null) {
	    		g.drawImage(lineHoughSpaceWatchImage, 0, originalImage.getHeight(), null);
	    	}

	    	if (circleWatchesImage != null) {
	    		//g.drawImage(circleWatchesImage, 0, 0, null);
	    		g.drawImage(circleWatchesImage, circleWatchesImage.getWidth(), 0, null);
	    	}
	    	if (circleHoughSpaceImage != null) {
	    		g.drawImage(circleHoughSpaceImage, originalImage.getWidth()*2, 0, null);
	    	}
	    	if (circleHoughSpaceWatchImage != null) {
	    		g.drawImage(circleHoughSpaceWatchImage, originalImage.getWidth()*2, 0, null);
	    	}
	    	
	    }
	}
	
	private JTree getShapeTree() {
		if (shapeTree == null) {
			shapeTree = new JTree(shapeTreeModel);
			shapeTree.setExpandsSelectedPaths(true);
			shapeTree.addTreeSelectionListener(new ShapeBrowserSelectionListener());
			shapeTree.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						ShapeTreeNode treeNode = (ShapeTreeNode)shapeTree.getAnchorSelectionPath().getLastPathComponent();
						if (shapeTreeModel.isCircleNode(treeNode)) {
							doubleClickedCircles.add((Circle)treeNode.getUserObject());
						}
						if (shapeTreeModel.isLineNode(treeNode)) {
							doubleClickedLines.add((Line)treeNode.getUserObject());
						}
						getImagePanel().repaint();
					}
				}
			});
		}

		return shapeTree;
	}
	
	private ImagePanel getImagePanel() {
		if (imagePanel == null) {
			imagePanel = new ImagePanel();
		}
		
		return imagePanel;
	}
	
	private void loadImage(File file, boolean performEdgeDetection) throws IOException {
		try {
			originalImage = ImageIO.read(file);
		} catch (IIOException e) {
			JOptionPane.showMessageDialog(null, "File not found:" + file);
			return;
		}
		
		if (performEdgeDetection) {
			CannyEdgeDetector8BitGray detector = printMaterialDetector.buildEdgeDetector(originalImage);
			detector.process();
			edgesImage = detector.getEdgesImage();
		} else {
			edgesImage = originalImage;
		}
		
		shapeTreeModel.clearShapes();
		doubleClickedLines.clear();
		doubleClickedCircles.clear();
		
		shapesImage = new BufferedImage(edgesImage.getWidth(), edgesImage.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		GenericHoughDetection<Line> lineHoughDetection = printMaterialDetector.buildLineDetection(edgesImage.getWidth(), edgesImage.getHeight());
		for (HoughReference reference : watchedLines) {
			lineHoughDetection.addWatch(reference, Color.BLUE);
		}
		GenericHoughDetection<Circle> circleHoughDetection = printMaterialDetector.buildCircleDetection(edgesImage.getWidth(), edgesImage.getHeight());
		for (HoughReference reference : watchedCircles) {
			circleHoughDetection.addWatch(reference, Color.CYAN);
		}
		
		lineHoughDetection.houghTransform(edgesImage);
		circleHoughDetection.houghTransform(edgesImage);
		
		List<Line> lineCenters = lineHoughDetection.getShapes();
		Graphics g = shapesImage.getGraphics();
		g.setColor(Color.RED);
		for (Line line : lineCenters) {
			g.drawLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
			shapeTreeModel.addLine(new ShapeTreeNode(line));
		}

		List<Circle> centers = circleHoughDetection.getShapes();
		g.setColor(Color.MAGENTA);
		for (Circle circle : centers) {
			g.drawOval(circle.getX() - circle.getRadius(), circle.getY() - circle.getRadius(), circle.getRadius() * 2, circle.getRadius() * 2);
			shapeTreeModel.addCircle(new ShapeTreeNode(circle));
		}
		
		circleWatchesImage = circleHoughDetection.generateWatchOverlayInImageSpace(edgesImage.getWidth(), edgesImage.getHeight(), null);
		circleHoughSpaceImage = circleHoughDetection.generateHoughSpaceImage(true);
		circleHoughSpaceWatchImage = circleHoughDetection.generateWatchOverlayInHoughSpace(null);
		
		lineWatchesImage = lineHoughDetection.generateWatchOverlayInImageSpace(edgesImage.getWidth(), edgesImage.getHeight(), 0);
		lineHoughSpaceImage = lineHoughDetection.generateHoughSpaceImage(true);
		lineHoughSpaceWatchImage = lineHoughDetection.generateWatchOverlayInHoughSpace(0);
		
		watchedLines.clear();
		watchedCircles.clear();
		
		getImagePanel().repaint();
		shapeTreeModel.reload();
	}
	
	public JComponent getShapeBrowser() throws Exception {
		final JCheckBox performEdgeDetection = new JCheckBox("Edges");
		performEdgeDetection.setSelected(true);
		if (firstFile != null) {
			loadImageText.setText(firstFile);
			loadImage(new File(loadImageText.getText()), performEdgeDetection.isSelected());
		}
		
		final JButton loadImageButton = new JButton("Detect Shapes");
		final JLabel mouseLabel = new JLabel("X:" + " Y:");
		final JPanel window = new JPanel();
		window.setLayout(new BorderLayout());
		
		getImagePanel().addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (edgesImage != null) {
					mouseLabel.setText("X:" + e.getX() + " Y:" + e.getY() + " D:" + (int)Math.sqrt(Math.pow(edgesImage.getWidth(), 2d) + Math.pow(edgesImage.getHeight(), 2d)));
				} else {
					mouseLabel.setText("X:" + e.getX() + " Y:" + e.getY());
				}
			}
		});
		getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				//TODO: No function yet.
			}
		});
		
		final JPanel bottomPanel = new JPanel(new FlowLayout());
		/*final JButton showLines = new JButton("Show Lines");
		final JButton showReferences = new JButton("References");
		showLines.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//TODO: Show Lines
			}
		});
		
		showReferences.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//TODO: show references
			}
		});*/
				
		loadImageButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					loadImage(new File(loadImageText.getText()), performEdgeDetection.isSelected());
				} catch (IOException e1) {
					logger.error("Couldn't load image:" + loadImageText.getText(), e1);
				}
			}
		});
				
		bottomPanel.add(mouseLabel);
		//bottomPanel.add(showLines);
		//bottomPanel.add(showReferences);
		bottomPanel.add(performEdgeDetection);
		bottomPanel.add(loadImageText);
		bottomPanel.add(loadImageButton);
		window.add(getImagePanel(), BorderLayout.CENTER);
		window.add(bottomPanel, BorderLayout.SOUTH);
				
		return window;
	}

	public ImageDetectionPanel() throws Exception {
		setTopComponent(getShapeBrowser());
		JScrollPane scroller = new JScrollPane(getShapeTree());
		setBottomComponent(scroller);
	}
}
