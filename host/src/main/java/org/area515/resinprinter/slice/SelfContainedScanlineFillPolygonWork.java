package org.area515.resinprinter.slice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.RecursiveAction;

import org.area515.resinprinter.stl.Face3d;
import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Point3d;
import org.area515.resinprinter.stl.Triangle3d;
import org.area515.resinprinter.stl.XYComparatorNormalImportantd;

public class SelfContainedScanlineFillPolygonWork extends RecursiveAction {
	private static final long serialVersionUID = 217859858236513212L;
	private static final int SMALLEST_UNIT_OF_WORK = 2;
	private List<List<Line3d>> polygons;
	private List<Line3d> scanLines = new ArrayList<Line3d>();
	private Set<Face3d> insideOutPolygons = new HashSet<Face3d>();
	private int buildArea;
	private int start;
	private int stop;
	private double z;
	
	public SelfContainedScanlineFillPolygonWork(List<List<Line3d>> polygons, int start, int stop, double z) {
		this.polygons = polygons;
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
	protected void compute() {
	     if (stop - start < SMALLEST_UNIT_OF_WORK) {
	    	 ArrayList<Line3d> potentialLinesInRange = new ArrayList<Line3d>();
	         for (List<Line3d> currentPolygon : polygons) {
	        	 for (Line3d currentLine : currentPolygon) {
	        		 double minY = currentLine.getMinY();
	        		 double maxY = currentLine.getMaxY();
	        		 if (minY <= (double)stop &&
	        		     maxY >= (double)start) {
	        			 potentialLinesInRange.add(currentLine); 
	        		 }
	        	 }
	         }
	         
	     	 List<Line3d> tempScanLines = new ArrayList<Line3d>();
	    	 List<Face3d> tempInsideOutPolygons = new ArrayList<Face3d>();
	         for (int y = start; y <= stop; y++) {
		    	 Set<Point3d> intersectedPoints = new TreeSet<Point3d>(new XYComparatorNormalImportantd(Triangle3d.EQUAL_TOLERANCE));
	        	 for (Line3d currentLine : potentialLinesInRange) {
	        		 double x = currentLine.getXIntersectionPoint(y);
	        		 if (x >= currentLine.getMinX() && x <= currentLine.getMaxX()) {
	        			 intersectedPoints.add(new Point3d(x, y, z, currentLine.getNormal(), currentLine.getOriginatingFace()));
	        		 }
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
	        				 tempScanLines.add(new Line3d(firstPoint, intersectedPoint, null, null, false));
	        				 firstPoint = null;
	        			 }
	        		 }
	        	 }
	         }
	         
	         scanLines.addAll(tempScanLines);
	         insideOutPolygons.addAll(tempInsideOutPolygons);
	     } else {
	         int mid = (start + stop) >>> 1;
	         
	         SelfContainedScanlineFillPolygonWork firstWork = new SelfContainedScanlineFillPolygonWork(polygons, start, mid, z);
	         firstWork.fork();
	         SelfContainedScanlineFillPolygonWork secondWork = new SelfContainedScanlineFillPolygonWork(polygons, mid + 1, stop, z);
	         secondWork.fork();
	         firstWork.join();
	         scanLines.addAll(firstWork.getScanLines());
	         insideOutPolygons.addAll(firstWork.getInsideOutPolygons());
	         secondWork.join();
	         scanLines.addAll(secondWork.getScanLines());
	         insideOutPolygons.addAll(secondWork.getInsideOutPolygons());
	     }
	}
}
