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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Point3d;
import org.area515.resinprinter.stl.Shape3d;
import org.area515.resinprinter.stl.StlFile;
import org.area515.resinprinter.stl.Triangle3d;
import org.area515.resinprinter.stl.XYComparatord;

public class TestSlicer {
	 private static int z = 115;
	 private static List<Polygon> coloredPolygons = null;
	 
	 public static boolean findLinkage(List<Line3d> currentWorkingLoop, Line3d currentLine, List<List<Line3d>> workingLoop, List<List<Line3d>> completedFillInLoops, List<List<Line3d>> completedDigOutLoops) {
			  Line3d firstInCurrentWorkingLoop = currentWorkingLoop.get(0);
			  Line3d lastInCurrentWorkingLoop = currentWorkingLoop.get(currentWorkingLoop.size() - 1);
			  if (currentLine.getPointTwo().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
				  //Check to determine if this loop is closed
				  if (currentLine.getPointOne().ceilingEquals(lastInCurrentWorkingLoop.getPointTwo())) {
					  workingLoop.remove(currentWorkingLoop);
					  completedFillInLoops.add(currentWorkingLoop);
				  }
				  
				  currentWorkingLoop.add(0, currentLine);
				  return true;
			  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(currentLine.getPointOne())) {
				  //Check to determine if this loop is closed
				  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(currentLine.getPointTwo())) {
					  workingLoop.remove(currentWorkingLoop);
					  completedFillInLoops.add(currentWorkingLoop);
				  }
				  
				  currentWorkingLoop.add(currentLine);
				  return true;
			  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(currentLine.getPointTwo())) {
				  //Check to determine if this loop is closed
				  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(currentLine.getPointOne())) {
					  workingLoop.remove(currentWorkingLoop);
					  completedFillInLoops.add(currentWorkingLoop);
				  }
				  
