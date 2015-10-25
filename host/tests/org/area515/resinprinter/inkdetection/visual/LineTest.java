package org.area515.resinprinter.inkdetection.visual;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Test;

public class LineTest {
	@Test
	public void generateHoughSpace() throws IOException {
		BufferedImage image = ImageIO.read(CircleTest.class.getResource("ToughSituation.png"));
		//BufferedImage image = ImageIO.read(new File("test64circle.jpg"));
		CannyEdgeDetector8BitGray detector = new CannyEdgeDetector8BitGray();
		detector.setGaussianKernelRadius(1.5f);
		detector.setLowThreshold(1.0f);
		detector.setHighThreshold(1.1f);
		detector.setSourceImage(image);
		detector.process();//*/
		
		BufferedImage edges = detector.getEdgesImage();
		LineDetector shapeDetector = new LineDetector(.005d);
		//TODO: I think that my threshold is off by a power of ten. I'll probably need to look at that someday...
		GenericHoughDetection<Line> houghDetection = new GenericHoughDetection<Line>(edges, null, shapeDetector, .06f, 0, false);
		houghDetection.houghTransform();
		List<Line> centers = houghDetection.getShapes();
		System.out.println(centers);
		Graphics g = edges.getGraphics();
		g.setColor(Color.WHITE);
		for (Line line : centers) {
			g.drawLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
		}//*/
		ImageIO.write(edges, "jpg", new File("outputline.png"));
		ImageIO.write(houghDetection.generateHoughSpaceImage(true), "png", new File("houghspaceline.png"));
		System.out.println("Complete");		
	}
}
