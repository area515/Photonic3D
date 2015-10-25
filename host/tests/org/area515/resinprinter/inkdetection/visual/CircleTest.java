package org.area515.resinprinter.inkdetection.visual;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Test;

public class CircleTest {
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
		CircleDetector shapeDetector = new CircleDetector(8, 10, 50, 1);
		GenericHoughDetection<Circle> houghDetection = new GenericHoughDetection<Circle>(edges, null, shapeDetector, 0.50f, 0, false);
		houghDetection.houghTransform();
		List<Circle> centers = houghDetection.getShapes();
		//detection.printHoughSpace();
		System.out.println(centers);
		Graphics g = edges.getGraphics();
		g.setColor(Color.WHITE);
		for (Circle circle : centers) {
			g.drawOval(circle.getX() - circle.getRadius(), circle.getY() - circle.getRadius(), circle.getRadius() * 2, circle.getRadius() * 2);
		}//*/
		
		ImageIO.write(edges, "png", new File("outputcircle.png"));
		ImageIO.write(houghDetection.generateHoughSpaceImage(true), "png", new File("houghspacecircle.png"));
		System.out.println("Complete");		
	}
}
