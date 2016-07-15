package org.area515.resinprinter.slice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SliceTester {
	public static void main(String[] args) throws IOException {
		 double pixelsPerMMX = 5;
		 double pixelsPerMMY = 5;
		 //double imageOffsetX = 35 * pixelsPerMMX;
		 //double imageOffsetY = 25 * pixelsPerMMY;
		 double sliceResolution = 0.1;
		 
		 final ZSlicer slicer = new ZSlicer(
				 //"C:\\Users\\wgilster\\git\\Creation-Workshop-Host\\host\\src\\test\\resources\\org\\area515\\resinprinter\\slice\\CornerBracket_2.stl"
				 //"C:\\Users\\wgilster\\Documents\\ArduinoMega.stl"
				 //"C:\\Users\\wgilster\\Documents\\Olaf_set3_whole.stl"
				 //"C:\\Users\\wgilster\\Documents\\fdhgg.stl"
				 1, 
				 pixelsPerMMX, 
				 pixelsPerMMY, 
				 sliceResolution,
				 0,
				 true,
				 false,
				 new CloseOffMend());
		 slicer.loadFile(new FileInputStream(new File("C:\\Users\\wgilster\\git\\Creation-Workshop-Host\\host\\src\\test\\resources\\org\\area515\\resinprinter\\slice\\CornerBracket_2.stl")), null, null);
		 
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
