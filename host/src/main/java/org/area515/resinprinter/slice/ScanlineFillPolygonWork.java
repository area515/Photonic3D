package org.area515.resinprinter.slice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.RecursiveTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.stl.Face3d;
import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Point3d;
import org.area515.resinprinter.stl.Triangle3d;
import org.area515.resinprinter.stl.XYComparatord;

public class ScanlineFillPolygonWork extends RecursiveTask<ScanlineFillPolygonWork> {
	private static final Logger logger = LogManager.getLogger();
	private static final long serialVersionUID = 217859858236513212L;
	public static final int SMALLEST_UNIT_OF_WORK = 20;
	private List<Line3d> potentialLinesInRange;
	private List<Line3d> scanLines = new ArrayList<Line3d>();
	private Set<Face3d> insideOutPolygons = new HashSet<Face3d>();
	private List<Triangle3d> watchedTriangles;
	private int buildArea;
	private int start;
	private int stop;
	private int z;
	private List<Integer> watchedYs;
	
	public ScanlineFillPolygonWork(List<Line3d> potentialLinesInRange, List<Triangle3d> watchedTriangles, List<Integer> watchedYs, int start, int stop, int z) {
		this.potentialLinesInRange = potentialLinesInRange;
		this.watchedTriangles = watchedTriangles;
		this.watchedYs = watchedYs;
		this.start = start;
		this.stop = stop;
		this.z = z;
	}
	
	public Set<Face3d> getInsideOutPolygons() {
		return insideOutPolygons;
	}
	
	public List<Line3d> getScanLines() {
		return scanLines;
	}
	
	public int getBuildArea() {
		return buildArea;
	}
	
	@Override
	protected ScanlineFillPolygonWork compute() {
			 boolean watch = false;
	         for (int y = start; y <= stop; y++) {
		    	 Set<Point3d> intersectedPoints = new TreeSet<Point3d>(new XYComparatord(Triangle3d.EQUAL_TOLERANCE));
	        	 for (Line3d currentLine : potentialLinesInRange) {
	        		 if (watchedTriangles != null) {
	        			 Face3d face = currentLine.getOriginatingFace();
	        			 if (watchedTriangles.contains(face)) {
	        				 logger.debug("Watch triangle:{}", face);
	        				 watch = true;
	        			 }
	        		 }
	        		 
	        		 if (y < currentLine.getMinY() || y > currentLine.getMaxY()) {
	        			 if (watch) {
	        				 logger.debug("Watch triangle eliminated through Y");
	        				 watch = false;
	        			 }
	        			 continue;
	        		 }
	        		 
	        		 double x = currentLine.getXIntersectionPoint(y);
	        		 if (x >= currentLine.getMinX() && x <= currentLine.getMaxX()) {//TODO: ceil/floor here?
	        			 Point3d intersection = new Point3d(x, y, z, currentLine.getNormal(), currentLine.getOriginatingFace());
	        			 intersectedPoints.add(intersection);
	        			 if (watch) {
		        		     logger.debug("Watch triangle intersections found:{}", intersection);
		        		     watch = false;
	        			 }
	        		 } else if (watch) {
	        		     logger.debug("Watch triangle eliminated through X");
	        			 watch = false;
	        		 }
	        	 }
	        	 
	        	 if (watchedYs != null && watchedYs.contains(y)) {
	        		 logger.debug("Watch y:{} intersection:{}", y, intersectedPoints);
	        	 }
	        	 
	        	 int drawingValue = 0;
	        	 Point3d firstPoint = null;
	        	 for (Point3d intersectedPoint : intersectedPoints) {
	        		 Point3d normal = intersectedPoint.getNormal();
	        		 if (normal.x > 0) {
	        			 drawingValue += 1;
	        		 } else if (normal.x < 0) {
	        			 drawingValue -= 1;
	        		 }
	        		 
	        		 if (firstPoint == null) {
	        			 if (drawingValue > 0) {
	        				 insideOutPolygons.add(intersectedPoint.getOriginatingShape());
	        			 } else if (drawingValue < 0) {
	        				 firstPoint = intersectedPoint;
	        			 }
	        		 } else {
	        			 if (drawingValue > 0) {
	        				 insideOutPolygons.add(intersectedPoint.getOriginatingShape());
	        			 } else if (drawingValue == 0) {
	        				 scanLines.add(new Line3d(firstPoint, intersectedPoint, null, null, false));
	        				 buildArea += intersectedPoint.x - firstPoint.x;
	        				 firstPoint = null;
	        			 }
	        		 }
	        	 }
	         }
	         
	         return this;
	}
}
