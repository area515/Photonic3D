package org.area515.resinprinter.slice;

import org.area515.resinprinter.stl.Line3d;
import org.area515.resinprinter.stl.Point3d;

public class LoopLink {
	 private boolean used;
	 private Point3d comparePoint;
	 private Line3d line;
	 
	 public LoopLink(Line3d line, boolean useFirst) {
		 this.line = line;
		 if (useFirst) {
			 comparePoint = line.getPointOne();
		 } else {
			 comparePoint = line.getPointTwo();
		 }
	 }
	 
 }