package org.area515.resinprinter.job;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.area515.resinprinter.server.HostProperties;

public class JobManager {

	public enum MachineAction {
		RUNNING, STOPPED
	}

	public static MachineAction Status = MachineAction.STOPPED;

	// Total slices
	private static AtomicInteger totalSlices = new AtomicInteger();
	public static int getTotalSlices(){return JobManager.totalSlices.get();}
	public static void setTotalSlices(int totalSlices){JobManager.totalSlices.set(totalSlices);}
	
	// Showing current slice
	private static AtomicInteger currentSlice = new AtomicInteger();
	public static int getCurrentSlice(){return JobManager.currentSlice.get();}
	public static void setCurrentSlice(int currentSlice){JobManager.currentSlice.set(currentSlice);}
	
	
	// Archive file 
	File job;
	public File getJob() {
		return job;
	}

	// Base direcotry where the archive will be unpacked
	File workingDir;
	public File getWorkingDir() {
		return workingDir;
	}

	// Directory containing gcode file and all images
	File unpackDir;
	public File getUnpackDir() {
		return unpackDir;
	}
	
	// Gcode file
	File gCode;
	public File getGCode(){
		return gCode;
	}

	public JobManager(File selectedArchive)
			throws JobManagerException, IOException {

		this.job = selectedArchive;
		this.workingDir = new File(HostProperties.Instance().getWorkingDir());

		if (!getJob().exists()) {
			throw new JobManagerException("Selected job does not exist");
		}
		if (!getJob().isFile()) {
			throw new JobManagerException("Selected job is not a file");
		}
		
		JobManager.setCurrentSlice(0);
		JobManager.setTotalSlices(0);
		
		setupJob();
	}

	private void setupJob() throws IOException {

		if (getWorkingDir().exists()) {
			FileUtils.deleteDirectory(getWorkingDir());
		}

		unpackDir();
	}

	private void unpackDir() throws IOException {
		ZipFile zipFile = null;
		InputStream in = null;
		OutputStream out = null;
		try {
			zipFile = new ZipFile(getJob());
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File entryDestination = new File(getWorkingDir(), entry.getName());
				entryDestination.getParentFile().mkdirs();
				if (entry.isDirectory())
					entryDestination.mkdirs();
				else {
					in = zipFile.getInputStream(entry);
					out = new FileOutputStream(entryDestination);
					IOUtils.copy(in, out);

				}

			}
			String basename = FilenameUtils.removeExtension(getJob().getName());
			System.out.println("BaseName: " + FilenameUtils.removeExtension(basename));
			this.unpackDir = new File(getWorkingDir(),basename+ ".slice");
			this.gCode = new File(getUnpackDir(),basename + ".gcode");
			System.out.println("Unpacked Dir: " + getUnpackDir().getAbsolutePath());
			System.out.println("Exists: " + getUnpackDir().exists());
			System.out.println("GCode file: " + getGCode().getAbsolutePath());
			System.out.println("Exists: " + getGCode().exists());
		} catch (IOException ioe) {
			throw ioe;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			zipFile.close();
		}

	}

}