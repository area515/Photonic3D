package org.area515.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.StlFile;
import org.area515.resinprinter.stl.Triangle3d;
import org.area515.resinprinter.stl.XYComparator;

public class TestSlicer {
	 private static double z = 0;
	 private static List<Polygon> coloredPolygons = null;
	 
	 public static boolean findLinkage(List<Line3d> currentWorkingLoop, Line3d currentLine, List<List<Line3d>> workingLoop, List<List<Line3d>> completedLoops) {
			  Line3d firstInCurrentWorkingGroup = currentWorkingLoop.get(0);
			  Line3d lastInCurrentWorkingGroup = currentWorkingLoop.get(currentWorkingLoop.size() - 1);
			  if (currentLine.getPointTwo().equals(firstInCurrentWorkingGroup.getPointOne())) {
				  //Check to determine if this loop is closed
				  if (currentLine.getPointOne().equals(lastInCurrentWorkingGroup.getPointTwo())) {
					  workingLoop.remove(currentWorkingLoop);
					  completedLoops.add(currentWorkingLoop);
				  }
				  
				  currentWorkingLoop.add(0, currentLine);
				  return true;
			  } else if (lastInCurrentWorkingGroup.getPointTwo().equals(currentLine.getPointOne())) {
				  //Check to determine if this loop is closed
				  if (firstInCurrentWorkingGroup.getPointOne().equals(currentLine.getPointTwo())) {
					  workingLoop.remove(currentWorkingLoop);
					  completedLoops.add(currentWorkingLoop);
				  }
				  
				  currentWorkingLoop.add(currentLine);
				  return true;
			  } else if (lastInCurrentWorkingGroup.getPointTwo().equals(currentLine.getPointTwo())) {
				  //Check to determine if this loop is closed
				  if (firstInCurrentWorkingGroup.getPointOne().equals(currentLine.getPointOne())) {
					  workingLoop.remove(currentWorkingLoop);
					  completedLoops.add(currentWorkingLoop);
				  }
				  
				  currentLine.swap();
				  currentWorkingLoop.add(currentLine);
				  return true;							  
			  } else if (currentLine.getPointOne().equals(firstInCurrentWorkingGroup.getPointOne())) {
				  //Check to determine if this loop is closed
				  if (firstInCurrentWorkingGroup.getPointTwo().equals(currentLine.getPointTwo())) {
					  workingLoop.remove(currentWorkingLoop);
					  completedLoops.add(currentWorkingLoop);
				  }
				  
				  currentLine.swap();
				  currentWorkingLoop.add(0, currentLine);
				  return true;							  
			  }
			  
			  return false;
	 }
	 
	/* public static boolean findLinkage(List<Line3d> currentWorkingLoop, List<Line3d> otherWorkingLoop, List<List<Line3d>> workingLoop, List<List<Line3d>> completedLoops) {
		  Line3d firstInCurrentWorkingGroup = currentWorkingLoop.get(0);
		  Line3d lastInCurrentWorkingGroup = currentWorkingLoop.get(currentWorkingLoop.size() - 1);
		  Line3d firstInOtherWorkingGroup = otherWorkingLoop.get(0);
		  Line3d lastInOtherWorkingGroup = otherWorkingLoop.get(otherWorkingLoop.size() - 1);
		  if (firstInOtherWorkingGroup.getPointTwo().equals(firstInCurrentWorkingGroup.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (firstInOtherWorkingGroup.getPointOne().equals(lastInCurrentWorkingGroup.getPointTwo())) {
				  workingLoop.remove(currentWorkingLoop);
				  completedLoops.add(currentWorkingLoop);
			  }
			  
			  currentWorkingLoop.add(0, currentLine);
			  return true;
		  } else if (lastInCurrentWorkingGroup.getPointTwo().equals(firstInOtherWorkingGroup.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingGroup.getPointOne().equals(lastInOtherWorkingGroup.getPointTwo())) {
				  workingLoop.remove(currentWorkingLoop);
				  completedLoops.add(currentWorkingLoop);
			  }
			  
			  currentWorkingLoop.add(currentLine);
			  return true;
		  } else if (lastInCurrentWorkingGroup.getPointTwo().equals(lastInOtherWorkingGroup.getPointTwo())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingGroup.getPointOne().equals(firstInOtherWorkingGroup.getPointOne())) {
				  workingLoop.remove(currentWorkingLoop);
				  completedLoops.add(currentWorkingLoop);
			  }
			  
			  currentLine.swap();
			  currentWorkingLoop.add(currentLine);
			  return true;							  
		  } else if (firstInOtherWorkingGroup.getPointOne().equals(firstInCurrentWorkingGroup.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingGroup.getPointTwo().equals(lastInOtherWorkingGroup.getPointTwo())) {
				  workingLoop.remove(currentWorkingLoop);
				  completedLoops.add(currentWorkingLoop);
			  }
			  
			  currentLine.swap();
			  currentWorkingLoop.add(0,currentLine);
			  return true;							  
		  }
		  
		  return false;
	}*/
	 
