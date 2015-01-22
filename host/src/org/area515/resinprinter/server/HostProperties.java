package org.area515.resinprinter.server;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.area515.resinprinter.discover.Advertiser;
import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.services.MachineService;

public class HostProperties {
	private static HostProperties INSTANCE = null;
	private static String MACHINE_EXTENSION = ".machine";
	
	private File machineDir = new File(System.getProperty("user.home"), "Machines");
	private File uploadDir;
	private File printDir;
	private boolean fakeSerial = false;
	private boolean fakedisplay = false;
	private ConcurrentHashMap<String, PrinterConfiguration> configurations;
	private List<Class<Advertiser>> advertisementClasses = new ArrayList<Class<Advertiser>>();
	private Class<SerialCommunicationsPort> serialPortClass;
	
	public synchronized static HostProperties Instance() {
		if (INSTANCE == null) {
			INSTANCE = new HostProperties();
		}
		return INSTANCE;
	}
	
	private HostProperties() {
		String printDirString = null;
		String uploadDirString = null;
		
		InputStream stream = null;
		File configPropertiesInMachinesDirectory = new File(machineDir, "config.properties");
		if (configPropertiesInMachinesDirectory.exists()) {
			try {
				stream = new FileInputStream(configPropertiesInMachinesDirectory);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		if (stream == null) {
			stream = HostProperties.class.getClassLoader().getResourceAsStream("config.properties");
		}
		
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
			fakedisplay = new Boolean(props.getProperty("fakedisplay", "false"));
			for (Entry<Object, Object> currentProperty : props.entrySet()) {
				String currentPropertyString = currentProperty.getKey() + "";
				if (currentPropertyString.startsWith("advertise.")) {
					currentPropertyString = currentPropertyString.replace("advertise.", "");
					if ("true".equalsIgnoreCase(currentProperty.getValue() + "")) {
						try {
							advertisementClasses.add((Class<Advertiser>)Class.forName(currentPropertyString));
						} catch (ClassNotFoundException e) {
							System.out.println("Failed to load advertiser:" + currentPropertyString);
						}
					}
				}
			}
			
			String serialCommClass = null;
			try {
				serialCommClass = props.getProperty("SerialCommunicationsImplementation", "org.area515.resinprinter.serial.RXTXSynchronousReadBasedCommPort");
				serialPortClass = (Class<SerialCommunicationsPort>)Class.forName(serialCommClass);
			} catch (ClassNotFoundException e) {
				System.out.println("Failed to load SerialCommunicationsImplementation:" + serialCommClass);
			}
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
	
	public boolean getFakeDisplay(){
		return fakedisplay;
	}
	
	public Class<SerialCommunicationsPort> getSerialCommunicationsClass() {
		return serialPortClass;
	}
	
	public List<Class<Advertiser>> getAdvertisers() {
		return advertisementClasses;
	}
	
	public List<PrinterConfiguration> getPrinterConfigurations() {
		if (configurations != null) {
			return new ArrayList<PrinterConfiguration>(configurations.values());
		}
		
		configurations = new ConcurrentHashMap<String, PrinterConfiguration>();

		if (!machineDir.exists()) {
			if (!machineDir.mkdirs()) {
				throw new IllegalArgumentException("Couldn't create machine directory:" + machineDir);
			}
			
			MachineService.INSTANCE.createPrinter("Autodetected Printer", DisplayManager.LAST_AVAILABLE_DISPLAY, SerialManager.FIRST_AVAILABLE_PORT);
		}
		
		File machineFiles[] = machineDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(MACHINE_EXTENSION)) {
					return true;
				}
				
				return false;
			}
		});
		
		for (File currentFile : machineFiles) {
			JAXBContext jaxbContext;
			try {
				jaxbContext = JAXBContext.newInstance(PrinterConfiguration.class);
				Unmarshaller jaxbUnMarshaller = jaxbContext.createUnmarshaller();
				//jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				
				PrinterConfiguration configuration = (PrinterConfiguration)jaxbUnMarshaller.unmarshal(currentFile);
				configuration.setName(currentFile.getName().replace(MACHINE_EXTENSION, ""));
	
				//We do not want to start the printer here
				//PrinterManager.Instance().createPrinter(configuration);
				configurations.put(configuration.getName(), configuration);
				System.out.println("Created printer configuration for:" + configuration);
			} catch (JAXBException e) {
				e.printStackTrace();
			}
		}
		
		return new ArrayList<PrinterConfiguration>(configurations.values());
	}	
	
	public PrinterConfiguration getPrinterConfiguration(String name) {
		getPrinterConfigurations();
		
		return configurations.get(name);
	}
	
	private void saveConfigurations() {
		for (PrinterConfiguration currentConfiguration : configurations.values()) {
			JAXBContext jaxbContext;
			try {
				jaxbContext = JAXBContext.newInstance(PrinterConfiguration.class);
				Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
				jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				
				File machineFile = new File(machineDir, currentConfiguration.getName() + MACHINE_EXTENSION);
				jaxbMarshaller.marshal(currentConfiguration, machineFile);
			} catch (JAXBException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void addPrinterConfiguration(PrinterConfiguration configuration) throws AlreadyAssignedException {
		getPrinterConfigurations();

		PrinterConfiguration otherConfiguration = configurations.putIfAbsent(configuration.getName(), configuration);
		if (otherConfiguration != null) {
			throw new AlreadyAssignedException("There is already a printer called:" + configuration.getName(), (Printer)null);
		}
		
		saveConfigurations();
	}
	
	public void removePrinterConfiguration(PrinterConfiguration configuration) throws InappropriateDeviceException {
		getPrinterConfigurations();

		File machine = new File(machineDir, configuration.getName() + MACHINE_EXTENSION);
		if (!machine.exists()) {
			throw new InappropriateDeviceException("Printer configuration doesn't exist for:" + configuration.getName());
		}
		
		if (!machine.isFile()) {
			throw new InappropriateDeviceException("Printer configuration wasn't a file:" + configuration.getName());
		}
		
		if (!machine.delete()) {
			throw new InappropriateDeviceException("Couldn't delete configuration for:" + configuration.getName());
		}
		
		configurations.remove(configuration.getName());
	}
}