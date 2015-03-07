package org.area515.resinprinter.stl;

public class Point3d implements Shape3d {
	public double x;
	public double y;
	public double z;
	private Point3d normal;
	private Face3d originatingShape;
	
	public Point3d(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Point3d(double x, double y, double z, Point3d normal, Face3d originatingShape) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.normal = normal;
		this.originatingShape = originatingShape;
	}
	
	public Point3d getNormal() {
		return normal;
	}
	
	public Face3d getOriginatingShape() {
		return originatingShape;
	}
	
	@Override
	public String toString() {
		return "(" + x + "," + y + "," + z + ")";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		Point3d other = (Point3d) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
			return false;
		return true;
	}
	
	public boolean ceilingEquals(Point3d otherPoint) {
		return Math.ceil(x) == Math.ceil(otherPoint.x) &&
				Math.ceil(y) == Math.ceil(otherPoint.y) &&
				Math.ceil(z) == Math.ceil(otherPoint.z);
	}
	
	@Override
	public double getMinX() {
		return x;
	}
	@Override
	public double getMinY() {
		return y;
	}
	public double getMaxX() {
		return x;
	}
	public double getMaxY() {
		return y;
	}
}
