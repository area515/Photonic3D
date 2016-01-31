package org.area515.resinprinter.inkdetection.visual;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class TestVisualPrintMaterialDetector {
    private static final Logger logger = LogManager.getLogger();

    @Test
	public void determineLiquidLevelInAToughSituation() throws IOException {
		logger.info("Determining liquid level in: ToughSituation.png.");
		
		long start = System.currentTimeMillis();
		VisualPrintMaterialDetector detector = new VisualPrintMaterialDetector();
		BufferedImage image = ImageIO.read(TestVisualPrintMaterialDetector.class.getResourceAsStream("ToughSituation.png"));
		float liquidRemaining = detector.getPrintMaterialRemainingFromPhoto(
				TestVisualPrintMaterialDetector.class.getResourceAsStream("ToughSituation.png"),
				detector.buildCircleDetection(image.getWidth(), image.getHeight()),
				detector.buildLineDetection(image.getWidth(), image.getHeight())) * 100;
		long timeTaken = System.currentTimeMillis() - start;
		
		logger.info(String.format("Time taken to perform visual inspection of remaining print resin: %1dms", timeTaken));
		logger.info(String.format("Remaining print resin: %1$.2f%%", liquidRemaining));
	}
	
	@Test
	public void testAgainstKnownPercentages() throws IOException {
		float tolerance = .09f;
		VisualPrintMaterialDetector detector = new VisualPrintMaterialDetector();

		HashMap<String, Float> knownFiles = new HashMap<>();
		knownFiles.put("CircleLine13-14.png", 13f/14f);
		knownFiles.put("CircleLine10-14.png", 10f/14f);
		knownFiles.put("CircleLine7-14.png", 7f/14f);
		knownFiles.put("CircleLine4-14.png", 4f/14f);
		knownFiles.put("CircleLine2-14.png", 2f/14f);
		knownFiles.put("CircleLineNull.png", Float.NaN);
		
		GenericHoughDetection<Circle> circleDetector = null;
		GenericHoughDetection<Line> lineDetector = null;
		Iterator<Map.Entry<String, Float>> pictures = knownFiles.entrySet().iterator();
		while (pictures.hasNext()) {
			Map.Entry<String, Float> entry = pictures.next();
			BufferedImage image = ImageIO.read(TestVisualPrintMaterialDetector.class.getResourceAsStream(entry.getKey()));
			int width = image.getWidth();
			int height = image.getHeight();
			
			if (circleDetector == null) {
				circleDetector = detector.buildCircleDetection(width, height);
			}
			if (lineDetector == null) {
				lineDetector = detector.buildLineDetection(width, height);
			}
			
			float percentage = detector.getPrintMaterialRemainingFromEdgeImage(image,circleDetector,lineDetector);
			if ((Float.isNaN(entry.getValue()) && !Float.isNaN(percentage))||(percentage - tolerance > entry.getValue() ||
				percentage + tolerance < entry.getValue())) {
				Assert.fail("Algorithm outcome:" + percentage + " for image:" + entry.getKey() + " not within tolerance:" + tolerance + " of predicted value:" + entry.getValue());
			} 
		}
	}
}
