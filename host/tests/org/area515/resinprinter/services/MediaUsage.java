package org.area515.resinprinter.services;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.imageio.ImageIO;
import javax.ws.rs.core.StreamingOutput;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.ByteStreams;

public class MediaUsage {
	@Test
	public void tookValidPicture() throws IOException {
		StreamingOutput output = MediaService.INSTANCE.takePicture("Unknown", 100, 100);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		output.write(outputStream);
		
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(outputStream.toByteArray()));
		Assert.assertNotNull(image);
		System.out.println("Picture taken successfully.");
	}
}
