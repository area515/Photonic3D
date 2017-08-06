package org.area515.resinprinter.slice;

import org.area515.resinprinter.stl.Face3d;
import org.area515.resinprinter.stl.Line3d;

public class StlError {
	private Face3d face;
	private Line3d nonManifoldEdge;
	private ErrorType type;
	
	public static enum ErrorType {
		NonManifold,
		Insideout
	}
	
	public StlError(Face3d face, Line3d nonManifoldEdge) {
		this.face = face;
		this.nonManifoldEdge = nonManifoldEdge;
		this.type = ErrorType.NonManifold;
	}
	
	public StlError(Face3d face, ErrorType type) {
		this.face = face;
		this.type = type;
	}

	public Face3d getFace() {
		return face;
	}
	public void setFace(Face3d triangle) {
		this.face = triangle;
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
		return type + " triangle:" + face + (nonManifoldEdge != null?(" edge:" + nonManifoldEdge):"");
	}
}
