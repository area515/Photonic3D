package org.area515.resinprinter.stl;

import java.util.Comparator;

public class XYComparatorNormalImportantd implements Comparator<Shape3d> {
	private double tolerance;
	
	public XYComparatorNormalImportantd(double tolerance) {
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
		
		if (first instanceof Point3d && 
			second instanceof Point3d && 
			((Point3d) first).pointCompare((Point3d)second) == 0) {
			Point3d normal1 = ((Point3d) first).getNormal();
			Point3d normal2 = ((Point3d)second).getNormal();
			if (normal1 == null) {
				if (normal2 == null) {
					return 0;
				} else {
					return -1;
				}
			} else if (normal2 != null) {
				return normal1.pointCompare(normal2);
			} else {
				return 1;//first.hashCode() - second.hashCode();
			}
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
			
			return compare(((Line3d)first).getNormal(), ((Line3d)second).getNormal());
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
