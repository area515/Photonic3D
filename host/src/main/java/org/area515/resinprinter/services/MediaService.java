package org.area515.resinprinter.services;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.util.security.PhotonicUser;
import org.area515.util.Log4jUtil;
import org.eclipse.jetty.http.HttpStatus;

import com.coremedia.iso.boxes.Container;
import com.google.common.io.ByteStreams;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.FragmentedMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.H264TrackImpl;

@Api(value="media")
@RolesAllowed(PhotonicUser.FULL_RIGHTS)
@Path("media")
public class MediaService {
    private static final Logger logger = LogManager.getLogger();
	private static final String boundaryDelimiter = "--EndOfJPEG";
	public static MediaService INSTANCE = new MediaService();
	
	//TODO: We need to Lock per Printer, not a global lock!
	private Lock processLock = new ReentrantLock();
	
	//All static for now, just to get this done...
	private File rawh264StreamFile = new File(System.getProperty("java.io.tmpdir"), "tempraw.h246");
	private File mp4StreamFile = new File(System.getProperty("java.io.tmpdir"), "temp.mp4");
	private Process rawH264ProducerProcess;
	
	//These are for live streaming only
	private Map<String, ClientStream> mjpegStreamerClients = new HashMap<String, ClientStream>();
	private Lock liveStreamerModificationLock = new ReentrantLock(true);
	private Future<byte[]> nextLiveStreamImage;
	private ExecutorService liveStreamingThrottlingService;
	
	private MediaService() {
		if (HostProperties.Instance().getLimitLiveStreamToOneCPU()) {
			liveStreamingThrottlingService = Executors.newSingleThreadScheduledExecutor();
		} else {
			liveStreamingThrottlingService = Executors.newWorkStealingPool();
		}
	}
	
	public File getRecordedFile() {
		return mp4StreamFile;
	}
	
	public class SourceImageReader implements Callable<byte[]> {
		private int x;
		private int y;
		private int timeBetweenFrames;
		private boolean firstIteration;
		
		public SourceImageReader(int x, int y, int timeBetweenFrames, boolean firstIteration) {
			this.x = x;
			this.y = y;
			this.timeBetweenFrames = timeBetweenFrames;
			this.firstIteration = firstIteration;
		}

		@Override
		public byte[] call() throws Exception {
			try {
				ImageSnapshotCapture taker = new ImageSnapshotCapture(x, y);
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				taker.write(stream);
				return stream.toByteArray();
			} catch (IOException | WebApplicationException e) {
				logger.error("Problem occurred while taking snapshot (recovering)", e);
				throw e;
			} finally {
				liveStreamerModificationLock.lock();//TODO: Can we can eliminate this critical section?
				try {
					if (mjpegStreamerClients.size() > 0) {
						nextLiveStreamImage = Main.GLOBAL_EXECUTOR.submit(new SourceImageReader(x, y, timeBetweenFrames, false));
					}
				} finally {
					liveStreamerModificationLock.unlock();
				}
				if (!firstIteration) {
					Thread.sleep(timeBetweenFrames);
				}
			}
		}
	}
	
	public class ImageSnapshotCapture implements StreamingOutput {
		private int x;
		private int y;
		