				  currentLine.swap();
				  currentWorkingLoop.add(currentLine);
				  return true;							  
			  } else if (currentLine.getPointOne().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
				  //Check to determine if this loop is closed
				  if (firstInCurrentWorkingLoop.getPointTwo().ceilingEquals(currentLine.getPointTwo())) {
					  workingLoop.remove(currentWorkingLoop);
					  completedFillInLoops.add(currentWorkingLoop);
				  }
				  
				  currentLine.swap();
				  currentWorkingLoop.add(0, currentLine);
				  return true;							  
			  }
			  
			  return false;
	 }
	 
	 public static boolean findLinkage(List<Line3d> currentWorkingLoop, List<Line3d> otherWorkingLoop, List<List<Line3d>> workingLoop, List<List<Line3d>> completedFillInLoops, List<List<Line3d>> completedDigOutLoops) {
		  Line3d firstInCurrentWorkingLoop = currentWorkingLoop.get(0);
		  Line3d lastInCurrentWorkingLoop = currentWorkingLoop.get(currentWorkingLoop.size() - 1);
		  Line3d firstInOtherWorkingLoop = otherWorkingLoop.get(0);
		  Line3d lastInOtherWorkingLoop = otherWorkingLoop.get(otherWorkingLoop.size() - 1);
		  if (lastInOtherWorkingLoop.getPointTwo().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  System.out.println("Found Link: 1");
			  //Check to determine if this loop is closed
			  if (firstInOtherWorkingLoop.getPointOne().ceilingEquals(lastInCurrentWorkingLoop.getPointTwo())) {
				  //workingLoop.remove(currentWorkingLoop);
				  completedFillInLoops.add(currentWorkingLoop);
				  
				  System.out.println("Completed Link: 1");
			  }
			  
			  otherWorkingLoop.addAll(currentWorkingLoop);
			  return true;
		  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(firstInOtherWorkingLoop.getPointOne())) {
			  System.out.println("Found Link: 2");
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(lastInOtherWorkingLoop.getPointTwo())) {
				  //workingLoop.remove(currentWorkingLoop);
				  completedFillInLoops.add(currentWorkingLoop);
				  
				  System.out.println("Completed Link: 2");
			  }
			  
			  otherWorkingLoop.addAll(0, currentWorkingLoop);
			  return true;
		  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(lastInOtherWorkingLoop.getPointTwo())) {
			  System.out.println("Found Link: 3");
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(firstInOtherWorkingLoop.getPointOne())) {
				  //workingLoop.remove(currentWorkingLoop);
				  completedFillInLoops.add(currentWorkingLoop);
				  
				  System.out.println("Completed Link: 3");
			  }
			  
			  System.out.println("lastInCurrentWorkingLoop:" + lastInCurrentWorkingLoop + " lastInOtherWorkingLoop:" + lastInOtherWorkingLoop);
			  for (Line3d currentLine : currentWorkingLoop) {
				  currentLine.swap();
			  }
			  Collections.reverse(currentWorkingLoop);
			  System.out.println("firstInCurrentWorkingLoop:" + currentWorkingLoop.get(0) + " lastInOtherWorkingLoop:" + lastInOtherWorkingLoop);

			  
			  otherWorkingLoop.addAll(currentWorkingLoop);
			  return true;							  
		  } else if (firstInOtherWorkingLoop.getPointOne().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  System.out.println("Found Link: 4");
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointTwo().ceilingEquals(lastInOtherWorkingLoop.getPointTwo())) {
				  //workingLoop.remove(currentWorkingLoop);
				  completedFillInLoops.add(currentWorkingLoop);
				  
				  System.out.println("Completed Link: 4");
			  }
			  
			  for (Line3d currentLine : currentWorkingLoop) {
				  currentLine.swap();
			  }
			  Collections.reverse(currentWorkingLoop);
			  otherWorkingLoop.addAll(0, currentWorkingLoop);
			  return true;							  
		  }
		  
		  return false;
	 }
	 
	  public static void main(String[] args) throws Exception {
		  final double pixelsPerMMX = 10;
		  final double pixelsPerMMY = 10;
		  final double imageOffsetX = 35 * pixelsPerMMX;
		  final double imageOffsetY = 25 * pixelsPerMMY;
		  final double sliceResolution = 0.1;
		  
		  final StlFile<Triangle3d> file = new StlFile<Triangle3d>() {
			  public void readFacetB(ByteBuffer in, int index) throws IOException {
			    // Read the Normal
				Point3d normal = new Point3d(
						in.getFloat() * pixelsPerMMX + imageOffsetX, 
						in.getFloat() * pixelsPerMMY + imageOffsetY, 
						in.getFloat() / sliceResolution);

			    // Read vertex1
				Point3d[] triangle = new Point3d[3];
				triangle[0] = new Point3d(
						in.getFloat() * pixelsPerMMX + imageOffsetX, 
						in.getFloat() * pixelsPerMMY + imageOffsetY, 
					in.getFloat() / sliceResolution);

			    // Read vertex2
				triangle[1] = new Point3d(
					in.getFloat() * pixelsPerMMX + imageOffsetX, 
					in.getFloat() * pixelsPerMMY + imageOffsetY, 
					in.getFloat() / sliceResolution);

			    // Read vertex3
				triangle[2] = new Point3d(
					in.getFloat() * pixelsPerMMX + imageOffsetX, 
					in.getFloat() * pixelsPerMMY + imageOffsetY, 
					in.getFloat() / sliceResolution);
				
			    triangles.add(new Triangle3d(triangle, normal));
			    
			    zmin = Math.min(triangle[0].z, Math.min(triangle[1].z, Math.min(triangle[2].z, zmin)));
			    zmax = Math.max(triangle[0].z, Math.max(triangle[1].z, Math.max(triangle[2].z, zmax)));
			  }// End of readFacetB

			@Override
			public Set<Triangle3d> createSet() {
				return new TreeSet<Triangle3d>(new XYComparatord());
			}
		  };
		  file.load("C:\\Users\\wgilster\\Documents\\ArduinoMega.stl");
		  file.load("C:\\Users\\wgilster\\Documents\\Olaf_set3_whole.stl");
		  
			JFrame window = new JFrame();
			window.setLayout(new BorderLayout());
			final JPanel panel = new JPanel() {
			    public void paintComponent(Graphics g) {
			    	super.paintComponent(g);
			    	
			    	Graphics2D g2 = (Graphics2D)g;
					  g.setColor(Color.red);
					  for (Triangle3d triangle : file.getTriangles()) {
						  if (triangle.intersectsZ(z)) {
							  Shape3d shape = triangle.getZIntersection(z);
							  if (shape instanceof Triangle3d) {
								  g.setColor(Color.blue);
								  Triangle3d tri = (Triangle3d)shape;
								  Polygon poly = new Polygon(tri.getX(), tri.gety(), 3);
								  g.drawPolygon(poly);
							  } else if (shape instanceof Line3d) {
								  g.setColor(Color.red);
								  Line3d line = (Line3d)shape;
								  g.drawLine((int)(line.getPointOne().x), 
											  (int)(line.getPointOne().y), 
											  (int)(line.getPointTwo().x), 
											  (int)(line.getPointTwo().y));
							  } else if (shape instanceof Point3d) {
								  g.setColor(Color.magenta);
								  Point3d point = (Point3d)shape;
								  g.drawLine((int)(point.x), 
										  (int)(point.y), 
										  (int)(point.x), 
										  (int)(point.y));
							  } else {
								  System.out.println("No intersection. WRONG!!!");
							  }
						  }
					  }
					  
					  g.setColor(Color.GREEN);
					  g2.setBackground(Color.GREEN);
					  if (coloredPolygons != null) {
						  for (Polygon currentPolygon : coloredPolygons) {
							  g.fillPolygon(currentPolygon);
							  g.drawPolygon(currentPolygon);
						  }
					  }
			    }
			};
	
			int min = (int)Math.ceil(file.getZmin());
			int max = (int)Math.floor(file.getZmax());
			JScrollBar bar = new JScrollBar(JScrollBar.VERTICAL, z, 0, min, max);
			bar.addAdjustmentListener(new AdjustmentListener(){
				@Override
				public void adjustmentValueChanged(AdjustmentEvent e) {
					z = e.getValue();
					coloredPolygons = null;
					panel.repaint();
				}
			});
			
			JButton colorize = new JButton("Colorize");
			colorize.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					  List<List<Line3d>> completedFillInLoops = new ArrayList<List<Line3d>>();
					  List<List<Line3d>> completedDigOutLoops = new ArrayList<List<Line3d>>();

					  Set<Line3d> zIntersectionsBySortedX = new TreeSet<Line3d>(new XYComparatord());
					  for (Triangle3d triangle : file.getTriangles()) {
						  if (triangle.intersectsZ(z)) {
							  Shape3d shape = triangle.getZIntersection(z);
							  if (shape instanceof Triangle3d) {
								  completedFillInLoops.add(triangle.getLines());
							  } else if (shape instanceof Line3d) {
								  zIntersectionsBySortedX.add((Line3d)shape);
							  }
							  //Ignore nulls and points they don't print well... :)
						  }
					  }
					  
					  //Join a set of loose lines into working loops of lines
					  //This algorithm is slightly more efficient than the below algorithm.
					  //So attempt as many linkages here than the below algorithm
					  List<List<Line3d>> brokenLoops = new ArrayList<List<Line3d>>();
					  List<List<Line3d>> workingLoop = new ArrayList<List<Line3d>>();
					  Iterator<Line3d> lineIterator = zIntersectionsBySortedX.iterator();
					  nextLine : while (lineIterator.hasNext()) {
						  Line3d currentLine = lineIterator.next();
						  for (List<Line3d> currentWorkingLoop : workingLoop) {
							  if (findLinkage(currentWorkingLoop, currentLine, workingLoop, completedFillInLoops, completedDigOutLoops)) {
								  continue nextLine;
							  }
						  }
						  
						  List<Line3d> newLoop = new ArrayList<Line3d>();
						  newLoop.add(currentLine);
						  workingLoop.add(newLoop);
					  }
					  
					  //Now combine workingLoops into completedLoops. This algorithm is a bit more inefficient
					  //but there shouldn't be that many stray loops left to connect...
					  nextWorkingLoop : while (workingLoop.size() > 0) {
						  List<Line3d> currentWorkingLoop = workingLoop.remove(0);

						  for (int otherIndex = 0; otherIndex < workingLoop.size(); otherIndex++) {
							  List<Line3d> otherWorkingLoop = workingLoop.get(otherIndex);
							  
							  if (findLinkage(currentWorkingLoop, otherWorkingLoop, workingLoop, completedFillInLoops, completedDigOutLoops)) {
								  continue nextWorkingLoop;
							  }
						  }
						  
						  brokenLoops.add(currentWorkingLoop);
					  }
					  
					  coloredPolygons = new ArrayList<Polygon>();
					  for (List<Line3d> lines : completedFillInLoops) {
						  int[] xpoints = new int[lines.size()];
						  int[] xpointsCheck = new int[lines.size()];
						  int[] ypoints = new int[lines.size()];
						  int[] ypointsCheck = new int[lines.size()];
						  for (int t = 0; t < lines.size(); t++) {
							  xpoints[t] = (int)(lines.get(t).getPointOne().x);
							  ypoints[t] = (int)(lines.get(t).getPointOne().y);
							  xpointsCheck[t] = (int)(lines.get(t).getPointTwo().x);
							  ypointsCheck[t] = (int)(lines.get(t).getPointTwo().y);
							  int prevPoint = t > 0? t - 1:lines.size() - 1;
							  int nextPoint = t < lines.size() - 1? t + 1:0;
							  if (!lines.get(t).getPointTwo().ceilingEquals(lines.get(nextPoint).getPointOne())) {
								  System.out.println("t=" + t + " line[t]:" + lines.get(t) + " line[next]:" + lines.get(nextPoint));
							  }
							  if (!lines.get(t).getPointOne().ceilingEquals(lines.get(prevPoint).getPointTwo())) {
								  System.out.println("t=" + t + " line[t]:" + lines.get(t) + " line[prev]:" + lines.get(prevPoint));
							  }
						  }
						  
						  System.out.println("Xpoints");
						  System.out.println(Arrays.toString(xpoints));
						  System.out.println(Arrays.toString(xpointsCheck));

						  System.out.println("Ypoints");
						  System.out.println(Arrays.toString(ypoints));
						  System.out.println(Arrays.toString(ypointsCheck));
						  
						  
						  Polygon polygon = new Polygon(xpoints, ypoints, xpoints.length);
						  coloredPolygons.add(polygon);
					  }					  
					  
					  System.out.println("Broken Loops(" + brokenLoops.size() + "):" + brokenLoops);
					  System.out.println("Completed Loops(" + completedFillInLoops.size() + "):" + completedFillInLoops);
					  System.out.println("Working Loops(" + workingLoop.size() + "):" + workingLoop);
					  panel.repaint();
				}
			});

			
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

