package org.area515.resinprinter.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

public class SSHClient {

	public static class SSHClientUI implements UserInfo {
		private String password;
		
		public SSHClientUI(String password) {
			this.password = password;
		}
		@Override
		public String getPassphrase() {
			return null;
		}
		@Override
		public String getPassword() {
			return password;
		}
		@Override
		public boolean promptPassphrase(String phraseToShow) {
			return true;
		}
		@Override
		public boolean promptPassword(String passwordPromptToShow) {
			return true;
		}
		@Override
		public boolean promptYesNo(String yesNoToShow) {
			return true;
		}
		@Override
		public void showMessage(String generalShowMessage) {
		}
	}
	
	public class JavaVersion {
		private String vendor;
		private double version;
		
		public JavaVersion(String vendor, double version) {
			this.vendor = vendor;
			this.version = version;
		}

		public String getVendor() {
			return vendor;
		}

		public double getVersion() {
			return version;
		}
		
		public boolean isOracleJVM() {
			return vendor.contains("HotSpot");
		}
		
		public boolean isOpenJDK() {
			return vendor.contains("OpenJDK");
		}
	}
	
	//private final String SET_TERM = "DaQPi\\\\DaQPi";
	private String TERM = "DaQPi\\\\DaQPi";
	private static final String ORIGNAL_TERM = "DaQPi\\\\DaQPi";
	private String username;
	private String password;
	private String host;
	private int port;
	private Session session;
	private Channel channel;
	private boolean shutdown = false;
	private boolean turnOffStty = true;//False doesn't work.  Escape chars are sent that push the unix prompt to be sent before the true end of output sequence
	
	private Lock lock = new ReentrantLock();
	private Condition newCommandReadyToSend = lock.newCondition();
	private Condition responseIsComplelyRead = lock.newCondition();
	private String[] lastResponse;
	private StringBuilder newCommand = new StringBuilder();
	private StringBuilder buildingLastResponse = new StringBuilder();
	private String installer = null;
	private String initDInstaller = null;
	private boolean sudoWasRequired = false;
	
	public void connect(String username, String password, String host, int port) throws JSchException {
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
		
		JSch shell = new JSch();
		session = shell.getSession(username, host, port);
		session.setUserInfo(new SSHClientUI(password));
		session.connect(20000);
		
		channel = session.openChannel("shell");
		channel.setInputStream(new InputStream() {
			@Override
			public int read() throws IOException {
				lock.lock();
				try {
					if (newCommand == null) {
						newCommandReadyToSend.await();
//System.out.println("New command started:" + newCommand);
						if (shutdown) {
							return -1;
						}
					} else if (newCommand.length() == 0) {
						newCommand = null;
						return -1;
					}
					int theChar = newCommand.charAt(0);
					newCommand.deleteCharAt(0);
//System.out.println(" Sending:" + theChar);
					return theChar;
				} catch (InterruptedException e) {
					throw new IOException("Thread was told to stop with an InterruptedException");
				} finally {
					lock.unlock();
				}
			}
		});
		channel.setOutputStream(new OutputStream(){
			@Override
			public void write(int b) throws IOException {
				lock.lock();
				try {
//System.out.println("Appending Response:(" + b + ") " + (char)b);
					buildingLastResponse.append((char)b);
					Pattern pattern = Pattern.compile(TERM);
					Matcher matcher = pattern.matcher(buildingLastResponse.toString());
					if (matcher.find()) {
//System.out.println("Found TERM:" + buildingLastResponse.toString());
//System.out.println("Groupcount:" + matcher.groupCount());
//System.out.println(" start:" + matcher.start());
//System.out.println(" start:" + matcher.end());
//System.out.println(" first N:" + buildingLastResponse.indexOf("\n"));
						lastResponse = new String[matcher.groupCount() + 2];
						buildingLastResponse.delete(matcher.start(), matcher.end());
						if (!turnOffStty) {
							buildingLastResponse.delete(0, buildingLastResponse.indexOf("\n") + 1);
						}
						lastResponse[0] = buildingLastResponse.toString();
						for (int t = 0; t < matcher.groupCount() + 1; t++) {
							lastResponse[1 + t] = matcher.group(t);
						}
						//This worked for quite some time and was exact but the terminal puts garbage in here to throw off our counts
						//buildingLastResponse.delete(0, lastResponse[0].length());
						//This is better because it deletes the whole string...
						buildingLastResponse.delete(0, buildingLastResponse.length());
//System.out.println(" lastResponse:" + Arrays.toString(lastResponse));
						responseIsComplelyRead.signalAll();
					}
				} finally {
					lock.unlock();
				}
			} 
		});
		channel.connect();
		
		send("PS1=" + TERM);//Change the prompt so that we know when it's logged in properly
		
		//Remove the SSH echo so we don't get screwed up with escape chars
		if (turnOffStty) {
			send("stty -echo");
		}
	}
	
