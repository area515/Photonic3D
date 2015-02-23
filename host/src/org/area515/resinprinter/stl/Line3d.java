package org.area515.resinprinter.stl;

import java.awt.Color;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

public class Line3d implements Shape3d {
	private Point3d one;
	private Point3d two;
	private Point3d normal;
	private Face3d originatingFace;//This is usually a Triangle3d
	
	public Line3d(Point3d one, Point3d two, Point3d normal, Face3d originatingFace, boolean swapIfNecessary) {
		if (!swapIfNecessary || one.x < two.x) {
			this.one = one;
			this.two = two;
		} else {
			this.one = two;
			this.two = one;
		}

		this.originatingFace = originatingFace;
		this.normal = normal;
	}
	
	public double getMinX() {
		return one.x;
	}
	public double getMinY() {
		return Math.min(one.y, two.y);
	}
	
	public Point3d getPointOne() {
		return one;
	}
	
	public Point3d getPointTwo() {
		return two;
	}

	public void swap() {
		Point3d swap = one;
		this.one = two;
		this.two = swap;
	}
	
	public Face3d getOriginatingFace() {
		return originatingFace;
	}

	@Override
	public String toString() {
		return "[" + one + "," + two + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((one == null) ? 0 : one.hashCode());
		result = prime * result + ((two == null) ? 0 : two.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Line3d other = (Line3d) obj;
		if (one == null) {
			if (other.one != null)
				return false;
		} else if (!one.equals(other.one))
			return false;
		if (two == null) {
			if (other.two != null)
				return false;
		} else if (!two.equals(other.two))
			return false;
		return true;
	}
}
