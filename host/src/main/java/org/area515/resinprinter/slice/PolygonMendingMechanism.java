package org.area515.resinprinter.slice;

import java.util.List;

import org.area515.resinprinter.stl.Line3d;

public interface PolygonMendingMechanism {
	public void mendPolygon(ZSlicer slicer,  List<List<Line3d>> brokenLoops, List<List<Line3d>> completedFillInLoops);
}