	public boolean sudoIfNotRoot() {
		String testFileName = "Test.For.Sudo";
		send("cd /");
		String[] response = send("mkdir " + testFileName);
		if (!response[0].toLowerCase().contains("permission denied")) {
			send("rmdir " + testFileName);
			return true;
		}
		
		TERM = "(?:" + ORIGNAL_TERM + "|denied|root)";//TODO: Can't sudo with this user
		response = send("sudo -s -p " + ORIGNAL_TERM);
		if (response[1].contains("denied")) {
			TERM = ORIGNAL_TERM;
			return false;
		}
		
		if (!response[1].contains("root")) {
			TERM = "(?:root|Sorry, try again|incorrect password attempts)";//TODO: Can't sudo with this user
			response = send(password);
			while (response[1].contains("try again")) {
				response = send(password);
			}
			if (response[1].contains("incorrect")) {
				TERM = ORIGNAL_TERM;
				return false;
			}
		}
		
		sudoWasRequired = true;
		TERM = ORIGNAL_TERM;
		send("PS1=" + TERM);
		
		return true;
	}
	
	public String getHost() {
		return host;
	}

	public JavaVersion getJavaVersion() {
		String javaVersion = send("java -version")[0];
		StringTokenizer tokenizer = new StringTokenizer(javaVersion, "\n");
		if (!tokenizer.hasMoreElements()) {
			return null;
		}
		
		Pattern javaVersionPattern = Pattern.compile("java version \"(\\d+[.]\\d+).*", Pattern.DOTALL);
		Pattern javaVendorPattern = Pattern.compile("(.*)\\(build[^)]*\\).*", Pattern.DOTALL);
		String vendor = null;
		Double version = null;
		while (tokenizer.hasMoreElements()) {
			javaVersion = tokenizer.nextToken();
			Matcher matcher = javaVersionPattern.matcher(javaVersion);
			if (matcher.matches()) {
				version = new Double(matcher.group(1));
			}
			matcher = javaVendorPattern.matcher(javaVersion);
			if (matcher.matches()) {
				vendor = matcher.group(1);
			}
		}
		
		if (vendor == null || version == null)
			return null;
		
		return new JavaVersion(vendor, version);
	}

	public void sendFile(File sourceFile, File destinationFile) throws JSchException {
		String location = destinationFile.getParent().replace("\\", "/");
		String name = destinationFile.getName();
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(sourceFile);
			sudoRequiredSendFile(location, name, stream, false);
		} catch (IOException e) {
			System.out.println("sourceFile:" + sourceFile + " destinationFile:" + destinationFile);
			e.printStackTrace();
			return;
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {}
			}
		}
