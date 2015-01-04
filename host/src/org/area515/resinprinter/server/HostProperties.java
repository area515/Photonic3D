package org.area515.resinprinter.server;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

public class HostProperties {
	private static HostProperties INSTANCE = null;
	private File uploadDir;
	private File printDir;
	private boolean fakeSerial = false;
	
	public synchronized static HostProperties Instance() {
		if (INSTANCE == null) {
			INSTANCE = new HostProperties();
		}
		return INSTANCE;
	}
	
	public HostProperties() {
		String printDirString = null;
		String uploadDirString = null;
		
		InputStream stream = HostProperties.class.getClassLoader().getResourceAsStream("config.properties");
		if (stream != null) {
			Properties props = new Properties();
			try {
				props.load(stream);
			} catch (IOException e) {
				throw new IllegalArgumentException("Couldn't load config.properties file", e);
			}

			printDirString = props.getProperty("printdir");
			uploadDirString = props.getProperty("uploaddir");
			fakeSerial = new Boolean(props.getProperty("fakeserial", "false"));
		}
		
		if (printDirString == null) {
			printDir = new File(System.getProperty("java.io.tmpdir"), "printdir");
		} else {
			printDir = new File(printDirString);
		}
		
		if (uploadDirString == null) {
			uploadDir = new File(System.getProperty("java.io.tmpdir"), "uploaddir");
		} else {
			uploadDir = new File(uploadDirString);
		}
		
		if(!printDir.exists()){
			try {
				FileUtils.forceMkdir(printDir);
			} catch (IOException e) {
				throw new IllegalArgumentException("Couldn't create print directory", e);
			}
		}
		
		if(!uploadDir.exists()){
			try {
				FileUtils.forceMkdir(uploadDir);
			} catch (IOException e) {
				throw new IllegalArgumentException("Couldn't create upload directory", e);
			}
		}
		
		System.out.println("WorkingDir: " + printDir);
		System.out.println("SourceDir: " + uploadDir);
		System.out.println("FakeSerial: " + fakeSerial);
	}

	public File getUploadDir(){
		return uploadDir;
	}
	public File getWorkingDir(){
		return printDir;
	}
	
	public boolean getFakeSerial(){
		return fakeSerial;
	}
}