package org.area515.resinprinter.stl;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.area515.resinprinter.slice.CheckSlicePoints;
import org.area515.resinprinter.slice.FillFile;
import org.area515.resinprinter.slice.FillPoint;
import org.area515.resinprinter.slice.SlicePointUtils;
import org.area515.resinprinter.slice.ZSlicer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ZSlicingGeometry {
	private ZSlicer slicer;
	private List<FillPoint> checkPoints;
	private BufferedImage image;
	private int x = 1024;
	private int y = 500;
	
	public ZSlicingGeometry(FillFile fillFile) throws IOException {
		slicer = new ZSlicer(
				fillFile.getStlScale(), 
				fillFile.getPixelsPerMMX(), 
				fillFile.getPixelsPerMMY(), 
				fillFile.getzSliceResolution(),
				fillFile.getzSliceOffset(),
				true, true);
		checkPoints = fillFile.getPoints();
		slicer.loadFile(CheckSlicePoints.class.getResourceAsStream(fillFile.getFileName()), (double)x, (double)y);
		image = new BufferedImage(x, y, BufferedImage.TYPE_INT_ARGB);
	}
	
	@Test
	public void testSpecialPoints() {
		List<FillPoint> brokenPoints = new ArrayList<FillPoint>();
		int currentSliceNumber = Integer.MIN_VALUE;
		Graphics2D g = (Graphics2D)image.createGraphics();
		for (FillPoint point : checkPoints) {
			if (point.getSliceNumber() == null) {
				continue;
			}
			
			if (currentSliceNumber != point.getSliceNumber()) {
				g.drawRect(0, 0, x, y);
				slicer.setZIndex(point.getSliceNumber());
				slicer.colorizePolygons(null, null);
				slicer.paintSlice(g);
			}
			
			int[] data = image.getRaster().getPixel(point.getX(), point.getY(), (int[])null);
			if (data[0] == 0 && data[1] == 0 && data[2] == 0) {
				brokenPoints.add(point);
			}
		}
		
		if (brokenPoints.size() > 0) {
			Assert.fail("These points are broken:" + brokenPoints);
		}
	}
	
	@Test
	public void testPointsOnAllSlices() {
		List<FillPoint> allPoints = new ArrayList<FillPoint>();
		for (FillPoint point : checkPoints) {
			if (point.getSliceNumber() == null) {
				allPoints.add(point);
			}
		}
		
		if (allPoints.size() == 0) {
			return;
		}
		
		List<FillPoint> brokenPoints = new ArrayList<FillPoint>();
		Graphics2D g = (Graphics2D)image.createGraphics();
		for (int z = slicer.getZMinIndex(); z <= slicer.getZMaxIndex(); z++) {
			slicer.setZIndex(z);
			slicer.colorizePolygons(null, null);
			slicer.paintSlice(g);
			g.drawRect(0, 0, x, y);
			
			for (FillPoint point : allPoints) {
				int[] data = image.getRaster().getPixel(point.getX(), point.getY(), (int[])null);
				if (data[0] == 0 && data[1] == 0 && data[2] == 0) {
					FillPoint currentPoint = new FillPoint();
					currentPoint.setX(point.getX());
					currentPoint.setY(point.getY());
					currentPoint.setSliceNumber(z);
					brokenPoints.add(currentPoint);
				}
			}
		}
		
		if (brokenPoints.size() > 0) {
			Assert.fail("These points are broken:" + brokenPoints);
		}
	}

	@Parameters
	public static Object[] data() throws IOException {
		Map<FillFile, FillFile> points = SlicePointUtils.loadPoints();
		return points.values().toArray();
	}
}
