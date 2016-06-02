package org.area515.resinprinter.slice;

import java.util.List;

import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Point3d;

public class CloseOffMend implements PolygonMendingMechanism {
	@Override
	public void mendPolygon(ZSlicer slicer, List<List<Line3d>> brokenLoops, List<List<Line3d>> completedFillInLoops) {
		  for (List<Line3d> currentBrokenLoop : brokenLoops) {
			  if (currentBrokenLoop.size() > 1) {
				  Line3d line1 = currentBrokenLoop.get(0);
				  Line3d line2 = currentBrokenLoop.get(currentBrokenLoop.size() - 1);
				  Point3d point1 = line1.getPointOne();
				  Point3d point2 = line2.getPointTwo();
				  Point3d normal;
				  if (point1.x < point2.x) {
					  normal = new Point3d(point2.y - point1.y, point2.x - point1.x, line1.getNormal().z - line2.getNormal().z);
				  } else {
					  normal = new Point3d(point1.y - point2.y, point1.x - point2.x, line1.getNormal().z - line2.getNormal().z);
				  }
				  
				  Line3d line = new Line3d(point2, point1, normal, null, false);
				  currentBrokenLoop.add(line);
			  }
			  
			  slicer.placeIntoCompletedLoopList(currentBrokenLoop, completedFillInLoops);
		  }
	}
}
