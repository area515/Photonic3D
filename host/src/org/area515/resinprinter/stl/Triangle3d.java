package org.area515.resinprinter.stl;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public class Triangle3d {
	private Point3d[] verticies;
	private Vector3d normal;
	private double zmin;
	private double zmax;
	private double[] xSlopes = new double[3];
	private double[] xIntercepts = new double[3];
	private double[] ySlopes = new double[3];
	private double[] yIntercepts = new double[3];
	
	public Triangle3d(Point3d[] points, Vector3d normal) {
		if (points.length != 3) {
			throw new IllegalArgumentException("A triangle must have exactly three verticies");
		}
		this.verticies = points;
		this.normal = normal;
		zmin = Math.min(points[0].z, Math.min(points[1].z, points[2].z));
		zmax = Math.max(points[0].z, Math.max(points[1].z, points[2].z));
		xSlopes[0] = (points[0].x - points[1].x) / (points[0].z - points[1].z);
		xSlopes[1] = (points[1].x - points[2].x) / (points[1].z - points[2].z);
		xSlopes[2] = (points[2].x - points[0].x) / (points[2].z - points[0].z);
		ySlopes[0] = (points[0].y - points[1].y) / (points[0].z - points[1].z);
		ySlopes[1] = (points[1].y - points[2].y) / (points[1].z - points[2].z);
		ySlopes[2] = (points[2].y - points[0].y) / (points[2].z - points[0].z);
		xIntercepts[0] = -(xSlopes[0] * points[0].z - points[0].x);
		xIntercepts[1] = -(xSlopes[1] * points[1].z - points[1].x);
		xIntercepts[2] = -(xSlopes[2] * points[2].z - points[2].x);
		yIntercepts[0] = -(ySlopes[0] * points[0].z - points[0].y);
		yIntercepts[1] = -(ySlopes[1] * points[1].z - points[1].y);
		yIntercepts[2] = -(ySlopes[2] * points[2].z - points[2].y);		
	}
	
	public boolean intersectsZ(double z) {
		return z >= zmin && z <= zmax;
	}
	
	public Point3d[] getZIntersection(double z) {
		int currentPoint = 0;
		Point3d line[] = new Point3d[2];
		for (int t = 0; t < 3; t++) {
			double x = xSlopes[t] * z + xIntercepts[t];
			double y = ySlopes[t] * z + yIntercepts[t];
			
			if ((x <= verticies[t].x && x >= verticies[t<2?t+1:0].x ||
				 x >= verticies[t].x && x <= verticies[t<2?t+1:0].x) &&
				(y <= verticies[t].y && y >= verticies[t<2?t+1:0].y ||
				y >= verticies[t].y && y <= verticies[t<2?t+1:0].y) &&
				currentPoint < 2) {
				line[currentPoint++] = new Point3d(x, y, z);
			}
		}
		
		return line;
	}
}
