package org.area515.resinprinter.stl;

import java.util.ArrayList;
import java.util.List;

public class MultiTriangleFace implements Face3d {
	private List<Triangle3d> faces = new ArrayList<>();
	
	public MultiTriangleFace(Face3d face1, Face3d face2) {
		if (face1 instanceof MultiTriangleFace) {
			faces.addAll(((MultiTriangleFace)face1).faces);
		} else if (face1 instanceof Triangle3d){
			faces.add((Triangle3d)face1);
		}
		if (face2 instanceof MultiTriangleFace) {
			faces.addAll(((MultiTriangleFace)face2).faces);
		} else if (face2 instanceof Triangle3d){
			faces.add((Triangle3d)face2);
		}
	}
	
	@Override
	public Point3d[] getBrokenEnds() {
		return null;
	}

	@Override
	public Point3d getNormal() {
		return faces.get(0).getNormal();
	}
	
	public List<Triangle3d> getFaces() {
		return faces;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((faces == null) ? 0 : faces.hashCode());
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
		MultiTriangleFace other = (MultiTriangleFace) obj;
		if (faces == null) {
			if (other.faces != null)
				return false;
		} else if (!faces.equals(other.faces))
			return false;
		return true;
	}
}
