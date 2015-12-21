package org.area515.resinprinter.job;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import org.area515.resinprinter.job.PrintFileProcessingAid.DataAid;
import org.area515.resinprinter.job.render.CurrentImageRenderer;
import org.area515.resinprinter.job.render.RenderingFileData;
import org.area515.resinprinter.printer.BuildDirection;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.slice.ZSlicer;
import org.area515.resinprinter.stl.Triangle3d;

public class STLFileProcessor implements PrintFileProcessor<Set<Triangle3d>> {
	private Map<PrintJob, RenderingFileData> dataByPrintJob = new HashMap<PrintJob, RenderingFileData>();

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
		RenderingFileData data = dataByPrintJob.get(printJob);
		SlicingProfile slicingProfile = printJob.getPrinter().getConfiguration().getSlicingProfile();
		return data.getCurrentArea() / (slicingProfile.getDotsPermmX() * slicingProfile.getDotsPermmY());
	}
	
	//TODO: Why does the image on the web show a scan line defect with the north side gray and the south side white?
	@Override
	public BufferedImage getCurrentImage(PrintJob printJob) {
		RenderingFileData data = dataByPrintJob.get(printJob);
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
		
		RenderingFileData stlData = new RenderingFileData(aid, dataAid.scriptEngine);
		dataByPrintJob.put(printJob, stlData);
		stlData.slicer = new ZSlicer(printJob.getJobFile(), 1, dataAid.xPixelsPerMM, dataAid.yPixelsPerMM, dataAid.sliceHeight, true);
		stlData.slicer.loadFile(new Double(dataAid.xResolution), new Double(dataAid.yResolution));
		printJob.setTotalSlices(stlData.slicer.getZMax() - stlData.slicer.getZMin());
		
		//Get the slicer queued up for the first image;
		stlData.slicer.setZ(stlData.slicer.getZMin());
		Object nextRenderingPointer = stlData.getCurrentRenderingPointer();
		Future<BufferedImage> currentImage = Main.GLOBAL_EXECUTOR.submit(new CurrentImageRenderer(stlData, nextRenderingPointer, dataAid.xResolution, dataAid.yResolution));
		
		int startPoint = dataAid.slicingProfile.getDirection() == BuildDirection.Bottom_Up?(stlData.slicer.getZMin() + 1): (stlData.slicer.getZMax() + 1);
		int endPoint = dataAid.slicingProfile.getDirection() == BuildDirection.Bottom_Up?(stlData.slicer.getZMax() + 1): (stlData.slicer.getZMin() + 1);
		for (int z = startPoint; z <= endPoint && dataAid.printer.isPrintActive(); z += dataAid.slicingProfile.getDirection().getVector()) {
			
			//Performs all of the duties that are common to most print files
			JobStatus status = aid.performPreSlice(stlData.slicer.getStlErrors());
			if (status != null) {
				return status;
			}
			
			//Wait until the image has been properly rendered. Most likely, it's already done though...
			BufferedImage image = currentImage.get();
			
			//Now that the image has been rendered, we can make the switch to use the pointer that we were using while we were rendering
			stlData.setCurrentRenderingPointer(nextRenderingPointer);
			
			//Cure the current image
			dataAid.printer.showImage(image);

			//Get the next pointer in line to start rendering the image into
			nextRenderingPointer = stlData.getNextRenderingPointer();
			
			//Render the next image while we are waiting for the current image to cure
			if (z < stlData.slicer.getZMax() + 1) {
				stlData.slicer.setZ(z);
				currentImage = Main.GLOBAL_EXECUTOR.submit(new CurrentImageRenderer(stlData, nextRenderingPointer, dataAid.xResolution, dataAid.yResolution));
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
		RenderingFileData data = dataByPrintJob.get(printJob);
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
