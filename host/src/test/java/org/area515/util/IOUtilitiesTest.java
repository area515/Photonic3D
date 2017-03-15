package org.area515.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.network.LinuxNetworkManager;
import org.area515.resinprinter.network.LinuxNetworkManagerTest;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.util.IOUtilities.ParseAction;
import org.area515.util.IOUtilities.ParseState;
import org.area515.util.IOUtilities.SearchStyle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*"})
public class IOUtilitiesTest {
    private static final Logger logger = LogManager.getLogger();

    public static class InputStreamReadDelayedAnswer implements Answer<Integer>{
		private int delay;
		private byte[] byteData;

		public InputStreamReadDelayedAnswer(int delay, byte[] byteData) {
			this.delay = delay;
			this.byteData = byteData;
		}

		@Override
		public Integer answer(InvocationOnMock invocation) throws Throwable {
			byte[] data = (byte[])invocation.getArguments()[0];
			int offset = (int)invocation.getArguments()[1];
			int length = (int)invocation.getArguments()[2];

			Assert.assertEquals(byteData.length, length);
			Thread.sleep(delay);
			System.arraycopy(byteData, 0, data, offset, length);
			return byteData.length;
		}
	}

	public static class SerialPortReadDelayedAnswer implements Answer<byte[]>{
		private int delay;
		private byte[] byteData;

		public SerialPortReadDelayedAnswer(int delay, byte[] byteData) {
			this.delay = delay;
			this.byteData = byteData;
		}

		@Override
		public byte[] answer(InvocationOnMock invocation) throws Throwable {
			Thread.sleep(delay);
			return byteData;
		}
	}

	private ByteArrayOutputStream mockRuntime(String dataToReturn) throws IOException {
		Runtime runtime = Mockito.mock(Runtime.class);
		PowerMockito.mockStatic(Runtime.class);
		PowerMockito.when(Runtime.getRuntime()).thenReturn(runtime);

		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		final Process mockedProcess = Mockito.mock(Process.class);
		Mockito.when(mockedProcess.getInputStream())
			.thenReturn(new StringBufferInputStream(dataToReturn));
		Mockito.when(mockedProcess.getOutputStream())
			.thenReturn(output);

		Mockito.when(runtime.exec(Mockito.any(String[].class)))
			.then(new Answer<Process>() {
				@Override
				public Process answer(InvocationOnMock invocation) throws Throwable {
					return mockedProcess;
				}
			});

		return output;
	}

	private void testSingleLine(String line, StringBuilder builder, InputStream stream) throws IOException {
		int cpuDelay = 10;
		byte[] firstBytes = line.getBytes();
		Mockito.when(stream.available()).thenReturn(firstBytes.length).thenReturn(firstBytes.length);
		Mockito.when(stream.read(Mockito.any(byte[].class), Mockito.eq(0), Mockito.eq(firstBytes.length))).thenAnswer(new InputStreamReadDelayedAnswer(0, firstBytes));

		ParseState state = IOUtilities.readLine(stream, builder, "[\n]", 0, 0, cpuDelay);

		Assert.assertEquals(0, state.parseLocation);
		Assert.assertEquals(line, state.currentLine);
	}



	@Test
	@PrepareForTest(IOUtilities.class)
	public void testNativeCommandExecution() throws IOException {
		String[] directories = new String[]{"Directory 1", "Directory2", "Final Directory"};
		StringBuilder builder = new StringBuilder();
		for (String directory : directories) {
			builder.append(directory);
			builder.append("\n");
		}

		mockRuntime(builder.toString());
		String[] returnedDirectories = IOUtilities.executeNativeCommand(new String[]{"ls {0}"}, null, "stuff");
		Assert.assertArrayEquals(directories, returnedDirectories);
	}

