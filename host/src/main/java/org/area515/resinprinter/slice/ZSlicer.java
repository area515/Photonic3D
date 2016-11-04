package org.area515.resinprinter.slice;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.slice.StlError.ErrorType;
import org.area515.resinprinter.stl.BrokenFace3d;
import org.area515.resinprinter.stl.Face3d;
import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Point3d;
import org.area515.resinprinter.stl.Shape3d;
import org.area515.resinprinter.stl.Triangle3d;
import org.area515.resinprinter.stl.XYComparatord;
import org.area515.util.Log4jTimer;

public class ZSlicer {
    private static final Logger logger = LogManager.getLogger();

     //We need to scale the whole stl large enough to have enough precision in front of the decimal point
	 //Too little and you get points that won't match, too much and you end up beating a double's precision
	 //This number is a balancing act.
	 private double precisionScaler = 1;
	 private double pixelsPerMMX = 5;
	 private double pixelsPerMMY = 5;
	 private double stlScale = 1;
	 
	 private Double imageOffsetX = null;
	 private Double imageOffsetY = null;
	 private double sliceResolution = 0.1;
	 private double zOffset = .05;
	 private StlFile<Triangle3d, Point3d> stlFile;
	 private boolean keepTrackOfErrors = false;
	 private boolean rewriteNormalsWithRightHandRule = false;
	 private PolygonMendingMechanism fixBrokenLoops;
	 
	 //These are the variables per z
	 private List<StlError> errors = new ArrayList<StlError>();
	 private List<Polygon> fillInPolygons = null;
	 private List<Line3d> fillInScanLines = null;
	 private int z = 0;
	 private int sliceMaxX;
	 private int sliceMaxY;
	 private int sliceMaxZ;
	 private int sliceMinX;
	 private int sliceMinY;
	 private int sliceMinZ;
	 private int buildArea;
	 
	 //TODO: Need to add in super sampling
	 public ZSlicer(double stlScale, double pixelsPerMMX, double pixelsPerMMY, double zSliceResolution, double zSliceOffset, boolean keepTrackOfErrors, boolean rewriteNormalsWithRightHandRule, PolygonMendingMechanism fixBrokenLoops) {
		 this.rewriteNormalsWithRightHandRule = rewriteNormalsWithRightHandRule;
		 this.stlScale = stlScale;
		 this.pixelsPerMMX = pixelsPerMMX;
		 this.pixelsPerMMY = pixelsPerMMY;
		 this.sliceResolution = zSliceResolution;
		 this.zOffset = zSliceOffset;
		 this.keepTrackOfErrors = keepTrackOfErrors;
		 this.fixBrokenLoops = fixBrokenLoops;
		 
		 stlFile = new StlFile<Triangle3d, Point3d>() {
			private Triangle3d lastTriangle;
			private Triangle3d firstTriangle;
			
			@Override
			protected Point3d buildPoint(double x, double y, double z) {
				return new Point3d(
						x * (ZSlicer.this.precisionScaler * ZSlicer.this.stlScale), 
						y * (ZSlicer.this.precisionScaler * ZSlicer.this.stlScale), 
						z * (ZSlicer.this.precisionScaler * ZSlicer.this.stlScale));
			}
			
			@Override
			public Set<Triangle3d> createSet() {
				return new TreeSet<Triangle3d>();
			}
			
			@Override
			protected void buildTriangle(Point3d point1, Point3d point2, Point3d point3, double[] normal) {
				Triangle3d newTriangle = new Triangle3d(new Point3d[]{point1, point2, point3}, new Point3d(normal[0], normal[1], normal[2]), null, null, triangles.size());
				if (lastTriangle != null) {
					lastTriangle.setNextTriangle(newTriangle);
				}
			    triangles.add(newTriangle);
			    
			    zmin = Math.min(point1.z, Math.min(point2.z, Math.min(point3.z, zmin)));
			    zmax = Math.max(point1.z, Math.max(point2.z, Math.max(point3.z, zmax)));
			    xmin = Math.min(point1.x, Math.min(point2.x, Math.min(point3.x, xmin)));
			    xmax = Math.max(point1.x, Math.max(point2.x, Math.max(point3.x, xmax)));
			    ymin = Math.min(point1.y, Math.min(point2.y, Math.min(point3.y, ymin)));
			    ymax = Math.max(point1.y, Math.max(point2.y, Math.max(point3.y, ymax)));
			    lastTriangle = newTriangle;
			    if (firstTriangle == null) {
			    	firstTriangle = newTriangle;
			    }
			}
			
			public Triangle3d getFirstTriangle() {
				return firstTriangle;
			}
		  };
	 }
	 
