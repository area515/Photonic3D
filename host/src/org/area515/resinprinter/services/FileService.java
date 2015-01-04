package org.area515.resinprinter.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.io.FileUtils;
import org.area515.resinprinter.job.JobManager;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.services.domain.Files;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
 
@Path("files")
public class FileService {
		@POST
		@Path("/upload")
		@Consumes("multipart/form-data")
		public Response uploadFile(MultipartFormDataInput input) {

			String fileName = "";

			Map<String, List<InputPart>> formParts = input.getFormDataMap();

			List<InputPart> inPart = formParts.get("file");

			for (InputPart inputPart : inPart) {

				 try {

					// Retrieve headers, read the Content-Disposition header to obtain the original name of the file
					MultivaluedMap<String, String> headers = inputPart.getHeaders();
					fileName = parseFileName(headers);

					// Handle the body of that part with an InputStream
					InputStream istream = inputPart.getBody(InputStream.class,null);

//					fileName = SERVER_UPLOAD_LOCATION_FOLDER + fileName;
					File newUploadFile = new File(HostProperties.Instance().getUploadDir(), fileName);

					saveFile(istream, newUploadFile.getAbsolutePath());

				  } catch (IOException e) {
					e.printStackTrace();
				  }

				}

	                String output = "File saved to server location : " + fileName;

	                ResponseBuilder response = Response.status(200);
//	                response.header("Access-Control-Allow-Origin", "*");
//	                response.header("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
//	                response.header("Access-Control-Allow-Headers", "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept");
//	                response.header("Access-Control-Max-Age", "1728000");
//	                response.entity(output);
//	          return response.build();      
////	                Response.status(200).header("Access-Control-Allow-Origin", "*");
			return Response.status(200).entity(output).build();
		}

		// Parse Content-Disposition header to get the original file name
		private String parseFileName(MultivaluedMap<String, String> headers) {

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
		private void saveFile(InputStream uploadedInputStream,
			String serverLocation) {

			try {
				OutputStream outpuStream = new FileOutputStream(new File(serverLocation));
				int read = 0;
				byte[] bytes = new byte[1024];

				outpuStream = new FileOutputStream(new File(serverLocation));
				while ((read = uploadedInputStream.read(bytes)) != -1) {
					outpuStream.write(bytes, 0, read);
				}
				outpuStream.flush();
				outpuStream.close();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
	
	
//	private static final String BASE_PATH = HostProperties.Instance().getSourceDir();
 
	
	
	
//	 @POST
//	    @Consumes("multipart/form-data")
//	    @Path("/image")
//	    public void image(@Multipart(value = "image", type = "image/jpeg") Attachment image){
//	    	
//	    	
//	    		DataHandler handler = image.getDataHandler();
//	            try {
//	               InputStream stream = handler.getInputStream();
//	               MultivaluedMap<String, String> map = image.getHeaders();
//	               System.out.println("fileName Here" + getFileName(map));
//	               OutputStream out = new FileOutputStream(new File(HostProperties.Instance().getSourceDir(),getFileName(map)));
//
//	               int read = 0;
//	               byte[] bytes = new byte[1024];
//	               while ((read = stream.read(bytes)) != -1) {
//	                  out.write(bytes, 0, read);
//	               }
//	               stream.close();
//	               out.flush();
//	               out.close();
//	            } catch (Exception e) {
//	               e.printStackTrace();
//	            }
//	            
//	    }
//	 private String getFileName(MultivaluedMap<String, String> header) {
//	        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
//	        for (String filename : contentDisposition) {
//	           if ((filename.trim().startsWith("filename"))) {
//	              String[] name = filename.split("=");
//	              String exactFileName = name[1].trim().replaceAll("\"", "");
//	              return exactFileName;
//	           }
//	        }
//	        return "unknown";
//	     }
//	@POST
//	@Path("upload")
//	@Consumes(MediaType.MULTIPART_FORM_DATA)
//	@Produces(MediaType.TEXT_PLAIN)
//	public String uploadFile(
//			@FormDataParam("file") InputStream fileInputStream,
//			@FormDataParam("file") FormDataContentDisposition fileDisposition)
//			throws FileNotFoundException, IOException {
// 
//		String fileName = fileDisposition.getFileName();
//		System.out.println("***** fileName " + fileDisposition.getFileName());
//		String filePath = BASE_PATH + fileName;
//		try (OutputStream fileOutputStream = new FileOutputStream(filePath)) {
//			int read = 0;
//			final byte[] bytes = new byte[1024];
//			while ((read = fileInputStream.read(bytes)) != -1) {
//				fileOutputStream.write(bytes, 0, read);
//			}
//		}
// 
//		return "File Upload Successfully !!";
//	}
	
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
    public Files getProjects() throws IOException {
//    	System.out.println("Getting projects");
//    	System.out.println(HostProperties.Instance().getUploadDir());
    	File dir = HostProperties.Instance().getUploadDir();
//    	System.out.println(dir.exists());
    	if("Vault user:".contains("")){
    		
    	}
		String[] extensions = new String[] { "zip", "cws" };
		
		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
		ArrayList<String> names = new ArrayList<String>();
		for(File file : files){
//			System.out.println("Found file: " + file.getAbsolutePath());
			names.add(file.getName());
		}
//		for(String name: names){
//			System.out.println("Project Name: " + name);
//		}
//		if(names.isEmpty()){System.out.println("Didn't find any files in the directory");}
    	Files projects = new Files(names);
    	
        return projects;
    }
	
}