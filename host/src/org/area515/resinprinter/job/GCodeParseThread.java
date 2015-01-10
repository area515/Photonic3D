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
import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.serial.SerialManager;

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
		File gCode = printJob.getGCodeFile();
		BufferedReader stream = null;
		BufferedImage bimage = null;

		try {
			System.out.println("Parsing file:" + gCode);
			stream = new BufferedReader(new FileReader(gCode));
			String currentLine;
			Integer sliceCount = null;
			Pattern slicePattern = Pattern.compile("\\s*;\\s*<\\s*Slice\\s*>\\s*(\\d+|blank)\\s*", Pattern.CASE_INSENSITIVE);
			Pattern delayPattern = Pattern.compile("\\s*;\\s*<\\s*Delay\\s*>\\s*(\\d+)\\s*", Pattern.CASE_INSENSITIVE);
			Pattern sliceCountPattern = Pattern.compile("\\s*;\\s*Number\\s*of\\s*Slices\\s*=\\s*(\\d+)\\s*", Pattern.CASE_INSENSITIVE);
			
			while ((currentLine = stream.readLine()) != null && printer.isPrintInProgress()) {
				if (currentLine.startsWith(";")) {
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
							if (bimage != null) {
								bimage.flush();
							}
							int incoming = Integer.parseInt(matcher.group(1));
							printJob.setCurrentSlice(incoming);
							String imageNumber = String.format("%0" + padLength(sliceCount) + "d", incoming);
							File imageLocation = new File(gCode.getParentFile(), FilenameUtils.removeExtension(gCode.getName()) + imageNumber + ".png");
							//FileInputStream imageStream = new FileInputStream(imageLocation);
							bimage = ImageIO.read(imageLocation);
							//try {imageStream.close();} catch (IOException e) {}
							System.out.println("Show picture: " + FilenameUtils.removeExtension(gCode.getName()) + imageNumber + ".png");

							printer.showImage(bimage);
						}
						
						continue;
					}
					
					matcher = delayPattern.matcher(currentLine);
					if (matcher.matches()) {
						try {
							System.out.println("Sleep");
							Thread.sleep(Integer.parseInt(matcher.group(1)));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					}
					matcher = sliceCountPattern.matcher(currentLine);
					if (matcher.matches()) {
						sliceCount = Integer.parseInt(matcher.group(1));
						printJob.setTotalSlices(sliceCount);
						System.out.println(sliceCount);
						continue;
					}
					
					// print out comments
					System.out.println(currentLine);
				} else {
					System.out.println("gcode: " + currentLine);
					
					printer.sendAndWaitForResponse(currentLine + "\r\n");
				}
			}
			
			printer.setStatus(JobStatus.Completed);			
			return printer.getStatus();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return JobStatus.Failed;
		} catch (IOException e) {
			e.printStackTrace();
			return JobStatus.Failed;
		} catch (InterruptedException e) {
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
			//Don't need to close the printer
			//printer.close();
			JobManager.Instance().removeJob(printJob);
			SerialManager.Instance().removeAssignment(printer);
			DisplayManager.Instance().removeAssignment(printer);
			PrinterManager.Instance().removeAssignment(printJob);
			System.out.println(Thread.currentThread().getName() + " ended.");
		}
	}

	public Integer padLength(Integer number) {
		if (number == null) {
			return null;
		}
		return number.toString().length() + 1;

	}

}