/*
		ChannelSftp channel = null;
		try {
			channel = (ChannelSftp)session.openChannel("sftp");
			channel.connect();
			channel.cd(location);
			channel.put(new FileInputStream(sourceFile), name);
		} catch (JSchException | FileNotFoundException | SftpException e) {
			LOG.error("sourceFile:" + sourceFile + " destinationFile:" + destinationFile, e);
		} finally {
			channel.disconnect();
		}*/
	}
	
	public void sendFile(File destinationFile, InputStream stream) {
		String location = destinationFile.getParent().replace("\\", "/");
		String name = destinationFile.getName();
		sudoRequiredSendFile(location, name, stream, false);
		
		/*ChannelSftp channel = null;
		try {
			channel = (ChannelSftp)session.openChannel("sftp");
			channel.connect();
			channel.cd(destinationFile.getParent().replace("\\", "/"));
			channel.put(stream, destinationFile.getName());
		} catch (JSchException | SftpException e) {
			LOG.error("sourceFile:" + destinationFile + " stream:" + stream, e);
		} finally {
			channel.disconnect();
		}*/
	}
	
	public void sendFile(String location, File fileToSend) throws JSchException {
		String name = fileToSend.getName();
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(fileToSend);
			sudoRequiredSendFile(location, name, stream, false);
		} catch (IOException e) {
			System.out.println("location:" + location + " fileToSend:" + fileToSend);
			e.printStackTrace();
			return;
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {}
			}
		}
		
		
