package org.area515.resinprinter.stream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.area515.resinprinter.services.MediaService;

public class ProgressiveDownloadServlet extends HttpServlet {
	private static final long serialVersionUID = 5110548757293069069L;
	
	public ProgressiveDownloadServlet() {
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("HTTPGet:" + request.getRequestURI());
		doAll(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("HTTPPost:" + request.getRequestURI());
		doAll(request, response);
	}
	
	private void doAll(HttpServletRequest request, HttpServletResponse response) {
		File servedFile = MediaService.INSTANCE.getRecordedFile();
		Path path = servedFile != null?servedFile.toPath():null;

		if (servedFile == null || path == null || !path.toFile().exists()) {
			if (servedFile == null) {
				System.out.println("Couldn't find resource:" + request.getRequestURI());
			} else {
				System.out.println("File doesn't exist:" + path + " for resource:" + request.getRequestURL());
			}
			try {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Couldn't send error back to client for path:" + request.getPathInfo());
			}
			return;
		}
		
		OutputStream outputStream = null;
		SeekableByteChannel input = null;
        try {
    		//lock.lock();

			long fileSize = Files.size(path);
	    	
	    	Pattern rangePattern = Pattern.compile("bytes=(\\d*)-(\\d*)");
	        response.setContentType(Files.probeContentType(path));
	        response.setHeader("Accept-Ranges", "bytes");
	        response.setHeader("TransferMode.DLNA.ORG", "Streaming");
	        response.setHeader("File-Size", fileSize + "");
	        
	    	/*Enumeration<String> headerNames = request.getHeaderNames();
	    	while (headerNames.hasMoreElements()) {
	    		String name = headerNames.nextElement();
	    		System.out.println(name + ":" + request.getHeader(name));
	    	}*/
	
	        OutputStream outStream = response.getOutputStream();
	        String rangeValue = request.getHeader("Range");
	        long start = 0;
	        long end = response.getBufferSize() - 1;
	        if (rangeValue != null) {
		        Matcher matcher = rangePattern.matcher(rangeValue);
		        if (matcher.matches()) {
		            String startGroup = matcher.group(1);
		            start = startGroup == null || startGroup.isEmpty() ? start : Integer.valueOf(startGroup);
		            start = start < 0 ? 0 : start;
		       
		            String endGroup = matcher.group(2);
		            end = endGroup == null || endGroup.isEmpty() ? (start + end < fileSize?start + end:fileSize - 1): Integer.valueOf(endGroup);
	                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
		        } else {
		        	rangeValue = null;
		        }
	        }
	        response.setStatus(rangeValue == null? HttpServletResponse.SC_OK:HttpServletResponse.SC_PARTIAL_CONTENT); // 206.
	        
	        int bytesRead;
	        long bytesLeft = rangeValue == null? fileSize: (int)(end - start + 1);
	        response.setContentLength((int)bytesLeft);
	        ByteBuffer buffer = ByteBuffer.allocate(response.getBufferSize());
	        input = Files.newByteChannel(path, StandardOpenOption.READ);
	        OutputStream output = response.getOutputStream();
	        input.position(start);
	        while ((bytesRead = input.read(buffer)) != -1 && bytesLeft > 0) {
	            buffer.clear();
	            output.write(buffer.array(), 0, bytesLeft < bytesRead ? (int)bytesLeft : bytesRead);
	            bytesLeft -= bytesRead;
	        }
	        
            outStream.flush();
        } catch (IOException e) {
        	e.printStackTrace();
        	System.out.println("Error handling file:" + path);
        } finally {
			if (input != null)
				try {input.close();} catch (IOException e) {}
			
			if (outputStream != null)
				try {outputStream.flush();} catch (IOException e) {}
			
			//lock.unlock();
		}
	}
}
