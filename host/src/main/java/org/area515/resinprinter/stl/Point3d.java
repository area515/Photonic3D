package org.area515.resinprinter.stl;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

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
	
	public Point3d(Vector3D point, Face3d originatingShape) {
		this.x = point.getX();
		this.y = point.getY();
		this.z = point.getZ();
		this.originatingShape = originatingShape;
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
		return "(x:" + x + ",y:" + y + ",z:" + z + (normal != null?("@x:" + normal.x + ",y:" + normal.y + ",z:" + normal.z):"") + ")";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((originatingShape == null) ? 0 : originatingShape.hashCode());
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
	@Deprecated
	public boolean ceilingEquals(Point3d otherPoint) {
	return Math.ceil(x) == Math.ceil(otherPoint.x) &&
			Math.ceil(y) == Math.ceil(otherPoint.y) &&
			Math.ceil(z) == Math.ceil(otherPoint.z);
	}

	//Does this point belong to a horizontal line?
	public boolean isInfiniteInverseSlopeOnIntegerBoundry() {
		return Double.isNaN(this.x) && Double.isNaN(this.y) && Double.isNaN(this.z);
	}
	
	public int pointCompare(Point3d other) {
		boolean thisInfiniteSlopeOnIntegerBoundry = isInfiniteInverseSlopeOnIntegerBoundry();
		boolean otherInfiniteSlopeOnIntegerBoundry =  other.isInfiniteInverseSlopeOnIntegerBoundry();
		if (thisInfiniteSlopeOnIntegerBoundry) {
			if (otherInfiniteSlopeOnIntegerBoundry) {
				return 0;
			}
			
			return 1;
		} else if (otherInfiniteSlopeOnIntegerBoundry){
			return -1;
		}
		
		double xdiff = this.x - other.x;
		double ydiff = this.y - other.y;
		double zdiff = this.z - other.z;
		
		if (xdiff > Triangle3d.EQUAL_TOLERANCE) {
			return -1;
		}
		if (xdiff < -Triangle3d.EQUAL_TOLERANCE) {
			return 1;
		}
		if (ydiff > Triangle3d.EQUAL_TOLERANCE) {
			return -1;
		}
		if (ydiff < -Triangle3d.EQUAL_TOLERANCE) {
			return 1;
		}
		if (zdiff > Triangle3d.EQUAL_TOLERANCE) {
			return -1;
		}
		if (zdiff < -Triangle3d.EQUAL_TOLERANCE) {
			return 1;
		}
		return 0;
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
		if (originatingShape == null) {
			if (other.originatingShape != null)
				return false;
		} else if (!originatingShape.equals(other.originatingShape))
			return false;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z) || 
			(other.z < z - Triangle3d.EQUAL_TOLERANCE && other.z > z + Triangle3d.EQUAL_TOLERANCE))
			return false;
		return true;
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
