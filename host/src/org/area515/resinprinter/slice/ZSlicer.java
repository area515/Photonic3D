package org.area515.resinprinter.slice;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import org.area515.resinprinter.slice.StlError.ErrorType;
import org.area515.resinprinter.stl.BrokenFace3d;
import org.area515.resinprinter.stl.Face3d;
import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Point3d;
import org.area515.resinprinter.stl.Shape3d;
import org.area515.resinprinter.stl.Triangle3d;
import org.area515.resinprinter.stl.XYComparatord;

public class ZSlicer {
	 //We need to scale the whole stl large enough to have enough precision in front of the decimal point
	 //Too little and you get points that won't match, too much and you end up beating a double's precision
	 //This number is a balancing act.
	 private double precisionScaler = 100000;
	 private double pixelsPerMMX = 5;
	 private double pixelsPerMMY = 5;
	 private double stlScale = 1;
	 
	 private Double imageOffsetX = null;
	 private Double imageOffsetY = null;
	 private double sliceResolution = 0.1;
	 private StlFile<Triangle3d> stlFile;
	 private File stlFileToSlice;
	 private boolean keepTrackOfErrors = false;
	 
	 //These are the variables per z
	 private List<StlError> errors = new ArrayList<StlError>();
	 private List<Polygon> fillInPolygons = null;
	 private List<Line3d> fillInScanLines = null;
	 private int z = 0;
	 private int sliceMaxX;
	 private int sliceMaxY;
	 private int sliceMinX;
	 private int sliceMinY;
	 private int buildArea;
	 
	 //TODO: Need to add in super sampling
	 public ZSlicer(File stlFileToSlice, double stlScale, double pixelsPerMMX, double pixelsPerMMY, double sliceResolution, boolean keepTrackOfErrors) {
		 this.stlScale = stlScale;
		 this.pixelsPerMMX = pixelsPerMMX;
		 this.pixelsPerMMY = pixelsPerMMY;
		 this.sliceResolution = sliceResolution;
		 this.stlFileToSlice = stlFileToSlice;
		 this.keepTrackOfErrors = keepTrackOfErrors;
		 
		 stlFile = new StlFile<Triangle3d>() {
			  public void readFacetB(ByteBuffer in, int index) throws IOException {
			    // Read the Normal
				Point3d normal = new Point3d(
					in.getFloat(), 
					in.getFloat(), 
					in.getFloat() / ZSlicer.this.sliceResolution);

			    // Read vertex1
				double p1[] = new double[]{in.getFloat(), in.getFloat(), in.getFloat()};
				double p2[] = new double[]{in.getFloat(), in.getFloat(), in.getFloat()};
				double p3[] = new double[]{in.getFloat(), in.getFloat(), in.getFloat()};
				Point3d[] triangle = new Point3d[3];
				triangle[0] = new Point3d(
					p1[0] * (ZSlicer.this.precisionScaler * ZSlicer.this.stlScale), 
					p1[1] * (ZSlicer.this.precisionScaler * ZSlicer.this.stlScale), 
					p1[2] / ZSlicer.this.sliceResolution);

			    // Read vertex2
				triangle[1] = new Point3d(
					p2[0] * (ZSlicer.this.precisionScaler * ZSlicer.this.stlScale), 
					p2[1] * (ZSlicer.this.precisionScaler * ZSlicer.this.stlScale), 
					p2[2] / ZSlicer.this.sliceResolution);

			    // Read vertex3
				triangle[2] = new Point3d(
					p3[0] * (ZSlicer.this.precisionScaler * ZSlicer.this.stlScale), 
					p3[1] * (ZSlicer.this.precisionScaler * ZSlicer.this.stlScale), 
					p3[2] / ZSlicer.this.sliceResolution);
				
				if (normal.x == 0 && normal.y == 0 && normal.z == 0) {
					/*normal.x = (p3[1] - p2[1]) * (p2[2] - p1[2]) - (p3[2] - p2[2]) * (p2[1] - p1[1]);
					normal.y = (p3[2] - p2[2]) * (p2[0] - p1[0]) - (p3[0] - p2[0]) * (p2[2] - p1[2]);
					normal.z = (p3[0] - p2[0]) * (p2[1] - p1[1]) - (p3[1] - p2[1]) * (p2[0] - p1[0]);*/
					normal.x = (p3[2] - p2[2]) * (p2[1] - p1[1]) - (p3[1] - p2[1]) * (p2[2] - p1[2]);
					normal.y = (p3[0] - p2[0]) * (p2[2] - p1[2]) - (p3[2] - p2[2]) * (p2[0] - p1[0]);
					normal.z = (p3[1] - p2[1]) * (p2[0] - p1[0]) - (p3[0] - p2[0]) * (p2[1] - p1[1]);
				}
			    triangles.add(new Triangle3d(triangle, normal));
			    
			    zmin = Math.min(triangle[0].z, Math.min(triangle[1].z, Math.min(triangle[2].z, zmin)));
			    zmax = Math.max(triangle[0].z, Math.max(triangle[1].z, Math.max(triangle[2].z, zmax)));
			    xmin = Math.min(triangle[0].x, Math.min(triangle[1].x, Math.min(triangle[2].x, xmin)));
			    xmax = Math.max(triangle[0].x, Math.max(triangle[1].x, Math.max(triangle[2].x, xmax)));
			    ymin = Math.min(triangle[0].y, Math.min(triangle[1].y, Math.min(triangle[2].y, ymin)));
			    ymax = Math.max(triangle[0].y, Math.max(triangle[1].y, Math.max(triangle[2].y, ymax)));
			  }// End of readFacetB

			@Override
			public Set<Triangle3d> createSet() {
				return new TreeSet<Triangle3d>(new XYComparatord());
			}
		  };
	 }
	 
