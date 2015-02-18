package org.area515.resinprinter.slice;

import java.io.FileNotFoundException;

public class SliceTester {
	public static void main(String[] args) throws FileNotFoundException {
		 int precisionScaler = 100000;//We need to scale the whole stl large enough to have enough precision before the decimal point
		 double pixelsPerMMX = 10;
		 double pixelsPerMMY = 10;
		 double imageOffsetX = 35 * pixelsPerMMX;
		 double imageOffsetY = 25 * pixelsPerMMY;
		 double sliceResolution = 0.1;
		 
		 final ZSlicer slicer = new ZSlicer(
				 //"C:\\Users\\wgilster\\Documents\\ArduinoMega.stl
				 "C:\\Users\\wgilster\\Documents\\Olaf_set3_whole.stl", 
				 precisionScaler, 
				 pixelsPerMMX, 
				 pixelsPerMMY, 
				 imageOffsetX, 
				 imageOffsetY, 
				 sliceResolution);
		 slicer.loadFile();
		 
		 for (int z = slicer.getZMin(); z < slicer.getZMax(); z++) {
			 slicer.setZ(z);
			 slicer.colorizePolygons();
			 if (slicer.getBrokenLoops().size() > 0) {
				 System.out.println("ERROR:" + z);
			 }
		 }
	}
}
