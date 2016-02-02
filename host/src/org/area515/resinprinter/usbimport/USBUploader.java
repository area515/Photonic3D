package org.area515.resinprinter.usbimport;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.plugin.Feature;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.services.PrintableService;
import org.area515.util.PrintFileFilter;

public class USBUploader implements Feature {
    private static final Logger logger = LogManager.getLogger();
	private HashMap<String, File> masterRoots;
	private ScheduledFuture<?> future;
	
	private void uploadFromRoot(File root) {
		for (File currentFile : root.listFiles(PrintFileFilter.INSTANCE)) {
			try (InputStream stream = new BufferedInputStream(new FileInputStream(currentFile))) {
				PrintableService.uploadFile(currentFile.getName(), stream, HostProperties.Instance().getUploadDir());
			} catch (IOException e) {
				logger.error("Couldn't upload file:" + currentFile, e);
			}
		}
	}
	
	@Override
	public void start(URI uri) {
		masterRoots = new HashMap<>();
		for (File root : File.listRoots()) {
			masterRoots.put(root.getAbsolutePath(), root);
		}
		
		future = Main.GLOBAL_EXECUTOR.scheduleWithFixedDelay(new Runnable(){
			@Override
			public void run() {
				Map<String, File> negativeList = new HashMap<>();
				negativeList.putAll(masterRoots);
				for (File root : File.listRoots()) {
					File foundItem = negativeList.remove(root.getAbsolutePath());
					if (foundItem == null) {
						uploadFromRoot(root);
						
						//Since we've uploaded all of the files from this root, we don't want to do it again.
						masterRoots.put(root.getAbsolutePath(), root);
					}
				}
				
				//These are the USB drives that were unplugged
				for (String name : negativeList.keySet()) {
					masterRoots.remove(name);
				}
			}
		}, 0, 3, TimeUnit.SECONDS);
	}

	@Override
	public void stop() {
		future.cancel(true);
		future = null;
	}
}