	 public List<StlError> getStlErrors() {
		 return errors;
	 }
	 
	 private void placeIntoCompletedLoopList(List<Line3d> completedLoop, List<List<Line3d>> completedFillInLoops) {
		 List<Line3d> lines = new ArrayList<Line3d>();
		 for (Line3d line : completedLoop) {
			 double x1 = line.getPointOne().x / (precisionScaler) * pixelsPerMMX + imageOffsetX;
			 double y1 = line.getPointOne().y / (precisionScaler) * pixelsPerMMY + imageOffsetY;
			 double x2 = line.getPointTwo().x / (precisionScaler) * pixelsPerMMX + imageOffsetX;
			 double y2 = line.getPointTwo().y / (precisionScaler) * pixelsPerMMY + imageOffsetY;
			 sliceMinX = (int)Math.min(sliceMinX, Math.min(Math.floor(x1), Math.floor(x2)));
			 sliceMaxX = (int)Math.max(sliceMaxX, Math.max(Math.ceil(x1), Math.ceil(x2)));
			 sliceMinY = (int)Math.min(sliceMinY, Math.min(Math.floor(y1), Math.floor(y2)));
			 sliceMaxY = (int)Math.max(sliceMaxY, Math.max(Math.ceil(y1), Math.ceil(y2)));
			 lines.add(new Line3d(new Point3d(x1, y1, line.getPointOne().z * sliceResolution),
					 			  new Point3d(x2, y2, line.getPointTwo().z * sliceResolution),
							 	  line.getNormal(), line.getOriginatingFace(), false));
			
		 }
		 
		 completedFillInLoops.add(lines);
		 
		 
		 
		 
		 
		 //All this stuff below was for fun...
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
		 /*Line3d higherYLine = null;
		 Line3d lowerYLine = null;
		 if (completedLoop.get(0).getPointOne().y > completedLoop.get(1).getPointTwo().y) {
			 higherYLine = completedLoop.get(0);
			 lowerYLine = completedLoop.get(1);
		 } else {
			 higherYLine = completedLoop.get(1);
			 lowerYLine = completedLoop.get(0);
		 }
		 if (higherYLine.getNormal().y > 0) {
			 completedFillInLoops.add(completedLoop);
		 } else {
			 completedDigOutLoops.add(completedLoop);
		 }*/
	 }
	 
