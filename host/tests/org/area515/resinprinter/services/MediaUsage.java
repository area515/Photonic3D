package org.area515.resinprinter.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.imageio.ImageIO;
import javax.ws.rs.core.StreamingOutput;

import org.junit.Assert;
import org.junit.Test;

public class MediaUsage {
	@Test
	public void tookValidPicture() throws IOException {
		StreamingOutput output = MediaService.INSTANCE.takePicture("Unknown", 100, 100);
		PipedInputStream inputStream = new PipedInputStream();
		PipedOutputStream pipedOutputStream;
		pipedOutputStream = new PipedOutputStream(inputStream);
		output.write(pipedOutputStream);
		BufferedImage image = ImageIO.read(inputStream);
		Assert.assertNotNull(image);
		System.out.println("Picture taken successfully.");
	}
}
