package org.area515.resinprinter.job;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Future;

import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.exception.SliceHandlingException;
import org.area515.resinprinter.job.render.RenderedData;
import org.area515.resinprinter.job.render.RenderingCache;
import org.area515.resinprinter.printer.BuildDirection;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.slice.CloseOffMend;
import org.area515.resinprinter.slice.StlError;
import org.area515.resinprinter.slice.ZSlicer;
import org.area515.resinprinter.stl.Triangle3d;
import org.area515.util.Log4jTimer;

public class STLFileProcessor extends AbstractPrintFileProcessor<Iterator<Triangle3d>, Set<StlError>> implements Previewable {
	public static String STL_OVERHEAD = "stlOverhead";

	private static final Logger logger = LogManager.getLogger();

	@Override
	public String[] getFileExtensions() {
		return new String[]{"stl"};
	}

	@Override
	public boolean acceptsFile(File processingFile) {
		return processingFile.getName().toLowerCase().endsWith("stl");
	}
	
	@Override
	public DataAid createDataAid(PrintJob printJob) throws JobManagerException {
		return new STLDataAid(printJob);
	}

	@Override
	public JobStatus processFile(PrintJob printJob) throws Exception {
		try {
			STLDataAid dataAid = (STLDataAid)initializeJobCacheWithDataAid(printJob);
			double zScale = 1.0;
			Customizer customizer = dataAid.printJob.getCustomizer();
			if (customizer != null) {
				if (customizer.getZScale() != null) {
					zScale = customizer.getZScale();
				}
			}

			boolean overrideNormals = dataAid.configuration.getMachineConfig().getOverrideModelNormalsWithRightHandRule() == null?false:dataAid.configuration.getMachineConfig().getOverrideModelNormalsWithRightHandRule();
			ZSlicer slicer = new ZSlicer(zScale, 
					dataAid.xPixelsPerMM / zScale, 
					dataAid.yPixelsPerMM / zScale, 
					dataAid.sliceHeight, 
					dataAid.sliceHeight / 2, 
					true, 
					overrideNormals,
					new CloseOffMend());
			dataAid.slicer = slicer;
			dataAid.slicer.loadFile(new FileInputStream(printJob.getJobFile()), null, null);
			printJob.setTotalSlices(slicer.getZMaxIndex() - slicer.getZMinIndex());
			
			//Get the slicer queued up for the first image;
			dataAid.slicer.setZIndex(slicer.getZMinIndex());
			Object nextRenderingPointer = dataAid.cache.getCurrentRenderingPointer();
			Future<RenderedData> currentImage = Main.GLOBAL_EXECUTOR.submit(new STLImageRenderer(dataAid, this, nextRenderingPointer, false));
			
			//Everything needs to be setup in the dataByPrintJob before we start the header
			performHeader(dataAid);
			
			int startPoint = dataAid.slicingProfile.getDirection() == BuildDirection.Bottom_Up?(slicer.getZMinIndex() + 1): (slicer.getZMaxIndex() + 1);
			int endPoint = dataAid.slicingProfile.getDirection() == BuildDirection.Bottom_Up?(slicer.getZMaxIndex() + 1): (slicer.getZMinIndex() + 1);
			for (int z = startPoint; z <= endPoint && dataAid.printer.isPrintActive(); z += dataAid.slicingProfile.getDirection().getVector()) {
				
				//Performs all of the duties that are common to most print files
				JobStatus status = performPreSlice(dataAid, slicer.getStlErrors());
				if (status != null) {
					return status;
				}
				
				logger.info("SliceOverheadStart:{}", ()->Log4jTimer.startTimer(STL_OVERHEAD));
				
				//Wait until the image has been properly rendered. Most likely, it's already done though...
				BufferedImage image = currentImage.get().getPrintableImage();
				
				logger.info("SliceOverhead:{}", ()->Log4jTimer.completeTimer(STL_OVERHEAD));
				
				//Now that the image has been rendered, we can make the switch to use the pointer that we were using while we were rendering
				dataAid.cache.setCurrentRenderingPointer(nextRenderingPointer);
				
				//Start the exposure timer
				// logger.info("ExposureStart:{}", ()->Log4jTimer.startTimer(EXPOSURE_TIMER));
				
				//Cure the current image
				//dataAid.printer.showImage(image);
				
				//Get the next pointer in line to start rendering the image into
				nextRenderingPointer = dataAid.cache.getNextRenderingPointer();
				
				//Render the next image while we are waiting for the current image to cure
				if (z < slicer.getZMaxIndex() + 1) {
					slicer.setZIndex(z);
					currentImage = Main.GLOBAL_EXECUTOR.submit(new STLImageRenderer(dataAid, this, nextRenderingPointer, false));
				}
				
				//Performs all of the duties that are common to most print files
				status = printImageAndPerformPostProcessing(dataAid, image);
				if (status != null) {
					return status;
				}
			}
			
			return performFooter(dataAid);
		} finally {
			clearDataAid(printJob);
		}
	}
	
	@Override
	public BufferedImage renderPreviewImage(DataAid aid) throws SliceHandlingException {
		try {
			STLDataAid dataAid = (STLDataAid)aid;
			boolean overrideNormals = dataAid.configuration.getMachineConfig().getOverrideModelNormalsWithRightHandRule() == null?false:dataAid.configuration.getMachineConfig().getOverrideModelNormalsWithRightHandRule();
			dataAid.slicer = new ZSlicer(1, dataAid.xPixelsPerMM, dataAid.yPixelsPerMM, dataAid.sliceHeight, dataAid.sliceHeight / 2, true, overrideNormals, new CloseOffMend());
			dataAid.slicer.loadFile(new FileInputStream(dataAid.printJob.getJobFile()), null, null);
			dataAid.printJob.setTotalSlices(dataAid.slicer.getZMaxIndex() - dataAid.slicer.getZMinIndex());
	
			//Get the slicer queued up for the first image;
			dataAid.slicer.setZIndex(dataAid.slicer.getZMinIndex());
			Object nextRenderingPointer = dataAid.cache.getCurrentRenderingPointer();
			STLImageRenderer renderer = new STLImageRenderer(dataAid, this, nextRenderingPointer, true);
			return renderer.call().getPrintableImage();
		} catch (IOException | JobManagerException e) {
			throw new SliceHandlingException(e);
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
		final STLDataAid data = (STLDataAid)getDataAid(printJob);
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
		STLDataAid data = (STLDataAid)getDataAid(printJob);
		if (data == null) {
			return null;
		}
		
		return new HashSet<>(data.slicer.getStlErrors());
	}

	@Override
	public boolean isThreeDimensionalGeometryAvailable() {
		return true;
	}
}
