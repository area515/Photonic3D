package org.area515.resinprinter.job;

import org.area515.resinprinter.job.Customizer.AffineTransformSettings;
import org.junit.Assert;
import org.junit.Test;

public class TestCustomizer {
	@Test
	public void TestCacheId() {
		AffineTransformSettings settings = new AffineTransformSettings();
		settings.setXScale(2.0);
		Customizer stuff = new Customizer();
		stuff.setName("hello");
		stuff.setAffineTransformSettings(settings);
		String first = stuff.getCacheId();
		String second = stuff.getCacheId();
		stuff.setCacheId(null);
		String third = stuff.getCacheId();
		Assert.assertEquals(first, second);
		Assert.assertEquals(second, third);
		settings.setYScale(3.0);
		stuff.setCacheId(null);
		Assert.assertNotEquals(stuff.getCacheId(), first);
	}
}
