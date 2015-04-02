package org.area515.resinprinter.stream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.area515.resinprinter.server.HostProperties;

public class CommandLineStreamingServlet extends HttpServlet {
	private static final long serialVersionUID = 2174785174671619450L;
	private final long maxTimeSinceLastRequest = 60000;
	private long lastRequest = 0;
	private final byte[] buffer = new byte[102400000];
	private int bufferTail = 0;
	private Process raspiVidProcess;
	private InputStream inputStream;
	private Lock raspiVidProcessLock = new ReentrantLock();
	
    public static void main(String[] args) throws IOException {
    	Path path = Paths.get("c:\\Users\\wgilster\\desktop\\toystory.mp4");
    	System.out.println(Files.probeContentType(path));
	}
    
	public boolean startVideo() {
		if (raspiVidProcess == null) {
			raspiVidProcessLock.lock();
			if (raspiVidProcess == null) {
				Thread thread = new Thread(new Runnable() {
					public void run() {
						
						try {
							int bytesToRead = 0;
							System.out.println("filling buffer");
					        while(bytesToRead > -1) {
								bytesToRead = inputStream.available();
								if (bytesToRead > buffer.length - bufferTail) {
									bytesToRead = buffer.length - bufferTail;
								}
								bytesToRead = inputStream.read(buffer, bufferTail, bytesToRead);
								bufferTail += bytesToRead;
								if (bufferTail >= buffer.length) {
									bufferTail = 0;
								}
								
								if (System.currentTimeMillis() - lastRequest > maxTimeSinceLastRequest) {
									return;
								}
					        }
						} catch (IOException e) {
				        	e.printStackTrace();
				        	throw new IllegalArgumentException("IOException when reading from buffer. Did the streamer die???", e);
						} finally {
							endVideo();
						}
					};
				}, "VideoStreamingThread");
				thread.setDaemon(true);
				
				try {
					raspiVidProcess = Runtime.getRuntime().exec(HostProperties.Instance().getStreamingCommand());
					inputStream = new BufferedInputStream(raspiVidProcess.getInputStream());
					thread.start();
					return true;
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				} finally {
					raspiVidProcessLock.unlock();
				}
			}
		}
		
		return true;
	}
	
	public void endVideo() {
		raspiVidProcessLock.lock();
		try {
			if (raspiVidProcess == null) {
				return;
			}
			raspiVidProcess.destroy();
			try {
				inputStream.close();
			} catch (IOException e) {}//I don't know why I'm closing this stream.
			raspiVidProcess = null;
			inputStream = null;
		} finally {
			raspiVidProcessLock.unlock();
		}
	}
	
	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	System.out.println("Servlet HEAD");
    	System.out.println("===============");
    	Enumeration<String> headerNames = request.getHeaderNames();
    	while (headerNames.hasMoreElements()) {
    		String name = headerNames.nextElement();
    		System.out.println(name + ":" + request.getHeader(name));
    	}

