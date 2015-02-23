package org.area515.resinprinter.slice;

import java.io.FileNotFoundException;
import java.util.List;

import org.area515.resinprinter.stl.Line3d;

public class SliceTester {
	public static void main(String[] args) throws FileNotFoundException {
		 int precisionScaler = 100000;//We need to scale the whole stl large enough to have enough precision before the decimal point
		 double pixelsPerMMX = 5;
		 double pixelsPerMMY = 5;
		 double imageOffsetX = 35 * pixelsPerMMX;
		 double imageOffsetY = 25 * pixelsPerMMY;
		 double sliceResolution = 0.1;
		 
		 final ZSlicer slicer = new ZSlicer(
				 "C:\\Users\\wgilster\\Documents\\Fat_Guy_Statue.stl",
				 //"C:\\Users\\wgilster\\Documents\\ArduinoMega.stl",
				 //"C:\\Users\\wgilster\\Documents\\Olaf_set3_whole.stl", 
				 precisionScaler, 
				 pixelsPerMMX, 
				 pixelsPerMMY, 
				 imageOffsetX, 
				 imageOffsetY, 
				 sliceResolution);
		 slicer.loadFile();
		 
		 for (int z = slicer.getZMin(); z < slicer.getZMax(); z++) {
			 slicer.setZ(z);
			 System.out.println("Testing Z:" + z);
			 slicer.colorizePolygons();
			 if (slicer.getBrokenLoops().size() > 0) {
				 for (List<Line3d> lines : slicer.getBrokenLoops()) {
					 for (Line3d line : lines) {
						 if (!line.getPointOne().ceilingEquals(line.getPointTwo())) {
							 System.out.println("Z: " + z + " Non point based broken loop:" + line);
						 }
					 }
				 }
			 }
		 }
	}
}
