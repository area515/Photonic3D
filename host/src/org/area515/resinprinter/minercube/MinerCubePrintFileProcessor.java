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

import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintFileProcessingAid;
import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.PrintFileProcessingAid.DataAid;
import org.area515.resinprinter.server.Main;

public class MinerCubePrintFileProcessor implements PrintFileProcessor<Object> {
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
	public double getBuildAreaMM(PrintJob printJob) {
		//TODO: haven't built any of this
		return -1;
	}

	@Override
	public JobStatus processFile(PrintJob printJob) throws Exception {
		PrintFileProcessingAid aid = new PrintFileProcessingAid();
		DataAid data = aid.performHeader(printJob);
	
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
			JobStatus status = aid.performPreSlice(null);
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
			
			aid.applyBulbMask(graphics);
			data.printer.showImage(image);
			printCube.currentImage = image;
			
			//Performs all of the duties that are common to most print files
			status = aid.performPostSlice(this);
			if (status != null) {
				return status;
			}

			if (firstSlices > 0) {
				firstSlices--;
			} else {
				rects = cube.buildNextPrintSlice(centerX, centerY);
			}
		}
		
		return aid.performFooter();
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
			e.printStackTrace();
			throw new JobManagerException("I was expecting a MinerCube XML file. I don't understand this file.");
		}
	}

	@Override
	public void cleanupEnvironment(File processingFile) throws JobManagerException {
		//Nothing to cleanup everything is done in memory.
	}

	@Override
	public Object getGeometry(PrintJob printJob) throws JobManagerException {
		return null;
	}

	@Override
	public String getFriendlyName() {
		return "Maze Cube";
	}
}
