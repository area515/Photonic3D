package org.area515.resinprinter.job;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterManager;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;

public class JobManager {
	private static JobManager INSTANCE;
	
	private ConcurrentHashMap<String, PrintJob> printJobsByName = new ConcurrentHashMap<String, PrintJob>();
	
	public static JobManager Instance() {
		if (INSTANCE == null) {
			INSTANCE = new JobManager();
		}
		return INSTANCE;
	}

	private JobManager() {
	}
	
	public PrintJob createJob(File archiveJob) throws JobManagerException {
		PrintJob newJob = new PrintJob(archiveJob);
		PrintJob otherJob = printJobsByName.putIfAbsent(newJob.getJobFile().getName(), newJob);
		if (otherJob != null) {
			throw new JobManagerException("The selected job is already running");
		}
		
		if (!archiveJob.exists()) {
			printJobsByName.remove(archiveJob.getName());
			throw new JobManagerException("The selected job does not exist");
		}
		if (!archiveJob.isFile()) {
			printJobsByName.remove(archiveJob.getName());
			throw new JobManagerException("The selected job is not a file");
		}
		
		newJob.setCurrentSlice(0);
		newJob.setTotalSlices(0);

		File extractDirectory = buildExtractionDirectory(archiveJob.getName());
		
		if (extractDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(extractDirectory);
			} catch (IOException e) {
				throw new JobManagerException("Couldn't clean directory for new job:" + extractDirectory, e);
			}
		}

		try {
			unpackDir(newJob, extractDirectory);
		} catch (IOException e) {
			throw new JobManagerException("Couldn't unpack new job:" + archiveJob + " into working directory:" + extractDirectory);
		}
		
		//TODO: Needs to clean up after itself!
		return newJob;
	}
	
	public static File buildExtractionDirectory(String archive) {
		return new File(HostProperties.Instance().getWorkingDir(), archive + "extract");
	}
	
	public Future<JobStatus> startJob(PrintJob job, Printer printer) throws AlreadyAssignedException {
		PrinterManager.Instance().assignPrinter(job, printer);
		Callable<JobStatus> worker = new GCodeParseThread(job, printer);
		Future<JobStatus> futureJobStatus = Main.GLOBAL_EXECUTOR.submit(worker);
		job.setFutureJobStatus(futureJobStatus);
		return futureJobStatus;
	}
	
	public PrintJob getJob(String jobId) {
		return printJobsByName.get(jobId);
	}
	
	public void removeJob(PrintJob job) {
		if (job == null)
			return;
		
		printJobsByName.remove(job.getJobFile().getName());
	}
	
	private File findGcodeFile(File root) throws JobManagerException{
	
            String[] extensions = {"gcode"};
            boolean recursive = true;

            //
            // Finds files within a root directory and optionally its
            // subdirectories which match an array of extensions. When the
            // extensions is null all files will be returned.
            //
            // This method will returns matched file as java.io.File
            //
            List<File> files = new ArrayList<File>(FileUtils.listFiles(root, extensions, recursive));

           if (files.size() > 1){
            	throw new JobManagerException("More than one gcode file exists in print directory");
            }else if (files.size() == 0){
            	throw new JobManagerException("Gcode file was not found in print directory");
            }
           
           return files.get(0);
         
        
		
		
	}
	
	private void unpackDir(PrintJob job, File extractDirectory) throws IOException, JobManagerException {
		ZipFile zipFile = null;
		InputStream in = null;
		OutputStream out = null;
		try {
			zipFile = new ZipFile(job.getJobFile());
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
			String basename = FilenameUtils.removeExtension(job.getJobFile().getName());
			System.out.println("BaseName: " + FilenameUtils.removeExtension(basename));
//			this.unpackDir = new File(getWorkingDir(),basename+ ".slice");
			job.setGCodeFile(findGcodeFile(extractDirectory));//new File(getUnpackDir(),basename + ".gcode");
//			System.out.println("Unpacked Dir: " + getUnpackDir().getAbsolutePath());
//			System.out.println("Exists: " + getUnpackDir().exists());
			//System.out.println("GCode file: " + getGCode().getAbsolutePath());
			//System.out.println("Exists: " + getGCode().exists());
		} catch (IOException ioe) {
			throw ioe;
		} finally {
			zipFile.close();
		}

	}

}