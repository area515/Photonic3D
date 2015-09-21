package org.area515.resinprinter.test;

import org.area515.resinprinter.inkdetection.visual.TestVisualPrintMaterialDetector;
import org.area515.resinprinter.printer.DetectFirmware;
import org.area515.resinprinter.projector.DetectProjector;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	DetectProjector.class,
	DetectFirmware.class,
	TestVisualPrintMaterialDetector.class
})

public class FullTestSuite {
}