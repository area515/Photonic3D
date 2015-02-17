package org.area515.resinprinter.slice;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Point3d;
import org.area515.resinprinter.stl.Shape3d;
import org.area515.resinprinter.stl.Triangle3d;
import org.area515.resinprinter.stl.XYComparatord;

public class ZSlicer {
	 private int z = 0;
	 private List<List<Line3d>> brokenLoops = new ArrayList<List<Line3d>>();
	 private List<Polygon> fillInPolygons = null;
	 private List<Polygon> digOutPolygons = null;
	 private int precisionScaler = 100000;//We need to scale the whole stl large enough to have enough precision before the decimal point
	 private double pixelsPerMMX = 10;
	 private double pixelsPerMMY = 10;
	 private double imageOffsetX = 35 * pixelsPerMMX;
	 private double imageOffsetY = 25 * pixelsPerMMY;
	 private double sliceResolution = 0.1;
	 private StlFile<Triangle3d> stlFile;
	 private String stlFileToSlice;
	 
	 public ZSlicer(String stlFileToSlice, int precisionScaler, double pixelsPerMMX, double pixelsPerMMY, double imageOffsetX, double imageOffsetY, double sliceResolution) {
		 this.precisionScaler = precisionScaler;
		 this.pixelsPerMMX = pixelsPerMMX;
		 this.pixelsPerMMY = pixelsPerMMY;
		 this.imageOffsetX = imageOffsetX;
		 this.imageOffsetY = imageOffsetY;
		 this.sliceResolution = sliceResolution;
		 this.stlFileToSlice = stlFileToSlice;
		 
		 stlFile = new StlFile<Triangle3d>() {
			  public void readFacetB(ByteBuffer in, int index) throws IOException {
			    // Read the Normal
				Point3d normal = new Point3d(
						in.getFloat() * ZSlicer.this.precisionScaler, 
						in.getFloat() * ZSlicer.this.precisionScaler, 
						in.getFloat() / ZSlicer.this.sliceResolution);

			    // Read vertex1
				Point3d[] triangle = new Point3d[3];
				triangle[0] = new Point3d(
						in.getFloat() * ZSlicer.this.precisionScaler, 
						in.getFloat() * ZSlicer.this.precisionScaler, 
					in.getFloat() / ZSlicer.this.sliceResolution);

			    // Read vertex2
				triangle[1] = new Point3d(
					in.getFloat() * ZSlicer.this.precisionScaler, 
					in.getFloat() * ZSlicer.this.precisionScaler, 
					in.getFloat() / ZSlicer.this.sliceResolution);

			    // Read vertex3
				triangle[2] = new Point3d(
					in.getFloat() * ZSlicer.this.precisionScaler, 
					in.getFloat() * ZSlicer.this.precisionScaler, 
					in.getFloat() / ZSlicer.this.sliceResolution);
				
			    triangles.add(new Triangle3d(triangle, normal));
			    
			    zmin = Math.min(triangle[0].z, Math.min(triangle[1].z, Math.min(triangle[2].z, zmin)));
			    zmax = Math.max(triangle[0].z, Math.max(triangle[1].z, Math.max(triangle[2].z, zmax)));
			  }// End of readFacetB

			@Override
			public Set<Triangle3d> createSet() {
				return new TreeSet<Triangle3d>(new XYComparatord());
			}
		  };
	 }
	 
