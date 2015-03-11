package org.area515.resinprinter.stl;

import java.util.Comparator;

public class XYOriginatingShapeComparatord implements Comparator<Shape3d> {
	
	@Override
	public int compare(Shape3d first, Shape3d second) {
		double value = first.getMinX() - second.getMinX();
		if (value > 0) {
			return 1;
		} 
		if (value < 0) {
			return -1;
		}
		
		value = first.getMinY() - second.getMinY();
		if (value < 0) {
			return -1;
		}
		if (value > 1) {
			return 1;
		}
		
		return first.hashCode() - second.hashCode();
	}
}