	@Test
	@PrepareForTest(IOUtilities.class)
	public void testNativeCommandCommunication() throws IOException {
		mockRuntime(LinuxNetworkManagerTest.SCAN_WIFI_DATA);

		List<ParseAction> actions = new ArrayList<ParseAction>();
		actions.add(new ParseAction(new String[]{"/bin/sh", "-c", "wpa_cli -i {0}"}, ">", SearchStyle.RepeatUntilMatch));
		actions.add(new ParseAction(new String[]{"scan\n"}, "[\\r\\s]*<\\d+>\\s*CTRL-EVENT-SCAN-RESULTS\\s*", SearchStyle.RepeatUntilMatch));
		actions.add(new ParseAction(new String[]{""}, "\\s*>", SearchStyle.RepeatUntilMatch));
		actions.add(new ParseAction(new String[]{"scan_results\n"}, "bssid.*", SearchStyle.RepeatUntilMatch));
		actions.add(new ParseAction(new String[]{""}, LinuxNetworkManager.WIFI_REGEX, SearchStyle.RepeatWhileMatching));

		List<String[]> dataReturned = IOUtilities.communicateWithNativeCommand(actions, "^>|\n", true, null, "wlan0");
		Assert.assertEquals("SomeNetwork", dataReturned.get(0)[4]);
		Assert.assertEquals("\\x00", dataReturned.get(1)[4]);
		Assert.assertEquals("&#9786;\\u0044\\\\\\x45\\\\u0044Test", dataReturned.get(2)[4]);
	}
	@Test
	public void inputStreamReadLineTest() throws IOException {
		StringBuilder builder = new StringBuilder();
		InputStream stream = org.mockito.Mockito.mock(InputStream.class);

		testSingleLine("ok\n", builder, stream);
		testSingleLine("\n", builder, stream);

		logger.info(builder);
	}

	@Test
	public void inputStreamSplitReadLineTest() throws IOException {
		int streamDelayTooLong = 100;
		int timeout = 50;
		int cpuDelay = 10;

		StringBuilder builder = new StringBuilder();
		InputStream stream = org.mockito.Mockito.mock(InputStream.class);
		byte[] bytes = "o".getBytes();
		byte[] bytes2 = "k\nwo".getBytes();
		byte[] bytes3 = "rl".getBytes();
		byte[] bytes4 = "d\n".getBytes();
		Mockito.when(stream.available())
			.thenReturn(bytes.length).thenReturn(bytes.length)
			.thenReturn(bytes2.length).thenReturn(bytes2.length)
			.thenReturn(bytes3.length).thenReturn(bytes3.length)
			.thenReturn(bytes4.length).thenReturn(bytes4.length)
			.thenReturn(0);
		Mockito.when(stream.read(Mockito.any(byte[].class), Mockito.any(Integer.class), Mockito.any(Integer.class)))
			.thenAnswer(new InputStreamReadDelayedAnswer(streamDelayTooLong, bytes))
			.thenAnswer(new InputStreamReadDelayedAnswer(streamDelayTooLong, bytes2))
			.thenAnswer(new InputStreamReadDelayedAnswer(streamDelayTooLong, bytes3))
			.thenAnswer(new InputStreamReadDelayedAnswer(streamDelayTooLong, bytes4))
			.thenThrow(new IllegalArgumentException("The read method should never have been called this time."));

		//This tests that an inputstream read wasn't able to complete a full line read in the given timeout period.
		ParseState state = IOUtilities.readLine(stream, builder, "[\n]", 0, timeout, cpuDelay);
		Assert.assertEquals(1, state.parseLocation);
		Assert.assertEquals(null, state.currentLine);

		//The next read should be able to read the the ok
		state = IOUtilities.readLine(stream, builder, "[\n]", state.parseLocation, timeout, cpuDelay);
		Assert.assertEquals(0, state.parseLocation);
		Assert.assertEquals("ok\n", state.currentLine);

		//Again, this read won't be able to read the full amount of data in the timeout period.
		state = IOUtilities.readLine(stream, builder, "[\n]", state.parseLocation, timeout, cpuDelay);
		Assert.assertEquals(4, state.parseLocation);
		Assert.assertEquals(null, state.currentLine);

		//There is nothing left on the stream to read so it continues to parse the rest of the StringBuilder
		state = IOUtilities.readLine(stream, builder, "[\n]", state.parseLocation, timeout, 10);
		Assert.assertEquals(0, state.parseLocation);
		Assert.assertEquals("world\n", state.currentLine);
	}