	 private boolean findLinkage(List<Line3d> currentWorkingLoop, Line3d currentLine, List<List<Line3d>> workingLoop, List<List<Line3d>> completedFillInLoops, List<List<Line3d>> completedDigOutLoops) {
		  Line3d firstInCurrentWorkingLoop = currentWorkingLoop.get(0);
		  Line3d lastInCurrentWorkingLoop = currentWorkingLoop.get(currentWorkingLoop.size() - 1);
		  if (currentLine.getPointTwo().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (currentLine.getPointOne().ceilingEquals(lastInCurrentWorkingLoop.getPointTwo())) {
				  workingLoop.remove(currentWorkingLoop);
				  completedFillInLoops.add(currentWorkingLoop);
				  System.out.println("Completed Link: 1 with [" + (currentWorkingLoop.size() + 1) + "] links (Round 1)");
			  }
			  
			  currentWorkingLoop.add(0, currentLine);
			  return true;
		  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(currentLine.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(currentLine.getPointTwo())) {
				  workingLoop.remove(currentWorkingLoop);
				  completedFillInLoops.add(currentWorkingLoop);
				  System.out.println("Completed Link: 2 with [" + (currentWorkingLoop.size() + 1) + "] links (Round 1)");
			  }
			  
			  currentWorkingLoop.add(currentLine);
			  return true;
		  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(currentLine.getPointTwo())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(currentLine.getPointOne())) {
				  workingLoop.remove(currentWorkingLoop);
				  completedFillInLoops.add(currentWorkingLoop);
				  System.out.println("Completed Link: 3 with [" + (currentWorkingLoop.size() + 1) + "] links (Round 1)");
			  }
			  
			  currentLine.swap();
			  currentWorkingLoop.add(currentLine);
			  return true;							  
		  } else if (currentLine.getPointOne().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointTwo().ceilingEquals(currentLine.getPointTwo())) {
				  workingLoop.remove(currentWorkingLoop);
				  completedFillInLoops.add(currentWorkingLoop);
				  System.out.println("Completed Link: 4 with [" + (currentWorkingLoop.size() + 1) + "] links (Round 1)");
			  }
			  
