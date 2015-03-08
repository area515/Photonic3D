package org.area515.resinprinter.slice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.RecursiveTask;

import org.area515.resinprinter.stl.Face3d;
import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Point3d;
import org.area515.resinprinter.stl.XYComparatord;

public class ScanlineFillPolygonWork extends RecursiveTask<ScanlineFillPolygonWork> {
	private static final long serialVersionUID = 217859858236513212L;
	public static final int SMALLEST_UNIT_OF_WORK = 20;
	private List<Line3d> potentialLinesInRange;
	private List<Line3d> scanLines = new ArrayList<Line3d>();
	private Set<Face3d> insideOutPolygons = new HashSet<Face3d>();
	private int buildArea;
	private int start;
	private int stop;
	private double z;
	
	public ScanlineFillPolygonWork(List<Line3d> potentialLinesInRange, int start, int stop, double z) {
		this.potentialLinesInRange = potentialLinesInRange;
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
	         for (int y = start; y <= stop; y++) {
		    	 Set<Point3d> intersectedPoints = new TreeSet<Point3d>(new XYComparatord());
	        	 for (Line3d currentLine : potentialLinesInRange) {
	        		 if (y < currentLine.getMinY() || y > currentLine.getMaxY()) {
	        			 continue;
	        		 }
	        		 double x = currentLine.getXIntersectionPoint(y);
	        		 if (x >= currentLine.getMinX() && x <= currentLine.getMaxX()) {//TODO: ceil/floor here?
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
