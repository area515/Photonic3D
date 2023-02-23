package org.area515.resinprinter.image;

import org.junit.Test;

import com.wgilster.dispmanx.window.NativeMemoryBackedBufferedImage;

public class NativeImageTest {
	@Test
	public void testNativeImage() {
		NativeMemoryBackedBufferedImage image = NativeMemoryBackedBufferedImage.newInstance(100, 100, true);
		image.getGraphics();
	}
}
