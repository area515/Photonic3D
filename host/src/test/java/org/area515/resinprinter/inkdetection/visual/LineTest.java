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

public class LineTest {
    private static final Logger logger = LogManager.getLogger();

    private void produceImagesFromEdgeImage(BufferedImage input) throws IOException {
		VisualPrintMaterialDetector printMaterialDetector = new VisualPrintMaterialDetector();
		BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		GenericHoughDetection<Line> houghDetection = printMaterialDetector.buildLineDetection(input.getWidth(), input.getHeight());
		//houghDetection.addWatch(new HoughReference(new int[]{460,  6, 0}, null), Color.BLUE);
		houghDetection.houghTransform(input);
		List<Line> centers = houghDetection.getShapes();
		logger.info(centers);
		Graphics g = output.getGraphics();
		g.drawImage(input, 0, 0, null);
		g.setColor(Color.RED);
		for (Line line : centers) {
			g.drawLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
		}
		//BufferedImage mask = houghDetection.generateWatchOverlayInImageSpace(input.getWidth(), input.getHeight(), 0);
		//g.drawImage(mask, 0, 0, null);
		ImageIO.write(output, "png", new File("images/outputline.png"));
		ImageIO.write(houghDetection.generateHoughSpaceImage(true), "png", new File("images/houghspaceline.png"));
		logger.info("Complete");	
	}
	
	@Test
	public void generateHoughSpace() throws IOException {
		VisualPrintMaterialDetector printMaterialDetector = new VisualPrintMaterialDetector();
		BufferedImage input = ImageIO.read(CircleTest.class.getResource("ToughSituation.png"));
		CannyEdgeDetector8BitGray detector = printMaterialDetector.buildEdgeDetector(input);
		detector.process();
		
		produceImagesFromEdgeImage(detector.getEdgesImage());
	}
	
	@Test
	public void testLines() throws IOException {
		BufferedImage input = ImageIO.read(CircleTest.class.getResource("CircleLine10-14.png"));
		produceImagesFromEdgeImage(input);
	}
}
