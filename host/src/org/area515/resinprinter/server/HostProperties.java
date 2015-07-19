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

import javax.ws.rs.core.NewCookie;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.area515.resinprinter.discover.Advertiser;
import org.area515.resinprinter.display.AlreadyAssignedException;
import org.area515.resinprinter.display.DisplayManager;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.notification.Notifier;
import org.area515.resinprinter.printer.MachineConfig;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.printer.PrinterConfiguration;
import org.area515.resinprinter.printer.SlicingProfile;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.resinprinter.services.MachineService;

public class HostProperties {
	public static String FULL_RIGHTS = "adminRole";

	private static String PROFILES_EXTENSION = ".slicing";
	public File PROFILES_DIR = new File(System.getProperty("user.home"), "Profiles");
	private static String MACHINE_EXTENSION = ".machine";
	public File MACHINE_DIR = new File(System.getProperty("user.home"), "Machines");
	private static String PRINTER_EXTENSION = ".printer";
	private File printerDir = new File(System.getProperty("user.home"), "3dPrinters");

	private static HostProperties INSTANCE = null;
	private File uploadDir;
	private File printDir;
	private boolean fakeSerial = false;
	private boolean fakedisplay = false;
	private ConcurrentHashMap<String, PrinterConfiguration> configurations;
	private List<Class<Advertiser>> advertisementClasses = new ArrayList<Class<Advertiser>>();
	private List<Class<Notifier>> notificationClasses = new ArrayList<Class<Notifier>>();
	private List<PrintFileProcessor> printFileProcessors = new ArrayList<PrintFileProcessor>();
	private Class<SerialCommunicationsPort> serialPortClass;
	private int versionNumber;
	private String deviceName;
	private String manufacturer;
	private Properties configurationProperties = new Properties();
	
	//SSL settings:
	private boolean useSSL;
	private int printerHostPort;
	private File keystoreFile;
	private String keypairPassword;
	private String keystorePassword;
	private String securityRealmName;
	//Optional SSL settings:
	private String externallyAccessableName;
	
	//This is for authentication
	private String clientUsername;
	private String clientPassword;
	
	//This is for Media
	private String streamingCommand;
	private String imagingCommand;
	
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
		
		if (!PROFILES_DIR.exists() && !PROFILES_DIR.mkdirs()) {
			System.out.println("Couldn't make profiles directory. No write access or disk full?" );
			throw new IllegalArgumentException("Couldn't make profiles directory. No write access or disk full?");
		}
		
		if (!MACHINE_DIR.exists() && !MACHINE_DIR.mkdirs()) {
			System.out.println("Couldn't make machine directory. No write access or disk full?" );
			throw new IllegalArgumentException("Couldn't make machine directory. No write access or disk full?");
		}

