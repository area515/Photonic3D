package org.area515.resinprinter.inkdetection.visual;

import java.io.IOException;

public class TestVisualPrintMaterialDetector {
	public static void main(String[] args) throws IOException {
		VisualPrintMaterialDetector detector = new VisualPrintMaterialDetector();
		float liquidRemaining = detector.getPrintMaterialRemaining(TestVisualPrintMaterialDetector.class.getResourceAsStream("ToughSituation.png"));
		System.out.println(liquidRemaining);
	}
}