			  currentLine.swap();
			  currentWorkingLoop.add(0, currentLine);
			  return true;							  
		  }
		  
		  return false;
	 }

	 private LinkageDiscovery findLinkage(List<Line3d> currentWorkingLoop, List<Line3d> otherWorkingLoop, List<List<Line3d>> completedFillInLoops, List<List<Line3d>> completedDigOutLoops) {
		 LinkageDiscovery completedLinkage = LinkageDiscovery.NoLinkFound;
		 Line3d firstInCurrentWorkingLoop = currentWorkingLoop.get(0);
		  Line3d lastInCurrentWorkingLoop = currentWorkingLoop.get(currentWorkingLoop.size() - 1);
		  Line3d firstInOtherWorkingLoop = otherWorkingLoop.get(0);
		  Line3d lastInOtherWorkingLoop = otherWorkingLoop.get(otherWorkingLoop.size() - 1);
		  if (lastInOtherWorkingLoop.getPointTwo().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  System.out.println("Found Link: 1 with [" + currentWorkingLoop.size() + "," + otherWorkingLoop.size() + "] links (Round 2)");
			  //Check to determine if this loop is closed
			  if (firstInOtherWorkingLoop.getPointOne().ceilingEquals(lastInCurrentWorkingLoop.getPointTwo())) {
				  completedFillInLoops.add(currentWorkingLoop);
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  System.out.println("Completed Link: 1");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentWorkingLoop.addAll(0, otherWorkingLoop);
		  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(firstInOtherWorkingLoop.getPointOne())) {
			  System.out.println("Found Link: 2 with [" + currentWorkingLoop.size() + "," + otherWorkingLoop.size() + "] links (Round 2)");
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(lastInOtherWorkingLoop.getPointTwo())) {
				  completedFillInLoops.add(currentWorkingLoop);
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  System.out.println("Completed Link: 2");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentWorkingLoop.addAll(otherWorkingLoop);
		  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(lastInOtherWorkingLoop.getPointTwo())) {
			  System.out.println("Found Link: 3 with [" + currentWorkingLoop.size() + "," + otherWorkingLoop.size() + "] links (Round 2)");
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(firstInOtherWorkingLoop.getPointOne())) {
				  completedFillInLoops.add(currentWorkingLoop);
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  System.out.println("Completed Link: 3");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  for (Line3d currentLine : otherWorkingLoop) {
				  currentLine.swap();
			  }
			  Collections.reverse(otherWorkingLoop);
			  currentWorkingLoop.addAll(otherWorkingLoop);					  
		  } else if (firstInOtherWorkingLoop.getPointOne().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  System.out.println("Found Link: 4 with [" + currentWorkingLoop.size() + "," + otherWorkingLoop.size() + "] links (Round 2)");
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointTwo().ceilingEquals(lastInOtherWorkingLoop.getPointTwo())) {
				  completedFillInLoops.add(currentWorkingLoop);
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  System.out.println("Completed Link: 4");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  for (Line3d currentLine : otherWorkingLoop) {
				  currentLine.swap();
			  }
			  Collections.reverse(otherWorkingLoop);
			  currentWorkingLoop.addAll(0, otherWorkingLoop);
		  }
		  
		  return completedLinkage;
	 }

	 private List<Polygon> compilePolygons(List<List<Line3d>> completedFillInLoops, double xOffset, double yOffset, double pixelsPerMMX, double pixelsPerMMY, int precisionScaler) {
		 List<Polygon> polygons = new ArrayList<Polygon>();
		  int count = 0;
		  for (List<Line3d> lines : completedFillInLoops) {
			  int[] xpoints = new int[lines.size()];
			  int[] xpointsCheck = new int[lines.size()];
			  int[] ypoints = new int[lines.size()];
			  int[] ypointsCheck = new int[lines.size()];
			  System.out.println("Checking out[" + count++ + "] element Count:" + lines.size());
			  for (int t = 0; t < lines.size(); t++) {
				  xpoints[t] = (int)(lines.get(t).getPointOne().x / precisionScaler * pixelsPerMMX + xOffset);
				  ypoints[t] = (int)(lines.get(t).getPointOne().y / precisionScaler * pixelsPerMMY + yOffset);
				  xpointsCheck[t] = (int)(lines.get(t).getPointTwo().x / precisionScaler * pixelsPerMMX + xOffset);
				  ypointsCheck[t] = (int)(lines.get(t).getPointTwo().y / precisionScaler * pixelsPerMMY + yOffset);
				  int prevPoint = t > 0? t - 1:lines.size() - 1;
				  int nextPoint = t < lines.size() - 1? t + 1:0;
				  if (!lines.get(t).getPointTwo().ceilingEquals(lines.get(nextPoint).getPointOne())) {
					  System.out.println("Compare second point[" + t + "]:" + lines.get(t) + " to first point[" + nextPoint + "]:" + lines.get(nextPoint));
				  }
				  if (!lines.get(t).getPointOne().ceilingEquals(lines.get(prevPoint).getPointTwo())) {
					  System.out.println("Compare first point[" + t + "]:" + lines.get(t) + " to second point[" + prevPoint + "]:" + lines.get(prevPoint));
				  }
			  }
			  
			  Polygon polygon = new Polygon(xpoints, ypoints, xpoints.length);
			  polygons.add(polygon);
		  }
		  
		  return polygons;
	 }

	 public void debugPaintSlice(Graphics2D g) {
		  g.setColor(Color.red);
		  for (Triangle3d triangle : stlFile.getTriangles()) {
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
					  g.drawLine((int)(line.getPointOne().x / precisionScaler * pixelsPerMMX + imageOffsetX), 
								  (int)(line.getPointOne().y / precisionScaler * pixelsPerMMY + imageOffsetY), 
								  (int)(line.getPointTwo().x / precisionScaler * pixelsPerMMX + imageOffsetX), 
								  (int)(line.getPointTwo().y / precisionScaler * pixelsPerMMY + imageOffsetY));
				  } else if (shape instanceof Point3d) {
					  g.setColor(Color.magenta);
					  Point3d point = (Point3d)shape;
					  g.drawLine((int)(point.x / precisionScaler * pixelsPerMMX + imageOffsetX), 
							  (int)(point.y / precisionScaler * pixelsPerMMY + imageOffsetX), 
							  (int)(point.x / precisionScaler * pixelsPerMMX + imageOffsetX), 
							  (int)(point.y / precisionScaler * pixelsPerMMY + imageOffsetX));
				  } else {
					  System.out.println("No intersection. WRONG!!!");
				  }
			  }
		  }
		  
		  g.setColor(Color.green);
		  g.setBackground(Color.green);
		  if (fillInPolygons != null) {
			  for (Polygon currentPolygon : fillInPolygons) {
				  g.fillPolygon(currentPolygon);
				  g.drawPolygon(currentPolygon);
			  }
		  }
		  g.setColor(Color.black);
		  g.setBackground(Color.black);
		  if (digOutPolygons != null) {
			  for (Polygon currentPolygon : digOutPolygons) {
				  g.fillPolygon(currentPolygon);
				  g.drawPolygon(currentPolygon);
			  }
		  }
	 }
	 
	 public void paintSlice(Graphics2D g) {
		  g.setColor(Color.white);
		  g.setBackground(Color.white);
		  if (fillInPolygons != null) {
			  for (Polygon currentPolygon : fillInPolygons) {
				  g.fillPolygon(currentPolygon);
				  g.drawPolygon(currentPolygon);
			  }
		  }
		  g.setColor(Color.black);
		  g.setBackground(Color.black);
		  if (digOutPolygons != null) {
			  for (Polygon currentPolygon : digOutPolygons) {
				  g.fillPolygon(currentPolygon);
				  g.drawPolygon(currentPolygon);
			  }
		  }
	 }

	 public void colorizePolygons() {
		  List<List<Line3d>> completedFillInLoops = new ArrayList<List<Line3d>>();
		  List<List<Line3d>> completedDigOutLoops = new ArrayList<List<Line3d>>();
		  brokenLoops = new ArrayList<List<Line3d>>();
		  
		  Set<Line3d> zIntersectionsBySortedX = new TreeSet<Line3d>(new XYComparatord());
		  for (Triangle3d triangle : stlFile.getTriangles()) {
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
		  //TODO: We need to fix this loop, we are modifying the original set inside the inner loop without telling the outer iterator
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
			  List<Line3d> currentWorkingLoop = workingLoop.get(0);

			  for (int otherIndex = 1; otherIndex < workingLoop.size(); otherIndex++) {
				  List<Line3d> otherWorkingLoop = workingLoop.get(otherIndex);
				  
				  switch (findLinkage(currentWorkingLoop, otherWorkingLoop, completedFillInLoops, completedDigOutLoops)) {
				  case FoundCompletion :
					  workingLoop.remove(otherIndex);
					  workingLoop.remove(0);
					  continue nextWorkingLoop;
				  case FoundLink :
					  workingLoop.remove(otherIndex);
					  continue nextWorkingLoop;
				  }
			  }
			  
			  brokenLoops.add(currentWorkingLoop);
			  workingLoop.remove(0);
		  }
		  
		  System.out.println("Fill Ins");
		  System.out.println("======");
		  fillInPolygons = compilePolygons(completedFillInLoops, imageOffsetX, imageOffsetY, pixelsPerMMX, pixelsPerMMY, precisionScaler);
		  
		  System.out.println("Dig Outs");
		  System.out.println("======");
		  digOutPolygons = compilePolygons(completedDigOutLoops, imageOffsetX, imageOffsetY, pixelsPerMMX, pixelsPerMMY, precisionScaler);
		  
		  System.out.println("TOTALS");
		  System.out.println("======");
		  System.out.println("Broken Loops(" + brokenLoops.size() + "):" + brokenLoops);
		  System.out.println("Completed Loops(" + completedFillInLoops.size() + "):" + completedFillInLoops);
		  System.out.println("Working Loops(" + workingLoop.size() + "):" + workingLoop);
	 }
	 
	 public List<List<Line3d>> getBrokenLoops() {
		 return brokenLoops;
	 }
	 
	 public void loadFile() throws FileNotFoundException {
		 stlFile.load(stlFileToSlice);
	 }
	 
	 public int getZ() {
 		return z; 
	 }
	
     public void setZ(int z) {
 		this.z = z;
		fillInPolygons = null;
		digOutPolygons = null;
		brokenLoops = null;
	 }
	
	 public int getZMin() {
		return (int)Math.ceil(stlFile.getZmin());
	 }
	
	 public int getZMax() {
		return (int)Math.floor(stlFile.getZmax());
	 }
}
