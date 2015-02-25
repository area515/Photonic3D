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

import org.area515.resinprinter.stl.BrokenFace3d;
import org.area515.resinprinter.stl.Face3d;
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
	 //We need to scale the whole stl large enough to have enough precision in front of the decimal point
	 //Too little and you get points that won't match, too much and you end up beating a double's precision
	 //This number is a balancing act.
	 private int precisionScaler = 100000;
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
					in.getFloat(), 
					in.getFloat(), 
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
	 
	 private void placeIntoCompletedLoopList(List<Line3d> completedLoop, List<List<Line3d>> completedFillInLoops, List<List<Line3d>> completedDigOutLoops) {
		 completedFillInLoops.add(completedLoop);
		 
		 /*Integer leastXIndex = null;
		 for (int t = 0; t < completedLoop.size(); t++) {
			 Line3d currentLine = completedLoop.get(t);
			 if (leastXIndex == null || currentLine.getMinX() < completedLoop.get(leastXIndex).getMinX()) {
				 Face3d triangle = currentLine.getOriginatingFace();
				 if (triangle instanceof Triangle3d) {
					 leastXIndex = t;
				 } else {
					 System.out.println("We should never have an instanceof of a broken face here!!!");
				 }
			 }
		 }
		 
		 Integer otherLeastXIndex = null;
		 Integer prevLeastIndex = leastXIndex == 0? completedLoop.size() - 1: leastXIndex - 1;
		 Integer nextLeastIndex = leastXIndex == completedLoop.size() - 1? 0: leastXIndex + 1;
		 if (Math.ceil(completedLoop.get(prevLeastIndex).getPointTwo().x) == Math.ceil(completedLoop.get(leastXIndex).getPointOne().x)) {
			 otherLeastXIndex = prevLeastIndex;
		 } else {
			 otherLeastXIndex = nextLeastIndex;
		 }*/
		 
		 //If otherLeastXIndex or 
	 }
	 
	 private LinkageDiscovery findLinkage(List<Line3d> currentWorkingLoop, Line3d currentLine, List<List<Line3d>> completedFillInLoops, List<List<Line3d>> completedDigOutLoops) {
		  LinkageDiscovery completedLinkage = LinkageDiscovery.NoLinkFound;
		  Line3d firstInCurrentWorkingLoop = currentWorkingLoop.get(0);
		  Line3d lastInCurrentWorkingLoop = currentWorkingLoop.get(currentWorkingLoop.size() - 1);
		  if (currentLine.getPointTwo().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (currentLine.getPointOne().ceilingEquals(lastInCurrentWorkingLoop.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  placeIntoCompletedLoopList(currentWorkingLoop, completedFillInLoops, completedDigOutLoops);
				  //System.out.println("Completed Link: 1 with [" + (currentWorkingLoop.size() + 1) + "] links (Link line)");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentWorkingLoop.add(0, currentLine);
		  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(currentLine.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(currentLine.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  placeIntoCompletedLoopList(currentWorkingLoop, completedFillInLoops, completedDigOutLoops);
				  //System.out.println("Completed Link: 2 with [" + (currentWorkingLoop.size() + 1) + "] links (Link line)");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentWorkingLoop.add(currentLine);
		  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(currentLine.getPointTwo())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(currentLine.getPointOne())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  placeIntoCompletedLoopList(currentWorkingLoop, completedFillInLoops, completedDigOutLoops);
				  //System.out.println("Completed Link: 3 with [" + (currentWorkingLoop.size() + 1) + "] links (Link line)");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentLine.swap();
			  currentWorkingLoop.add(currentLine);  
		  } else if (currentLine.getPointOne().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointTwo().ceilingEquals(currentLine.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  placeIntoCompletedLoopList(currentWorkingLoop, completedFillInLoops, completedDigOutLoops);
				  //System.out.println("Completed Link: 4 with [" + (currentWorkingLoop.size() + 1) + "] links (Link line)");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentLine.swap();
			  currentWorkingLoop.add(0, currentLine);
		  }
		  
		  return completedLinkage;
	 }

	 private LinkageDiscovery findLinkage(List<Line3d> currentWorkingLoop, List<Line3d> otherWorkingLoop, List<List<Line3d>> completedFillInLoops, List<List<Line3d>> completedDigOutLoops) {
		 LinkageDiscovery completedLinkage = LinkageDiscovery.NoLinkFound;
		 Line3d firstInCurrentWorkingLoop = currentWorkingLoop.get(0);
		  Line3d lastInCurrentWorkingLoop = currentWorkingLoop.get(currentWorkingLoop.size() - 1);
		  Line3d firstInOtherWorkingLoop = otherWorkingLoop.get(0);
		  Line3d lastInOtherWorkingLoop = otherWorkingLoop.get(otherWorkingLoop.size() - 1);
		  if (lastInOtherWorkingLoop.getPointTwo().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  //System.out.println("Found Link: 1 with [" + currentWorkingLoop.size() + "," + otherWorkingLoop.size() + "] links (Link Loop)");
			  //Check to determine if this loop is closed
			  if (firstInOtherWorkingLoop.getPointOne().ceilingEquals(lastInCurrentWorkingLoop.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  placeIntoCompletedLoopList(currentWorkingLoop, completedFillInLoops, completedDigOutLoops);
				  //System.out.println("Completed Link: 1 with [" + (currentWorkingLoop.size() + otherWorkingLoop.size()) + "] links (Link Loop)");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentWorkingLoop.addAll(0, otherWorkingLoop);
		  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(firstInOtherWorkingLoop.getPointOne())) {
			  //System.out.println("Found Link: 2 with [" + currentWorkingLoop.size() + "," + otherWorkingLoop.size() + "] links (Link Loop)");
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(lastInOtherWorkingLoop.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  placeIntoCompletedLoopList(currentWorkingLoop, completedFillInLoops, completedDigOutLoops);
				  //System.out.println("Completed Link: 2 with [" + (currentWorkingLoop.size() + otherWorkingLoop.size()) + "] links (Link Loop)");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentWorkingLoop.addAll(otherWorkingLoop);
		  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(lastInOtherWorkingLoop.getPointTwo())) {
			  //System.out.println("Found Link: 3 with [" + currentWorkingLoop.size() + "," + otherWorkingLoop.size() + "] links (Link Loop)");
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(firstInOtherWorkingLoop.getPointOne())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  placeIntoCompletedLoopList(currentWorkingLoop, completedFillInLoops, completedDigOutLoops);
				  //System.out.println("Completed Link: 3 with [" + (currentWorkingLoop.size() + otherWorkingLoop.size()) + "] links (Link Loop)");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  for (Line3d currentLine : otherWorkingLoop) {
				  currentLine.swap();
			  }
			  Collections.reverse(otherWorkingLoop);
			  currentWorkingLoop.addAll(otherWorkingLoop);					  
		  } else if (firstInOtherWorkingLoop.getPointOne().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  //System.out.println("Found Link: 4 with [" + currentWorkingLoop.size() + "," + otherWorkingLoop.size() + "] links (Link Loop)");
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointTwo().ceilingEquals(lastInOtherWorkingLoop.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  placeIntoCompletedLoopList(currentWorkingLoop, completedFillInLoops, completedDigOutLoops);
				  //System.out.println("Completed Link: 4 with [" + (currentWorkingLoop.size() + otherWorkingLoop.size()) + "] links (Link Loop)");
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
		  for (List<Line3d> lines : completedFillInLoops) {
			  int[] xpoints = new int[lines.size()];
			  int[] xpointsCheck = new int[lines.size()];
			  int[] ypoints = new int[lines.size()];
			  int[] ypointsCheck = new int[lines.size()];
			  //System.out.println("Checking out[" + count++ + "] element Count:" + lines.size());
			  for (int t = 0; t < lines.size(); t++) {
				  xpoints[t] = (int)(lines.get(t).getPointOne().x / precisionScaler * pixelsPerMMX + xOffset);
				  ypoints[t] = (int)(lines.get(t).getPointOne().y / precisionScaler * pixelsPerMMY + yOffset);
				  xpointsCheck[t] = (int)(lines.get(t).getPointTwo().x / precisionScaler * pixelsPerMMX + xOffset);
				  ypointsCheck[t] = (int)(lines.get(t).getPointTwo().y / precisionScaler * pixelsPerMMY + yOffset);
				  int prevPoint = t > 0? t - 1:lines.size() - 1;
				  int nextPoint = t < lines.size() - 1? t + 1:0;
				  if (!lines.get(t).getPointTwo().ceilingEquals(lines.get(nextPoint).getPointOne())) {
					  //System.out.println("Compare second point[" + t + "]:" + lines.get(t) + " to first point[" + nextPoint + "]:" + lines.get(nextPoint));
				  }
				  if (!lines.get(t).getPointOne().ceilingEquals(lines.get(prevPoint).getPointTwo())) {
					  //System.out.println("Compare first point[" + t + "]:" + lines.get(t) + " to second point[" + prevPoint + "]:" + lines.get(prevPoint));
				  }
			  }
			  
			  Polygon polygon = new Polygon(xpoints, ypoints, xpoints.length);
			  polygons.add(polygon);
		  }
		  
		  return polygons;
	 }

	 public void debugPaintSlice(Graphics2D g) {
		 int t = 0;
		  for (Triangle3d triangle : stlFile.getTriangles()) {
			  if (triangle.intersectsZ(z)) {
				  t++;
				  Shape3d shape = triangle.getZIntersection(z);
				  if (shape instanceof Triangle3d) {
					  g.setColor(Color.blue);
					  Triangle3d tri = (Triangle3d)shape;
					  Polygon poly = new Polygon(tri.getX(), tri.gety(), 3);
					  g.drawPolygon(poly);
				  } else if (shape instanceof Line3d) {
					  Line3d line = (Line3d)shape;
					  g.setColor(Color.orange);
					  g.drawLine((int)(line.getPointTwo().x / precisionScaler * pixelsPerMMX + imageOffsetX), 
							  (int)(line.getPointTwo().y / precisionScaler * pixelsPerMMY + imageOffsetY), 
							  (int)((line.getPointTwo().x / precisionScaler + line.getNormal().x) * pixelsPerMMX + imageOffsetX), 
							  (int)((line.getPointTwo().y / precisionScaler + line.getNormal().y) * pixelsPerMMY + imageOffsetY));
					  
					  g.setColor(Color.cyan);
					  g.drawLine((int)(line.getPointOne().x / precisionScaler * pixelsPerMMX + imageOffsetX), 
							  (int)(line.getPointOne().y / precisionScaler * pixelsPerMMY + imageOffsetY), 
							  (int)((line.getPointOne().x / precisionScaler + line.getNormal().x) * pixelsPerMMX + imageOffsetX), 
							  (int)((line.getPointOne().y / precisionScaler + line.getNormal().y) * pixelsPerMMY + imageOffsetY));
					  
					  g.setColor(Color.red);
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
					  //System.out.println("No intersection. WRONG!!!");
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
	 
	 private List<Line3d> findPathThroughTrianglesAndBrokenLoops(Point3d beginning, Point3d ending, List<Line3d> path, List<Face3d> brokenFaceMaze, List<Integer> usedFaces, int currentTriangleIndex) {
		 if (currentTriangleIndex >= brokenFaceMaze.size()) {
			 return null;
		 }
		 if (usedFaces.contains(currentTriangleIndex)) {
			 return findPathThroughTrianglesAndBrokenLoops(beginning, ending, path, brokenFaceMaze, usedFaces, currentTriangleIndex + 1);
		 }
		 
		 Face3d currentBrokenFace = brokenFaceMaze.get(currentTriangleIndex);
		 Point3d[] brokenEnds = currentBrokenFace.getBrokenEnds();
		 for (int t = 0; t < brokenEnds.length; t++) {
			 Point3d checkPoint = brokenEnds[t];
			 Point3d previousPoint = brokenEnds[t == 0?brokenEnds.length - 1:t-1];
			 Point3d nextPoint = brokenEnds[t == brokenEnds.length - 1?0:t+1];
			 if (checkPoint.ceilingEquals(beginning)) {
				 usedFaces.add(currentTriangleIndex);
				 
				 //First check if we can end this fiasco...
				 if (nextPoint.ceilingEquals(ending)) {
					 path.add(new Line3d(checkPoint, ending, null, currentBrokenFace, false));
					 return path;
				 } else if (previousPoint.ceilingEquals(ending)) {
					 path.add(new Line3d(checkPoint, ending, null, currentBrokenFace, false));
					 return path;
				 }
				 
				 List<Line3d> pathFound = findPathThroughTrianglesAndBrokenLoops(nextPoint, ending, path, brokenFaceMaze, usedFaces, 0);
				 if (pathFound != null) {
					 return path;
				 }
				 
				 pathFound = findPathThroughTrianglesAndBrokenLoops(previousPoint, ending, path, brokenFaceMaze, usedFaces, 0);
				 if (pathFound != null) {
					 return path;
				 }
				 
				 //Remove the last face we put on the stack
				 usedFaces.remove(usedFaces.size() - 1);
			 }
		 }
		 
		 return findPathThroughTrianglesAndBrokenLoops(beginning, ending, path, brokenFaceMaze, usedFaces, currentTriangleIndex + 1);
	 }
	 
	 public void colorizePolygons() {
		  List<Face3d> trianglesAndBrokenFacesForMazeTraversal = new ArrayList<Face3d>();
		  List<List<Line3d>> completedFillInLoops = new ArrayList<List<Line3d>>();
		  List<List<Line3d>> completedDigOutLoops = new ArrayList<List<Line3d>>();
		  brokenLoops = new ArrayList<List<Line3d>>();
		  
		  //Find all intersections and put them into a sorted list.
		  //We put them in a sorted list because the join algorithm can be executed in virtually constant time
		  //Effectively, this loop is log n due to the sort into XYComparator
		  Set<Line3d> zIntersectionsBySortedX = new TreeSet<Line3d>(new XYComparatord());
		  for (Triangle3d triangle : stlFile.getTriangles()) {
			  if (triangle.intersectsZ(z)) {
				  Shape3d shape = triangle.getZIntersection(z);
				  if (shape instanceof Triangle3d) {
					  completedFillInLoops.add(triangle.getLines());
					  trianglesAndBrokenFacesForMazeTraversal.add((Triangle3d)shape);
					  //System.out.println("Triangle:" + shape);
				  } else if (shape instanceof Line3d) {
					  zIntersectionsBySortedX.add((Line3d)shape);
				  }
				  //Ignore nulls and points(they don't print well...) :)
			  }
		  }
		  
		  
		  //Even though this algorithm is structured to be n^2 it executes in (n * constant) time because of the comparator
		  //We join a set of loose lines into working loops of lines
		  //This algorithm is slightly more efficient than the below algorithm since reversals are less expensive
		  List<List<Line3d>> workingLoop = new ArrayList<List<Line3d>>();
		  Iterator<Line3d> lineIterator = zIntersectionsBySortedX.iterator();
		  nextLine : while (lineIterator.hasNext()) {
			  Line3d currentLine = lineIterator.next();
			  
			  Iterator<List<Line3d>> workingLoopIter = workingLoop.iterator();
			  while (workingLoopIter.hasNext()) {
				  List<Line3d> currentWorkingLoop = workingLoopIter.next();
				  switch (findLinkage(currentWorkingLoop, currentLine, completedFillInLoops, completedDigOutLoops)) {
				  case FoundCompletion :
					  workingLoopIter.remove();
					  continue nextLine;
				  case FoundLink :
					  continue nextLine;
				  }
			  }
			  
			  List<Line3d> newLoop = new ArrayList<Line3d>();
			  newLoop.add(currentLine);
			  workingLoop.add(newLoop);
		  }
		  
		  //Empirically I've found that about half of all loops need to be joined with this method
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
			  
			  //System.out.println("Broken loop discovered[" + currentWorkingLoop.size() + "]:" + currentWorkingLoop);
			  brokenLoops.add(currentWorkingLoop);
			  workingLoop.remove(0);
		  }
		  
		  //empirically I've found that this block of code will only execute 1 in 100 times.
		  //So here is where things get complicated.
		  //We need to find our way through a maze of triangles and broken loops to create a full loop
		  //We can't just simply close broken loops because we could be closing over the top of an area that cuts back into the loop.
		  if (brokenLoops.size() > 0) {
			  //workingLoop.addAll(brokenLoops);
			  Iterator<List<Line3d>> brokenLoopIter = brokenLoops.iterator();
			  while (brokenLoopIter.hasNext()) {
				  List<Line3d> currentLoop = brokenLoopIter.next();
				  
				  //TODO: Somehow we are allowing a lost single point to find it's way to this code. That is indicitive of a bug earlier in the above sections
				  if (currentLoop.size() == 1 &&
					  currentLoop.get(0).getPointOne().ceilingEquals(currentLoop.get(0).getPointTwo())) {
					  continue;
				  }
				  
				  trianglesAndBrokenFacesForMazeTraversal.add(new BrokenFace3d(currentLoop));
				  brokenLoopIter.remove();
			  }
			  
			  int currentElementIndex = 0;
			  while (currentElementIndex < trianglesAndBrokenFacesForMazeTraversal.size()) {
				  Face3d currentBrokenFace = trianglesAndBrokenFacesForMazeTraversal.get(currentElementIndex);
				  if (currentBrokenFace instanceof Triangle3d) {
					  currentElementIndex++;
					  continue;
				  }
				  
				  List<Line3d> currentBrokenLoop = ((BrokenFace3d)currentBrokenFace).getLines();
				  List<Line3d> path = new ArrayList<Line3d>();
				  List<Integer> assembledFaces = new ArrayList<Integer>();
				  
				  //TODO: Technically this line was made from two originating faces, but we are only recording the first one for error reporting purposes
				  path.add(new Line3d(currentBrokenLoop.get(0).getPointOne(), currentBrokenLoop.get(currentBrokenLoop.size() - 1).getPointTwo(), null, currentBrokenLoop.get(0).getOriginatingFace(), false));
				  assembledFaces.add(currentElementIndex);
				  path = findPathThroughTrianglesAndBrokenLoops(currentBrokenLoop.get(0).getPointOne(), currentBrokenLoop.get(currentBrokenLoop.size() - 1).getPointTwo(), path, trianglesAndBrokenFacesForMazeTraversal, assembledFaces, 0);
				  if (path != null) {
					  System.out.println("Found path");
					  //We skip the first element on the path because it's we already know it's currentBrokenLoop
					  for (int t = 1; t < assembledFaces.size(); t++) {
						  Face3d usedFace = trianglesAndBrokenFacesForMazeTraversal.get(assembledFaces.get(t));
						  if (usedFace instanceof BrokenFace3d) {
							  if (findLinkage(currentBrokenLoop, ((BrokenFace3d)usedFace).getLines(), completedFillInLoops, completedDigOutLoops) == LinkageDiscovery.NoLinkFound) {
								  //System.out.println("Maze traversal problem on face:" + ((BrokenFace3d)usedFace).getLines());
							  }
						  } else {
							  if (findLinkage(currentBrokenLoop, path.get(0), completedFillInLoops, completedDigOutLoops) == LinkageDiscovery.NoLinkFound) {
								  //System.out.println("Maze traversal problem on triangle:" + path.get(0));
							  }
						  }
					  }
					  
					  //Reversing the order of the face indexes so that they can be removed in reverse order so that we don't have to worry about the reordering problems on removals
					  Collections.sort(assembledFaces, Collections.reverseOrder());
					  for (Integer usedFaceIndex : assembledFaces) {
						  Face3d usedFace = trianglesAndBrokenFacesForMazeTraversal.get(usedFaceIndex);
						  if (usedFace instanceof BrokenFace3d) {
							  trianglesAndBrokenFacesForMazeTraversal.remove(usedFaceIndex);
						  } //There is no else block because we can't remove triangles because they could be involved in multiple broken loops.
					  }
					  
					  completedFillInLoops.add(path);
				  } else {
					  brokenLoops.add(currentBrokenLoop);
				  }
				  
				  trianglesAndBrokenFacesForMazeTraversal.remove(currentElementIndex);
			  }
		  }
		  
		  //System.out.println("Fill Ins");
		  //System.out.println("======");
		  fillInPolygons = compilePolygons(completedFillInLoops, imageOffsetX, imageOffsetY, pixelsPerMMX, pixelsPerMMY, precisionScaler);
		  
		  //System.out.println("Dig Outs");
		  //System.out.println("======");
		  digOutPolygons = compilePolygons(completedDigOutLoops, imageOffsetX, imageOffsetY, pixelsPerMMX, pixelsPerMMY, precisionScaler);
		  
		  //System.out.println("TOTALS");
		  //System.out.println("======");
		  //System.out.println("Broken Loops(" + brokenLoops.size() + "):" + brokenLoops);
		  //System.out.println("Completed Loops(" + completedFillInLoops.size() + "):" + completedFillInLoops);
		  //System.out.println("Working Loops(" + workingLoop.size() + "):" + workingLoop);
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
