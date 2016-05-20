package org.area515.resinprinter.stl;

import java.util.Comparator;

public class XYComparatord implements Comparator<Shape3d> {
	private double tolerance;
	
	public XYComparatord(double tolerance) {
		this.tolerance = tolerance;
	}
	
	private int compareTo(double first, double second) {
		double value = first - second;
		if (value > 0) {
			return 1;
		} else if (value < 1) {
			return -1;
		}
		
		return 0;
	}
	
	@Override
	public int compare(Shape3d first, Shape3d second) {
		if (first instanceof Line3d && ((Line3d) first).pointsEqual(second)) {
			return 0;
		}
		
		if (first instanceof Point3d && ((Point3d) first).pointEquals((Point3d)second)) {
			return 0;
		}
		
		if (first instanceof Line3d && second instanceof Line3d) {
			int comp = compareTo(((Line3d)first).getPointOne().x, ((Line3d)second).getPointOne().x);
			if (comp != 0) {
				return comp;
			}
			comp = compareTo(((Line3d)first).getPointTwo().x, ((Line3d)second).getPointTwo().x);
			if (comp != 0) {
				return comp;
			}
			
			comp = compareTo(((Line3d)first).getPointOne().y, ((Line3d)second).getPointOne().y);
			if (comp != 0) {
				return comp;
			}
			comp = compareTo(((Line3d)first).getPointTwo().y, ((Line3d)second).getPointTwo().y);
			if (comp != 0) {
				return comp;
			}
			
			comp = compareTo(((Line3d)first).getPointOne().z, ((Line3d)second).getPointOne().z);
			if (comp != 0) {
				return comp;
			}
			comp = compareTo(((Line3d)first).getPointTwo().z, ((Line3d)second).getPointTwo().z);
			if (comp != 0) {
				return comp;
			}
		}
		
		double value = first.getMinX() - second.getMinX();
		if (value > tolerance) {
			return 1;
		} 
		if (value < -tolerance) {
			return -1;
		}
		
		value = first.getMinY() - second.getMinY();
		if (value > tolerance) {
			return 1;
		}
		if (value < -tolerance) {
			return -1;
		}
		
		if (first.equals(second)) {
			return 0;
		}

		return first.hashCode() - second.hashCode();
	}
}
