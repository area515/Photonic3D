package org.area515.resinprinter.inkdetection.visual;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

public class TestVisualPrintMaterialDetector {
	@Test
	public void determineLiquidLevelInAToughSituation() throws IOException {
		System.out.println("Determining liquid level in: ToughSituation.png.");
		
		long start = System.currentTimeMillis();
		VisualPrintMaterialDetector detector = new VisualPrintMaterialDetector();
		float liquidRemaining = detector.getPrintMaterialRemaining(TestVisualPrintMaterialDetector.class.getResourceAsStream("ToughSituation.png")) * 100;
		long timeTaken = System.currentTimeMillis() - start;
		
		System.out.println(String.format("Time taken to perform visual inspection of remaining print resin: %1dms", timeTaken));
		System.out.println(String.format("Remaining print resin: %1$.2f%%", liquidRemaining));
	}
	
	//@Test
	public void testAgainstKnownPercentages() throws IOException {
		float tolerance = .09f;
		VisualPrintMaterialDetector detector = new VisualPrintMaterialDetector();

		HashMap<String, Float> knownFiles = new HashMap<>();
		knownFiles.put("CircleLine13-14.png", 13f/14f);
		knownFiles.put("CircleLine10-14.png", 10f/14f);
		knownFiles.put("CircleLine7-14.png", 7f/14f);
		knownFiles.put("CircleLine4-14.png", 4f/14f);
		knownFiles.put("CircleLine2-14.png", 2f/14f);
		knownFiles.put("CircleLineNull.png", -1f);
		
		Iterator<Map.Entry<String, Float>> pictures = knownFiles.entrySet().iterator();
		while (pictures.hasNext()) {
			
			Map.Entry<String, Float> entry = pictures.next();
			BufferedImage image = ImageIO.read(TestVisualPrintMaterialDetector.class.getResourceAsStream(entry.getKey()));
			float percentage = detector.getPrintMaterialRemaining(image);
			
			if (percentage - tolerance > entry.getValue() ||
				percentage + tolerance < entry.getValue()) {
				//Assert.fail("Algorithm outcome:" + percentage + " for image:" + entry.getKey() + " not within tolerance:" + tolerance + " of predicted value:" + entry.getValue());
			} 
		}
	}
}
