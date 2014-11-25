package org.area515.resinprinter.server;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

public class HostProperties {

	private static HostProperties m_instance = null;
	public static HostProperties Instance() throws IOException {
		if (m_instance == null) {
			m_instance = new HostProperties();
		}
		return m_instance;
	}
	
	public HostProperties() throws IOException{HostProperties.init();}
	
	static String uploadDir = "";
	public static String getUploadDir(){
		return uploadDir;
	}
	static String printDir = "";
	public static String getWorkingDir(){
		return printDir;
	}
	
	static boolean fakeSerial = false;
	public static boolean getFakeSerial(){return fakeSerial;}
	
	public static void init() throws IOException{
//		workingDir = HostProperties.getHostProperties().getProperty("printdir");		
//		sourceDir = HostProperties.getHostProperties().getProperty("sourcedir");
//		fakeSerial = Boolean.parseBoolean(HostProperties.getHostProperties().getProperty("fakeserial"));
		// Webapp war plugin doesn't cant find my config.properties - hardcoding for now
//		workingDir = "/home/user/CreationWorkshop/Host/workingdir";
//		sourceDir = "/home/user/CreationWorkshop/Host/sourcedir";
//		fakeSerial = true;
		
		printDir = "C:\\nonsync\\personal\\ResinPrinterHost\\printdir";
		uploadDir = "C:\\nonsync\\personal\\ResinPrinterHost\\uploads";
		fakeSerial = true;
		
		if(!new File(printDir).exists()){FileUtils.forceMkdir(new File(printDir));}
		if(!new File(uploadDir).exists()){FileUtils.forceMkdir(new File(uploadDir));}
		
		System.out.println("WorkingDir: " + printDir);
		System.out.println("SourceDir: " + uploadDir);
		System.out.println("FakeSerial: " + fakeSerial);
	}
	
	public static Properties getHostProperties() throws IOException{
		Properties properties = new Properties();
		properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
		return properties;
	}
	
}