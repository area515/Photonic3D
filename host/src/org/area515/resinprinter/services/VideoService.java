package org.area515.resinprinter.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.area515.resinprinter.server.HostProperties;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.FragmentedMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.H264TrackImpl;

@Path("video")
public class VideoService {
	public static VideoService INSTANCE = new VideoService();

	//All static for now, just to get this done...
	private File rawh264StreamFile = new File(System.getProperty("java.io.tmpdir"), "tempraw.h246");
	private File mp4StreamFile = new File(System.getProperty("java.io.tmpdir"), "temp.mp4");
	private Process rawH264ProducerProcess;
	private Lock processLock = new ReentrantLock();
	
	private VideoService(){}

	public File getRecordedFile() {
		return mp4StreamFile;
	}
	
	@GET
	@Path("startrecordvideo/{cameraName}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse startVideo(@PathParam("cameraName") String cameraName) {
		processLock.lock();
		try {
			if (rawH264ProducerProcess != null) {
				return new MachineResponse("startrecord", true, "Camera:" + cameraName + " already started");
			}

			System.out.println("Attempting to start video");
			final String streamingCommand = HostProperties.Instance().getStreamingCommand();
			try {
				rawH264ProducerProcess = Runtime.getRuntime().exec(streamingCommand);
				final BufferedInputStream inputStream = new BufferedInputStream(rawH264ProducerProcess.getInputStream());
				final FileOutputStream outputStream = new FileOutputStream(rawh264StreamFile);

				Thread thread = new Thread(new Runnable() {
					public void run() {
						try {
							IOUtils.copy(inputStream, outputStream);
						} catch (IOException e) {
							System.out.println("Copy interrupted:" + streamingCommand + " (this is probably normal)");
						} finally {
							if (inputStream != null) {
								try {
									inputStream.close();
								} catch (IOException e) {
								}
							}
							if (outputStream != null) {
								try {
									outputStream.close();
								} catch (IOException e) {
								}
							}
						}
					};
				}, "OriginalVideoWritingThread");
				thread.setDaemon(true);
				thread.start();
				return new MachineResponse("startrecord", true, "Camera:" + cameraName + " started");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Couldn't start command line process:" + streamingCommand);
				rawH264ProducerProcess.destroy();
				rawH264ProducerProcess = null;
				return new MachineResponse("startrecord", false, "Camera:" + cameraName + " couldn't be started");
			}
		} finally {
			processLock.unlock();
		}
	}

	@GET
	@Path("stopvideorecord/{cameraName}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse stopVideo(@PathParam("cameraName") String cameraName) {
		processLock.lock();
		try {
			if (rawH264ProducerProcess == null) {
				return new MachineResponse("stopvideorecord", false, "A recording hasn't been started yet for this camera: " + cameraName);
			}

			System.out.println("Attempting to end video");
			rawH264ProducerProcess.destroy();
			Thread thread = new Thread(new Runnable() {
				public void run() {
					FileOutputStream publishStream = null;
					FileDataSourceImpl originalFileSource = null;
					try {
						publishStream = new FileOutputStream(mp4StreamFile);
						originalFileSource = new FileDataSourceImpl(rawh264StreamFile);

						H264TrackImpl h264Track = new H264TrackImpl(
								originalFileSource);
						Movie m = new Movie();
						m.addTrack(h264Track);

						Container out = new FragmentedMp4Builder().build(m);
						FileChannel fc = publishStream.getChannel();
						out.writeContainer(fc);
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("Couldn't convert file from:" + rawh264StreamFile + " to:" + mp4StreamFile);
					} finally {
						if (publishStream != null) {
							try {
								publishStream.close();
							} catch (IOException e) {
							}
						}
						if (originalFileSource != null) {
							try {
								originalFileSource.close();
							} catch (IOException e) {
							}
							if (!rawh264StreamFile.delete()) {
								System.out.println("Couldn't delete old file:" + rawh264StreamFile);
							}
						}
						rawH264ProducerProcess = null;
					}
				};
			}, "VideoMuxingThread");
			thread.setDaemon(true);
			thread.start();
			
			//TODO: Should we be waiting until we are done muxing?
			try {
				thread.join();
				return new MachineResponse("stopvideorecord", true, "MP4 Creation complete.");
			} catch (InterruptedException e) {
				return new MachineResponse("stopvideorecord", false, "Interrupted while waiting for MP4 to complete.");
			}
		} finally {
			processLock.unlock();
		}
	}
}
