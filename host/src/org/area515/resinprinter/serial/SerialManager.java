package org.area515.resinprinter.serial;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;

import org.area515.resinprinter.server.HostProperties;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class SerialManager implements SerialPortEventListener {
	private static SerialManager serialManager = null;

	public static SerialManager Instance() throws IOException {
		if (serialManager == null) {
			serialManager = new SerialManager();
		}
		return serialManager;
	}
	
	private SerialManager() throws IOException{
		if(HostProperties.Instance().getFakeSerial()){
			setupFakeSerial();
		}else{
			initialize();
		}
	}
	
	private void setupFakeSerial(){
		responseData="";
		System.out.println("SerialManager: setup complete");
	}
	
	public void sendFakeSerial(String command){
		System.out.println("Writing serial: " + command);
		responseData = "*" + command + "*";
		System.out.println("Received reply: " + responseData);
	}
	
	/*
	 * Custom
	 * http://marc.info/?l=rxtx&m=135092551225124&w=2
	 */
	private final Object responseSync = new Object();
	private String responseData;
	public String getResponseData(){return responseData;}
	
	 public void send(String cmd) throws IOException, InterruptedException {
		 if(HostProperties.Instance().getFakeSerial()){
			SerialManager.Instance().sendFakeSerial(cmd); 
		 }else{
	        synchronized (responseSync) {
	            output.write(cmd.getBytes());
	            output.write("\n".getBytes());
	            responseSync.wait();
	            // when we reach this line a valid response is available in
	            // responseData field
	            System.out.println("Response: " + responseData);
	            
	            // read responseData here and throw exception on error
	            // or return results ... whatever
	        }
		 }
	    }
	
	 private class Callback implements SerialPortEventListener {

//	        private byte[] readBuff = new byte[256];

	        @Override
	        public void serialEvent(SerialPortEvent spe) {
	            synchronized (responseSync) {
	                if (spe.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
//	                    try {
	                    	
	                    	try {
	            				String inputLine=input.readLine();
	            				System.out.println(inputLine);
	            				responseData = inputLine;
	            			} catch (Exception e) {
	            				System.err.println(e.toString());
	            			}
//	                        int av = input.available();
//	                        int read = input.read(readBuff, 0, av);
	                        // parse readBuff from 0 to read-1 for packet end
	                        // append data to responseData

	                        // when packet end reached:
	                        responseSync.notifyAll();
	                        // this will wake the waiting thread at putAtCommand
//	                    } catch (IOException ex) {
//	                        // handle exception
//	                    }
	                }
	            }
	        }
	    }
	 
	 
	 /*
	  * End Custom
	  */
	SerialPort serialPort;
        /** The port we're normally going to use. */
	private static final String PORT_NAMES[] = { 
			"/dev/tty.usbserial-A9007UX1", // Mac OS X
                        "/dev/ttyACM0", // Raspberry Pi
			"/dev/ttyUSB0", // Linux
			"COM3", // Windows
	};
	/**
	* A BufferedReader which will be fed by a InputStreamReader 
	* converting the bytes into characters 
	* making the displayed results codepage independent
	*/
	private BufferedReader input;
	/** The output stream to the port */
	private OutputStream output;
	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;
	/** Default bits per second for COM port. */
	private static final int DATA_RATE = 9600;

	public void initialize() {
                // the next line is for Raspberry Pi and 
                // gets us into the while loop and was suggested here was suggested http://www.raspberrypi.org/phpBB3/viewtopic.php?f=81&t=32186
                System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyACM0");

		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		//First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			System.out.println("Could not find COM port.");
			return;
		}

		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(),
					TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			// open the streams
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();

			// add event listeners
//			serialPort.addEventListener(this);
			serialPort.addEventListener(new Callback());
			serialPort.notifyOnDataAvailable(true);
			
//			output.write("Hello\n".getBytes());
//			serialPort.
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}
	
	/**
	 * This should be called when you stop using the port.
	 * This will prevent port locking on platforms like Linux.
	 */
	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	/**
	 * Handle an event on the serial port. Read the data and print it.
	 */
	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				String inputLine=input.readLine();
				System.out.println(inputLine);
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
		// Ignore all the other eventTypes, but you should consider the other ones.
	}

	public static void main(String[] args) throws Exception {
		SerialManager main = new SerialManager();
		main.initialize();
		Thread t=new Thread() {
			public void run() {
				//the following line will keep this app alive for 1000 seconds,
				//waiting for events to occur and responding to them (printing incoming messages to console).
				try {Thread.sleep(1000000);} catch (InterruptedException ie) {}
			}
		};
		
		t.start();
		System.out.println("Started");
	}
	
}