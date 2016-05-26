package org.area515.resinprinter.slice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SliceTester {
	public static void main(String[] args) throws IOException {
		 int precisionScaler = 100000;//We need to scale the whole stl large enough to have enough precision before the decimal point
		 double pixelsPerMMX = 5;
		 double pixelsPerMMY = 5;
		 //double imageOffsetX = 35 * pixelsPerMMX;
		 //double imageOffsetY = 25 * pixelsPerMMY;
		 double sliceResolution = 0.1;
		 
		 final ZSlicer slicer = new ZSlicer(
				 //"C:\\Users\\wgilster\\Documents\\ArduinoMega.stl",
				 //"C:\\Users\\wgilster\\Documents\\Olaf_set3_whole.stl", 
				 precisionScaler, 
				 pixelsPerMMX, 
				 pixelsPerMMY, 
				 sliceResolution,
				 sliceResolution / 2,
				 false,
				 true);
		 slicer.loadFile(new FileInputStream(new File("C:\\Users\\wgilster\\Documents\\Fat_Guy_Statue.stl")), null, null);
		 
		 //Using 780
		 for (int z = slicer.getZMinIndex(); z < slicer.getZMaxIndex(); z++) {
			 slicer.setZIndex(z);
			 System.out.println("Testing Z:" + z);
			 slicer.colorizePolygons(null, null);
			 if (slicer.getStlErrors().size() > 0) {
				 for (StlError error : slicer.getStlErrors()) {
					 System.out.println(error);
				 }
			 }
		 }
	}
}
