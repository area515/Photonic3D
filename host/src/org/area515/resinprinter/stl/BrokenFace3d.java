package org.area515.resinprinter.stl;

import java.util.List;

public class BrokenFace3d implements Face3d {
	private List<Line3d> lines;
	private Point3d[] brokenEnds;
	
	public BrokenFace3d(List<Line3d> lines) {
		this.lines = lines;
		brokenEnds = new Point3d[]{lines.get(0).getPointOne(), lines.get(lines.size() - 1).getPointTwo()};
	}
	
	public Point3d[] getBrokenEnds() {
		return brokenEnds;
	}
	
	public List<Line3d> getLines() {
		return lines;
	}
	
	public Point3d getNormal() {
		throw new UnsupportedOperationException("It's unnecessary to implement this");
	}
}
