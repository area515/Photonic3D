package org.area515.util;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ExceptionMarshaller implements ExceptionMapper<Exception> {
	
	@Context
	private HttpHeaders headers;
	
	@Override
	public Response toResponse(Exception exception) {
		exception.printStackTrace();
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"type\":\"" + exception.getClass().getSimpleName() + "\",");
		sb.append("\"message\":\"" + exception.getMessage() + "\"}");
		Response response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(sb.toString()).build();
		return response;
	}
}