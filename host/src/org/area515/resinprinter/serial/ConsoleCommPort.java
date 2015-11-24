package org.area515.resinprinter.serial;

import org.area515.resinprinter.printer.MachineConfig.ComPortSettings;

public class ConsoleCommPort implements SerialCommunicationsPort {
	public static final String CONSOLE_COMM_PORT = "Console Testing";
	
	private String name = CONSOLE_COMM_PORT;
	private int readCount;
	private int timeout;
	
	@Override
	public void open(String printerName, int timeout, ComPortSettings settings) {
		readCount = 0;
		this.timeout = timeout;
		System.out.println("Printer opened");
	}

	@Override
	public void close() {
		System.out.println("Printer closed");
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void write(byte[] gcode) {
		System.out.println("Printer received:" + new String(gcode));
	}

	@Override
	public byte[] read() {
		switch (readCount) {
		case 0:
			readCount = 1;
			return "Console chitchat\n".getBytes();
		case 1:
			try {
				Thread.sleep(timeout+1);
			} catch (InterruptedException e) {}
			readCount = 2;
			return null;
		}
		
		return "ok\n".getBytes();
	}

	public String toString() {
		return name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		ConsoleCommPort other = (ConsoleCommPort) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
