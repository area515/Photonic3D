package org.area515.resinprinter.slice;

import java.io.FileNotFoundException;

public class SliceTester {
	public static void main(String[] args) throws FileNotFoundException {
		 int precisionScaler = 100000;//We need to scale the whole stl large enough to have enough precision before the decimal point
		 double pixelsPerMMX = 5;
		 double pixelsPerMMY = 5;
		 //double imageOffsetX = 35 * pixelsPerMMX;
		 //double imageOffsetY = 25 * pixelsPerMMY;
		 double sliceResolution = 0.1;
		 
		 final ZSlicer slicer = new ZSlicer(
				 "C:\\Users\\wgilster\\Documents\\Fat_Guy_Statue.stl",
				 //"C:\\Users\\wgilster\\Documents\\ArduinoMega.stl",
				 //"C:\\Users\\wgilster\\Documents\\Olaf_set3_whole.stl", 
				 precisionScaler, 
				 pixelsPerMMX, 
				 pixelsPerMMY, 
				 sliceResolution,
				 true);
		 slicer.loadFile(null, null);
		 
		 for (int z = 780/*slicer.getZMin()*/; z < slicer.getZMax(); z++) {
			 slicer.setZ(z);
			 System.out.println("Testing Z:" + z);
			 slicer.colorizePolygons();
			 if (slicer.getStlErrors().size() > 0) {
				 for (StlError error : slicer.getStlErrors()) {
					 System.out.println(error);
				 }
			 }
		 }
	}
}
