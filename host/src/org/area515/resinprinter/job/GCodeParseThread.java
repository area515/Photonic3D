package org.area515.resinprinter.job;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterManager;

public class GCodeParseThread implements Callable<JobStatus> {
	private PrintJob printJob = null;
	private Printer printer;
	
	public GCodeParseThread(PrintJob printJob, Printer printer) {
		this.printJob = printJob;
		this.printer = printer;
	}

	@Override
	public JobStatus call() {
		System.out.println(Thread.currentThread().getName() + " Start");
		printer.setStatus(JobStatus.Printing);
		File gCodeFile = printJob.getGCodeFile();
		BufferedReader stream = null;
		BufferedImage bimage = null;
		printJob.setStartTime(System.currentTimeMillis());
		long startOfLastImageDisplay = -1;
		try {
			System.out.println("Parsing file:" + gCodeFile);
			int padLength = determinePadLength(gCodeFile);
			stream = new BufferedReader(new FileReader(gCodeFile));
			String currentLine;
			Integer sliceCount = null;
			Pattern slicePattern = Pattern.compile("\\s*;\\s*<\\s*Slice\\s*>\\s*(\\d+|blank)\\s*", Pattern.CASE_INSENSITIVE);
			Pattern delayPattern = Pattern.compile("\\s*;\\s*<\\s*Delay\\s*>\\s*(\\d+)\\s*", Pattern.CASE_INSENSITIVE);
			Pattern liftSpeedPattern = Pattern.compile(   "\\s*;\\s*\\(?\\s*Z\\s*Lift\\s*Feed\\s*Rate\\s*=\\s*([\\d\\.]+)\\s*(?:[Mm]{2}?/[Ss])?\\s*\\)?\\s*", Pattern.CASE_INSENSITIVE);
			Pattern liftDistancePattern = Pattern.compile("\\s*;\\s*\\(?\\s*Lift\\s*Distance\\s*=\\s*([\\d\\.]+)\\s*(?:[Mm]{2})?\\s*\\)?\\s*", Pattern.CASE_INSENSITIVE);
			Pattern sliceCountPattern = Pattern.compile("\\s*;\\s*Number\\s*of\\s*Slices\\s*=\\s*(\\d+)\\s*", Pattern.CASE_INSENSITIVE);
			Pattern gCodePattern = Pattern.compile("\\s*([^;]+)\\s*;?.*", Pattern.CASE_INSENSITIVE);
			
			while ((currentLine = stream.readLine()) != null && printer.isPrintInProgress()) {
					Matcher matcher = slicePattern.matcher(currentLine);
					if (matcher.matches()) {
						if (sliceCount == null) {
							throw new IllegalArgumentException("No 'Number of Slices' line in gcode file");
						}

						if (matcher.group(1).toUpperCase().equals("BLANK")) {
							System.out.println("Show Blank");
							printer.showBlankImage();
							
							//This is the perfect time to wait for a pause if one is required.
							printer.waitForPauseIfRequired();
						} else {
							if (startOfLastImageDisplay > -1) {
								printJob.setCurrentSliceTime(System.currentTimeMillis() - startOfLastImageDisplay);
							}
							startOfLastImageDisplay = System.currentTimeMillis();
							
							if (bimage != null) {
								bimage.flush();
							}
							int incoming = Integer.parseInt(matcher.group(1));
							printJob.setCurrentSlice(incoming);
							String imageNumber = String.format("%0" + padLength + "d", incoming);
							String imageFilename = FilenameUtils.removeExtension(gCodeFile.getName()) + imageNumber + ".png";
							File imageFile = new File(gCodeFile.getParentFile(), imageFilename);
							bimage = ImageIO.read(imageFile);
							System.out.println("Show picture: " + imageFilename);

							printer.showImage(bimage);
						}
						continue;
					}
					
					matcher = delayPattern.matcher(currentLine);
					if (matcher.matches()) {
						try {
							int sleepTime = Integer.parseInt(matcher.group(1));
							if (printJob.isExposureTimeOverriden()) {
								sleepTime = printJob.getExposureTime();
							} else {
								printJob.setExposureTime(sleepTime);
							}
							System.out.println("Sleep:" + sleepTime);
							Thread.sleep(sleepTime);
							System.out.println("Sleep complete");
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					}
					
					matcher = sliceCountPattern.matcher(currentLine);
					if (matcher.matches()) {
						sliceCount = Integer.parseInt(matcher.group(1));
						printJob.setTotalSlices(sliceCount);
						System.out.println("Found:" + sliceCount + " slices");
						continue;
					}
					
					matcher = liftSpeedPattern.matcher(currentLine);
					if (matcher.matches()) {
						double liftSpeed = Double.parseDouble(matcher.group(1));
						printJob.setZLiftSpeed(liftSpeed);
						System.out.println("Found:lift speed of:" + String.format("%1.3f", liftSpeed));
						continue;
					}
					
					matcher = liftDistancePattern.matcher(currentLine);
					if (matcher.matches()) {
						double liftDistance = Double.parseDouble(matcher.group(1));
						printJob.setZLiftDistance(liftDistance);
						System.out.println("Found:lift distance of:" + String.format("%1.3f", liftDistance));
						continue;
					}
					
					matcher = gCodePattern.matcher(currentLine);
					if (matcher.matches()) {
						String gCode = matcher.group(1).trim();
						System.out.println("Send GCode:" + gCode);

						for (int t = 0; t < 3; t++) {
							gCode = printer.getGCodeControl().sendGcodeReturnIfPrinterStops(gCode);
							if (gCode != null) {
								break;
							}
							System.out.println("Printer timed out:" + t);
						}
						System.out.print("Printer Response:" + gCode);
						continue;
					}
					
					// print out comments
					System.out.println("Ignored line:" + currentLine);
			}
			
			printer.setStatus(JobStatus.Completed);
			System.out.println("Job Complete:" + Thread.currentThread().getName());
			return printer.getStatus();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return JobStatus.Failed;
		} catch (IOException e) {
			e.printStackTrace();
			return JobStatus.Failed;
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
			
			if (bimage != null) {
				bimage.flush();
			}
			//Don't need to close the printer or dissassociate the serial and display devices
			//printer.close();
			//SerialManager.Instance().removeAssignment(printer);
			//DisplayManager.Instance().removeAssignment(printer);
			printer.showBlankImage();
			JobManager.Instance().removeJob(printJob);
			PrinterManager.Instance().removeAssignment(printJob);
			System.out.println(Thread.currentThread().getName() + " ended.");
		}
	}

	public int determinePadLength(File gCode) throws FileNotFoundException {
		File currentFile = null;
		for (int t = 1; t < 10; t++) {
			currentFile = new File(gCode.getParentFile(), FilenameUtils.removeExtension(gCode.getName()) + String.format("%0" + t + "d", 0) + ".png");
			if (currentFile.exists()) {
				return t;
			}
		}
		
		throw new FileNotFoundException(currentFile + "");
	}
}