package org.area515.resinprinter.printer;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.serial.SerialManager;
import org.area515.util.IOUtilitiesTest.SerialPortReadDelayedAnswer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class DetectFirmwareMock {
    private static final Logger logger = LogManager.getLogger();
	
	@Test
	public void ensureCRLFWorkInFirmwareDetection() throws IOException {
		logger.info("Firmware detection test.");
		String grblChitChat = "Grbl 0.9i ['$' for help]\r\nok\r\n";
		String grblOk = "ok\r\n";
		SerialCommunicationsPort serialPort = Mockito.mock(SerialCommunicationsPort.class);
		Mockito.when(serialPort.read())
			.thenAnswer(new SerialPortReadDelayedAnswer(SerialManager.TIME_OUT, grblChitChat.getBytes()))
			.thenAnswer(new SerialPortReadDelayedAnswer(SerialManager.TIME_OUT, null))
			.thenAnswer(new SerialPortReadDelayedAnswer(SerialManager.TIME_OUT, grblOk.getBytes()))
			.thenAnswer(new SerialPortReadDelayedAnswer(SerialManager.TIME_OUT, null));
		ComPortSettings comPort = Mockito.mock(ComPortSettings.class);
		 
		Assert.assertTrue(SerialManager.Instance().is3dFirmware(serialPort, comPort));
	}
}
