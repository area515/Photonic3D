package org.area515.resinprinter.inkdetection.visual;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class CircleTest {
    private static final Logger logger = LogManager.getLogger();

    @Test
	public void generateHoughSpace() throws IOException {
		VisualPrintMaterialDetector printMaterialDetector = new VisualPrintMaterialDetector();
		
		BufferedImage image = ImageIO.read(CircleTest.class.getResource("ToughSituation.png"));
		CannyEdgeDetector8BitGray detector = printMaterialDetector.buildEdgeDetector(image);
		detector.process();
		
		BufferedImage edges = detector.getEdgesImage();
		GenericHoughDetection<Circle> houghDetection = printMaterialDetector.buildCircleDetection(edges.getWidth(), edges.getHeight());
		houghDetection.houghTransform(edges);
		List<Circle> centers = houghDetection.getShapes();
		//detection.printHoughSpace();
		logger.info(centers);
		Graphics g = edges.getGraphics();
		g.setColor(Color.WHITE);
		for (Circle circle : centers) {
			g.drawOval(circle.getX() - circle.getRadius(), circle.getY() - circle.getRadius(), circle.getRadius() * 2, circle.getRadius() * 2);
		}//*/
		
		ImageIO.write(edges, "png", new File("images/outputcircle.png"));
		ImageIO.write(houghDetection.generateHoughSpaceImage(true), "png", new File("images/houghspacecircle.png"));
		logger.info("Complete");		
	}
}
