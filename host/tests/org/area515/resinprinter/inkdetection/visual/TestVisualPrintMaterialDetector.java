package org.area515.resinprinter.inkdetection.visual;

import java.io.IOException;

import org.junit.Test;

public class TestVisualPrintMaterialDetector {
	@Test
	public void determineLiquidLevelInAToughSituation() throws IOException {
		VisualPrintMaterialDetector detector = new VisualPrintMaterialDetector();
		float liquidRemaining = detector.getPrintMaterialRemaining(TestVisualPrintMaterialDetector.class.getResourceAsStream("ToughSituation.png"));
		System.out.println(liquidRemaining);
	}
}
