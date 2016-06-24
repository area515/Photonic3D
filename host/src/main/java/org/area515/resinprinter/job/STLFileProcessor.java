package org.area515.resinprinter.job;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import java.awt.image.*;
import javax.imageio.*;

import org.area515.resinprinter.job.render.RenderingFileData;
import org.area515.resinprinter.printer.BuildDirection;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.printer.SlicingProfile.InkConfig;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.job.*;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.slice.CloseOffMend;
import org.area515.resinprinter.slice.StlError;
import org.area515.resinprinter.slice.ZSlicer;
import org.area515.resinprinter.stl.Triangle3d;
import org.area515.resinprinter.server.HostProperties;



public class STLFileProcessor extends AbstractPrintFileProcessor<Iterator<Triangle3d>, Set<StlError>> {
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
	public Double getBuildAreaMM(PrintJob printJob) {
		RenderingFileData data = dataByPrintJob.get(printJob);
		if (data == null) {
			return null;
		}
		
		SlicingProfile slicingProfile = printJob.getPrinter().getConfiguration().getSlicingProfile();
		return data.getCurrentArea() / (slicingProfile.getDotsPermmX() * slicingProfile.getDotsPermmY());
	}
	
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
		try {
			DataAid dataAid = initializeDataAid(printJob);
			RenderingFileData stlData = new RenderingFileData();
			dataByPrintJob.put(printJob, stlData);
			
			stlData.slicer = new ZSlicer(1, dataAid.xPixelsPerMM, dataAid.yPixelsPerMM, dataAid.sliceHeight, dataAid.sliceHeight / 2, true, new CloseOffMend());
			stlData.slicer.loadFile(new FileInputStream(printJob.getJobFile()), new Double(dataAid.xResolution), new Double(dataAid.yResolution));
			printJob.setTotalSlices(stlData.slicer.getZMaxIndex() - stlData.slicer.getZMinIndex());
			
			//Get the slicer queued up for the first image;
			stlData.slicer.setZIndex(stlData.slicer.getZMinIndex());
			Object nextRenderingPointer = stlData.getCurrentRenderingPointer();
			Future<BufferedImage> currentImage = Main.GLOBAL_EXECUTOR.submit(new STLImageRenderer(dataAid, this, stlData, nextRenderingPointer, dataAid.xResolution, dataAid.yResolution));
			
			//Everything needs to be setup in the dataByPrintJob before we start the header
			performHeader(dataAid);
			
			int startPoint = dataAid.slicingProfile.getDirection() == BuildDirection.Bottom_Up?(stlData.slicer.getZMinIndex() + 1): (stlData.slicer.getZMaxIndex() + 1);
			int endPoint = dataAid.slicingProfile.getDirection() == BuildDirection.Bottom_Up?(stlData.slicer.getZMaxIndex() + 1): (stlData.slicer.getZMinIndex() + 1);
			for (int z = startPoint; z <= endPoint && dataAid.printer.isPrintActive(); z += dataAid.slicingProfile.getDirection().getVector()) {
				
				//Performs all of the duties that are common to most print files
				JobStatus status = performPreSlice(dataAid, stlData.slicer.getStlErrors());
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
				if (z < stlData.slicer.getZMaxIndex() + 1) {
					stlData.slicer.setZIndex(z);
					currentImage = Main.GLOBAL_EXECUTOR.submit(new STLImageRenderer(dataAid, this, stlData, nextRenderingPointer, dataAid.xResolution, dataAid.yResolution));
				}
				
				//Performs all of the duties that are common to most print files
				status = performPostSlice(dataAid);
				if (status != null) {
					return status;
				}
			}
			
			return performFooter(dataAid);
		} finally {
			dataByPrintJob.remove(printJob);
		}
	}
	//TODO: Create PreviewSlice0 method that copies processfile code
	// might have to pass in an array of printers 
	// passing in printable rn but idk if it works? 
	// should i be passing in a printable or a jobFile? or should I just be passing in a printJob?
	
	//public void previewSlice(List<Printer> printers, File jobFile) throws Exception {
	//
	//probably trying to do too much higher level things but i juust wanted the code to compile
	public void previewSlice(PrintJob printJob) throws Exception {
		try {
			//Initialize DataAid
			//TODO: Create dataaid manually based on started printer
			//how do access list of printers?
			//printerservice.getprinters
			//if printer.isPrintActive() {
			//	create dataaid
			//}
			//
			
			//TODO: This doesn't work
			List<PrinterConfiguration> identifiers = HostProperties.Instance().getPrinterConfigurations();
			Printer activePrinter = null;
			for (PrinterConfiguration current : identifiers) {
				try {
					Printer printer = PrinterManager.Instance().getPrinter(current.getName());
					if (printer == null) {
						printer = new Printer(current);
					}
					if (printer.isPrintActive()) {
						activePrinter = printer;
						break;
					}
				} catch (Exception e) {
				    throw new Exception("Error getting printer list", e);
				}
			}
			
			//basically dataaid using printer 
			PrinterConfiguration configuration = activePrinter.getConfiguration();
			SlicingProfile slicingProfile = configuration.getSlicingProfile();
			InkConfig inkConfiguration = slicingProfile.getSelectedInkConfig();
			double xPixelsPerMM = slicingProfile.getDotsPermmX();
			double yPixelsPerMM = slicingProfile.getDotsPermmY();
			int xResolution = slicingProfile.getxResolution();
			int yResolution = slicingProfile.getyResolution();
			
			//TODO: Does this file processor requires an ink configuration?
			if (inkConfiguration == null) {
				throw new Exception("Your printer doesn't have a selected ink configuration.");
			}
			double sliceHeight = inkConfiguration.getSliceHeight();

			// DataAid dataAid = initializeDataAid(printJob);
			RenderingFileData stlData = new RenderingFileData();
			
			stlData.slicer = new ZSlicer(1, xPixelsPerMM, yPixelsPerMM, sliceHeight, sliceHeight / 2, true, new CloseOffMend());
			//TODO: What is jobfile?
			stlData.slicer.loadFile(new FileInputStream(printJob.getJobFile()), new Double(xResolution), new Double(yResolution));
			printJob.setTotalSlices(stlData.slicer.getZMaxIndex() - stlData.slicer.getZMinIndex());
			
			//Get the slicer queued up for the first image;
			stlData.slicer.setZIndex(stlData.slicer.getZMinIndex());
			Object nextRenderingPointer = stlData.getCurrentRenderingPointer();
			//TODO: this calls dataAid...how do I not call data-aid
			//TODO: place holder > delete this later
			DataAid dataAid = initializeDataAid(printJob);
			//should i create a new method that takes in only printer, and not dataAid/printJob? 
			Future<BufferedImage> currentImage = Main.GLOBAL_EXECUTOR.submit(new STLImageRenderer(dataAid, this, stlData, nextRenderingPointer, xResolution, yResolution));
			//do i need to preslice? what even does preslice do?
			
			//store slice 0 
			BufferedImage image = currentImage.get();
			File outputfile = new File("previewSlice0.png");
			ImageIO.write(image, "png", outputfile);
			//saves slice 0 into previewSlice0.png	
		} finally {
			System.out.println("failed lol");
		}
	}

	@Override
	public void prepareEnvironment(File processingFile, PrintJob printJob) throws JobManagerException {
	}

	@Override
	public void cleanupEnvironment(File processingFile) throws JobManagerException {
	}

	@Override
	public Iterator<Triangle3d> getGeometry(PrintJob printJob) throws JobManagerException {
		RenderingFileData data = dataByPrintJob.get(printJob);
		if (data == null) {
			return null;
		}
		
		return new Iterator<Triangle3d>() {
			private Triangle3d nextTriangle;
			{
				nextTriangle = data.slicer.getFirstTriangle();
			}
			
			@Override
			public boolean hasNext() {
				return nextTriangle != null;
			}

			@Override
			public Triangle3d next() {
				Triangle3d t = nextTriangle;
				nextTriangle = nextTriangle.getNextTriangle();
				return t;
			}
		};
	}

	@Override
	public String getFriendlyName() {
		return "STL 3D Model";
	}

	@Override
	public Set<StlError> getErrors(PrintJob printJob) throws JobManagerException {
		RenderingFileData data = dataByPrintJob.get(printJob);
		if (data == null) {
			return null;
		}
		
		return new HashSet<>(data.slicer.getStlErrors());
	}
}