	 private LinkageDiscovery findLinkage(List<Line3d> currentWorkingLoop, Line3d currentLine) {
		  LinkageDiscovery completedLinkage = LinkageDiscovery.NoLinkFound;
		  Line3d firstInCurrentWorkingLoop = currentWorkingLoop.get(0);
		  Line3d lastInCurrentWorkingLoop = currentWorkingLoop.get(currentWorkingLoop.size() - 1);
		  if (currentLine.getPointTwo().ceilingEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (currentLine.getPointOne().ceilingEquals(lastInCurrentWorkingLoop.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  //System.out.println("Completed Link: 1 with [" + (currentWorkingLoop.size() + 1) + "] links (Link line)");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentWorkingLoop.add(0, currentLine);
		  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(currentLine.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(currentLine.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  //System.out.println("Completed Link: 2 with [" + (currentWorkingLoop.size() + 1) + "] links (Link line)");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentWorkingLoop.add(currentLine);
		  } else if (lastInCurrentWorkingLoop.getPointTwo().ceilingEquals(currentLine.getPointTwo())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().ceilingEquals(currentLine.getPointOne())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
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
				  //System.out.println("Completed Link: 4 with [" + (currentWorkingLoop.size() + 1) + "] links (Link line)");
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentLine.swap();
			  currentWorkingLoop.add(0, currentLine);
		  }
		  
		  return completedLinkage;
	 }

	 private LinkageDiscovery findLinkage(List<Line3d> currentWorkingLoop, List<Line3d> otherWorkingLoop) {
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

	 private List<Polygon> compilePolygons(List<List<Line3d>> completedFillInLoops) {
		 List<Polygon> polygons = new ArrayList<Polygon>();
		  for (List<Line3d> lines : completedFillInLoops) {
			  int[] xpoints = new int[lines.size()];
			  int[] xpointsCheck = new int[lines.size()];
			  int[] ypoints = new int[lines.size()];
			  int[] ypointsCheck = new int[lines.size()];
			  //System.out.println("Checking out[" + count++ + "] element Count:" + lines.size());
			  for (int t = 0; t < lines.size(); t++) {
				  xpoints[t] = (int)(lines.get(t).getPointOne().x);
				  ypoints[t] = (int)(lines.get(t).getPointOne().y);
				  xpointsCheck[t] = (int)(lines.get(t).getPointTwo().x);
				  ypointsCheck[t] = (int)(lines.get(t).getPointTwo().y);
				  int prevPoint = t > 0? t - 1:lines.size() - 1;
				  int nextPoint = t < lines.size() - 1? t + 1:0;
				  
				  //These are a double check for situations that should never happen other than if a single line(from a broken loop) was placed into the completedFillInLoops
				  if (lines.size() > 1) {
					  if (!lines.get(t).getPointTwo().ceilingEquals(lines.get(nextPoint).getPointOne())) {
						  System.out.println("Compare second point[" + t + "]:" + lines.get(t) + " to first point[" + nextPoint + "]:" + lines.get(nextPoint));
					  }
					  if (!lines.get(t).getPointOne().ceilingEquals(lines.get(prevPoint).getPointTwo())) {
						  System.out.println("Compare first point[" + t + "]:" + lines.get(t) + " to second point[" + prevPoint + "]:" + lines.get(prevPoint));
					  }
				  }
			  }
			  
			  Polygon polygon = new Polygon(xpoints, ypoints, xpoints.length);
			  polygons.add(polygon);
		  }
		  
		  return polygons;
	 }
	 
	 public Set<Triangle3d> getAllTriangles() {
		 return stlFile.getTriangles();
	 }
	 
	 public List<Shape3d> getTrianglesAt(int x, int y) {
		 List<Shape3d> intersections = new ArrayList<Shape3d>();
		 //System.out.println("x:" + x + " y:" + y);
		  for (Shape3d shape : getPolygonsOnSlice()) {
			  if (shape instanceof Triangle3d) {
				  
			  } else if (shape instanceof Line3d) {
				  Line3d line = (Line3d)shape;
				  double translatedX1 = (x - 1 - imageOffsetX) * precisionScaler / pixelsPerMMX;
				  double translatedY1 = (y - 1 - imageOffsetY) * precisionScaler / pixelsPerMMY;
				  double translatedX2 = (x + 1 - imageOffsetX) * precisionScaler / pixelsPerMMX;
				  double translatedY2 = (y + 1 - imageOffsetY) * precisionScaler / pixelsPerMMY;
	
				  if (line.intersects(translatedX1, translatedY1, translatedX2, translatedY2)) {
					  intersections.add(line);
				  }
			  }
		  }
		  
		  return intersections;
	 }
	 
	 public String translateLineToString(Line3d line) {
		 return "line: x1:" + translateX(line.getPointOne().x) + 
			",y1:" + translateY(line.getPointOne().y) + 
			" x2:" + translateX(line.getPointTwo().x) + 
			",y2:" + translateY(line.getPointTwo().y) + 
			" ylength:" + (translateY(line.getPointTwo().y) - translateY(line.getPointOne().y)) + 
			" xlength:" + (translateX(line.getPointTwo().x) - translateX(line.getPointOne().x));
	 }
	 
	 public Line3d translateLine(Line3d line) {
		 return new Line3d(translatePoint(line.getPointOne()), translatePoint(line.getPointTwo()), translatePoint(line.getNormal()), line.getOriginatingFace(), false);
	 }
	 
	 public Point3d translatePoint(Point3d point) {
		 return new Point3d(translateX(point.x), translateY(point.y), point.z * sliceResolution);
	 }
	 
	 public double translateX(double x) {
		 return (x / (precisionScaler) * pixelsPerMMX + imageOffsetX);
	 }
	 
	 public double translateY(double y) {
		 return (y / (precisionScaler) * pixelsPerMMY + imageOffsetY);
	 }
	 
	 private List<Shape3d> getPolygonsOnSlice() {
		 List<Shape3d> shapes = new ArrayList<Shape3d>();
		  for (Triangle3d triangle : stlFile.getTriangles()) {
			  if (triangle.intersectsZ(z)) {
				  Shape3d shape = triangle.getZIntersection(z);
				  shapes.add(shape);
			  }
		  }
		  
		  return shapes;
	 }
	 
	 public void debugPaintSlice(Graphics2D g) {
		  for (Shape3d shape : getPolygonsOnSlice()) {
				  if (shape instanceof Triangle3d) {
					  g.setColor(Color.blue);
					  Triangle3d tri = (Triangle3d)shape;
					  Polygon poly = new Polygon(tri.getX(), tri.gety(), 3);
					  g.drawPolygon(poly);
				  } else if (shape instanceof Line3d) {
					  Line3d line = (Line3d)shape;
					  g.setColor(Color.orange);
					  g.drawLine((int)(line.getPointTwo().x / (precisionScaler) * pixelsPerMMX + imageOffsetX), 
							  (int)(line.getPointTwo().y / (precisionScaler) * pixelsPerMMY + imageOffsetY), 
							  (int)((line.getPointTwo().x / (precisionScaler) + line.getNormal().x) * pixelsPerMMX + imageOffsetX), 
							  (int)((line.getPointTwo().y / (precisionScaler) + line.getNormal().y) * pixelsPerMMY + imageOffsetY));
					  
					  g.setColor(Color.cyan);
					  g.drawLine((int)(line.getPointOne().x / (precisionScaler) * pixelsPerMMX + imageOffsetX), 
							  (int)(line.getPointOne().y / (precisionScaler) * pixelsPerMMY + imageOffsetY), 
							  (int)((line.getPointOne().x / (precisionScaler) + line.getNormal().x) * pixelsPerMMX + imageOffsetX), 
							  (int)((line.getPointOne().y / (precisionScaler) + line.getNormal().y) * pixelsPerMMY + imageOffsetY));
					  
					  g.setColor(Color.red);
					  g.drawLine((int)(line.getPointOne().x / (precisionScaler) * pixelsPerMMX + imageOffsetX), 
							  (int)(line.getPointOne().y / (precisionScaler) * pixelsPerMMY + imageOffsetY), 
							  (int)(line.getPointTwo().x / (precisionScaler) * pixelsPerMMX + imageOffsetX), 
							  (int)(line.getPointTwo().y / (precisionScaler) * pixelsPerMMY + imageOffsetY));
					  
				  } else if (shape instanceof Point3d) {
					  g.setColor(Color.magenta);
					  Point3d point = (Point3d)shape;
					  g.drawLine((int)(point.x / (precisionScaler) * pixelsPerMMX + imageOffsetX), 
							  (int)(point.y / (precisionScaler) * pixelsPerMMY + imageOffsetX), 
							  (int)(point.x / (precisionScaler) * pixelsPerMMX + imageOffsetX), 
							  (int)(point.y / (precisionScaler) * pixelsPerMMY + imageOffsetX));
				  } else {
					  //System.out.println("No intersection. WRONG!!!");
				  }
		  }
		  
		  if (fillInPolygons != null) {
			  g.setColor(new Color(0, 0xff, 0, 50));
			  g.setBackground(new Color(0, 0xff, 0, 50));
			  for (Polygon currentPolygon : fillInPolygons) {
				  g.fillPolygon(currentPolygon);
				  g.drawPolygon(currentPolygon);
			  }
		  }
		  
		  if (fillInScanLines != null) {
			  g.setColor(new Color(0xff, 0xff/2, 0xff/2, 75));
			  g.setBackground(new Color(0xff, 0xff/2, 0xff/2, 75));
			  for (Line3d currentLine : fillInScanLines) {
				  g.drawLine((int)currentLine.getPointOne().x, (int)currentLine.getPointOne().y, (int)currentLine.getPointTwo().x, (int)currentLine.getPointTwo().y);
			  }
		  }
	 }
	 
	 public void paintSlice(Graphics2D g) {
		g.setBackground(Color.black);
		Rectangle r = g.getDeviceConfiguration().getBounds();
		g.clearRect(0, 0, r.width, r.height);

		/*g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	    g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);*/
	    
	    /*float data[] = { -1.0f, -1.0f, -1.0f, -1.0f, 9.0f, -1.0f, -1.0f, -1.0f,
	            -1.0f };
	    Kernel kernel = new Kernel(3, 3, data);
	    ConvolveOp convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
	    */
		if (fillInScanLines != null) {
			for (Line3d currentLine : fillInScanLines) {
				int x1 = (int)Math.floor(currentLine.getPointOne().x);
				int y1 = (int)Math.round(currentLine.getPointOne().y);
				int x2 = (int)Math.ceil(currentLine.getPointTwo().x);
				int y2 = (int)Math.round(currentLine.getPointTwo().y);
				
				g.setColor(Color.white);
				g.drawLine(x1, y1, x2, y2);
				//g.setColor(Color.darkGray);
				//g.drawLine(x1, y1, x1, y1);
				//g.drawLine(x2, y2, x2, y2);
			}
		}

		if (fillInPolygons != null) {
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setColor(Color.white);
			for (Polygon currentPolygon : fillInPolygons) {
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
					 Line3d line = new Line3d(checkPoint, ending, null, currentBrokenFace, false);
					 path.add(line);
					 return path;
				 } else if (previousPoint.ceilingEquals(ending)) {
					 Line3d line = new Line3d(checkPoint, ending, null, currentBrokenFace, false);
					 path.add(line);
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
	 
	 public List<List<Line3d>> colorizePolygons() {
		  sliceMaxX = -Integer.MAX_VALUE;
		  sliceMaxY = -Integer.MAX_VALUE;
		  sliceMinX = Integer.MAX_VALUE;
		  sliceMinY = Integer.MAX_VALUE;
		  ForkJoinPool pool = new ForkJoinPool();
		  List<Face3d> trianglesAndBrokenFacesForMazeTraversal = new ArrayList<Face3d>();
		  List<List<Line3d>> completedFillInLoops = new ArrayList<List<Line3d>>();
		  List<List<Line3d>> brokenLoops = new ArrayList<List<Line3d>>();
		  errors.clear();

		  //Find all intersections and put them into a sorted list.
		  //We put them in a sorted list because the join algorithm can be executed in virtually constant time
		  //Effectively, this loop is log n due to the sort into XYComparator
		  Set<Line3d> zIntersectionsBySortedX = new TreeSet<Line3d>(new XYComparatord());
		  for (Triangle3d triangle : stlFile.getTriangles()) {
			  if (triangle.intersectsZ(z)) {
				  Shape3d shape = triangle.getZIntersection(z);
				  if (shape instanceof Triangle3d) {
					  placeIntoCompletedLoopList(triangle.getLines(), completedFillInLoops);
					  trianglesAndBrokenFacesForMazeTraversal.add((Triangle3d)shape);
					  //System.out.println("Triangle:" + shape);
				  } else if (shape instanceof Line3d) {
					  zIntersectionsBySortedX.add((Line3d)shape);
				  }
				  //Ignore nulls and points(they don't print well...) :)
			  }
		  }
		  
		  /*System.out.println("===================");
		  System.out.println("zIntersectionsBySortedX:" + zIntersectionsBySortedX.size());
		  System.out.println("completedFillInLoops:" + completedFillInLoops.size());
		  System.out.println("===================");//*/
		  
		  //Even though this algorithm is structured to be n^2 it executes in (n * constant) time because of the comparator
		  //We join a set of loose lines into working loops of lines
		  //This algorithm is slightly more efficient than the below algorithm since reversals are less expensive
		  List<List<Line3d>> workingLoops = new ArrayList<List<Line3d>>();
		  Iterator<Line3d> lineIterator = zIntersectionsBySortedX.iterator();
		  nextLine : while (lineIterator.hasNext()) {
			  Line3d currentLine = lineIterator.next();
			  
			  Iterator<List<Line3d>> workingLoopIter = workingLoops.iterator();
			  while (workingLoopIter.hasNext()) {
				  List<Line3d> currentWorkingLoop = workingLoopIter.next();
				  switch (findLinkage(currentWorkingLoop, currentLine)) {
				  case FoundCompletion :
					  placeIntoCompletedLoopList(currentWorkingLoop, completedFillInLoops);
					  workingLoopIter.remove();
					  continue nextLine;
				  case FoundLink :
					  continue nextLine;
				  }
			  }
			  
			  List<Line3d> newLoop = new ArrayList<Line3d>();
			  newLoop.add(currentLine);
			  workingLoops.add(newLoop);
		  }
		  
		  /*System.out.println("===================");
		  System.out.println("zIntersectionsBySortedX:" + zIntersectionsBySortedX.size());
		  System.out.println("completedFillInLoops count:" + completedFillInLoops.size());
		  int value = 0;
		  for (List<Line3d> loop : completedFillInLoops) {
			  value += loop.size();
		  }
		  System.out.println("completedFillInLoops lines:" + value);
		  System.out.println("workingLoops count:" + workingLoops.size());
		  value = 0;
		  for (List<Line3d> loop : workingLoops) {
			  value += loop.size();
		  }
		  System.out.println("workingLoops lines:" + value);
		  System.out.println("===================");//*/
		  
		  //Empirically I've found that about half of all loops need to be joined with this method
		  //Now combine workingLoops into completedLoops. This algorithm is a bit more inefficient
		  //but there shouldn't be that many stray loops left to connect...
		  nextWorkingLoop : while (workingLoops.size() > 0) {
			  List<Line3d> currentWorkingLoop = workingLoops.get(0);

			  for (int otherIndex = 1; otherIndex < workingLoops.size(); otherIndex++) {
				  List<Line3d> otherWorkingLoop = workingLoops.get(otherIndex);
				  
				  switch (findLinkage(currentWorkingLoop, otherWorkingLoop)) {
				  case FoundCompletion :
					  placeIntoCompletedLoopList(currentWorkingLoop, completedFillInLoops);
					  workingLoops.remove(otherIndex);
					  workingLoops.remove(0);
					  continue nextWorkingLoop;
				  case FoundLink :
					  workingLoops.remove(otherIndex);
					  continue nextWorkingLoop;
				  }
			  }
			  
			  //System.out.println("Broken loop discovered[" + currentWorkingLoop.size() + "]:" + currentWorkingLoop);
			  brokenLoops.add(currentWorkingLoop);
			  workingLoops.remove(0);
		  }
		  
		  /*System.out.println("===================");
		  System.out.println("zIntersectionsBySortedX:" + zIntersectionsBySortedX.size());
		  System.out.println("completedFillInLoops count:" + completedFillInLoops.size());
		  value = 0;
		  for (List<Line3d> loop : completedFillInLoops) {
			  value += loop.size();
		  }
		  System.out.println("completedFillInLoops lines:" + value);
		  System.out.println("workingLoops count:" + workingLoops.size());
		  value = 0;
		  for (List<Line3d> loop : workingLoops) {
			  value += loop.size();
		  }
		  System.out.println("workingLoops lines:" + value);
		  System.out.println("brokenLoops count:" + brokenLoops.size());
		  value = 0;
		  for (List<Line3d> loop : brokenLoops) {
			  value += loop.size();
		  }
		  System.out.println("brokenLoops lines:" + value);
		  System.out.println("===================");//*/
		  
		  //empirically I've found that this block of code will only execute 1 in 100 times.
		  //So here is where things get complicated.
		  //We need to find our way through a maze of triangles and broken loops to create a full loop
		  //We can't just simply close broken loops because we could be closing over the top of an area that cuts back into the loop.
		  if (false && brokenLoops.size() > 0) {
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
				  Line3d line = new Line3d(currentBrokenLoop.get(0).getPointOne(), currentBrokenLoop.get(currentBrokenLoop.size() - 1).getPointTwo(), null, currentBrokenLoop.get(0).getOriginatingFace(), false);
				  path.add(line);
				  assembledFaces.add(currentElementIndex);
				  path = findPathThroughTrianglesAndBrokenLoops(currentBrokenLoop.get(0).getPointOne(), currentBrokenLoop.get(currentBrokenLoop.size() - 1).getPointTwo(), path, trianglesAndBrokenFacesForMazeTraversal, assembledFaces, 0);
				  if (path != null) {
					  System.out.println("Found path through maze");
					  //We skip the first element on the path because it's we already know it's currentBrokenLoop
					  for (int t = 1; t < assembledFaces.size(); t++) {
						  Face3d usedFace = trianglesAndBrokenFacesForMazeTraversal.get(assembledFaces.get(t));
						  if (usedFace instanceof BrokenFace3d) {
							  LinkageDiscovery discovery = findLinkage(currentBrokenLoop, ((BrokenFace3d)usedFace).getLines());
							  if (discovery == LinkageDiscovery.FoundCompletion) {
								  placeIntoCompletedLoopList(currentBrokenLoop, completedFillInLoops);
							  } else {
								  System.out.println("Maze traversal problem on face:" + ((BrokenFace3d)usedFace).getLines() + " discovery:" + discovery);
							  }
						  } else {
							  LinkageDiscovery discovery = findLinkage(currentBrokenLoop, path.get(0));
							  if (discovery == LinkageDiscovery.FoundCompletion) {
								  placeIntoCompletedLoopList(currentBrokenLoop, completedFillInLoops);
							  } else {
								  System.out.println("Maze traversal problem on triangle:" + path.get(0) + " discovery:" + discovery);
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
					  
					  placeIntoCompletedLoopList(path, completedFillInLoops);
				  } else {
					  brokenLoops.add(currentBrokenLoop);
				  }
				  
				  trianglesAndBrokenFacesForMazeTraversal.remove(currentElementIndex);
			  }
		  }
		  
		  
		  if (keepTrackOfErrors && brokenLoops.size() > 0) {
			  //System.out.println("Broken Loops(" + brokenLoops.size() + "):" + brokenLoops);
			  for (List<Line3d> currentBrokenLoop : brokenLoops) {
				  Line3d side = currentBrokenLoop.get(0);
				  errors.add(new StlError((Triangle3d)side.getOriginatingFace(), side));
				  if (currentBrokenLoop.size() > 1) {
					  side = currentBrokenLoop.get(0);
					  errors.add(new StlError((Triangle3d)side.getOriginatingFace(), side));
				  }
			  }
		  }

		  //close loops manually since we couldn't find a solution for these broken loops
		  for (List<Line3d> currentBrokenLoop : brokenLoops) {
			  if (currentBrokenLoop.size() > 1) {
				  Line3d line1 = currentBrokenLoop.get(0);
				  Line3d line2 = currentBrokenLoop.get(currentBrokenLoop.size() - 1);
				  Point3d normal = new Point3d(line1.getNormal().x + line2.getNormal().x, line1.getNormal().y + line2.getNormal().y, line1.getNormal().z + line2.getNormal().z);
				  Line3d line = new Line3d(line2.getPointTwo(), line1.getPointOne(), normal, null, false);
				  currentBrokenLoop.add(line);
			  }
			  
			  placeIntoCompletedLoopList(currentBrokenLoop, completedFillInLoops);
		  }
		  
		  //Preperation work for the Scanline algorithm
		  Map<Integer, List<Line3d>> inRangeLines = new HashMap<Integer, List<Line3d>>();
		  int breakupSize = (sliceMaxY - sliceMinY) / ScanlineFillPolygonWork.SMALLEST_UNIT_OF_WORK;
		  if (completedFillInLoops.size() % ScanlineFillPolygonWork.SMALLEST_UNIT_OF_WORK > 0) {
			  breakupSize++;
		  }
		  for (List<Line3d> currentPolygon : completedFillInLoops) {
			 for (Line3d currentLine : currentPolygon) {
				 double minY = currentLine.getMinY();
				 double maxY = currentLine.getMaxY();
				 for (int t = 0; t < breakupSize; t++) {
					 if (minY <= (double)(t + 1) * ScanlineFillPolygonWork.SMALLEST_UNIT_OF_WORK + sliceMinY  &&
					     maxY >= (double)t * ScanlineFillPolygonWork.SMALLEST_UNIT_OF_WORK + sliceMinY) {
						 List<Line3d> range = inRangeLines.get(t);
						 if (range == null) {
							 range = new ArrayList<Line3d>();
							 inRangeLines.put(t, range);
						 }
						 
						 range.add(currentLine); 
					 }
				 }
			 }
		  }
          
		  List<Future<ScanlineFillPolygonWork>> completedWork = new ArrayList<Future<ScanlineFillPolygonWork>>();
		  for (int y = 0; y < breakupSize; y++) {
			  List<Line3d> inRange = inRangeLines.get(y);
			  if (inRange == null) {
				  continue;
			  }
			  
			  ScanlineFillPolygonWork work = new ScanlineFillPolygonWork(
					  inRange, 
					  y * ScanlineFillPolygonWork.SMALLEST_UNIT_OF_WORK + sliceMinY,
					  (y + 1) * ScanlineFillPolygonWork.SMALLEST_UNIT_OF_WORK + sliceMinY - 1,
					  z);
			  completedWork.add(pool.submit(work));
		  }
		  
		  fillInScanLines = new ArrayList<Line3d>();
		  buildArea = 0;
		  for (Future<ScanlineFillPolygonWork> currentWork : completedWork) {
			  ScanlineFillPolygonWork work;
				try {
					work = currentWork.get();
					if (keepTrackOfErrors) {
						  for (Face3d currentInsideOutPolygon : work.getInsideOutPolygons()) {
							  errors.add(new StlError((Triangle3d)currentInsideOutPolygon, ErrorType.Insideout));
						  }
					}
					
					fillInScanLines.addAll(work.getScanLines());
					buildArea += work.getBuildArea();
				} catch (InterruptedException | ExecutionException e) {
					// TODO Do something better than this!!!
					e.printStackTrace();
				}
		  }
		  
		  //I'm not sure I want to do this. It just traces the polygon but doesn't provide much value other than an edge blur.
		  //System.out.println("Polygons");
		  //System.out.println("======");
          fillInPolygons = compilePolygons(completedFillInLoops);
		  
		  /*System.out.println("TOTALS");
		  System.out.println("======");
		  System.out.println("Completed Loops(" + completedFillInLoops.size() + "):" + completedFillInLoops);
		  for (List<Line3d> loop : completedFillInLoops) {
			  System.out.println(loop.get(0));
			  if (loop.size() > 1) {
				  System.out.println(loop.get(loop.size() - 1));
			  }
			  System.out.println();
		  }
		  System.out.println("Working Loops(" + workingLoops.size() + "):" + workingLoops);
		  System.out.println("======");//*/
		  
		  pool.shutdown();
		  return completedFillInLoops;
	 }
	 
	 public void loadFile(Double buildPlatformXPixels, Double buildPlatformYPixels) throws FileNotFoundException {
		 stlFile.load(stlFileToSlice);
		 
		 if (imageOffsetX == null && buildPlatformXPixels != null) {
			 imageOffsetX = (buildPlatformXPixels / 2) - (stlFile.getWidth() / precisionScaler * pixelsPerMMX / 2) - (stlFile.getXmin() / precisionScaler * pixelsPerMMX); 
		 }
		 if (imageOffsetY == null && buildPlatformYPixels != null) {
			 imageOffsetY = (buildPlatformYPixels / 2) - (stlFile.getHeight() / precisionScaler * pixelsPerMMY / 2) - (stlFile.getYmin() / precisionScaler * pixelsPerMMY); 
		 }
	 }
	 
	 public int getZ() {
 		return z; 
	 }
	
     public void setZ(int z) {
 		this.z = z;
		fillInPolygons = null;
		fillInScanLines = null;
		buildArea = 0;
	 }
	
	 public int getBuildArea() {
		return buildArea;
	}

	public int getZMin() {
		return (int)Math.ceil(stlFile.getZmin());
	 }
	
	 public int getZMax() {
		return (int)Math.floor(stlFile.getZmax());
	 }
}