/*		ChannelSftp channel = null;
		try {
			channel = (ChannelSftp)session.openChannel("sftp");
			channel.connect();
			channel.cd(location);
			channel.put(new FileInputStream(fileToSend), fileToSend.getName());
		} catch (JSchException | FileNotFoundException | SftpException e) {
			LOG.error("location:" + location + " fileToSend:" + fileToSend, e);
		} finally {
			channel.disconnect();
		}*/
	}
	
	public void sendFile(String location, String name, InputStream stream, boolean makeExecutable) throws JSchException {
		sudoRequiredSendFile(location, name, stream, makeExecutable);
/*		ChannelSftp channel = null;
		try {
			channel = (ChannelSftp)session.openChannel("sftp");
			channel.connect();
			channel.cd(location);
			channel.put(fileToSend, name);
			if (makeExecutable) {
				channel.chmod(0777, location + "/" + name);
			}
		} catch (JSchException | SftpException e) {
			LOG.error("location:" + location + " name:" + name + " fileToSend:" + fileToSend + " makeExecutable:" + makeExecutable, e);
		} finally {
			channel.disconnect();
		}*/
	}
	
	private void sudoRequiredSendFile(String location, String name, InputStream fileToSend, boolean makeExecutable) {
		ChannelSftp channel = null;
		try {
			channel = (ChannelSftp)session.openChannel("sftp");
			channel.connect();
			channel.put(fileToSend, name);
			
			send("mkdir -p '" + location + "'");
			send("chmod 777 '" + location + "'");
			send("mv '" + channel.getHome() + "/" + name + "' " + location);
			if (!location.endsWith("/")) {
				location += "/";
			}
			send("chmod 777 " + location + name);
		} catch (JSchException e) {
			System.out.println("location:" + location + " name:" + name + " fileToSend:" + fileToSend + " makeExecutable:" + makeExecutable);
			e.printStackTrace();
		} catch (SftpException e) {
			System.out.println("location:" + location + " name:" + name + " fileToSend:" + fileToSend + " makeExecutable:" + makeExecutable + (e.id == ChannelSftp.SSH_FX_FAILURE?" Disk full?":""));
			e.printStackTrace();
		} finally {
			channel.disconnect();
		}
	}
	
	public void receiveFile(String sourcePath, String sourceFile, File destinationFile) {
		ChannelSftp channel = null;
		FileOutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(destinationFile);
			channel = (ChannelSftp)session.openChannel("sftp");
			channel.connect();
			channel.cd(sourcePath);
			channel.get(sourceFile, outputStream);
		} catch (JSchException | FileNotFoundException | SftpException e) {
			System.out.println("sourcePath:" + sourcePath + " sourceFile:" + sourceFile + " destinationFile:" + destinationFile);
			e.printStackTrace();
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {}
			}
			channel.disconnect();
		}
	}
	
	public boolean isConnected() {
		return session != null && session.isConnected();
	}
	
	public void disconnect() {
		if (isConnected()) {
			shutdown = true;
			lock.lock();
			try {
				newCommandReadyToSend.signalAll();
			} finally {
				lock.unlock();
			}
			session.disconnect();
			channel.disconnect();
		}
	}
	
	public String[] send(String command) {
		if (!isConnected()) {
			throw new IllegalArgumentException("Not Connected");
		}
		lock.lock();
		try {
			newCommand = new StringBuilder();
			newCommand.append(command);
			newCommand.append("\n");
			newCommandReadyToSend.signalAll();
			responseIsComplelyRead.await();
			return lastResponse;
		} catch (InterruptedException e) {
			return null;
		} finally {
			lock.unlock();
		}
	}
	
	public void installInitD(String fileName, String startString, String stopString) {
		String program = "#!/bin/sh\n" +
		"### BEGIN INIT INFO\n" +
		"# Provides:          " + fileName + "\n" +
		"# Required-Start: $all\n" +
		"# Required-Stop:\n" +
		"# Default-Start:	2 3 4 5\n" +
		"# Default-Stop:	0 1 6\n" +
		"# Short-Description: Starts the triple stream devices\n" +
		"# Description: Starts the triple stream devices\n" +
		"### END INIT INFO\n\n" +
		"case \"$1\" in \n" +
		"	start)\n" +
		startString + 
        ";;\n" +
        "	stop)\n" +
        stopString + 
        ";;\n" +
        "	restart)\n" +
        stopString + 
		startString + 
        ";;\n" +
        "*)\n" +
        "  echo \"Usage: $0 {start|stop|restart}\"\n" +
        "esac\n" +
        "exit 0\n";
		
		try {
			sendFile("/etc/init.d", fileName, new ByteArrayInputStream(program.getBytes()), true);
			installInitD(fileName);
		} catch (JSchException e) {
			System.out.println("fileName:" + fileName + " startString:" + startString + " stopString:" + stopString);
			e.printStackTrace();
		}
	}
	
	private boolean validCommand(String commandOutput) {
		return !commandOutput.contains("ommand not found") && 
				!commandOutput.contains("o such file or directory") && 
			    !commandOutput.contains("not recognized") && 
			    !commandOutput.contains("currently not installed");
	}
	
	public void installInitD(String fileName) {
		if (initDInstaller == null) {
			String commandOutput = send("chkconfig")[0];
			if (validCommand(commandOutput)) {
				initDInstaller = "chkconfig ";
			} else {
				commandOutput = send("update-rc.d")[0];
				if (validCommand(commandOutput)) {
					initDInstaller = "update-rc.d ";
				}
			}
		}
		
		if (initDInstaller == null) {
			throw new IllegalStateException("chkconfig and update-rc.d don't work.  What do I do?");
		}
		
		if (initDInstaller.equals("chkconfig")) {
			send("chkconfig " + fileName + " on");
		} else {
			send("update-rc.d " + fileName + " start");
			send("update-rc.d " + fileName + " enable");//This is needed for kali linux since network services are disabled by default
			send("update-rc.d " + fileName + " defaults");//This is needed for ubuntu
		}
	}
	
	public void installPackage(String aptGetNewPackage, String yumNewPackage) {
		if (installer == null) {
			String commandOutput = send("apt-get")[0];
			if (validCommand(commandOutput)) {
				installer = "apt-get install --yes --force-yes ";
			} else {
				commandOutput = send("yum")[0];
				if (validCommand(commandOutput)) {
					installer = "yum install -y ";
				}
			}
		}
		
		if (installer == null) {
			throw new IllegalStateException("apt-get and yum don't work.  What do I do?");
		}
		
		if (installer.contains("apt-get")) {
			send(installer + aptGetNewPackage);//--force-yes
		} else {
			send(installer + yumNewPackage);
		}
	}
}
