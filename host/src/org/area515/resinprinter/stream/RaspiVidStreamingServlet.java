package org.area515.resinprinter.stream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RaspiVidStreamingServlet extends HttpServlet {
	private Process raspiVidProcess;
	private Lock raspiVidProcessLock = new ReentrantLock();
	private AtomicInteger viewers = new AtomicInteger();
	private BufferedInputStream inputStream;
    private byte[] buffer;
    
	public boolean createViewer() {
		if (viewers.addAndGet(1) == 1) {
			raspiVidProcessLock.lock();
			try {
				raspiVidProcess = Runtime.getRuntime().exec("raspivid -w 100 -h 100 -n -t 10000 -o -");
				inputStream = new BufferedInputStream(raspiVidProcess.getInputStream());
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				viewers.decrementAndGet();
				return false;
			} finally {
				raspiVidProcessLock.unlock();
			}
		}
		
		return true;
	}
	
	public void freeViewer() {
		if (viewers.decrementAndGet() == 0) {
			raspiVidProcessLock.lock();
			if (viewers.get() == 0) {
				try {
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
		}
	}
	
	public byte[] getNextBuffer(final HttpServletResponse resp, int timeout, TimeUnit unit) {
		CyclicBarrier barrier = new CyclicBarrier(
				viewers.get(),
				new Runnable() {
					public void run() {
						if (buffer == null) {
							buffer = new byte[resp.getBufferSize() - 12];//12 bytes less due to:http://stackoverflow.com/questions/9031311/slow-transfers-in-jetty-with-chunked-transfer-encoding-at-certain-buffer-size
						}
						
						raspiVidProcessLock.lock();
						try {
							if (inputStream == null) {
								throw new IllegalArgumentException("Nobody is left to stream??? How did this happen?");
							}
							int bytesRead = 0;
					        while( (bytesRead += inputStream.read(buffer, bytesRead, buffer.length - bytesRead)) < buffer.length ) {
					        }
				        } catch (IOException e) {
				        	e.printStackTrace();
				        	throw new IllegalArgumentException("IOException when reading from buffer. Nobody left to stream???", e);
				        } finally {
				        	raspiVidProcessLock.unlock();
				        }
					};
				});
		try {
			barrier.await(timeout, unit);
		} catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
			e.printStackTrace();
		}
		
		return buffer;
	}
	
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        OutputStream outStream = response.getOutputStream();
        if (!createViewer()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        
        try {
            while( true ) {//Basically you stream until a client disconnects
                byte[] buffer = getNextBuffer(response, 7, TimeUnit.SECONDS);//We only give 7 seconds to offload the entire buffer to the rest of the clients
                outStream.write(buffer, 0, buffer.length);
                outStream.flush();
            }
        } catch (IOException e) {
        	e.printStackTrace();
        } finally {
            if( outStream != null ) {
            	try {
            		outStream.close();
            	} catch (IOException e) {}
            }
            
            freeViewer();
        }
    }
}