	 public List<StlError> getStlErrors() {
		 return errors;
	 }
	 
	 public void placeIntoCompletedLoopList(List<Line3d> completedLoop, List<List<Line3d>> completedFillInLoops) {
		 List<Line3d> lines = new ArrayList<Line3d>();
		 for (Line3d line : completedLoop) {
			 double x1 = line.getPointOne().x / precisionScaler * pixelsPerMMX + imageOffsetX;
			 double y1 = line.getPointOne().y / precisionScaler * pixelsPerMMY + imageOffsetY;
			 double z1 = line.getPointOne().z / precisionScaler;// / sliceResolution;
			 double x2 = line.getPointTwo().x / precisionScaler * pixelsPerMMX + imageOffsetX;
			 double y2 = line.getPointTwo().y / precisionScaler * pixelsPerMMY + imageOffsetY;
			 double z2 = line.getPointTwo().z / precisionScaler;// / sliceResolution;
			 sliceMinX = (int)Math.min(sliceMinX, Math.min(Math.floor(x1), Math.floor(x2)));
			 sliceMaxX = (int)Math.max(sliceMaxX, Math.max(Math.ceil(x1), Math.ceil(x2)));
			 sliceMinY = (int)Math.min(sliceMinY, Math.min(Math.floor(y1), Math.floor(y2)));
			 sliceMaxY = (int)Math.max(sliceMaxY, Math.max(Math.ceil(y1), Math.ceil(y2)));
			 sliceMinZ = (int)Math.min(sliceMinZ, Math.min(Math.floor(z1), Math.floor(z2)));
			 sliceMaxZ = (int)Math.max(sliceMaxZ, Math.max(Math.ceil(z1), Math.ceil(z2)));
			 lines.add(new Line3d(new Point3d(x1, y1, z1),
					 			  new Point3d(x2, y2, z2),
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
					 logger.debug("We should never have an instanceof of a broken face here!!!");
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
		  if (currentLine.getPointTwo().pointEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (currentLine.getPointOne().pointEquals(lastInCurrentWorkingLoop.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  logger.debug("Completed Link: 1 with [{}] links (Link line)", currentWorkingLoop.size() + 1);
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentWorkingLoop.add(0, currentLine);
		  } else if (lastInCurrentWorkingLoop.getPointTwo().pointEquals(currentLine.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().pointEquals(currentLine.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  logger.debug("Completed Link: 2 with [{}] links (Link line)", currentWorkingLoop.size() + 1);
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentWorkingLoop.add(currentLine);
		  } else if (lastInCurrentWorkingLoop.getPointTwo().pointEquals(currentLine.getPointTwo())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().pointEquals(currentLine.getPointOne())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  logger.debug("Completed Link: 3 with [{}] links (Link line)", currentWorkingLoop.size() + 1);
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentLine.swap();
			  currentWorkingLoop.add(currentLine);  
		  } else if (currentLine.getPointOne().pointEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointTwo().pointEquals(currentLine.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  logger.debug("Completed Link: 4 with [{}] links (Link line)", currentWorkingLoop.size() + 1);
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
		  if (lastInOtherWorkingLoop.getPointTwo().pointEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  logger.debug("Found Link: 1 with [{},{}] links (Link Loop)", currentWorkingLoop.size(), otherWorkingLoop.size());
			  //Check to determine if this loop is closed
			  if (firstInOtherWorkingLoop.getPointOne().pointEquals(lastInCurrentWorkingLoop.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  logger.debug("Completed Link: 1 with [{}] links (Link Loop)", currentWorkingLoop.size() + otherWorkingLoop.size());
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentWorkingLoop.addAll(0, otherWorkingLoop);
		  } else if (lastInCurrentWorkingLoop.getPointTwo().pointEquals(firstInOtherWorkingLoop.getPointOne())) {
			  logger.debug("Found Link: 2 with [{},{}] links (Link Loop)", currentWorkingLoop.size(), otherWorkingLoop.size());
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().pointEquals(lastInOtherWorkingLoop.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  logger.debug("Completed Link: 2 with [{}] links (Link Loop)", currentWorkingLoop.size() + otherWorkingLoop.size());
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  currentWorkingLoop.addAll(otherWorkingLoop);
		  } else if (lastInCurrentWorkingLoop.getPointTwo().pointEquals(lastInOtherWorkingLoop.getPointTwo())) {
			  logger.debug("Found Link: 3 with [{},{}] links (Link Loop)", currentWorkingLoop.size(), otherWorkingLoop.size());
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointOne().pointEquals(firstInOtherWorkingLoop.getPointOne())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  logger.debug("Completed Link: 3 with [{}] links (Link Loop)", currentWorkingLoop.size() + otherWorkingLoop.size());
			  } else {
				  completedLinkage = LinkageDiscovery.FoundLink;
			  }
			  
			  for (Line3d currentLine : otherWorkingLoop) {
				  currentLine.swap();
			  }
			  Collections.reverse(otherWorkingLoop);
			  currentWorkingLoop.addAll(otherWorkingLoop);					  
		  } else if (firstInOtherWorkingLoop.getPointOne().pointEquals(firstInCurrentWorkingLoop.getPointOne())) {
			  logger.debug("Found Link: 4 with [{},{}] links (Link Loop)", currentWorkingLoop.size(), otherWorkingLoop.size());
			  //Check to determine if this loop is closed
			  if (firstInCurrentWorkingLoop.getPointTwo().pointEquals(lastInOtherWorkingLoop.getPointTwo())) {
				  completedLinkage = LinkageDiscovery.FoundCompletion;
				  logger.debug("Completed Link: 4 with [{}] links (Link Loop)", currentWorkingLoop.size() + otherWorkingLoop.size());
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
			  logger.debug("Checking out[{}] element Count:{}", lines, lines.size());
			  for (int t = 0; t < lines.size(); t++) {
				  xpoints[t] = (int)Math.round(lines.get(t).getPointOne().x);
				  ypoints[t] = (int)Math.round(lines.get(t).getPointOne().y);
				  xpointsCheck[t] = (int)Math.round(lines.get(t).getPointTwo().x);
				  ypointsCheck[t] = (int)Math.round(lines.get(t).getPointTwo().y);
				  int prevPoint = t > 0? t - 1:lines.size() - 1;
				  int nextPoint = t < lines.size() - 1? t + 1:0;
				  
				  //These are a double check for situations that should never happen other than if a single line(from a broken loop) was placed into the completedFillInLoops
				  if (lines.size() > 1) {
					  if (!lines.get(t).getPointTwo().pointEquals(lines.get(nextPoint).getPointOne())) {
						  logger.warn("Compare second point[{}]:{} to first point[{}]:{}", t, lines.get(t), nextPoint, lines.get(nextPoint));
					  }
					  if (!lines.get(t).getPointOne().pointEquals(lines.get(prevPoint).getPointTwo())) {
						  logger.warn("Compare first point[{}]:{} to second point[{}]:{}", t, lines.get(t), prevPoint, lines.get(prevPoint));
					  }
				  }
			  }
			  
			  Polygon polygon = new Polygon(xpoints, ypoints, xpoints.length);
			  polygons.add(polygon);
		  }
		  
		  return polygons;
	 }
	 
	 public Triangle3d getFirstTriangle() {
		 return stlFile.getFirstTriangle();
	 }
	 
	 public Collection<Triangle3d> getAllTriangles() {
		 return stlFile.getTriangles();
	 }
	 
	 private boolean isIntersecting(Line3d line, int x, int y) {
		  double translatedX1 = (x - 1 - imageOffsetX) * precisionScaler / pixelsPerMMX;
		  double translatedY1 = (y - 1 - imageOffsetY) * precisionScaler / pixelsPerMMY;
		  double translatedX2 = (x + 1 - imageOffsetX) * precisionScaler / pixelsPerMMX;
		  double translatedY2 = (y + 1 - imageOffsetY) * precisionScaler / pixelsPerMMY;

		  return line.intersects(translatedX1, translatedY1, translatedX2, translatedY2);
	 }
	 
	 //Not used in org.area515.resinprinter.job.STLImageRenderer.STLImageRenderer
	 public List<Shape3d> getTrianglesAt(int x, int y) {
		 List<Shape3d> intersections = new ArrayList<Shape3d>();
		 logger.debug("x:{} y:{}",x, y);
		  for (Shape3d shape : getPolygonsOnSlice()) {
			  if (shape instanceof Triangle3d) {
				  for (Line3d line : ((Triangle3d)shape).getLines()) {
					  if (isIntersecting(line, x, y)) {
						  intersections.add(line);
					  }
				  }
			  } else if (shape instanceof Line3d) {
				  Line3d line = (Line3d)shape;
				  if (isIntersecting(line, x, y)) {
					  intersections.add(line);
				  }
			  } else {
				  logger.debug("Found unknown instance:{}", shape);
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
	 
	 public Triangle3d translateTriangle(Triangle3d triangle) {
		 List<Point3d> points = triangle.getPoints();
		 Face3d parentShape = triangle.getOriginatingShape() == null? triangle: triangle.getOriginatingShape();
		 return new Triangle3d(
				 new Point3d[]{
						 translatePoint(points.get(0)), 
						 translatePoint(points.get(1)), 
						 translatePoint(points.get(2))}, 
				 triangle.getNormal(), 
				 parentShape,
				 null,
				 parentShape instanceof Triangle3d?((Triangle3d)parentShape).getOriginalIndex():null);
	 }
	 
	 public Point3d translatePoint(Point3d point) {
		 return new Point3d(translateX(point.x), translateY(point.y), translateZ(point.z));
	 }
	 
	 public double translateX(double x) {
		 return (x / precisionScaler * pixelsPerMMX + imageOffsetX);
	 }
	 
	 public double translateY(double y) {
		 return (y / precisionScaler * pixelsPerMMY + imageOffsetY);
	 }
	 
	 public double translateZ(double z) {
		 return z / precisionScaler;// / sliceResolution;
	 }
	 
	 //NOT used in org.area515.resinprinter.job.STLImageRenderer.STLImageRenderer
	 private List<Shape3d> getPolygonsOnSlice() {
		 List<Shape3d> shapes = new ArrayList<Shape3d>();
		  for (Triangle3d triangle : stlFile.getTriangles()) {
			  //if (triangle.intersectsZ(z + zOffset)) {
				  Shape3d shape = triangle.getZIntersection(z * precisionScaler * sliceResolution + zOffset);
				  if (shape != null) {
					  shapes.add(shape);
				  }
			  //}
		  }
		  
		  return shapes;
	 }
	 
	 private int[] convertX(int[] x) {
		 return new int[]{(int)translateX(x[0]), (int)translateX(x[1]), (int)translateX(x[2])};
	 }
	 
	 private int[] convertY(int[] y) {
		 return new int[]{(int)translateY(y[0]), (int)translateY(y[1]), (int)translateY(y[2])};
	 }
	 
	 //Not used in org.area515.resinprinter.job.STLImageRenderer.STLImageRenderer
	 public void debugPaintSlice(Graphics2D g) {
		  for (Shape3d shape : getPolygonsOnSlice()) {
				  if (shape instanceof Triangle3d) {
					  g.setColor(Color.darkGray);
					  Triangle3d tri = (Triangle3d)shape;
					  Polygon poly = new Polygon(convertX(tri.getX()), convertY(tri.gety()), 3);
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
							  (int)(point.y / (precisionScaler) * pixelsPerMMY + imageOffsetY), 
							  (int)(point.x / (precisionScaler) * pixelsPerMMX + imageOffsetX), 
							  (int)(point.y / (precisionScaler) * pixelsPerMMY + imageOffsetY));
				  } else {
					  logger.debug("No intersection. WRONG!!!");
				  }
		  }
		  
		  //Green alpha
		  if (fillInPolygons != null) {
			  g.setColor(new Color(0, 0xff, 0, 50));
			  g.setBackground(new Color(0, 0xff, 0, 50));
			  for (Polygon currentPolygon : fillInPolygons) {
				  g.fillPolygon(currentPolygon);
				  g.drawPolygon(currentPolygon);
			  }
		  }
		  
		  //Red alpha
		  if (fillInScanLines != null) {
			  g.setColor(new Color(0xff, 0xff/2, 0xff/2, 75));
			  g.setBackground(new Color(0xff, 0xff/2, 0xff/2, 75));
			  for (Line3d currentLine : fillInScanLines) {
				  g.drawLine((int)currentLine.getPointOne().x, (int)currentLine.getPointOne().y, (int)currentLine.getPointTwo().x, (int)currentLine.getPointTwo().y);
			  }
		  }
	 }
	 
	 public void paintSlice(Graphics2D g, boolean fillBlackFirst) {
		 if (fillBlackFirst) {
			g.setBackground(Color.black);
			Rectangle r = g.getDeviceConfiguration().getBounds();
			g.clearRect(0, 0, r.width, r.height);
		 }
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
				int x1 = (int)Math.round(currentLine.getPointOne().x);
				int y1 = (int)Math.round(currentLine.getPointOne().y);
				int x2 = (int)Math.round(currentLine.getPointTwo().x);
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
			 if (checkPoint.pointEquals(beginning)) {
				 usedFaces.add(currentTriangleIndex);
				 
				 //First check if we can end this fiasco...
				 if (nextPoint.pointEquals(ending)) {
					 Line3d line = new Line3d(checkPoint, ending, null, currentBrokenFace, false);//TODO: Use proper normal
					 path.add(line);
					 return path;
				 } else if (previousPoint.pointEquals(ending)) {
					 Line3d line = new Line3d(checkPoint, ending, null, currentBrokenFace, false);//TODO: Use proper normal
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

	 //used in org.area515.resinprinter.job.STLImageRenderer.STLImageRenderer
	 public List<List<Line3d>> colorizePolygons(List<Triangle3d> watchedTriangles, List<Integer> watchedYs) {
		 
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
		  
		  logger.info("===================");
		  logger.info("ZSlice started", ()->Log4jTimer.startTimer("sliceTime"));
		  Set<Line3d> zIntersectionsBySortedX = new TreeSet<Line3d>(new XYComparatord(Triangle3d.EQUAL_TOLERANCE));
		  for (Triangle3d triangle : stlFile.getTriangles()) {
			  if (watchedTriangles != null && watchedTriangles.contains(triangle)) {
				  logger.debug("Watched triangle:{}", ()-> translateTriangle(triangle));
			  }
			  /*if (triangle.onZeroZ())  {
				  logger.debug("on z");//123456
			  }*/
			  double actualZ = (double)z * precisionScaler * sliceResolution + zOffset;
			  if (triangle.intersectsZ(actualZ)) {
				  Shape3d shape = triangle.getZIntersection(actualZ);
				  if (shape instanceof Triangle3d) {
					  //TODO: This is experimental
					  //placeIntoCompletedLoopList(((Triangle3d)shape).getLines(), completedFillInLoops);
					  //trianglesAndBrokenFacesForMazeTraversal.add((Triangle3d)shape);
					  //TODO: This is experimental
					  zIntersectionsBySortedX.addAll(((Triangle3d)shape).getLines());
					  logger.debug("Triangle:{}", ()-> translateTriangle((Triangle3d)shape));
				  } else if (shape instanceof Line3d) {
					  zIntersectionsBySortedX.add((Line3d)shape);
					  logger.debug("Line:" +  translateLine((Line3d)shape));
				  } else if (shape != null) {
					  logger.debug("Ignored Point:{}", () -> translatePoint((Point3d)shape));
				  }	 else {
					  logger.debug("No geometrical intersection");
				  }
			  } else {
				  logger.debug("Intersection was optimized out");
			  }//*/
		  }
		  
		  logger.info("IntersectionTime:{}", ()->Log4jTimer.splitTimer("sliceTime"));
		  logger.debug("===================");
		  logger.debug("zIntersectionsBySortedX:{}", zIntersectionsBySortedX.size());
		  logger.debug("completedFillInLoops:{}", completedFillInLoops.size());
		  logger.debug("===================");
		  
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
		  
		  logger.info("Primary linkage search:{}", ()->Log4jTimer.splitTimer("sliceTime"));

		  if (logger.isDebugEnabled()) {
			  logger.debug("===================");
			  logger.debug("zIntersectionsBySortedX:{}", zIntersectionsBySortedX.size());
			  logger.debug("completedFillInLoops count:{}", completedFillInLoops.size());
			  int value = 0;
			  for (List<Line3d> loop : completedFillInLoops) {
				  value += loop.size();
			  }
			  logger.debug("completedFillInLoops lines:{}", value);
			  logger.debug("workingLoops count:{}", + workingLoops.size());
			  value = 0;
			  for (List<Line3d> loop : workingLoops) {
				  value += loop.size();
			  }
			  logger.debug("workingLoops lines:{}", value);
			  logger.debug("===================");//*/
			  logger.debug("Debug print time:{}", ()->Log4jTimer.splitTimer("sliceTime"));
		  }
		  
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
			  
			  logger.debug("Broken loop discovered[{}]:{}", currentWorkingLoop.size(), currentWorkingLoop);
			  brokenLoops.add(currentWorkingLoop);
			  workingLoops.remove(0);
		  }
		  
		  logger.info("Secondary linkage search:{}", ()->Log4jTimer.splitTimer("sliceTime"));

		  if (logger.isDebugEnabled()) {
			  logger.debug("===================");
			  logger.debug("zIntersectionsBySortedX:{}", zIntersectionsBySortedX.size());
			  logger.debug("completedFillInLoops count:{}", completedFillInLoops.size());
			  int value = 0;
			  for (List<Line3d> loop : completedFillInLoops) {
				  value += loop.size();
			  }
			  logger.debug("completedFillInLoops lines:{}", value);
			  logger.debug("workingLoops count:{}", workingLoops.size());
			  value = 0;
			  for (List<Line3d> loop : workingLoops) {
				  value += loop.size();
			  }
			  logger.debug("workingLoops lines:{}", value);
			  logger.debug("brokenLoops count:{}", brokenLoops.size());
			  value = 0;
			  for (List<Line3d> loop : brokenLoops) {
				  value += loop.size();
			  }
			  logger.debug("brokenLoops lines:{}", value);
			  logger.debug("===================");
			  logger.debug("Print broken loops:{}", ()->Log4jTimer.splitTimer("sliceTime"));
		  }
		  
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
					  currentLoop.get(0).getPointOne().pointEquals(currentLoop.get(0).getPointTwo())) {
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
				  Line3d line = new Line3d(currentBrokenLoop.get(0).getPointOne(), currentBrokenLoop.get(currentBrokenLoop.size() - 1).getPointTwo(), null, currentBrokenLoop.get(0).getOriginatingFace(), false);//TODO: Proper normal
				  path.add(line);
				  assembledFaces.add(currentElementIndex);
				  path = findPathThroughTrianglesAndBrokenLoops(currentBrokenLoop.get(0).getPointOne(), currentBrokenLoop.get(currentBrokenLoop.size() - 1).getPointTwo(), path, trianglesAndBrokenFacesForMazeTraversal, assembledFaces, 0);
				  if (path != null) {
					  logger.info("Found path through maze");
					  //We skip the first element on the path because it's we already know it's currentBrokenLoop
					  for (int t = 1; t < assembledFaces.size(); t++) {
						  Face3d usedFace = trianglesAndBrokenFacesForMazeTraversal.get(assembledFaces.get(t));
						  if (usedFace instanceof BrokenFace3d) {
							  LinkageDiscovery discovery = findLinkage(currentBrokenLoop, ((BrokenFace3d)usedFace).getLines());
							  if (discovery == LinkageDiscovery.FoundCompletion) {
								  placeIntoCompletedLoopList(currentBrokenLoop, completedFillInLoops);
							  } else {
								  logger.info("Maze traversal problem on face:{} discovery:{}", ((BrokenFace3d)usedFace).getLines(), discovery);
							  }
						  } else {
							  LinkageDiscovery discovery = findLinkage(currentBrokenLoop, path.get(0));
							  if (discovery == LinkageDiscovery.FoundCompletion) {
								  placeIntoCompletedLoopList(currentBrokenLoop, completedFillInLoops);
							  } else {
								  logger.info("Maze traversal problem on triangle:{} discovery:{}", path.get(0), discovery);
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
			  logger.debug("Broken Loops({}):{}",brokenLoops.size(), brokenLoops);
			  for (List<Line3d> currentBrokenLoop : brokenLoops) {
				  Line3d side = currentBrokenLoop.get(0);
				  errors.add(new StlError((Triangle3d)side.getOriginatingFace(), side));
				  if (currentBrokenLoop.size() > 1) {
					  side = currentBrokenLoop.get(currentBrokenLoop.size() - 1);
					  errors.add(new StlError((Triangle3d)side.getOriginatingFace(), side));
				  }
			  }
			  
			  logger.info("Stl error capturing:{}", ()->Log4jTimer.splitTimer("sliceTime"));
		  }

		  //close loops manually since we couldn't find a solution for these broken loops
		  if (fixBrokenLoops != null && brokenLoops.size() > 0) {
			  fixBrokenLoops.mendPolygon(this, brokenLoops, completedFillInLoops);
			  logger.info("Broken loop mending:{}", ()->Log4jTimer.splitTimer("sliceTime"));
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
		  logger.info("Break scanline up into pieces:{}", ()->Log4jTimer.splitTimer("sliceTime"));

		  List<Future<ScanlineFillPolygonWork>> completedWork = new ArrayList<Future<ScanlineFillPolygonWork>>();
		  for (int y = 0; y < breakupSize; y++) {
			  List<Line3d> inRange = inRangeLines.get(y);
			  if (inRange == null) {
				  continue;
			  }
			  
			  ScanlineFillPolygonWork work = new ScanlineFillPolygonWork(
					  inRange, 
					  watchedTriangles,
					  watchedYs,
					  y * ScanlineFillPolygonWork.SMALLEST_UNIT_OF_WORK + sliceMinY,
					  (y + 1) * ScanlineFillPolygonWork.SMALLEST_UNIT_OF_WORK + sliceMinY - 1,
					  z);
			  completedWork.add(pool.submit(work));
		  }
		  logger.info("Submit scanline work:{}", ()->Log4jTimer.splitTimer("sliceTime"));
		  
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
					logger.error("Error in executing polygon work", e);
				}
		  }
		  logger.info("Wait for scanline work:{}", ()->Log4jTimer.splitTimer("sliceTime"));
		  
		  //I'm not sure I want to do this. It just traces the polygon but doesn't provide much value other than an edge blur.
		  logger.debug("Polygons");
		  logger.debug("======");
          fillInPolygons = compilePolygons(completedFillInLoops);
		  logger.info("Compile polygons:{}", ()->Log4jTimer.splitTimer("sliceTime"));
			  
		  if (logger.isDebugEnabled()) {
	          logger.debug("TOTALS");
	          logger.debug("======");
	          logger.debug("Completed Loops({}):{}", completedFillInLoops.size(), completedFillInLoops);
			  for (List<Line3d> loop : completedFillInLoops) {
				  logger.debug(loop.get(0));
				  if (loop.size() > 1) {
					  logger.debug("last loop element with size > 1:{}", loop.get(loop.size() - 1));
				  }
				  logger.debug("");
			  }
			  logger.debug("Working Loops({}):{}",workingLoops.size(), workingLoops);
			  logger.debug("======");//*/
			  logger.debug("Print working loops:{}", ()->Log4jTimer.splitTimer("sliceTime"));
		  }
		  pool.shutdown();
		  logger.info("ZSlice complete:{}", ()->Log4jTimer.completeTimer("sliceTime"));
		  return completedFillInLoops;
	 }
	 
	 public void loadFile(InputStream stream, Double buildPlatformXPixels, Double buildPlatformYPixels) throws IOException {
		  logger.info("Load file start", ()->Log4jTimer.startTimer("fileLoadTime"));
		  stlFile.load(stream, rewriteNormalsWithRightHandRule);
 
		if (imageOffsetX == null) {
			if (buildPlatformXPixels != null) {
				imageOffsetX = (buildPlatformXPixels / 2) 
						- (stlFile.getWidth() / precisionScaler * pixelsPerMMX / 2)
						- (stlFile.getXmin() / precisionScaler * pixelsPerMMX);
			} else {
				imageOffsetX = -stlFile.getXmin() / precisionScaler * pixelsPerMMX;
			}
		}
		if (imageOffsetY == null) {
			if (buildPlatformYPixels != null) {
				imageOffsetY = (buildPlatformYPixels / 2) 
						- (stlFile.getHeight() / precisionScaler * pixelsPerMMY / 2)
						- (stlFile.getYmin() / precisionScaler * pixelsPerMMY);
			} else {
				imageOffsetY = -stlFile.getYmin() / precisionScaler * pixelsPerMMY;
			}
		}
		logger.info("Load file stop:{}", ()->Log4jTimer.completeTimer("fileLoadTime"));
	 }
	 
	 public int getZIndex() {
 		return z; 
	 }
	
     public void setZIndex(int z) {
 		this.z = z;
		fillInPolygons = null;
		fillInScanLines = null;
		buildArea = 0;
	 }
	
	 public int getBuildArea() {
		return buildArea;
	}

	public double getSliceResolution() {
		return sliceResolution;
	}

	public double getzOffset() {
		return zOffset;
	}

	public double getStlScale() {
		return stlScale;
	}
	
	public double getWidthPixels() {
		return stlFile.getWidth() * pixelsPerMMX;
	}
	
	public double getHeightPixels() {
		return stlFile.getHeight() * pixelsPerMMY;
	}
	
	public int getZMinIndex() {
		return (int)Math.ceil(stlFile.getZmin() / precisionScaler / sliceResolution - zOffset);
	}
	
	public int getZMaxIndex() {
		return (int)Math.floor(stlFile.getZmax() / precisionScaler / sliceResolution - zOffset);
	}
}
