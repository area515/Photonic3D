package org.area515.resinprinter.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.area515.resinprinter.job.PrintJobManager;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;
import org.area515.util.PrintFileFilter;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
 
@Path("files")
public class FileService {
	public static FileService INSTANCE = new FileService();
	public static final String UNKNOWN_FILE = "I don't know how do deal with a file of this type:";
	public static final String NO_FILE = "You didn't attempt to upload a file, or the filename was Blank.";
	
	private FileService() {
	}
	
	public static Response uploadFile(MultipartFormDataInput input, File directory) {
		String fileName = "";
		Map<String, List<InputPart>> formParts = input.getFormDataMap();
	
		List<InputPart> inPart = formParts.get("file");
		if (inPart == null) {
			System.out.println("No file specified in multipart mime!");
			return Response.status(500).build();
		}
		
		File newUploadFile = null;
		for (InputPart inputPart : inPart) {
			try {
				// Retrieve headers, read the Content-Disposition header to
				// obtain the original name of the file
				MultivaluedMap<String, String> headers = inputPart.getHeaders();
				fileName = parseFileName(headers);

				// If the filename was blank we aren't interested in the file.
				if (fileName == null || fileName.isEmpty()) {
					return Response.status(Status.BAD_REQUEST).entity(NO_FILE).build();
				}

				// Handle the body of that part with an InputStream
				InputStream istream = inputPart.getBody(InputStream.class, null);

				// fileName = SERVER_UPLOAD_LOCATION_FOLDER + fileName;
				newUploadFile = new File(directory, fileName);

				if (!saveFile(istream, newUploadFile.getAbsoluteFile())) {
					return Response.status(Status.BAD_REQUEST).entity(UNKNOWN_FILE + fileName).build();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	    String output = "File saved to location: " + newUploadFile;
		return Response.status(Status.OK).entity(output).build();
	}

	@POST
	@Path("/uploadPrintableFile")
	@Consumes("multipart/form-data")
	public Response uploadPrintableFile(MultipartFormDataInput input) {
		return uploadFile(input, HostProperties.Instance().getUploadDir());
	}

		// Parse Content-Disposition header to get the original file name
		static String parseFileName(MultivaluedMap<String, String> headers) {
			String[] contentDispositionHeader = headers.getFirst("Content-Disposition").split(";");
			for (String name : contentDispositionHeader) {
				if ((name.trim().startsWith("filename"))) {
					String[] tmp = name.split("=");
					String fileName = tmp[1].trim().replaceAll("\"","");
					return fileName;
				}
			}
			
			return "randomName";
		}

		// save uploaded file to a defined location on the server
		static boolean saveFile(InputStream uploadedInputStream, File permanentFile) throws IOException {
			OutputStream output = null;
			try {
				File tempFile = File.createTempFile("upload", permanentFile.getName().substring(permanentFile.getName().lastIndexOf(".")));
				output = new FileOutputStream(tempFile);
				IOUtils.copy(uploadedInputStream, output);
				try {output.close();} catch (IOException e) {}
				Files.move(tempFile.toPath(), permanentFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				if (PrintFileFilter.INSTANCE.accept(permanentFile)) {
					NotificationManager.fileUploadComplete(permanentFile);
					return true;
				}
					
				Files.delete(permanentFile.toPath());
				return false;
			} finally {
				if (output != null) {
					try {output.close();} catch (IOException e) {}
				}
				if (uploadedInputStream != null) {
					try {uploadedInputStream.close();} catch (IOException e) {}
				}
			}
		}
	
	
    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     * @throws IOException 
     */
    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getProjects() throws IOException {
    	File dir = HostProperties.Instance().getUploadDir();
		File[] acceptedFiles = dir.listFiles(PrintFileFilter.INSTANCE);
		ArrayList<String> names = new ArrayList<String>();
		for(File file : acceptedFiles){
			names.add(file.getName());
		}
		
        return names;
    }
	
	 /**
	  * Method handling HTTP GET requests. The returned object will be sent
	  * to the client as "text/plain" media type.
	  * 
	  * @return String that will be returned as a text/plain response.
	  * @throws IOException
	  */
	 @GET
	 @Path("delete/{filename}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse deleteFile(@PathParam("filename") String fileName) {
		List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(fileName);
		for (PrintJob currentJob : jobs) {
			if (currentJob != null && currentJob.getPrinter().isPrintInProgress()) {
				return new MachineResponse("delete", false, "Can't delete job:" + fileName + " while print is in progress.");
			}
		}
	
		File currentFile = new File(HostProperties.Instance().getUploadDir(), fileName);
		if (!currentFile.delete()) {
			return new MachineResponse("delete", false, "Unable to delete:" + fileName);
		}
	
		for (PrintFileProcessor currentProcessor : HostProperties.Instance().getPrintFileProcessors()) {
			if (currentProcessor.acceptsFile(currentFile)) {
				try {
					currentProcessor.cleanupEnvironment(currentFile);
					return new MachineResponse("delete", true, "Deleted:" + fileName);
				} catch (JobManagerException e) {
					return new MachineResponse("delete", false, e.getMessage());
				}
			}
		}
		
		return new MachineResponse("delete", true, "I couldn't figure out how to clean up:" + fileName);
	 }
	 
	 @POST
	 @Path("uploadviaurl/{filename}/{uri}")
	 @Produces(MediaType.APPLICATION_JSON)
	 public MachineResponse uploadViaURL(@PathParam("uri") String uriString, @PathParam("filename") String filename) {
		URI uri;
		try {
			uri = new URI(uriString);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return new MachineResponse("uploadviaurl", false, "That was not a valid URI");
		}
		
		String fileName = filename;//uri.getPath().replaceFirst(".*/([^/]+)", "$1") + filetype;
		List<PrintJob> jobs = PrintJobManager.Instance().getJobsByFilename(fileName);
		for (PrintJob currentJob : jobs) {
			if (currentJob != null && currentJob.getPrinter().isPrintInProgress()) {
				return new MachineResponse("delete", false, "Can't delete job:" + fileName + " while print is in progress.");
			}
		}
		
		File currentFile = new File(HostProperties.Instance().getUploadDir(), fileName);
		for (PrintFileProcessor currentProcessor : HostProperties.Instance().getPrintFileProcessors()) {
			if (currentProcessor.acceptsFile(currentFile)) {
				final File newUploadFile = new File(HostProperties.Instance().getUploadDir(), fileName);
				try {
			        CloseableHttpClient httpclient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
			        HttpGet httpget = new HttpGet(uri);
			        httpget.setHeader("User-Agent","Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.90 Safari/537.36");
			        CloseableHttpResponse response = httpclient.execute(httpget);
			        
					final InputStream stream = response.getEntity().getContent();
			        Main.GLOBAL_EXECUTOR.submit(new Runnable() {
						@Override
						public void run() {
							try {
								saveFile(stream, newUploadFile);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
					return new MachineResponse("uploadviaurl", true, "Upload started:" + fileName);
				} catch (MalformedURLException | IllegalStateException e) {
					return new MachineResponse("uploadviaurl", false, "I didn't understand this URI:" + uri);
				} catch (IOException e) {
					return new MachineResponse("uploadviaurl", false, "Error while downloading URI:" + uri);
				}
			}
		}
		
		return new MachineResponse("uploadviaurl", false, UNKNOWN_FILE + filename);
	 }
}