		File configPropertiesInPrintersDirectory = new File(printerDir, "config.properties");
		if (configPropertiesInPrintersDirectory.exists()) {
			try {
				stream = new FileInputStream(configPropertiesInPrintersDirectory);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		if (stream == null) {
			stream = HostProperties.class.getClassLoader().getResourceAsStream("config.properties");
		}
		
		if (stream != null) {
			try {
				configurationProperties.load(stream);
			} catch (IOException e) {
				throw new IllegalArgumentException("Couldn't load config.properties file", e);
			}

			printDirString = configurationProperties.getProperty("printdir");
			uploadDirString = configurationProperties.getProperty("uploaddir");
			fakeSerial = new Boolean(configurationProperties.getProperty("fakeserial", "false"));
			fakedisplay = new Boolean(configurationProperties.getProperty("fakedisplay", "false"));
			
			//This loads advertisers
			for (Entry<Object, Object> currentProperty : configurationProperties.entrySet()) {
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
			
			//This loads notifiers
			for (Entry<Object, Object> currentProperty : configurationProperties.entrySet()) {
				String currentPropertyString = currentProperty.getKey() + "";
				if (currentPropertyString.startsWith("notify.")) {
					currentPropertyString = currentPropertyString.replace("notify.", "");
					if ("true".equalsIgnoreCase(currentProperty.getValue() + "")) {
						try {
							notificationClasses.add((Class<Notifier>)Class.forName(currentPropertyString));
						} catch (ClassNotFoundException e) {
							System.out.println("Failed to load notifier:" + currentPropertyString);
						}
					}
				}
			}
			
			//This loads print file processors
			for (Entry<Object, Object> currentProperty : configurationProperties.entrySet()) {
				String currentPropertyString = currentProperty.getKey() + "";
				if (currentPropertyString.startsWith("printFileProcessor.")) {
					currentPropertyString = currentPropertyString.replace("printFileProcessor.", "");
					if ("true".equalsIgnoreCase(currentProperty.getValue() + "")) {
						try {
							PrintFileProcessor processor = ((Class<PrintFileProcessor>)Class.forName(currentPropertyString)).newInstance();
							printFileProcessors.add(processor);
						} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
							System.out.println("Failed to load PrintFileProcessor:" + currentPropertyString);
						}
					}
				}
			}
			
			
			String serialCommClass = null;
			try {
				serialCommClass = configurationProperties.getProperty("SerialCommunicationsImplementation", "org.area515.resinprinter.serial.RXTXSynchronousReadBasedCommPort");
				serialPortClass = (Class<SerialCommunicationsPort>)Class.forName(serialCommClass);
			} catch (ClassNotFoundException e) {
				System.out.println("Failed to load SerialCommunicationsImplementation:" + serialCommClass);
			}
			
			//Here are all of the server configuration settings
			String keystoreFilename = configurationProperties.getProperty("keystoreFilename");
			if (keystoreFilename != null) {
				keystoreFile = new File(keystoreFilename);
			}
			useSSL = new Boolean(configurationProperties.getProperty("useSSL", "false"));
			printerHostPort = new Integer(configurationProperties.getProperty("printerHostPort", useSSL?"443":"9091"));
			externallyAccessableName = configurationProperties.getProperty("externallyAccessableName");
			keypairPassword = configurationProperties.getProperty("keypairPassword");
			keystorePassword = configurationProperties.getProperty("keystorePassword");
			deviceName = configurationProperties.getProperty("deviceName", "3D Multiprint Host");
			manufacturer = configurationProperties.getProperty("manufacturer", "Wes & Sean");
			securityRealmName = configurationProperties.getProperty("securityRealmName", "SecurityRealm");
			clientUsername = configurationProperties.getProperty(securityRealmName + ".clientUsername", "");
			clientPassword = configurationProperties.getProperty(securityRealmName + ".clientPassword", "");
			streamingCommand = configurationProperties.getProperty("streamingCommand");
			imagingCommand = configurationProperties.getProperty("imagingCommand");
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
		
		File versionFile = new File("build.number");
		if (versionFile.exists()) {
			Properties newProperties = new Properties();
			try {
				newProperties.load(new FileInputStream(versionFile));
				versionNumber = Integer.valueOf((String)newProperties.get("build.number"));
			} catch (IOException e) {
				System.out.println("Version file is missing:" + versionFile);
			}
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

	public Properties getConfigurationProperties() {
		return configurationProperties;
	}

	public String getClientUsername() {
		return clientUsername;
	}

	public String getClientPassword() {
		return clientPassword;
	}

	public String getSecurityRealmName() {
		return securityRealmName;
	}

	public int getVersionNumber() {
		return versionNumber;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public int getPrinterHostPort() {
		return printerHostPort;
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
	
	public String getKeypairPassword() {
		return keypairPassword;
	}

	public String getKeystorePassword() {
		return keystorePassword;
	}

	public Class<SerialCommunicationsPort> getSerialCommunicationsClass() {
		return serialPortClass;
	}
	
	public List<Class<Advertiser>> getAdvertisers() {
		return advertisementClasses;
	}
	
	public List<Class<Notifier>> getNotifiers() {
		return notificationClasses;
	}
	
	public List<PrintFileProcessor> getPrintFileProcessors() {
		return printFileProcessors;
	}
	
	public boolean isUseSSL() {
		return useSSL;
	}

	public String getExternallyAccessableName() {
		return externallyAccessableName;
	}

	public File getKeystoreFile() {
		return keystoreFile;
	}

	public String getStreamingCommand() {
		return streamingCommand;
	}
	
	public String getImagingCommand() {
		return imagingCommand;
	}

	public List<PrinterConfiguration> getPrinterConfigurations() {
		if (configurations != null) {
			return new ArrayList<PrinterConfiguration>(configurations.values());
		}
		
		configurations = new ConcurrentHashMap<String, PrinterConfiguration>();

		if (!printerDir.exists()) {
			if (!printerDir.mkdirs()) {
				throw new IllegalArgumentException("Couldn't create printer directory:" + printerDir);
			}
			
			MachineService.INSTANCE.createPrinter("Autodetected Printer", DisplayManager.LAST_AVAILABLE_DISPLAY, SerialManager.FIRST_AVAILABLE_PORT);
		}
		
		File machineFiles[] = printerDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(PRINTER_EXTENSION)) {
					return true;
				}
				
				return false;
			}
		});
		
		for (File currentFile : machineFiles) {
			JAXBContext jaxbContext;
			try {
				jaxbContext = JAXBContext.newInstance(PrinterConfiguration.class, MachineConfig.class, SlicingProfile.class);
				Unmarshaller jaxbUnMarshaller = jaxbContext.createUnmarshaller();
				
				PrinterConfiguration configuration = (PrinterConfiguration)jaxbUnMarshaller.unmarshal(currentFile);
				configuration.setName(currentFile.getName().replace(PRINTER_EXTENSION, ""));
	
				configuration.setMachineConfig((MachineConfig)jaxbUnMarshaller.unmarshal(new File(MACHINE_DIR, configuration.getMachineConfigName() + MACHINE_EXTENSION)));
				configuration.setSlicingProfile((SlicingProfile)jaxbUnMarshaller.unmarshal(new File(PROFILES_DIR, configuration.getSlicingProfileName() + PROFILES_EXTENSION)));
				
				//We do not want to start the printer here
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

				MachineConfig machineConfig = currentConfiguration.getMachineConfig();
				SlicingProfile slicingProfile = currentConfiguration.getSlicingProfile();
				
				File machineFile = new File(MACHINE_DIR, currentConfiguration.getMachineConfigName() + MACHINE_EXTENSION);
				jaxbMarshaller.marshal(machineConfig, machineFile);

				File slicingFile = new File(PROFILES_DIR, currentConfiguration.getSlicingProfileName() + PROFILES_EXTENSION);
				jaxbMarshaller.marshal(slicingProfile, slicingFile);

				File printerFile = new File(printerDir, currentConfiguration.getName() + PRINTER_EXTENSION);
				jaxbMarshaller.marshal(new PrinterConfiguration(currentConfiguration.getMachineConfigName(), currentConfiguration.getSlicingProfileName()), printerFile);
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

		File machine = new File(printerDir, configuration.getName() + PRINTER_EXTENSION);
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