package org.area515.resinprinter.supports;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.PolyhedronsSet;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.area515.resinprinter.slice.StlFile;
import org.area515.resinprinter.stl.Triangle3d;

public class ImpossiblePerformance {
	private static class PolyFile extends StlFile<PolyhedronsSet, Vector3D> {
		private List<Vector3D> vertices = new ArrayList<>();
		private List<int[]> facets = new ArrayList<>();
		private HashMap<Vector3D, Integer> vertexLookup = new HashMap<>();
		private int currentPoint = 0;
		private int[] currentIndexes = new int[3];
		
		@Override
		protected void buildTriangle(Vector3D point1, Vector3D point2, Vector3D point3, double[] normal) {
			facets.add(currentIndexes);
			currentPoint = 0;
			currentIndexes = new int[3];
		}

		@Override
		protected Vector3D buildPoint(double x, double y, double z) {
			
			Vector3D newPoint = new Vector3D(x, y, z);
			Integer index = vertexLookup.get(newPoint);
			if (index != null) {
				currentIndexes[currentPoint] = index;
			} else {
				currentIndexes[currentPoint] = vertices.size();
				vertexLookup.put(newPoint, currentIndexes[currentPoint]);
				vertices.add(newPoint);
			}
			currentPoint++;
			return newPoint;
		}
		
		@Override
		protected Collection<PolyhedronsSet> createSet() {
			return new ArrayList<PolyhedronsSet>();
		}

		@Override
		protected PolyhedronsSet getFirstTriangle() {
			return new PolyhedronsSet(vertices, facets, Triangle3d.EQUAL_TOLERANCE);
		}
	};
	
	public static void main(String[] args) throws Exception {
		PolyFile figure = new PolyFile();
		//figure.load(StlFile.class.getResourceAsStream("lenscap-36mm.stl"), true);
		figure.load(new FileInputStream("C:\\Users\\wgilster\\uploaddir\\Fat_Guy_Statue.stl"), true);
		//figure.load(StlFile.class.getResourceAsStream("Homebrew_Finds_Magnet_Mounting_Thingy.stl"), true);
		long start = System.currentTimeMillis();
		PolyhedronsSet polys = figure.getFirstTriangle();
		long second = System.currentTimeMillis();
		System.out.println(second - start);
		System.out.println(polys.getBarycenter());
		System.out.println(System.currentTimeMillis() - second);
	}
}