		public ImageSnapshotCapture(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		@Override
		public void write(OutputStream output) throws IOException, WebApplicationException {
			logger.debug("Image snapshot start", ()->Log4jUtil.startTimer("PictureTimer"));
			String[] streamingCommand = HostProperties.Instance().getImagingCommand();
			String[] replacedCommands = new String[streamingCommand.length];
			for (int t = 0; t < streamingCommand.length; t++) {
				replacedCommands[t] = MessageFormat.format(streamingCommand[t], x, y);
			}
			
			InputStream inputStream = null;
			processLock.lock();
			Process imagingProcess = null;
			try {
				imagingProcess = Runtime.getRuntime().exec(replacedCommands);
				ByteStreams.copy(imagingProcess.getInputStream(), output);
				logger.debug("Image snapshot complete {}ms", ()-> Log4jUtil.completeTimer("PictureTimer"));
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
					}
				}
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
					}
				}
				if (imagingProcess != null && imagingProcess.exitValue() != 0) {
					ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
					ByteStreams.copy(imagingProcess.getErrorStream(), errorOutput);
					String error = new String(errorOutput.toByteArray());
					logger.error(error);
					throw new WebApplicationException(error, 400);
				}
				processLock.unlock();
			}
		}
	}
	
	private enum CloseType {
		Normal,
		DontRemoveClient
	}

	public class ClientStream implements StreamingOutput {
		private String clientId;
		private CloseType closeNow;
		
		public ClientStream(String clientId) {
			this.clientId = clientId;
		}
		
		@Override
		public void write(OutputStream outputStream) throws IOException, WebApplicationException {
			while (true) {//Stream forever until they tell us to quit.
				try {
					logger.debug("Client asking to stream", ()->Log4jUtil.startTimer("ClientStreamTimer"));
					
					byte[] imageData = nextLiveStreamImage.get();
					Future<Object> run = liveStreamingThrottlingService.submit(new Callable<Object>() {
						public Object call() throws IOException {
						    outputStream.write((
						    		boundaryDelimiter + "\r\n" +
						            "Content-type: image/jpg\r\n" +
						            "Content-Length: " + imageData.length + "\r\n\r\n").getBytes());
					        outputStream.write(imageData);
					        outputStream.write("\r\n\r\n".getBytes());
					        outputStream.flush();
							return null;
						}
					});
					logger.debug("Client waiting to stream image {}ms", ()->Log4jUtil.splitTimer("ClientStreamTimer"));
					run.get();//It may seem strange to setup a future and then run get, but this limits the concurrency level in the event we have a crazy amount of clients
					logger.debug("Client streamed image {}ms", ()-> Log4jUtil.completeTimer("ClientStreamTimer"));
					
					//We've been asked to close nicely from the browser instead of the user just closing the page.
					if (closeNow != null) {
						outputStream.close();
					}
				} catch (InterruptedException | ExecutionException e) {
					logger.debug("Client destroyed with close status of:{}", closeNow);
					
					if (closeNow == null || closeNow == CloseType.Normal) {
						liveStreamerModificationLock.lock();
						try {
							mjpegStreamerClients.remove(clientId);
						} finally {
							liveStreamerModificationLock.unlock();
						}
					}
					if (e instanceof InterruptedException) {
						e.printStackTrace();
						break;
					}
					if (e.getCause().getCause() instanceof IOException) {
						throw (IOException)e.getCause().getCause();
					}
					
					logger.error("Unknown error while streaming", e);
				}
			}
		}
		public void closeWithoutRemovingClient() {
			closeNow = CloseType.DontRemoveClient;
		}
		public void close() {
			if (closeNow == null) {
				closeNow = CloseType.Normal;
			}
		}
	}
	
	public class StreamCopier implements Runnable {
		private InputStream inputStream;
		private OutputStream outputStream;
		private String streamingCommandString;
		
		public StreamCopier(InputStream inputStream, OutputStream outputStream, String streamingCommandString) {
			this.inputStream = inputStream;
			this.outputStream = outputStream;
			this.streamingCommandString = streamingCommandString;
		}
		
		public void run() {
			try {
				IOUtils.copy(inputStream, outputStream);
			} catch (IOException e) {
				logger.error("Copy complete:{} Message:{}", streamingCommandString, e.getMessage());
			} finally {
				logger.info("Copy process is complete for:{}", streamingCommandString);
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
		}
		
	}
	
    @ApiOperation(value = "Starts recording video with the supplied(width:x, height:y) for the printer specified using the settings specified in the config.properties streamingCommand=[jsonFormattedNativeOSCommand]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	//TODO: We need to actually get the printer by printername and then get the commandLineParameters from the MachineConfig not the HostProperties!!
	@GET
	@Path("startrecordvideo/{printerName}/x/{x}/y/{y}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse startVideo(@PathParam("printerName") String printerName, @PathParam("x") int x, @PathParam("y") String y) {
		processLock.lock();
		try {
			if (rawH264ProducerProcess != null) {
				return new MachineResponse("startrecord", true, "Printer:" + printerName + " already started recording");
			}

			logger.info("Attempting to start video");
			final String[] streamingCommand = HostProperties.Instance().getStreamingCommand();
			try {
				String[] replacedCommands = new String[streamingCommand.length];
				StringBuffer buffer = new StringBuffer();
				for (int t = 0; t < streamingCommand.length; t++) {
					replacedCommands[t] = MessageFormat.format(streamingCommand[t], x, y);
					buffer.append(replacedCommands[t]);
					buffer.append(" ");
				}				
				
				rawH264ProducerProcess = Runtime.getRuntime().exec(replacedCommands);
				final BufferedInputStream inputStream = new BufferedInputStream(rawH264ProducerProcess.getInputStream());
				final FileOutputStream outputStream = new FileOutputStream(rawh264StreamFile);
				final BufferedInputStream errorStream = new BufferedInputStream(rawH264ProducerProcess.getErrorStream());
				
				Thread writingThread = new Thread(new StreamCopier(inputStream, outputStream, buffer.toString()), "OriginalVideoWritingThread");
				writingThread.setDaemon(true);
				writingThread.start();
				Thread errorThread = new Thread(new StreamCopier(errorStream, System.err, buffer.toString() + " LOG"), "VideoErrorLoggingThread");
				errorThread.setDaemon(true);
				errorThread.start();
				return new MachineResponse("startrecord", true, "Printer:" + printerName + " started recording");
			} catch (IOException e) {
				logger.error("Couldn't start command line process:" + Arrays.toString(streamingCommand), e);
				if (rawH264ProducerProcess != null) {
					rawH264ProducerProcess.destroy();
					rawH264ProducerProcess = null;
				}
				return new MachineResponse("startrecord", false, "Printer:" + printerName + " couldn't record");
			}
		} finally {
			processLock.unlock();
		}
	}

    @ApiOperation(value = "Stops recording video for the printer specified.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("stoprecordvideo/{printerName}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse stopVideo(@PathParam("printerName") String printerName) {
		processLock.lock();
		try {
			if (rawH264ProducerProcess == null) {
				return new MachineResponse("stopvideorecord", false, "A recording hasn't been started yet for this printer: " + printerName);
			}

			logger.info("Attempting to end video");
			rawH264ProducerProcess.destroy();
			Thread thread = new Thread(new Runnable() {
				public void run() {
					FileOutputStream publishStream = null;
					FileDataSourceImpl originalFileSource = null;
					try {
						publishStream = new FileOutputStream(mp4StreamFile);
						originalFileSource = new FileDataSourceImpl(rawh264StreamFile);

						H264TrackImpl h264Track = new H264TrackImpl(originalFileSource);
						Movie m = new Movie();
						m.addTrack(h264Track);

						Container out = new FragmentedMp4Builder().build(m);
						FileChannel fc = publishStream.getChannel();
						out.writeContainer(fc);
					} catch (Exception e) {
						logger.error("Couldn't convert file from:" + rawh264StreamFile + " to:" + mp4StreamFile, e);
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
								logger.warn("Couldn't delete old file:{}", rawh264StreamFile);
							}
						}
						rawH264ProducerProcess = null;
					}
				};
			}, "VideoMuxingThread");
			thread.setDaemon(true);
			thread.start();
			
			try {
				//You MUST wait for the MUXing process to complete before this method returns. The client depends on it...
				thread.join();
				return new MachineResponse("stopvideorecord", true, "MP4 Creation complete.");
			} catch (InterruptedException e) {
				return new MachineResponse("stopvideorecord", false, "Interrupted while waiting for MP4 to complete.");
			}
		} finally {
			processLock.unlock();
		}
	}
	
    @ApiOperation(value = "Starts a live mjpeg stream using the settings available in config.properties for key imagingCommand=[jsonFormattedNativeOSCommand]. "
    		+ "clientId refers can any unique id that a client would like to create. "
    		+ "This clientId is later sent to the stoplivemjpegstream when it wants to notify the host server that a stream is no longer needed.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	//TODO: We need to actually get the printer by printername and then get the commandLineParameters from the MachineConfig not the HostProperties!!
	@GET
	@Path("startlivemjpegstream/{printerName}/clientid/{clientId}/x/{x}/y/{y}")
	@Produces("multipart/x-mixed-replace; boundary=" + boundaryDelimiter)
	public Response startLiveStream(@PathParam("printerName") String printerName, @PathParam("clientId") String clientId, @PathParam("x") int x, @PathParam("y") int y) {
		ClientStream stream = null;
		liveStreamerModificationLock.lock();
		try {
			stream = mjpegStreamerClients.get(clientId);
			if (stream != null) {
				stream.closeWithoutRemovingClient();//We don't want to remove the client because it will happen after this critical section and it will actually remove us and not the "old" one!!
			}
			stream = new ClientStream(clientId);
			
			if (mjpegStreamerClients.size() == 0) {
				nextLiveStreamImage = Main.GLOBAL_EXECUTOR.submit(new SourceImageReader(x, y, 1000, true));
			}
			mjpegStreamerClients.put(clientId, stream);
		} finally {
			liveStreamerModificationLock.unlock();
		}
		
		return Response.ok().entity(stream).build();
	}
	
    @ApiOperation(value = "Stops a live mjpeg stream. The clientId that was used to start the original stream should be used to stop this stream.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	//TODO: We need to actually get the printer by printername and then get the commandLineParameters from the MachineConfig not the HostProperties!!
	@GET
	@Path("stoplivemjpegstream/{printerName}/clientid/{clientId}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse stopLiveStream(@PathParam("clientId") String clientId) {
		liveStreamerModificationLock.lock();
		try {
			ClientStream stream = mjpegStreamerClients.remove(clientId);
			if (stream != null) {
				stream.close();
			}
		} finally {
			liveStreamerModificationLock.unlock();
		}
		
		return new MachineResponse("stoplivemjpegstream", true, "Live stream stopped");
	}

    @ApiOperation(value = "Takes a snapshot of the current build setup in config.properties for imagingCommand=[jsonFormattedNativeOSCommand]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	//TODO: We need to actually get the printer by printername and then get the commandLineParameters from the MachineConfig not the HostProperties!!
	@GET
	@Path("takesnapshot/{printerName}/x/{x}/y/{y}")
    @Produces("image/jpg")
	public StreamingOutput takePicture(@PathParam("printerName") String printerName, @PathParam("x") final int x, @PathParam("y") final int y) {
	    return new ImageSnapshotCapture(x, y);
	}
}
