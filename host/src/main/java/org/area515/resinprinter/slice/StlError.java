package org.area515.resinprinter.slice;

import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Triangle3d;

public class StlError {
	private Triangle3d triangle;
	private Line3d nonManifoldEdge;
	private ErrorType type;
	
	public static enum ErrorType {
		NonManifold,
		Insideout
	}
	
	public StlError(Triangle3d triangle, Line3d nonManifoldEdge) {
		this.triangle = triangle;
		this.nonManifoldEdge = nonManifoldEdge;
		this.type = ErrorType.NonManifold;
	}
	
	public StlError(Triangle3d triangle, ErrorType type) {
		this.triangle = triangle;
		this.type = type;
	}

	public Triangle3d getTriangle() {
		return triangle;
	}
	public void setTriangle(Triangle3d triangle) {
		this.triangle = triangle;
	}

	public Line3d getNonManifoldEdge() {
		return nonManifoldEdge;
	}
	public void setNonManifoldEdge(Line3d nonManifoldEdge) {
		this.nonManifoldEdge = nonManifoldEdge;
	}

	public ErrorType getType() {
		return type;
	}
	public void setType(ErrorType type) {
		this.type = type;
	}
	
	public String toString() {
		return type + " triangle:" + triangle + (nonManifoldEdge != null?(" edge:" + nonManifoldEdge):"");
	}
}
