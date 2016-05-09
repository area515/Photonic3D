package org.area515.resinprinter.stl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Triangle3d implements Shape3d, Face3d {
    private static final Logger logger = LogManager.getLogger();

    public static final double EQUAL_TOLERANCE = 1.0e-10;
	private Point3d normal;
	private Vector3D[] verticies = new Vector3D[3];
	private Point3d[] points;
	private Line[] lines = new Line[3];
	private double min[] = new double[3];
	private double max[] = new double[3];
	
	public Triangle3d(Point3d[] points, Point3d normal) {
		if (points.length != 3) {
			throw new IllegalArgumentException("A triangle must have exactly three verticies");
		}

		this.normal = normal;
		
		this.verticies[0] = new Vector3D(points[0].x, points[0].y, points[0].z);
		this.verticies[1] = new Vector3D(points[1].x, points[1].y, points[1].z);
		this.verticies[2] = new Vector3D(points[2].x, points[2].y, points[2].z);
		this.points = points;
		this.lines[0] = this.verticies[0].equals(this.verticies[1])?null:new Line(this.verticies[0], this.verticies[1], EQUAL_TOLERANCE);
		this.lines[1] = this.verticies[1].equals(this.verticies[2])?null:new Line(this.verticies[1], this.verticies[2], EQUAL_TOLERANCE);
		this.lines[2] = this.verticies[2].equals(this.verticies[0])?null:new Line(this.verticies[2], this.verticies[0], EQUAL_TOLERANCE);
		min[0] = Math.min(points[0].x, Math.min(points[1].x, points[2].x));
		max[0] = Math.max(points[0].x, Math.max(points[1].x, points[2].x));
		min[1] = Math.min(points[0].y, Math.min(points[1].y, points[2].y));
		max[1] = Math.max(points[0].y, Math.max(points[1].y, points[2].y));
		min[2] = Math.min(points[0].z, Math.min(points[1].z, points[2].z));
		max[2] = Math.max(points[0].z, Math.max(points[1].z, points[2].z));
	}
	
	public Point3d getNormal() {
		return normal;
	}

	public Point3d[] getBrokenEnds() {
		return points;
	}
	
	public int[] getX() {
		return new int[] {(int)points[0].x, (int)points[1].x, (int)points[2].x};
	}
	
	public int[] gety() {
		return new int[] {(int)points[0].y, (int)points[1].y, (int)points[2].y};
	}
	
	public List<Line3d> getLines() {
		List<Line3d> lines = new ArrayList<Line3d>();
		lines.add(new Line3d(points[0], points[1], null, this, false));//!Not the right normal!
		lines.add(new Line3d(points[1], points[2], null, this, false));//!Not the right normal!
		lines.add(new Line3d(points[2], points[0], null, this, false));//!Not the right normal!
		//stop the swap here!!
		return lines;
	}
	public List<Point3d> getPoints() {
		return Arrays.asList(points);
	}
	public double getMinZ() {
		return min[2];
	}	
	
	public double getMinY() {
		return min[1];
	}
	
	public double getMinX() {
		return min[0];
	}
	
	public double getMaxX() {
		return max[0];
	}
	public double getMaxY() {
		return max[1];
	}

	public boolean intersectsZ(double z) {
		return z >= min[2] && z <= max[2];
	}

	public Shape3d getZIntersection(double z) {
		Plane zPlane = new Plane(new Vector3D(0, 0, z), new Vector3D(0,0,1), EQUAL_TOLERANCE);
		//Plane zPlane = new Plane(new Vector3D(100, 3, z), new Vector3D(200,4, z), new Vector3D(30, 100, z), EQUAL_TOLERANCE);
		List<Point3d> intersectedPoints = new ArrayList<Point3d>();
		for (int t = 0; t < 3; t++) {
			if (lines[t] == null) {
				continue;
			}
			
			Vector3D point = zPlane.intersection(lines[t]);
			if (point != null) {
				double ix = point.getX();
				double iy = point.getY();
				double iz = point.getZ();
				
				if ((ix <= points[t].x && ix >= points[t<2?t+1:0].x ||
					 ix >= points[t].x && ix <= points[t<2?t+1:0].x) &&
					(iy <= points[t].y && iy >= points[t<2?t+1:0].y ||
					 iy >= points[t].y && iy <= points[t<2?t+1:0].y) &&
					(iz <= points[t].z && iz >= points[t<2?t+1:0].z ||
					 iz >= points[t].z && iz <= points[t<2?t+1:0].z)) {
					intersectedPoints.add(new Point3d(ix, iy, z, normal, this));
				}
			}
		}
		
		switch (intersectedPoints.size()) {
		case 3:
			return new Triangle3d(intersectedPoints.toArray(new Point3d[3]), normal);
		case 2:
			return new Line3d(intersectedPoints.get(0), intersectedPoints.get(1), normal, this, true);
		case 1:
			return intersectedPoints.get(0);
		}
		
		return null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((normal == null) ? 0 : normal.hashCode());
		result = prime * result + Arrays.hashCode(verticies);
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
		Triangle3d other = (Triangle3d) obj;
		if (normal == null) {
			if (other.normal != null)
				return false;
		} else if (!normal.equals(other.normal))
			return false;
		if (!Arrays.equals(verticies, other.verticies))
			return false;
		return true;
	}

	/*public boolean onZeroZ() {
		return verticies[0].z == 0 && verticies[1].z == 0 && verticies[2].z == 0;
	}*/
	
	public String toString() {
		return Arrays.toString(verticies) + "@" + normal;
	}
}
