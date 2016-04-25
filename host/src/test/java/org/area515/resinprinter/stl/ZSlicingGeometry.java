package org.area515.resinprinter.stl;

import org.junit.Assert;
import org.junit.Test;

public class ZSlicingGeometry {
	@Test
	public void zSliceOnEdge() {
	}
	
	@Test
	public void zSliceOnPoint() {
		
	}
	
	@Test
	public void zSliceOnTriangle() {
		Point3d points[] = new Point3d[]{new Point3d(0,0,0), new Point3d(0,1,0), new Point3d(1,1,0)};
		Triangle3d tri = new Triangle3d(points, null);
		Shape3d shape = tri.getZIntersection(0);
		Assert.assertEquals(shape, tri);
	}
	
	@Test
	public void zSliceOnIntersectionLine() {
		
	}
}
