package org.area515.resinprinter.test;

import org.area515.resinprinter.inkdetection.visual.CircleTest;
import org.area515.resinprinter.inkdetection.visual.LineTest;
import org.area515.resinprinter.inkdetection.visual.TestVisualPrintMaterialDetector;
import org.area515.resinprinter.network.LinuxNetworkManagerTest;
import org.area515.util.IOUtilitiesTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	IOUtilitiesTest.class,
	TestVisualPrintMaterialDetector.class,
	CircleTest.class,
	LineTest.class,
	LinuxNetworkManagerTest.class
})

public class FullTestSuite {
}