	  public static void main(String[] args) throws Exception {
		  final double pixelsPerMMX = 10;
		  final double pixelsPerMMY = 10;
		  final double imageOffsetX = 45 * pixelsPerMMX;
		  final double imageOffsetY = 30 * pixelsPerMMY;
		  final double sliceResolution = 0.1;
		  final StlFile file = new StlFile();
		  file.load("C:\\Users\\wgilster\\Documents\\ArduinoMega.stl");
		  file.load("C:\\Users\\wgilster\\Documents\\Olaf_set3_whole.stl");
		  
			JFrame window = new JFrame();
			window.setLayout(new BorderLayout());
			final JPanel panel = new JPanel() {
			    public void paintComponent(Graphics g) {
			    	super.paintComponent(g);
			    	
			    	Graphics2D g2 = (Graphics2D)g;
					  g.setColor(Color.red);
					  TreeSet<Line3d> zIntersectionsBySortedX = new TreeSet<Line3d>(new XYComparator());
					  for (Triangle3d triangle : file.getTriangles()) {
						  if (triangle.intersectsZ(z)) {
							  Line3d line = triangle.getZIntersection(z);
							  zIntersectionsBySortedX.add(line);
							  g.drawLine((int)((line.getPointOne().x * pixelsPerMMX) + imageOffsetX), 
									  (int)((line.getPointOne().y * pixelsPerMMY) + imageOffsetY), 
									  (int)((line.getPointTwo().x * pixelsPerMMX) + imageOffsetX), 
									  (int)((line.getPointTwo().y * pixelsPerMMY) + imageOffsetY));
						  }
					  }
					  
					  g.setColor(Color.BLUE);
					  g2.setBackground(Color.CYAN);
					  if (coloredPolygons != null) {
						  for (Polygon currentPolygon : coloredPolygons) {
							  g.fillPolygon(currentPolygon);
							  g.drawPolygon(currentPolygon);
						  }
					  }
			    }
			};
	
			JScrollBar bar = new JScrollBar(JScrollBar.VERTICAL);
			bar.addAdjustmentListener(new AdjustmentListener(){
				@Override
				public void adjustmentValueChanged(AdjustmentEvent e) {
					z = e.getValue() * sliceResolution;
					coloredPolygons = null;
					panel.repaint();
				}
			});
			
			JButton colorize = new JButton("Colorize");
			colorize.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					  //Set<Line3d> zIntersectionsBySortedX = new LinkedHashSet<Line3d>();//
					  Set<Line3d> zIntersectionsBySortedX = new TreeSet<Line3d>(new XYComparator());
					  for (Triangle3d triangle : file.getTriangles()) {
						  if (triangle.intersectsZ(z)) {
							  Line3d line = triangle.getZIntersection(z);
							  zIntersectionsBySortedX.add(line);
						  }
					  }
					  
					  //Join a set of loose lines into working loops of lines
					  //This algorithm is slightly more efficient than the below algorithm.
					  //So attempt as many linkages here than the below algorithm
					  List<List<Line3d>> completedLoops = new ArrayList<List<Line3d>>();
					  List<List<Line3d>> brokenLoops = new ArrayList<List<Line3d>>();
					  List<List<Line3d>> workingLoop = new ArrayList<List<Line3d>>();
					  Iterator<Line3d> lineIterator = zIntersectionsBySortedX.iterator();
					  nextLine : while (lineIterator.hasNext()) {
						  Line3d currentLine = lineIterator.next();
						  for (List<Line3d> currentWorkingLoop : workingLoop) {
							  if (findLinkage(currentWorkingLoop, currentLine, workingLoop, completedLoops)) {
								  continue nextLine;
							  }
						  }
						  
						  List<Line3d> newLoop = new ArrayList<Line3d>();
						  newLoop.add(currentLine);
						  workingLoop.add(newLoop);
					  }
					  
					  //Now combine workingLoops into completedLoops
					  /*nextWorkingLoop : for (int index = 0; index < workingLoop.size(); index++) {
						  List<Line3d> currentWorkingLoop = workingLoop.get(index);
						  
						  for (int otherIndex = index; otherIndex < workingLoop.size(); otherIndex++) {
							  List<Line3d> otherCurrentWorkingLoop = workingLoop.get(otherIndex);
							  
							  if (currentWorkingLoop.get(0).getPointOne().equals(otherCurrentWorkingLoop.get(otherCurrentWorkingLoop.size() - 1).getPointTwo())) {
								  if (otherCurrentWorkingLoop.get(0).equals(currentWorkingLoop.get(currentWorkingLoop.size() - 1))) {
									  workingLoop.remove(otherCurrentWorkingLoop);
									  completedLoops.add(otherCurrentWorkingLoop);
								  }
								  
								  otherCurrentWorkingLoop.addAll(currentWorkingLoop);
								  continue nextWorkingLoop;
							  } else if (currentWorkingLoop.get(currentWorkingLoop.size() - 1).getPointTwo().equals(otherCurrentWorkingLoop.get(0).getPointOne())) {
								  if (currentWorkingLoop.get(0).equals(otherCurrentWorkingLoop.get(otherCurrentWorkingLoop.size() - 1))) {
									  workingLoop.remove(otherCurrentWorkingLoop);
									  completedLoops.add(otherCurrentWorkingLoop);
								  }
								  
								  otherCurrentWorkingLoop.addAll(0, currentWorkingLoop);
								  continue nextWorkingLoop;
							  }
						  }
						  
						  brokenLoops.add(currentWorkingLoop);
					  }*/
					  
					  coloredPolygons = new ArrayList<Polygon>();
					  for (List<Line3d> lines : workingLoop) {
						  int[] xpoints = new int[lines.size()];
						  int[] ypoints = new int[lines.size()];
						  for (int t = 0; t < lines.size(); t++) {
							  xpoints[t] = (int)(lines.get(t).getPointOne().x * pixelsPerMMX + imageOffsetX);
							  ypoints[t] = (int)(lines.get(t).getPointOne().y * pixelsPerMMY + imageOffsetY);
						  }
						  Polygon polygon = new Polygon(xpoints, ypoints, xpoints.length);
						  coloredPolygons.add(polygon);
					  }
					  for (List<Line3d> lines : completedLoops) {
						  int[] xpoints = new int[lines.size()];
						  int[] ypoints = new int[lines.size()];
						  for (int t = 0; t < lines.size(); t++) {
							  xpoints[t] = (int)(lines.get(t).getPointOne().x * pixelsPerMMX + imageOffsetX);
							  ypoints[t] = (int)(lines.get(t).getPointOne().y * pixelsPerMMY + imageOffsetY);
						  }
						  Polygon polygon = new Polygon(xpoints, ypoints, xpoints.length);
						  coloredPolygons.add(polygon);
					  }					  
					  
					  System.out.println("Broken Loops(" + brokenLoops.size() + "):" + brokenLoops);
					  System.out.println("Completed Loops(" + completedLoops.size() + "):" + completedLoops);
					  System.out.println("Working Loops(" + workingLoop.size() + "):" + workingLoop);
					  panel.repaint();
				}
			});
			
			bar.setMaximum((int)((file.getZmax() - file.getZmin()) / sliceResolution));
			window.add(bar, BorderLayout.EAST);
			window.add(panel, BorderLayout.CENTER);
			window.add(colorize, BorderLayout.SOUTH);
			
			window.setTitle("Printer Simulation");
			window.setVisible(true);
			window.setExtendedState(JFrame.MAXIMIZED_BOTH);
			window.setMinimumSize(new Dimension(500, 500));
			//window.getGraphics().setColor(Color.red);
			//window.paint(g);
	
	
	  }
}

