package org.area515.util;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ExceptionMarshaller implements ExceptionMapper<Exception> {
	public Response toResponse(Exception e) {
		e.printStackTrace();
		return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
	}
}