package org.area515.resinprinter.inkdetection.visual;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.ws.rs.core.StreamingOutput;

import org.area515.resinprinter.inkdetection.PrintMaterialDetector;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.services.MediaService;

public class VisualPrintMaterialDetector implements PrintMaterialDetector {
	//TODO: There are a ton of ways to make this lighting fast on a raspberry pi 2 and much faster on a Raspberry pi.
	//		Consider optimizations from both from a multi-processor standpoint, image tiling standpoint and one or two general algorithmic optimizations
	//      This method is pretty much just conceptual for now, just to show something that works
	@Override
	public Float getPercentageOfPrintMaterialRemaining(Printer printer) {
		//Make sure to use takePicture() method to keep everything synchronized...
		StreamingOutput output = MediaService.INSTANCE.takePicture(printer.getName(), 100, 100);
		PipedInputStream inputStream = new PipedInputStream();
		PipedOutputStream pipedOutputStream;
		try {
			pipedOutputStream = new PipedOutputStream(inputStream);
			output.write(pipedOutputStream);
			return getPrintMaterialRemaining(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public float getPrintMaterialRemaining(InputStream inputStream) throws IOException {
		BufferedImage image = ImageIO.read(inputStream);
		CannyEdgeDetector8BitGray detector = new CannyEdgeDetector8BitGray();
		detector.setGaussianKernelRadius(1.5f);
		detector.setLowThreshold(1.0f);
		detector.setHighThreshold(1.1f);
		detector.setSourceImage(image);
		detector.process();
		
		BufferedImage edgesImage = detector.getEdgesImage();
		CircleDetector circleDector = new CircleDetector(8, 10, 50, 1);//TODO: these need to be based off of the image size
		GenericHoughDetection<Circle> houghCircleDetection = new GenericHoughDetection<Circle>(edgesImage, null, circleDector, 0.50f, 0, false);
		houghCircleDetection.houghTransform();
		List<Circle> circles = houghCircleDetection.getShapes();
		//System.out.println(circles);
		
		LineDetector lineDetector = new LineDetector(.005d);
		GenericHoughDetection<Line> houghLineDetection = new GenericHoughDetection<Line>(edgesImage, null, lineDetector, 0.06f, 0, false);
		houghLineDetection.houghTransform();
		List<Line> lines = houghLineDetection.getShapes();
		//System.out.println(lines);
		
		//This assumes the camera is oriented such that +y = direction that gravity pulls objects
		List<Float> percentages = new ArrayList<Float>();
		for (Circle currentCircle : circles) {
			for (Line currentLine : lines) {
				Line intersectedLine = currentCircle.intersection(currentLine);
				if (intersectedLine != null) {
					double distance = intersectedLine.getDistanceFromLineMidPointToPoint(currentCircle.getX(), currentCircle.getY());
					if (distance < 0) {
						percentages.add(.5f - (float)(distance / (double)currentCircle.getRadius()) * .5f);
					} else {
						percentages.add(.5f + (float)(distance / (double)currentCircle.getRadius()) * .5f);
					}
				}
			}
		}
		
		float total = 0;
		for (Float aFloat : percentages) {
			total += aFloat;
		}
		
		return total / percentages.size();
	}
}