	@Test
	public void serialPortSplitReadLineTest() throws IOException {
		int streamDelayTooLong = 100;
		int timeout = 50;
		int cpuDelay = 10;

		StringBuilder builder = new StringBuilder();
		SerialCommunicationsPort serial = org.mockito.Mockito.mock(SerialCommunicationsPort.class);
		Printer printer = org.mockito.Mockito.mock(Printer.class);
		byte[] bytes = "o".getBytes();
		byte[] bytes2 = "k\nwo".getBytes();
		byte[] bytes3 = "rl".getBytes();
		byte[] bytes4 = "d\n".getBytes();
		Mockito.when(serial.read())
			.thenAnswer(new SerialPortReadDelayedAnswer(streamDelayTooLong, bytes))
			.thenAnswer(new SerialPortReadDelayedAnswer(streamDelayTooLong, bytes2))
			.thenAnswer(new SerialPortReadDelayedAnswer(streamDelayTooLong, bytes3))
			.thenAnswer(new SerialPortReadDelayedAnswer(streamDelayTooLong, bytes4))
			.thenThrow(new IllegalArgumentException("The read method should never have been called this time."));

		//This tests that an inputstream read wasn't able to complete a full line read in the given timeout period.
		ParseState state = IOUtilities.readLine(printer, serial, builder, 0, timeout, cpuDelay);
		Assert.assertEquals(1, state.parseLocation);
		Assert.assertEquals(null, state.currentLine);

		//The next read should be able to read the the ok
		state = IOUtilities.readLine(printer, serial, builder, state.parseLocation, timeout, cpuDelay);
		Assert.assertEquals(0, state.parseLocation);
		Assert.assertEquals("ok\n", state.currentLine);

		//Again, this read won't be able to read the full amount of data in the timeout period.
		state = IOUtilities.readLine(printer, serial, builder, state.parseLocation, timeout, cpuDelay);
		Assert.assertEquals(4, state.parseLocation);
		Assert.assertEquals(null, state.currentLine);

		//There is nothing left on the stream to read so it continues to parse the rest of the StringBuilder
		state = IOUtilities.readLine(printer, serial, builder, state.parseLocation, timeout, cpuDelay);
		Assert.assertEquals(0, state.parseLocation);
		Assert.assertEquals("world\n", state.currentLine);
	}

	@Test
	public void readWithTimeoutSerialPortTest() throws IOException, InterruptedException {
		int streamDelayTooLong = 200;
		int streamDelayOk = 5;
		int timeout = 100;
		int cpuDelay = 10;

		SerialCommunicationsPort serial = org.mockito.Mockito.mock(SerialCommunicationsPort.class);
		byte[] bytes = "j".getBytes();
		byte[] bytes2 = "k".getBytes();
		Mockito.when(serial.read())
					.thenAnswer(new SerialPortReadDelayedAnswer(streamDelayTooLong, null))
					.thenAnswer(new SerialPortReadDelayedAnswer(streamDelayTooLong, bytes))
					.thenAnswer(new SerialPortReadDelayedAnswer(streamDelayTooLong, null))
					.thenAnswer(new SerialPortReadDelayedAnswer(streamDelayOk, bytes2))
					.thenAnswer(new SerialPortReadDelayedAnswer(streamDelayOk, null));

		String data = IOUtilities.readWithTimeout(serial, timeout, cpuDelay);
		Assert.assertEquals("", data);

		data = IOUtilities.readWithTimeout(serial, timeout, cpuDelay);
		Assert.assertEquals("j", data);

		data = IOUtilities.readWithTimeout(serial, timeout, cpuDelay);
		Assert.assertEquals("k", data);

	}

	@Test
	public void readWithTimeoutInputStreamTest() throws IOException, InterruptedException {
		int streamDelayTooLong = 200;
		int streamDelayOk = 5;
		int timeout = 100;
		int cpuDelay = 10;

		InputStream stream = org.mockito.Mockito.mock(InputStream.class);
		byte[] bytes = "hello".getBytes();
		byte[] bytes2 = "worlds".getBytes();
		byte[] checkBytes = new byte[bytes.length];
		byte[] checkBytes2 = new byte[bytes2.length];

		Mockito.when(stream.available())
			.thenReturn(0)
			.thenReturn(bytes.length)
			.thenReturn(bytes2.length)
			.thenReturn(0);
		Mockito.when(stream.read(Mockito.any(byte[].class), Mockito.any(Integer.class), Mockito.any(Integer.class)))
			.thenAnswer(new InputStreamReadDelayedAnswer(streamDelayTooLong, new byte[0]))
			.thenAnswer(new InputStreamReadDelayedAnswer(streamDelayTooLong, bytes))
			.thenAnswer(new InputStreamReadDelayedAnswer(streamDelayOk, bytes2));

		int dataRead = IOUtilities.readWithTimeout(stream, checkBytes, timeout, cpuDelay);
		Assert.assertEquals(0, dataRead);

		dataRead = IOUtilities.readWithTimeout(stream, checkBytes, timeout, cpuDelay);
		Assert.assertEquals(bytes.length, dataRead);
		Assert.assertArrayEquals(bytes, checkBytes);

		dataRead = IOUtilities.readWithTimeout(stream, checkBytes2, timeout, cpuDelay);
		Assert.assertArrayEquals(bytes2, checkBytes2);
	}
}
