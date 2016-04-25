package org.area515.resinprinter.stl;

public class Line3i implements Shape3i {
	private Point3i one;
	private Point3i two;
	private Point3i normal;
	
	public Line3i(Point3i one, Point3i two, Point3i normal) {
		if (one.x < two.x) {
			this.one = one;
			this.two = two;
		} else {
			this.one = two;
			this.two = one;
		}

		this.normal = normal;
	}
	
	public int getMinX() {
		return one.x;
	}
	public int getMinY() {
		return Math.min(one.y, two.y);
	}
	
	public Point3i getPointOne() {
		return one;
	}
	
	public Point3i getPointTwo() {
		return two;
	}

	public void swap() {
		Point3i swap = one;
		this.one = two;
		this.two = swap;
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
		Line3i other = (Line3i) obj;
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