        if (!startVideo()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        
		Cookie cookies[] = request.getCookies();
		int bufferHead = 0;
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("BufferHead")) {
					bufferHead = Integer.parseInt(cookie.getValue());
					System.out.println("BufferHead:" + bufferHead);
				}
			}
		}
	}
	
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	System.out.println("Servlet GET");
    	System.out.println("===============");
    	
        lastRequest = System.currentTimeMillis();
    	Pattern rangePattern = Pattern.compile("bytes=(\\d*)-(\\d*)");
    	
        response.setContentType("video/mp4");
        response.setHeader("Accept-Ranges", "bytes");
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.
        response.setHeader("Connection", "close");
        
    	Enumeration<String> headerNames = request.getHeaderNames();
    	while (headerNames.hasMoreElements()) {
    		String name = headerNames.nextElement();
    		System.out.println(name + ":" + request.getHeader(name));
    	}

        OutputStream outStream = response.getOutputStream();
        if (!startVideo()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        
        String rangeValue = request.getHeader("Range");
        int start = 0;
        int end = 0;
        if (rangeValue != null) {
	        Matcher matcher = rangePattern.matcher(rangeValue);
	        if (matcher.matches()) {
	            String startGroup = matcher.group(1);
	            start = startGroup == null || startGroup.isEmpty() ? start : Integer.valueOf(startGroup);
	            start = start < 0 ? 0 : start;
	       
	            String endGroup = matcher.group(2);
	            end = endGroup == null || endGroup.isEmpty() ? end : Integer.valueOf(endGroup);
	        }
        }
        
		/*Cookie cookies[] = request.getCookies();
		//int bufferHead = 0;
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("BufferHead") && start > 0) {
					bufferHead = Integer.parseInt(cookie.getValue());
					System.out.println("BufferHead:" + bufferHead);
				}
			}
		}*/
		
        try {
        	int recordedBufferTail = bufferTail;
        	int bufferHead = start % buffer.length;
        	
        	//This determines the initial amount of lag we'll have when we start streaming
			while (bufferHead >= recordedBufferTail) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
				
				recordedBufferTail = bufferTail;
        	}
        	
        	//This section is when the client is asking for bounded limits on how much data they want.
        	if (end > 0) {
        		int requestedAmount = end - start + 1;
        		if (bufferHead > recordedBufferTail) {
        			int inc = buffer.length - bufferHead + recordedBufferTail;
        			if (requestedAmount < inc) {
        				recordedBufferTail -= inc - requestedAmount;
        			}
        			if (recordedBufferTail < 0) {
        				recordedBufferTail += buffer.length;
        			}
        		} else {
        			int inc = recordedBufferTail - bufferHead;
        			if (requestedAmount < inc) {
        				recordedBufferTail -= inc - requestedAmount;
        			}
        		}
        	}
        	
        	//This section deals with a maximum buffer size that should be sent to the client
        	int requestedAmount = response.getBufferSize();
    		if (bufferHead > recordedBufferTail) {
    			int inc = buffer.length - bufferHead + recordedBufferTail;
    			if (requestedAmount < inc) {
    				recordedBufferTail -= inc - requestedAmount;
    			}
    			if (recordedBufferTail < 0) {
    				recordedBufferTail += buffer.length;
    			}
    		} else {
    			int inc = recordedBufferTail - bufferHead;
    			if (requestedAmount < inc) {
    				recordedBufferTail -= inc - requestedAmount;
    			}
    		}        	

    		//End must at least be as large as start!
			end = end == 0?start:end;

        	//Since this is a circular buffer our head could come after the tail
    		if (bufferHead > recordedBufferTail) {
    			int inc = buffer.length - bufferHead + recordedBufferTail;
        		end += inc;
        		response.setContentLengthLong(inc);
                response.setHeader("Content-Range", "bytes " + start + "-" + (end - 1) + "/999999999999");
        		Cookie cookie = new Cookie("BufferHead", recordedBufferTail + "");
        		response.addCookie(cookie);
    			outStream.write(buffer, bufferHead, buffer.length - bufferHead);
    			outStream.write(buffer, 0, recordedBufferTail);
    			System.out.println("bytes starting from:" + bufferHead + " to:" + (bufferHead + inc - 1));
        		bufferHead += inc;
    		} else {
    			int inc = recordedBufferTail - bufferHead;
        		end += inc;
        		Cookie cookie = new Cookie("BufferHead", recordedBufferTail + "");
        		response.addCookie(cookie);
        		response.setHeader("Content-Range", "bytes " + start + "-" + (end - 1) + "/999999999999");
        		response.setContentLengthLong(inc);
    			outStream.write(buffer, bufferHead, inc);
    			System.out.println("bytes starting from:" + bufferHead + " to:" + (bufferHead + inc - 1));
        		bufferHead += inc;
    		}
    		
            outStream.flush();
        } catch (IOException e) {
        	e.printStackTrace();
        } finally {
            if( outStream != null ) {
            	try {
            		outStream.close();
            	} catch (IOException e) {}
            }
        }
    }
}