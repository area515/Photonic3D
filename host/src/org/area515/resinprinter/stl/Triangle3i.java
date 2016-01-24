package org.area515.resinprinter.stl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Triangle3i implements Shape3i {
    private static final Logger logger = LogManager.getLogger();

    private Point3i[] verticies;
	private Point3i normal;
	private int[] min = new int[3];
	private int[] max = new int[3];
	private double[] xSlopes = new double[3];
	private double[] xIntercepts = new double[3];
	private double[] ySlopes = new double[3];
	private double[] yIntercepts = new double[3];
	
	public Triangle3i(Point3i[] points, Point3i normal) {
		if (points.length != 3) {
			throw new IllegalArgumentException("A triangle must have exactly three verticies");
		}
		
		this.verticies = points;
		this.normal = normal;
		min[0] = Math.min(points[0].x, Math.min(points[1].x, points[2].x));
		max[0] = Math.max(points[0].x, Math.max(points[1].x, points[2].x));
		min[1] = Math.min(points[0].y, Math.min(points[1].y, points[2].y));
		max[1] = Math.max(points[0].y, Math.max(points[1].y, points[2].y));
		min[2] = Math.min(points[0].z, Math.min(points[1].z, points[2].z));
		max[2] = Math.max(points[0].z, Math.max(points[1].z, points[2].z));
		xSlopes[0] = (double)(points[0].x - points[1].x) / (double)(points[0].z - points[1].z);
		xSlopes[1] = (double)(points[1].x - points[2].x) / (double)(points[1].z - points[2].z);
		xSlopes[2] = (double)(points[2].x - points[0].x) / (double)(points[2].z - points[0].z);
		ySlopes[0] = (double)(points[0].y - points[1].y) / (double)(points[0].z - points[1].z);
		ySlopes[1] = (double)(points[1].y - points[2].y) / (double)(points[1].z - points[2].z);
		ySlopes[2] = (double)(points[2].y - points[0].y) / (double)(points[2].z - points[0].z);
		xIntercepts[0] = -(xSlopes[0] * points[0].z - points[0].x);
		xIntercepts[1] = -(xSlopes[1] * points[1].z - points[1].x);
		xIntercepts[2] = -(xSlopes[2] * points[2].z - points[2].x);
		yIntercepts[0] = -(ySlopes[0] * points[0].z - points[0].y);
		yIntercepts[1] = -(ySlopes[1] * points[1].z - points[1].y);
		yIntercepts[2] = -(ySlopes[2] * points[2].z - points[2].y);
	}
	
	public int[] getX() {
		return new int[] {verticies[0].x, verticies[1].x, verticies[2].x};
	}
	
	public int[] gety() {
		return new int[] {verticies[0].y, verticies[1].y, verticies[2].y};
	}
	
	public List<Line3i> getLines() {
		Set<Line3i> lines = new TreeSet<Line3i>(new XYComparatori());
		lines.add(new Line3i(verticies[0], verticies[1], normal));//!Not the right normal!
		lines.add(new Line3i(verticies[1], verticies[2], normal));//!Not the right normal!
		lines.add(new Line3i(verticies[2], verticies[0], normal));//!Not the right normal!
		return new ArrayList<Line3i>(lines);
	}
	
	public double getMinZ() {
		return min[2];
	}	
	
	public int getMinY() {
		return min[1];
	}
	
	public int getMinX() {
		return min[0];
	}
	
	public boolean intersectsZ(int z) {
		return z >= min[2] && z <= max[2];
	}
	
	public Shape3i getZIntersection(int z) {
		int currentPoint = 0;
		Point3i line[] = new Point3i[3];
		for (int t = 0; t < 3; t++) {
			if (Double.isInfinite(xSlopes[t]) || Double.isNaN(xSlopes[t])) {
				if (z != verticies[t].z) {
					logger.warn("Could this siutation happen and be a proper intersection?");
				} else {
					line[currentPoint++] = new Point3i(verticies[t].x, verticies[t].y, verticies[t].z);
				}
				continue;
			}
			
			int x = (int)((xSlopes[t] * z) + xIntercepts[t]);
			int y = (int)((ySlopes[t] * z) + yIntercepts[t]);
			
			if ((x <= verticies[t].x && x >= verticies[t<2?t+1:0].x ||
				 x >= verticies[t].x && x <= verticies[t<2?t+1:0].x) &&
				(y <= verticies[t].y && y >= verticies[t<2?t+1:0].y ||
				 y >= verticies[t].y && y <= verticies[t<2?t+1:0].y)) {
					line[currentPoint++] = new Point3i(x, y, z);
			}
		}
		
		//They didn't intersect...
		if (line[0] == null) {
			return null;
		}
		
		if (line[1] == null) {
			return new Point3i(line[0].x, line[0].y, line[0].z);
		}
		
		if (line[2] == null) {
			return new Line3i(line[0], line[1], normal);
		}

		return new Triangle3i(line, normal);
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
		Triangle3i other = (Triangle3i) obj;
		if (normal == null) {
			if (other.normal != null)
				return false;
		} else if (!normal.equals(other.normal))
			return false;
		if (!Arrays.equals(verticies, other.verticies))
			return false;
		return true;
	}

	public String toString() {
		return Arrays.toString(verticies) + "@" + normal;
	}
}
