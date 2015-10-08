package org.area515.resinprinter.job;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.ScriptEngine;

import org.area515.resinprinter.job.PrintFileProcessingAid.DataAid;
import org.area515.resinprinter.job.STLFileProcessor.STLFileData.ImageData;
import org.area515.resinprinter.printer.BuildDirection;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.slice.ZSlicer;
import org.area515.resinprinter.stl.Triangle3d;

public class STLFileProcessor implements PrintFileProcessor<Set<Triangle3d>> {
	private Map<PrintJob, STLFileData> dataByPrintJob = new HashMap<PrintJob, STLFileData>();

	public static class STLFileData {
		public static class ImageData {
			public BufferedImage image;
			private double area;
			private ReentrantLock lock = new ReentrantLock();
			
			public ImageData(BufferedImage image, double area) {
				this.image = image;
				this.area = area;
			}
		}
		
		public ScriptEngine scriptEngine;
		public Map<Boolean, ImageData> imageSync = new HashMap<>();
		public boolean currentImagePointer;
		public ZSlicer slicer;
		public PrintFileProcessingAid aid;
		
		public STLFileData(PrintFileProcessingAid aid, ScriptEngine scriptEngine) {
			this.scriptEngine = scriptEngine;
			this.aid = aid;
		}
		
		public ReentrantLock getSpecificLock(boolean lockPointer) {
			return imageSync.get(lockPointer).lock;
		}
		
		public ReentrantLock getCurrentLock() {
			return imageSync.get(currentImagePointer).lock;
		}
		
		public BufferedImage getCurrentImage() {
			return imageSync.get(currentImagePointer).image;
		}
		
		public double getCurrentArea() {
			return imageSync.get(currentImagePointer).area;
		}
	}
	
	public class CurrentImageRenderer implements Callable<BufferedImage> {
		private STLFileData data;
		private boolean imageToBuild;
		
		public CurrentImageRenderer(STLFileData data, boolean imageToBuild, int width, int height) {
			this.data = data;
			
			ImageData imageData = data.imageSync.get(imageToBuild);
			if (imageData == null) {
				imageData = new ImageData(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE), 0.0);
			}
		}
		
		public BufferedImage call() {
			data.slicer.colorizePolygons();
			Lock lock = data.getSpecificLock(imageToBuild);
			lock.lock();
			try {
				Graphics2D g2 = (Graphics2D)data.getCurrentImage().getGraphics();
				data.slicer.paintSlice(g2);
				ImageData imageData = data.imageSync.get(imageToBuild);
				imageData.area = (double)data.slicer.getBuildArea();
				data.aid.applyBulbMask(g2);
				return data.getCurrentImage();
			} finally {
				lock.unlock();
			}
		}
	}

	@Override
	public String[] getFileExtensions() {
		return new String[]{"stl"};
	}

	@Override
	public boolean acceptsFile(File processingFile) {
		return processingFile.getName().toLowerCase().endsWith("stl");
	}
	
	@Override
	public double getBuildAreaMM(PrintJob printJob) {
		STLFileData data = dataByPrintJob.get(printJob);
		SlicingProfile slicingProfile = printJob.getPrinter().getConfiguration().getSlicingProfile();
		return data.getCurrentArea() / (slicingProfile.getDotsPermmX() * slicingProfile.getDotsPermmY());
	}
	
	//TODO: Why does the image on the web show a scan line defect with the north side gray and the south side white?
	@Override
	public BufferedImage getCurrentImage(PrintJob printJob) {
		STLFileData data = dataByPrintJob.get(printJob);
		if (data == null) {
			return null;
		}
		
		ReentrantLock lock = data.getCurrentLock();
		lock.lock();
		try {
			BufferedImage currentImage = data.getCurrentImage();
			if (currentImage == null)
				return null;
			
			return currentImage.getSubimage(0, 0, currentImage.getWidth(), currentImage.getHeight());
		} finally {
			lock.unlock();
		}
	}

	@Override
	public JobStatus processFile(PrintJob printJob) throws Exception {
		PrintFileProcessingAid aid = new PrintFileProcessingAid();
		DataAid dataAid = aid.performHeader(printJob);
		
		STLFileData stlData = new STLFileData(aid, dataAid.scriptEngine);
		dataByPrintJob.put(printJob, stlData);
		stlData.slicer = new ZSlicer(printJob.getJobFile(), 1, dataAid.xPixelsPerMM, dataAid.yPixelsPerMM, dataAid.sliceHeight, true);
		stlData.slicer.loadFile(new Double(dataAid.xResolution), new Double(dataAid.yResolution));
		printJob.setTotalSlices(stlData.slicer.getZMax() - stlData.slicer.getZMin());
		
		//Get the slicer queued up for the first image;
		stlData.slicer.setZ(stlData.slicer.getZMin());
		stlData.currentImagePointer = true;
		Future<BufferedImage> currentImage = Main.GLOBAL_EXECUTOR.submit(new CurrentImageRenderer(stlData, stlData.currentImagePointer, dataAid.xResolution, dataAid.yResolution));
		
		int startPoint = dataAid.slicingProfile.getDirection() == BuildDirection.Bottom_Up?(stlData.slicer.getZMin() + 1): (stlData.slicer.getZMax() + 1);
		int endPoint = dataAid.slicingProfile.getDirection() == BuildDirection.Bottom_Up?(stlData.slicer.getZMax() + 1): (stlData.slicer.getZMin() + 1);
		for (int z = startPoint; z <= endPoint && dataAid.printer.isPrintInProgress(); z += dataAid.slicingProfile.getDirection().getVector()) {
			
			//Performs all of the duties that are common to most print files
			JobStatus status = aid.performPreSlice(stlData.slicer.getStlErrors());
			if (status != null) {
				return status;
			}
			
			//Wait until the image has been properly rendered. Most likely, it's already done though...
			BufferedImage image = currentImage.get();
			
			//This swaps the image pointer to the next image that was being rendered while we were showing this image.
			stlData.currentImagePointer = !stlData.currentImagePointer;
			
			//Cure the current image
			dataAid.printer.showImage(image);
			
			//Render the next image while we are waiting for the current image to cure
			if (z < stlData.slicer.getZMax() + 1) {
				stlData.slicer.setZ(z);
				currentImage = Main.GLOBAL_EXECUTOR.submit(new CurrentImageRenderer(stlData, !stlData.currentImagePointer, dataAid.xResolution, dataAid.yResolution));
			}
			
			//Performs all of the duties that are common to most print files
			status = aid.performPostSlice(this);
			if (status != null) {
				return status;
			}
		}
		
		return aid.performFooter();
	}

	@Override
	public void prepareEnvironment(File processingFile, PrintJob printJob) throws JobManagerException {
	}

	@Override
	public void cleanupEnvironment(File processingFile) throws JobManagerException {
	}

	@Override
	public Set<Triangle3d> getGeometry(PrintJob printJob) throws JobManagerException {
		STLFileData data = dataByPrintJob.get(printJob);
		if (data == null) {
			return null;
		}
		
		return data.slicer.getAllTriangles();
	}

	@Override
	public String getFriendlyName() {
		return "STL 3D Model";
	}
}
