package org.area515.resinprinter.stl;

import java.util.Comparator;

public class XYComparatori implements Comparator<Shape3i> {
	
	@Override
	public int compare(Shape3i first, Shape3i second) {
		int value = first.getMinX() - second.getMinX();
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
