package org.area515.resinprinter.inkdetection.visual;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.inkdetection.PrintMaterialDetector;
import org.area515.resinprinter.inkdetection.PrintMaterialDetectorSettings;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.services.MediaService;

public class VisualPrintMaterialDetector implements PrintMaterialDetector {
	private static final Logger logger = LogManager.getLogger();
	private static final HashMap<Printer, ShapeDetectionCache> buildPictures = new HashMap<>();
	private static int WIDTH = 100;
	private static int HEIGHT = 100;
	
	public static class ShapeDetectionCache {
		private StreamingOutput output;
		private GenericHoughDetection<Circle> circleDetection;
		private GenericHoughDetection<Line> lineDetection;
		
		public void setStreamingOutput(StreamingOutput output) {
			this.output = output;
		}
		public StreamingOutput getStreamingOutput() {
			return output;
		}
		
		public GenericHoughDetection<Circle> getCircleDetection() {
			return circleDetection;
		}
		public void setCircleDetection(GenericHoughDetection<Circle> circleDetection) {
			this.circleDetection = circleDetection;
		}
		
		public GenericHoughDetection<Line> getLineDetection() {
			return lineDetection;
		}
		public void setLineDetection(GenericHoughDetection<Line> lineDetection) {
			this.lineDetection = lineDetection;
		}
	}
	
	@Override
	public void startMeasurement(Printer printer) {
		ShapeDetectionCache cache = buildPictures.get(printer);
		if (cache == null) {
			cache = new ShapeDetectionCache();
			buildPictures.put(printer, cache);
		}
		
		//Make sure to use takePicture() method to keep everything synchronized...
		cache.setStreamingOutput(MediaService.INSTANCE.takePicture(printer.getName(), WIDTH, HEIGHT));
	}

	//TODO: There are a ton of ways to make this lighting fast on a raspberry pi 2 and much faster on a Raspberry pi.
	//		Consider optimizations from both from a multi-processor standpoint, image tiling(ROI) standpoint and one or two general algorithmic optimizations
	//      This method is pretty much just conceptual for now, just to show something that works
	@Override
	public float getPercentageOfPrintMaterialRemaining(Printer printer) throws IOException {
		final ShapeDetectionCache cache = buildPictures.get(printer);
		PipedInputStream inputStream = new PipedInputStream();
		final PipedOutputStream pipedOutputStream = new PipedOutputStream(inputStream);
		Main.GLOBAL_EXECUTOR.submit(new Runnable() {
			@Override
			public void run() {
				try {
					cache.getStreamingOutput().write(pipedOutputStream);
				} catch (WebApplicationException | IOException e) {
					logger.error("Couldn't log stream", e);
				}
			}
		});

		if (cache.getCircleDetection() == null) {
			cache.setCircleDetection(buildCircleDetection(WIDTH, HEIGHT));
		}
		
		if (cache.getLineDetection() == null) {
			cache.setLineDetection(buildLineDetection(WIDTH, HEIGHT));
		}
		
		return getPrintMaterialRemainingFromPhoto(inputStream, cache.getCircleDetection(), cache.getLineDetection());
	}
	
	GenericHoughDetection<Circle> buildCircleDetection(int width, int height) {
		float imageSize = (float)Math.sqrt(width * height);//8, 5, 50, 1
		CircleDetector circleDector = new CircleDetector(.4f * imageSize, (int)(.15 * imageSize), (int)(.5 * imageSize), 1);
		GenericHoughDetection<Circle> houghCircleDetection = new GenericHoughDetection<Circle>(new Rectangle(0,  0, width, height), circleDector, 0.55f, 0, false);
		return houghCircleDetection;
	}
	
	GenericHoughDetection<Line> buildLineDetection(int width, int height) {
		LineDetector lineDetector = new LineDetector(.01d);                                                                                
		GenericHoughDetection<Line> houghLineDetection = new GenericHoughDetection<Line>(new Rectangle(0,  0, width, height), lineDetector, 0.29f, 0, false);
		return houghLineDetection;
	}
	
	float getPrintMaterialRemainingFromEdgeImage(
			BufferedImage edgesImage, 
			GenericHoughDetection<Circle> houghCircleDetection, 
			GenericHoughDetection<Line> houghLineDetection) throws IOException {
		houghCircleDetection.houghTransform(edgesImage);
		List<Circle> circles = houghCircleDetection.getShapes();
		//logger.info(circles);
		
		houghLineDetection.houghTransform(edgesImage);
		List<Line> lines = houghLineDetection.getShapes();
		//logger.info(lines);
		
		//Remove vertical lines
		Iterator<Line> lineIter = lines.iterator();
		while (lineIter.hasNext()) {
			Line line = lineIter.next();
			double slope = Math.abs(line.getSlope());
			if (Double.isNaN(slope) || slope > 1) {
				lineIter.remove();
			}
		}
		
		//This assumes the camera is oriented such that +y = direction that gravity pulls objects
		List<Float> percentages = new ArrayList<Float>();
		for (Circle currentCircle : circles) {
			for (Line currentLine : lines) {
				Line intersectedLine = currentCircle.intersection(currentLine);
				if (intersectedLine != null) {
					double distance = intersectedLine.getDistanceFromLineMidPointToPoint(currentCircle.getX(), currentCircle.getY());
					percentages.add(.5f + (float)(distance / (double)currentCircle.getRadius()) * .5f);
				}
			}
		}
		
		float total = 0;
		for (Float aFloat : percentages) {
			total += aFloat;
		}
		
		return total / percentages.size();
	}
	
	CannyEdgeDetector8BitGray buildEdgeDetector(BufferedImage image) {
		CannyEdgeDetector8BitGray detector = new CannyEdgeDetector8BitGray();
		detector.setGaussianKernelRadius(1.5f);
		detector.setLowThreshold(1.0f);
		detector.setHighThreshold(1.1f);
		detector.setSourceImage(image);
		return detector;
	}
	
	float getPrintMaterialRemainingFromPhoto(
			InputStream inputStream,
			GenericHoughDetection<Circle> houghCircleDetection, 
			GenericHoughDetection<Line> houghLineDetection) throws IOException {

		BufferedImage image = ImageIO.read(inputStream);
		CannyEdgeDetector8BitGray detector = buildEdgeDetector(image);
		detector.process();
		
		BufferedImage edgesImage = detector.getEdgesImage();
		return getPrintMaterialRemainingFromEdgeImage(edgesImage, houghCircleDetection, houghLineDetection);
	}

	@Override
	public void initializeDetector(PrintMaterialDetectorSettings settings) {
		//No custom settings right now.
	}
}
