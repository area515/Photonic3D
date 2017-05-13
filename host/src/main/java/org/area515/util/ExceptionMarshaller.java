package org.area515.util;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Provider
public class ExceptionMarshaller implements ExceptionMapper<Exception> {
    private static final Logger logger = LogManager.getLogger();

    public Response toResponse(Exception e) {
		logger.error("Error caught by exception marshaller and relayed to browser", e);
		if (e.getMessage() == null) {
			return Response.status(Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity("Internal server error").build();
		}
		
		return Response.status(Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
	}
}