package org.area515.resinprinter.job;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.ScriptEngine;

import org.area515.resinprinter.minercube.PrintFileProcessingAid;
import org.area515.resinprinter.minercube.PrintFileProcessingAid.DataAid;
import org.area515.resinprinter.printer.BuildDirection;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.slice.ZSlicer;
import org.area515.resinprinter.stl.Triangle3d;

public class STLFileProcessor implements PrintFileProcessor<Set<Triangle3d>> {
	private Map<PrintJob, STLFileData> dataByPrintJob = new HashMap<PrintJob, STLFileData>();

	public class STLFileData {
		public ScriptEngine scriptEngine;
		public BufferedImage trueImage;
		public BufferedImage falseImage;
		public AtomicBoolean currentImagePointer;
		public Lock renderingImage = new ReentrantLock();
		public ZSlicer slicer;
		public PrintFileProcessingAid aid;
		
		public STLFileData(PrintFileProcessingAid aid, ScriptEngine scriptEngine) {
			this.scriptEngine = scriptEngine;
			this.aid = aid;
		}
	}

	public class CurrentImageRenderer implements Callable<BufferedImage> {
		private BufferedImage currentImage = null;
		private STLFileData data;
		
		public CurrentImageRenderer(STLFileData data, int width, int height) {
			this.data = data;
			
			if (data.currentImagePointer.get()) {
				if (data.falseImage == null) {
					data.falseImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
				}
				data.renderingImage.lock();
				try {
					data.currentImagePointer.set(false);
					currentImage = data.falseImage;
				} finally {
					data.renderingImage.unlock();
				}
			} else {
				if (data.trueImage == null) {
					data.trueImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
				}
				data.renderingImage.lock();
				try {
					data.currentImagePointer.set(true);
					currentImage = data.trueImage;
				} finally {
					data.renderingImage.unlock();
				}
			}
		}
		
		public BufferedImage call() {
			data.slicer.colorizePolygons();
			Graphics2D g2 = (Graphics2D)currentImage.getGraphics();
			data.slicer.paintSlice(g2);
			data.aid.applyMask(g2);
			return currentImage;
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
		return data.slicer.getBuildArea() / (slicingProfile.getDotsPermmX() * slicingProfile.getDotsPermmY());
	}
	
	//TODO: Why does the image on the web show a scan line defect with the north side gray and the south side white?
	@Override
	public BufferedImage getCurrentImage(PrintJob printJob) {
		STLFileData data = dataByPrintJob.get(printJob);
		if (data == null) {
			return null;
		}
		
		if (data.currentImagePointer == null) {
			return null;
		}
		
		data.renderingImage.lock();
		try {
			BufferedImage currentImage = data.currentImagePointer.get() && data.falseImage != null?data.falseImage:data.trueImage;
			return currentImage.getSubimage(0, 0, currentImage.getWidth(), currentImage.getHeight());
		} finally {
			data.renderingImage.unlock();
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
		stlData.currentImagePointer = new AtomicBoolean(true);
		Future<BufferedImage> currentImage = Main.GLOBAL_EXECUTOR.submit(new CurrentImageRenderer(stlData, dataAid.xResolution, dataAid.yResolution));

		int startPoint = dataAid.slicingProfile.getDirection() == BuildDirection.Bottom_Up?(stlData.slicer.getZMin() + 1): (stlData.slicer.getZMax() + 1);
		int endPoint = dataAid.slicingProfile.getDirection() == BuildDirection.Bottom_Up?(stlData.slicer.getZMax() + 1): (stlData.slicer.getZMin() + 1);
		for (int z = startPoint; z <= endPoint && dataAid.printer.isPrintInProgress(); z += dataAid.slicingProfile.getDirection().getVector()) {
			
			//Performs all of the duties that are common to most print files
			JobStatus status = aid.performPreSlice(stlData.slicer.getStlErrors());
			if (status != null) {
				return status;
			}
			
			//Cure the current image
			dataAid.printer.showImage(currentImage.get());

			//Render the next image while we are waiting for the current image to cure
			if (z < stlData.slicer.getZMax() + 1) {
				stlData.slicer.setZ(z);
				currentImage = Main.GLOBAL_EXECUTOR.submit(new CurrentImageRenderer(stlData, dataAid.xResolution, dataAid.yResolution));
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
}
