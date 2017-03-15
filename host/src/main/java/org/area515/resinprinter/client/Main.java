package org.area515.resinprinter.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.SplashScreen;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.area515.resinprinter.client.SubnetScanner.Box;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDevice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;

public class Main {
	public static final String PRINTER_TYPE = "3DPrinterHost";
	public static final String PRINTERS_DIRECTORY = "printers";
	public static final String BRANCH = "master";
	public static String REPO = "area515/Photonic3D";
	
	private static Set<PrintableDevice> foundDevices = new HashSet<PrintableDevice>();
	private static long maxLengthToWait = 5000;
	private static long maxLengthToWaitForAll = 7000;
	
	public static class StatusPanel extends JLabel {
		private static final long serialVersionUID = 6639519775722442260L;
		
		private ScreenUpdater updater;
		
		public StatusPanel(String text, Icon icon, int horizontalAlignment) {
			super(text, icon, horizontalAlignment);
		}

		public void setScreenUpdater(ScreenUpdater updater) {
			this.updater = updater;
		}
		
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (updater != null) {
				updater.updateGraphics((Graphics2D)g);
			}
		}
	}
	
	public static class ScreenUpdater {
		private Graphics2D graphics;
		private SplashScreen screen;
		private Rectangle outerBounds;
		private String showText;
		private StatusPanel statusPanel;
		private Color textColor;
		
		private ScreenUpdater(SplashScreen splashScreen, Color textColor) {
			this.graphics = splashScreen.createGraphics();
			this.screen = splashScreen;
			this.outerBounds = graphics.getDeviceConfiguration().getBounds();
			this.textColor = textColor;
		}
		
		private ScreenUpdater(StatusPanel statusPanel, Color textColor) {
			this.outerBounds = statusPanel.getBounds();
			this.statusPanel = statusPanel;
			this.statusPanel.setScreenUpdater(this);
			this.textColor = textColor;
		}
		
		public void showProgress(String showText) {
			this.showText = showText;
			if (graphics != null) {
				graphics.setBackground(new Color(0, true));
				graphics.clearRect(0, 0, outerBounds.width, outerBounds.height);
				
				updateGraphics(graphics);
			}
			updateScreen();
		}
		
		public void updateGraphics(Graphics2D graphics) {
			if (showText == null) {
				return;
			}
			
			graphics.setColor(textColor);
			Rectangle2D bounds = graphics.getFontMetrics().getStringBounds(showText, graphics);
			graphics.drawString(showText, (int)(outerBounds.width / 2 - bounds.getWidth() / 2), (int)bounds.getHeight());
		}
		
		public void close() {
			if (statusPanel != null) {
				javax.swing.SwingUtilities.getWindowAncestor(statusPanel).dispose();
			}
			if (screen != null && screen.isVisible()) {
				screen.close();
			}
		}
		
		public void updateScreen() {
			if (screen != null) {
				screen.update();
			}
			if (statusPanel != null) {
				statusPanel.updateUI();
			}
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PrinterEntry {
		private String name;
		
		private String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public String toString() {
			int prefixIndex = name.lastIndexOf(".");
			if (prefixIndex < 0) {
				return name;
			}
			
			return name.substring(0, prefixIndex);
		}
	}
	
	public static class PrintableDevice {
		public Device device;
		
		public PrintableDevice(Device device) {
			this.device = device;
		}
		
		public String toString() {
			return device.getDisplayString() + " (" + device.getDetails().getPresentationURI() + ")";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((device == null) ? 0 : device.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PrintableDevice other = (PrintableDevice) obj;
			if (device == null) {
				if (other.device != null)
					return false;
			} else if (!device.equals(other.device))
				return false;
			return true;
		}
	}
	
	private static char[] getPassword(String title, String prompt) {
		JPanel panel = new JPanel(new BorderLayout());
		JLabel label = new JLabel(prompt);
		JPasswordField pass = new JPasswordField(10);
		panel.add(label, BorderLayout.CENTER);
		panel.add(pass, BorderLayout.SOUTH);
		String[] options = new String[]{"OK", "Cancel"};
		int option = JOptionPane.showOptionDialog(null, panel, title, JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		if(option == 0) {
		    char[] password = pass.getPassword();
		    return password;
		}
		
		return null;
	}
	
	private static boolean findSuccessLine(String[] lines, String matchesLine) {
		for (String line : lines) {
			if (line.matches(matchesLine)) {
				return true;
			}
		}
		
		return false;
	}
	
	private static void writeOutput(String[] lines) {
		for (String line : lines) {
			System.out.println(line);
		}
	}
	
	public static SSHClient raspberryPiSpecificChanges(SSHClient client, JOptionPane progress) {
		progress.setMessage("Enabling camera...");
		client.send("sed /boot/config.txt -i -e \"s/^start_x/#start_x/\"");
		client.send("sed /boot/config.txt -i -e \"s/^gpu_mem/#gpu_mem/\"");
		client.send("sed /boot/config.txt -i -e \"s/^startx/#startx/\"");
		client.send("sed /boot/config.txt -i -e \"s/^fixup_file/#fixup_file/\"");
		client.send("echo \"\nstart_x=1\ngpu_mem=128\n\" >> /boot/config.txt");

		progress.setMessage("Setting up user...");
		client.send("rm -f /etc/profile.d/raspi-config.sh");
		client.send("sed /boot/config.txt -i -e \"s/^#\\(.*\\)#\\s*RPICFG_TO_ENABLE\\s*/\\1/\"");
		client.send("sed /boot/config.txt -i -e \"/#\\s*RPICFG_TO_DISABLE/d\"");
		
		progress.setMessage("Extending disk partition...");
		String[] rootPartition = client.send("readlink /dev/root");
		if (!rootPartition[0].startsWith("mmcblk0p")) {
			System.out.println("rootPartition:" + Arrays.toString(rootPartition));
			System.out.println("/dev/root either doesn't exist or isn't an SD card.");
			return client;
		}
		rootPartition[0] = rootPartition[0].substring(8).trim();
		if (!rootPartition[0].equals("2")) {
			System.out.println("rootPartition:" + Arrays.toString(rootPartition));
			System.out.println("/dev/root either doesn't exist or isn't an SD card.");
			return client;
		}
		String[] lastPartition = client.send("parted /dev/mmcblk0 -ms unit s p | tail -n 1 | cut -f 1 -d:");
		lastPartition[0] = lastPartition[0].trim();
		if (!lastPartition[0].equals(rootPartition[0])) {
			System.out.println("rootPartition:" + Arrays.toString(rootPartition));
			System.out.println("lastPartition:" + Arrays.toString(lastPartition));
			System.out.println("/dev/root must be the last partition in order to expand it.");
			return client;
		}
		
		String[] startOfPartition = client.send("parted /dev/mmcblk0 -ms unit s p | grep \"^" + lastPartition[0] + "\" | cut -f 2 -d:");
		System.out.println("startOfPartition");
		writeOutput(startOfPartition);
		startOfPartition[0] = startOfPartition[0].trim();
		String[] fdiskReturn = client.send("echo \"\np\nd\n" + rootPartition[0] + "\nn\np\n" + rootPartition[0] + "\n" + startOfPartition[0] + "\n\np\nw\n\" | fdisk /dev/mmcblk0");
		System.out.println("fdiskReturn");
		writeOutput(fdiskReturn);
		
		//Execute this in a different thread because I'm not sure if I can ensure that we will get a prompt back by the time our shell is disconnected by the OS.
		final SSHClient threadClient = client;
		long start = System.currentTimeMillis();
		Executors.newSingleThreadExecutor().submit(new Runnable() {
			@Override
			public void run() {
				threadClient.send("reboot");
			}
		});

		try {
			//We need to sleep long enough for the reboot to execute so we don't immediately connect to a doomed shell.
			//This is perhaps the first time I've found that a sleep "should" be used to solve a race condition, and I'm
			//still not very convinced. Maybe I could wait for: "The system is going down for reboot NOW!" to be our 
			//trigger to attempt a new SSH connection. However, what if we were able to sneak a quick connection on our
			//existing doomed shell? Sleep doesn't seem too bad now huh?
			Thread.sleep(3000);
		} catch (InterruptedException e1) {}
		progress.setMessage("Restarting device...");
		client.disconnect();
		
		String host = client.getHost();
		while (System.currentTimeMillis() - start < 60000) {
			client = new SSHClient();
			try {
				client.connect("pi", "raspberry", host, 22);
				break;
			} catch (JSchException e) {
				System.out.println("Reconnect unsuccessful:"  + e.getMessage());
				client = null;
			}
		}
		
		System.out.println("Found client:" + client + " after:" + (System.currentTimeMillis() - start));
		if (client == null) {
			return null;
		}
		
		progress.setMessage("Finishing partition resize...");
		System.out.println("sudo required:" + client.sudoIfNotRoot());
		String[] resizeResponse = client.send("resize2fs /dev/root");
		System.out.println("resizeResponse");
		writeOutput(resizeResponse);
		return client;
	}
	
	public static boolean installPrinterProfile(Box installToBox) {
		HttpClientBuilder builder = HttpClientBuilder.create();
		CloseableHttpClient httpClient = builder.build();
		
	    // specify the host, protocol, and port
		HttpHost restTarget = new HttpHost("api.github.com", 443, "https");
		HttpHost cwhTarget = new HttpHost(installToBox.getName(), 9091, "http");
		HttpGet getRequest = new HttpGet("/repos/" + REPO + "/contents/host/" + PRINTERS_DIRECTORY + "?ref=" + BRANCH);
		
		PrinterEntry[] printers = null;
		try {
			System.out.println("Request:" + getRequest);
			HttpResponse httpResponse = httpClient.execute(restTarget, getRequest);
			System.out.println("Response:" + httpResponse.getStatusLine());
			HttpEntity entity = httpResponse.getEntity();
			ObjectMapper mapper = new ObjectMapper(new JsonFactory());//EntityUtils.toString(entity)entity.consumeContent();
			printers = mapper.readValue(entity.getContent(), new TypeReference<PrinterEntry[]>(){});
		} catch (IOException e) {
			e.printStackTrace();
			try {httpClient.close();} catch (IOException e1) {}
			return false;
		}
		
		PrinterEntry selectedPrinter = (PrinterEntry)JOptionPane.showInputDialog(null, 
				"Please choose a printer profile that you would like to install.",
				"Install a Printer?",
				JOptionPane.QUESTION_MESSAGE,
				null,
				printers,
				printers[0]);
		if (selectedPrinter == null) {
			try {httpClient.close();} catch (IOException e1) {}
			return false;
		}
		
		//https://raw.githubusercontent.com/area515/Creation-Workshop-Host/master/host/.classpath
		HttpHost rawTarget = new HttpHost("raw.githubusercontent.com", 443, "https");;
		try {
			getRequest = new HttpGet("/" + REPO + "/Creation-Workshop-Host/master/host/" + PRINTERS_DIRECTORY + "/" + URLEncoder.encode(selectedPrinter.getName(), "ASCII").replaceAll("\\+", "%20"));
		} catch (UnsupportedEncodingException e2) {
			e2.printStackTrace();
			return false;
		}
			 
		try {
			System.out.println("Request:" + getRequest);
			HttpResponse httpResponse = httpClient.execute(rawTarget, getRequest);
			System.out.println("Response:" + httpResponse.getStatusLine());
			HttpEntity printerJsonOutput = httpResponse.getEntity();
			String json = EntityUtils.toString(printerJsonOutput);
			
			HttpPost installNewPrinterPost = new HttpPost("/services/printers/save");
			StringEntity printerJsonInput = new StringEntity(json);
			printerJsonInput.setContentType("application/json");
			installNewPrinterPost.setEntity(printerJsonInput);
			System.out.println("Request:" + installNewPrinterPost);
			CloseableHttpResponse response = httpClient.execute(cwhTarget, installNewPrinterPost);
			System.out.println("Response:" + httpResponse.getStatusLine());
			return response.getStatusLine().getStatusCode() == 200;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {httpClient.close();} catch (IOException e1) {}
		}
	}
	
	public static boolean performInstall(Box box, String username, String oldPassword) throws IOException, JSchException  {
		System.out.println("User chose install on:" + box);
		
		final JOptionPane installOptionPane = new JOptionPane("Installing Photonic3D...", JOptionPane.INFORMATION_MESSAGE, JOptionPane.CANCEL_OPTION, null, new String[]{"Cancel"}, "Cancel");
		JDialog installPane = null;
		installPane = new JDialog();
		installPane.setTitle("Installing Photonic3D");
		installPane.setResizable(false);
		installPane.setAlwaysOnTop(true);
		installPane.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		installPane.add(installOptionPane);
		installPane.pack();
		installPane.setLocationRelativeTo(null);
		installPane.setVisible(true);

		SSHClient client = new SSHClient();
		try {
			installOptionPane.setMessage("Connecting to printer...");
			client.connect(username, oldPassword, box.getName(), 22);
			System.out.println("sudo required:" + client.sudoIfNotRoot());
			client.send("rm start.sh");
						
			//A couple of specialty items if it's a Raspberry Pi
			if (box.isRaspberryPi()) {
				client = raspberryPiSpecificChanges(client, installOptionPane);
				if (client == null) {
					installPane.setVisible(false);
					JOptionPane.showConfirmDialog(null, "Lost contact with the device after attempting to extend the disk partition.", "Installation Failed", JOptionPane.ERROR);
					return false;
				}
			}
			
			installOptionPane.setMessage("Downloading installation scripts...");
			String[] output = client.send("wget https://github.com/" + REPO + "/raw/master/host/bin/start.sh");
			if (!findSuccessLine(output, "(?s:.*start.sh.*saved.*)")) {
				writeOutput(output);
				throw new IOException("This device can't seem to reach the internet.");
			}
			output = client.send("chmod 777 *.sh");
			
			installOptionPane.setMessage("Performing installation...");
			output = client.send("./start.sh");
			if (!findSuccessLine(output, "(?s:.*Starting printer host server.*)")) {
				writeOutput(output);
				throw new IOException("There was a problem installing Photonic3D. Please refer to logs.");
			}
			
			if (box.isRaspberryPi()) {
				installPane.setVisible(false);
				
				char[] password = getPassword("Please Secure This Device", 
						"<html>This device was setup with a default password<br>" + 
						"from the manufacturer. It is not advisable that<br>" + 
						"you keep this password. Please enter another<br>" + 
						"password to help secure your printer.<html>");
				if (password != null) {
					output = client.send("echo 'pi:" + new String(password) + "' | chpasswd");
					//JOptionPane.showMessageDialog(null, "Failed to set password. " + output[0], "Bad Password", JOptionPane.WARNING_MESSAGE);
				}
			}
			
			installPrinterProfile(box);
			return true;

		} finally {
			installPane.setVisible(false);
			client.disconnect();
		}
	}
	
	private static StatusPanel showSplashscreenIfJWrapperDoesntSupportIt(String splash) throws MalformedURLException {
		StatusPanel panel = new StatusPanel("", new ImageIcon(Main.class.getClassLoader().getResource(splash)), SwingConstants.CENTER);
		JFrame window = new JFrame();
		window.setUndecorated(true);
		window.setResizable(false);
		window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		window.setFocusable(false);
		window.getContentPane().add(panel);
		window.pack();
		window.setVisible(true);
		window.toFront();
		window.setLocationRelativeTo(null);
		return panel;
	}
	
	/**
	 * return 0 for printer found
	 * returns -1 for user cancelled operation
	 * returns -2 for error during operation
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws MalformedURLException {
		PosixParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("h", "help", false, "Shows this help screen.");
		options.addOption("r", "repo", true, "If the user chooses to install Photonic3d on a remote device, then Photonic3d will be installed from the following github repo");
		options.addOption("w", "jwrappersplash", true, "JWrapper doesn't support the Java splash screen so this client will build it's own and use the parameter sent to this method");
		options.addOption("t", "progresstextcolor", true, "Color [Red(0-255),Green(0-255),Blue(0-255),Alpha(0-255)] of text on splash screen to show printers as they are found on the network");
		CommandLine commands;
		try {
			commands = parser.parse(options, args);
		} catch (ParseException e2) {
			e2.printStackTrace();
			throw new IllegalArgumentException("Couldn't understand arguments sent in command line");
		}
		
		if (commands.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java org.area515.resinprinter.client.Main [options]", options);
			return;
		}
		
		if (commands.hasOption("r")) {
			REPO = commands.getOptionValue("r");
		}
		
		Color textColor = null;
		if (commands.hasOption("t")) {
			String colors[] = commands.getOptionValue("t").split(",");
			if (colors.length != 4) {
				System.out.println("TextColor must be in format r,g,b,a (0-255,0-255,0-255,0-255)");
				System.exit(-2);
			}
			textColor = new Color(Integer.valueOf(colors[0]),Integer.valueOf(colors[1]),Integer.valueOf(colors[2]),Integer.valueOf(colors[3]));
		} else {
			textColor = Color.WHITE;
		}
		
		ScreenUpdater updater = null;
		if (commands.hasOption("w")) {
			StatusPanel manualSplash = showSplashscreenIfJWrapperDoesntSupportIt(commands.getOptionValue("w"));
			updater = new ScreenUpdater(manualSplash, textColor);
		} else {
			SplashScreen splashScreen = SplashScreen.getSplashScreen();
			if (splashScreen != null) {
				updater = new ScreenUpdater(splashScreen, textColor);
			}
		}
		final ScreenUpdater finalUpdater = updater;
		
		boolean installCompletedOnThisLoopIteration = false;
		boolean userHasbeenAskedToInstall = false;
		SubnetScanner scanner = new SubnetScanner();
		try {
			scanner.startSubnetScan();
		} catch (SocketException e) {
			e.getStackTrace();//No network available?
		}
		
		JDialog searchPane = null;
		final JOptionPane searchOptionPane = new JOptionPane("Searching for all 3d printers on network...", JOptionPane.INFORMATION_MESSAGE, JOptionPane.CANCEL_OPTION, null, new String[]{"Cancel"}, "Cancel");

		do {
			if (installCompletedOnThisLoopIteration) {
				installCompletedOnThisLoopIteration = false;
			}
			
			final CountDownLatch waitForURLFound = new CountDownLatch(1);
			Thread searchThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						UpnpService upnpService = new UpnpServiceImpl();
						upnpService.getControlPoint().search(new STAllHeader());
						long timeStarted = System.currentTimeMillis();
						while ((foundDevices.size() > 0 && System.currentTimeMillis() - timeStarted < maxLengthToWait) ||
							   (foundDevices.size() == 0 && System.currentTimeMillis() - timeStarted < maxLengthToWaitForAll)) {
							if (searchOptionPane.getValue() != null && searchOptionPane.getValue().equals("Cancel")) {
								System.exit(-1);
							}
							
							Collection<RemoteDevice> devices = upnpService.getRegistry().getRemoteDevices();
							for (Device currentDevice : devices) {
								if (currentDevice.getType().getType().equals(PRINTER_TYPE)) {
									foundDevices.add(new PrintableDevice(currentDevice));
									System.out.println("Found printer URL here:" + currentDevice.getDetails().getPresentationURI());
									if (finalUpdater != null) {
										finalUpdater.showProgress("Found printer: " + currentDevice.getDisplayString());
									}
								}
							}
							
							Thread.currentThread().sleep(200);
						}
	
						waitForURLFound.countDown();
						upnpService.shutdown();
					} catch (Throwable e) {
						if (foundDevices.size() == 0) {
							e.printStackTrace();
							System.exit(-2);
						}
					}
				}
			});
			searchThread.start();
			
			try {
				waitForURLFound.await(maxLengthToWaitForAll + 2000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e1) {
				//Unlikely to happen
			}
			
			if (foundDevices.size() == 0 && !userHasbeenAskedToInstall) {
				userHasbeenAskedToInstall = true;
				try {
					List<Box> boxes = scanner.waitForDevicesWithPossibleRemoteInstallCapability();
					if (finalUpdater != null) {
						finalUpdater.close();
					}
					if (boxes.size() > 0) {
						Box box = (Box)JOptionPane.showInputDialog(null, 
						        "I couldn't find Photonic3D installed on your network.\n"
						        + "I did find place(s) where I might be able to install it.\n"
						        + "Choose any of the following locations to install Photonic3D.\n"
						        + "Click 'Cancel' if you've already installed Photonic3D.", 
						        "Install Photonic3D?",
						        JOptionPane.QUESTION_MESSAGE,
						        null,
						        boxes.toArray(),
						        boxes.iterator().next());
						if (box != null) {
							try {
								if (box.isRaspberryPi()) {
									if (!performInstall(box, "pi", "raspberry")) {
										System.exit(-1);
									}
									installCompletedOnThisLoopIteration = true;
								} else {
									JOptionPane.showConfirmDialog(null, "Installation on this device is not yet supported");
								}
							} catch (JSchException | IOException e) {
								e.printStackTrace();
								JOptionPane.showConfirmDialog(null, e.getMessage(), "Unable To Install Photonic3D", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
								System.exit(-2);
							}
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (!installCompletedOnThisLoopIteration) {
					if (finalUpdater != null) {
						finalUpdater.close();
					}
					System.out.println("3d printer not found after waiting:" + maxLengthToWaitForAll);
					JOptionPane optionPane = new JOptionPane();
					optionPane.setMessage("Couldn't find your printer on this network.\n"
							+ "Make sure your printer has been started and it is advertising on this network.\n"
							+ "If you just configured WIFI capabilities and you are waiting for the printer to restart,\n"
							+ "then click 'Ok' to try this operation again.");
					optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
					optionPane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
					
					JDialog dialog = optionPane.createDialog("Couldn't Find 3D Printer");
					dialog.setResizable(false);
					dialog.setModal(true);
					dialog.setAlwaysOnTop(true);
					dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				
					if (searchPane != null) {
						searchPane.setVisible(false);
					}
					dialog.setVisible(true);
					
					if (optionPane.getValue() == null ||
						optionPane.getValue().equals(JOptionPane.CANCEL_OPTION)) {
						System.exit(-1);
					}
				}
				
				searchPane = new JDialog();
				searchPane.setTitle("Searching for Printer");
				searchPane.setResizable(false);
				searchPane.setAlwaysOnTop(true);
				searchPane.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				searchPane.add(searchOptionPane);
				searchPane.pack();
				searchPane.setLocationRelativeTo(null);
				searchPane.setVisible(true);
				if (finalUpdater != null) {
					finalUpdater.close();
				}
			}
		} while (foundDevices.size() == 0);
		
		if (!Desktop.isDesktopSupported()) {
			for (PrintableDevice device : foundDevices) {
				System.out.println("The printer host software is at the URL:" + device.device.getDetails().getPresentationURI());
			}
		}
		
		PrintableDevice chosenPrinter = null;
		if (foundDevices.size() == 1) {
			chosenPrinter = foundDevices.iterator().next();
		} else {
			if (finalUpdater != null) {
				finalUpdater.close();
			}
			chosenPrinter = (PrintableDevice)JOptionPane.showInputDialog(null, 
		        "There were multiple 3d printers found on this network.\nWhich printer would you like to view?", 
		        "Choose a Printer",
		        JOptionPane.QUESTION_MESSAGE,
		        null,
		        foundDevices.toArray(),
		        foundDevices.iterator().next());
			
			if (chosenPrinter == null) {
				System.exit(-1);
			}
		}
		
	    System.out.println("User chose:" + chosenPrinter.device.getDisplayString());
	    
		try {
			Desktop.getDesktop().browse(chosenPrinter.device.getDetails().getPresentationURI());
			System.exit(0);
		} catch (IOException e) {
			JOptionPane optionPane = new JOptionPane();
			optionPane.setMessage("There was a problem starting the default browser for this machine.");
			optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
			JDialog dialog = optionPane.createDialog("Browser Not Found");
			dialog.setAlwaysOnTop(true);
			dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
			System.exit(-2);
		}
	}
}
