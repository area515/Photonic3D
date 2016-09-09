package org.area515.resinprinter.minercube;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.server.Main;
import org.area515.util.Log4jTimer;

public class MinerCubePrintFileProcessor extends AbstractPrintFileProcessor<Object,Object> {
    private static final Logger logger = LogManager.getLogger();
	private Map<PrintJob, PrintCube> minerCubesByPrintJob = new HashMap<PrintJob, PrintCube>();
	
	private class PrintCube {
		Future<MinerCube> cube;
		BufferedImage currentImage;
	}
	
	@Override
	public String[] getFileExtensions() {
		return new String[]{"cubemaze"};
	}

	@Override
	public boolean acceptsFile(File processingFile) {
		
		return processingFile.getName().toLowerCase().endsWith("cubemaze");
	}

	@Override
	public BufferedImage getCurrentImage(PrintJob printJob) {
		PrintCube printCube = minerCubesByPrintJob.get(printJob);
		return printCube.currentImage;
	}

	@Override
	public Double getBuildAreaMM(PrintJob printJob) {
		//TODO: haven't built any of this
		return null;
	}

	@Override
	public JobStatus processFile(PrintJob printJob) throws Exception {
		try {
			DataAid data = initializeDataAid(printJob);
			
			//Everything needs to be setup in the dataByPrintJob before we start the header
			performHeader(data);
	
			PrintCube printCube = minerCubesByPrintJob.get(printJob);
			MinerCube cube = printCube.cube.get();
			cube.startPrint(data.xPixelsPerMM, data.yPixelsPerMM, data.sliceHeight);
			//TODO: need to set the total slices for a percentage complete: printJob.setTotalSlices();
	
			int centerX = data.xResolution / 2;
			int centerY = data.yResolution / 2;
	
			int firstSlices = data.inkConfiguration.getNumberOfFirstLayers();
			List<Rectangle> rects = cube.buildNextPrintSlice(centerX, centerY);
			while (cube.hasPrintSlice()) {
				//Performs all of the duties that are common to most print files
				JobStatus status = performPreSlice(data, null);
				if (status != null) {
					return status;
				}
				
				BufferedImage image = new BufferedImage(data.xResolution, data.yResolution, BufferedImage.TYPE_INT_ARGB_PRE);
				Graphics2D graphics = (Graphics2D)image.getGraphics();
				graphics.setColor(Color.black);
				graphics.fillRect(0, 0, data.xResolution, data.yResolution);
				graphics.setColor(Color.white);
				for (Rectangle currentRect : rects) {
					//graphics.fillRect(currentRect.x, currentRect.y, currentRect.width, currentRect.height);
					graphics.fillRect(currentRect.x, currentRect.y, currentRect.width, currentRect.height);
				}

				image = applyImageTransforms(data, image, data.xResolution, data.yResolution);
				//applyBulbMask(data, graphics, data.xResolution, data.yResolution);
				
				//Performs all of the duties that are common to most print files
				status = printImageAndPerformPostProcessing(data, printCube.currentImage = image);
				if (status != null) {
					return status;
				}
	
				if (firstSlices > 0) {
					firstSlices--;
				} else {
					rects = cube.buildNextPrintSlice(centerX, centerY);
				}
			}
			
			return performFooter(data);
		} finally {
			minerCubesByPrintJob.remove(printJob);
		}
	}
	
	@Override
	public void prepareEnvironment(File processingFile, PrintJob printJob) throws JobManagerException {
		JAXBContext jaxbContext;
		try {
			jaxbContext = JAXBContext.newInstance(MinerCube.class);
			Unmarshaller jaxbUnMarshaller = jaxbContext.createUnmarshaller();
			final MinerCube cube = (MinerCube)jaxbUnMarshaller.unmarshal(processingFile);
			Future<MinerCube> future = Main.GLOBAL_EXECUTOR.submit(new Callable<MinerCube>() {
				@Override
				public MinerCube call() throws Exception {
					cube.buildMaze();
					return cube;
				}
			});
			PrintCube printCube = new PrintCube();
			printCube.cube = future;
			minerCubesByPrintJob.put(printJob, printCube);
		} catch (JAXBException e) {
			logger.error("Marshalling error while processing file:" + processingFile, e);
			throw new JobManagerException("I was expecting a MinerCube XML file. I don't understand this file.");
		}
	}

	@Override
	public void cleanupEnvironment(File processingFile) throws JobManagerException {
		//Nothing to cleanup everything is done in memory.
	}

	@Override
	public Object getGeometry(PrintJob printJob) throws JobManagerException {
		throw new JobManagerException("You can't get geometry from this type of file");
	}

	@Override
	public Object getErrors(PrintJob printJob) throws JobManagerException {
		throw new JobManagerException("You can't get error geometry from this type of file");
	}

	@Override
	public String getFriendlyName() {
		return "Maze Cube";
	}

	@Override
	public boolean isThreeDimensionalGeometryAvailable() {
		return false;
	}
}
