package org.area515.resinprinter.inkdetection.visual;

import java.io.IOException;

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
}
