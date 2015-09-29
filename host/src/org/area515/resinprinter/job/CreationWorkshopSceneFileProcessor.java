package org.area515.resinprinter.job;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.server.HostProperties;

public class CreationWorkshopSceneFileProcessor implements PrintFileProcessor {
	private HashMap<PrintJob, BufferedImage> currentlyDisplayedImage = new HashMap<PrintJob, BufferedImage>();
	
	@Override
	public String[] getFileExtensions() {
		return new String[]{"zip", "cws"};
	}

	@Override
	public boolean acceptsFile(File processingFile) {
		//TODO: we shouldn't except all zip files only those that have embedded gif/jpg/png information.
		return processingFile.getName().toLowerCase().endsWith(".zip") || processingFile.getName().toLowerCase().endsWith(".cws");
	}
	
	@Override
	public BufferedImage getCurrentImage(PrintJob processingFile) {
		return currentlyDisplayedImage.get(processingFile);
	}

	@Override
	public double getBuildAreaMM(PrintJob processingFile) {
		return -1;
	}
	
	@Override
	public JobStatus processFile(final PrintJob printJob) throws Exception {
		File gCodeFile = null;
		try {
			gCodeFile = findGcodeFile(printJob.getJobFile());
		} catch (JobManagerException e) {
			e.printStackTrace();
			return JobStatus.Failed;
		}
		
		Printer printer = printJob.getPrinter();
		BufferedReader stream = null;
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
			
			//We can't set these values, that means they aren't set to helpful values when this job starts
			//data.printJob.setExposureTime(data.inkConfiguration.getExposureTime());
			//data.printJob.setZLiftDistance(data.slicingProfile.getLiftFeedRate());
			//data.printJob.setZLiftSpeed(data.slicingProfile.getLiftDistance());

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
					//printJob.setCurrentSliceTime(System.currentTimeMillis() - startOfLastImageDisplay);
								printJob.addNewSlice(System.currentTimeMillis() - startOfLastImageDisplay, 0);
							}
							startOfLastImageDisplay = System.currentTimeMillis();
							
							BufferedImage oldImage = null;
							if (currentlyDisplayedImage != null) {
								oldImage = currentlyDisplayedImage.get(printJob);
							}
							int incoming = Integer.parseInt(matcher.group(1));
					//printJob.setCurrentSlice(incoming);
							String imageNumber = String.format("%0" + padLength + "d", incoming);
							String imageFilename = FilenameUtils.removeExtension(gCodeFile.getName()) + imageNumber + ".png";
							File imageFile = new File(gCodeFile.getParentFile(), imageFilename);
							currentlyDisplayedImage.put(printJob, ImageIO.read(imageFile));
							System.out.println("Show picture: " + imageFilename);
							
							//Notify the client that the printJob has increased the currentSlice
							NotificationManager.jobChanged(printer, printJob);

							printer.showImage(currentlyDisplayedImage.get(printJob));
							if (oldImage != null) {
								oldImage.flush();
							}
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
						double foundLiftSpeed = Double.parseDouble(matcher.group(1));
						if (printJob.isZLiftSpeedOverriden()) {
							System.out.println("Override: LiftDistance:" + String.format("%1.3f", foundLiftSpeed) + " overrided to:" + String.format("%1.3f", printJob.getZLiftSpeed()));
						} else {
							printJob.setZLiftSpeed(foundLiftSpeed);
							System.out.println("Found: LiftSpeed of:" + String.format("%1.3f", foundLiftSpeed));
						}
						continue;
					}
					
					matcher = liftDistancePattern.matcher(currentLine);
					if (matcher.matches()) {
						double foundLiftDistance = Double.parseDouble(matcher.group(1));
						if (printJob.isZLiftDistanceOverriden()) {
							System.out.println("Override: LiftDistance:" + String.format("%1.3f", foundLiftDistance) + " overrided to:" + String.format("%1.3f", printJob.getZLiftDistance()));
						} else {
							printJob.setZLiftDistance(foundLiftDistance);
							System.out.println("Found: LiftDistance of:" + String.format("%1.3f", foundLiftDistance));
						}
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
			
			return printer.getStatus();
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
			
			if (currentlyDisplayedImage != null) {
				currentlyDisplayedImage.get(printJob).flush();
			}
		}
	}
	
	public static File buildExtractionDirectory(String archive) {
		return new File(HostProperties.Instance().getWorkingDir(), archive + "extract");
	}

	@Override
	public void prepareEnvironment(File processingFile, PrintJob printJob) throws JobManagerException {
		List<PrintJob> printJobs = PrintJobManager.Instance().getJobsByFilename(processingFile.getName());
		
		if (printJobs.size() > 1) {
			throw new JobManagerException("It currently isn't possible to print more than 1 " + getFriendlyName() + " file at once.");
		}
		
		File extractDirectory = buildExtractionDirectory(processingFile.getName());
		
		if (extractDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(extractDirectory);
			} catch (IOException e) {
				throw new JobManagerException("Couldn't clean directory for new job:" + extractDirectory, e);
			}
		}

		try {
			unpackDir(processingFile);
		} catch (IOException e) {
			throw new JobManagerException("Couldn't unpack new job:" + processingFile + " into working directory:" + extractDirectory);
		}
	}

	@Override
	public void cleanupEnvironment(File processingFile) throws JobManagerException {
		File extractDirectory = buildExtractionDirectory(processingFile.getName());
		if (extractDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(extractDirectory);
			} catch (IOException e) {
				e.printStackTrace();
				throw new JobManagerException("Couldn't clean up extract directory");
			}
		}
	}
	
	
	private File findGcodeFile(File jobFile) throws JobManagerException{
	
            String[] extensions = {"gcode"};
            boolean recursive = true;
            
            //
            // Finds files within a root directory and optionally its
            // subdirectories which match an array of extensions. When the
            // extensions is null all files will be returned.
            //
            // This method will returns matched file as java.io.File
            //
            List<File> files = new ArrayList<File>(FileUtils.listFiles(buildExtractionDirectory(jobFile.getName()), extensions, recursive));

           if (files.size() > 1){
            	throw new JobManagerException("More than one gcode file exists in print directory");
            }else if (files.size() == 0){
            	throw new JobManagerException("Gcode file was not found. Did you include the Gcode when you exported your scene?");
            }
           
           return files.get(0);
	}
	
	private void unpackDir(File jobFile) throws IOException, JobManagerException {
		ZipFile zipFile = null;
		InputStream in = null;
		OutputStream out = null;
		File extractDirectory = buildExtractionDirectory(jobFile.getName());
		try {
			zipFile = new ZipFile(jobFile);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File entryDestination = new File(extractDirectory, entry.getName());
				entryDestination.getParentFile().mkdirs();
				if (entry.isDirectory())
					entryDestination.mkdirs();
				else {
					in = zipFile.getInputStream(entry);
					out = new FileOutputStream(entryDestination);
					IOUtils.copy(in, out);
					IOUtils.closeQuietly(in);
					IOUtils.closeQuietly(out);
				}
			}
			String basename = FilenameUtils.removeExtension(jobFile.getName());
			System.out.println("BaseName: " + FilenameUtils.removeExtension(basename));
			findGcodeFile(jobFile);
		} catch (IOException ioe) {
			throw ioe;
		} finally {
			zipFile.close();
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

	@Override
	public Object getGeometry(PrintJob printJob) throws JobManagerException {
		throw new JobManagerException("You can't get geometry from this type of file");
	}

	@Override
	public String getFriendlyName() {
		return "Creation Workshop Scene";
	}
}
