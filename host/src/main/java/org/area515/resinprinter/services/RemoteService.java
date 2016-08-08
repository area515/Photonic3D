package org.area515.resinprinter.services;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.security.RolesAllowed;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.naming.InvalidNameException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.security.UserManagementException;
import org.area515.resinprinter.security.keystore.RendezvousClient;
import org.area515.resinprinter.util.security.PhotonicCrypto;
import org.area515.resinprinter.util.security.PhotonicUser;
import org.jboss.resteasy.core.Headers;

@Api(value="remote")
@RolesAllowed({PhotonicUser.FULL_RIGHTS, PhotonicUser.REMOTE_EXECUTION})
@Path("remote")
public class RemoteService {
	private static final String API_DOC = "This method allows a user to securely execute any restful function on a remote instance of Photonic3d. "
			+ "The returned response from this method should be identical to the returned response that occurred on the remote instance. "
			+ "The <b class=\"code\">toRemoteUserId</b> is the userId of the remote user that has friended the local user and allowed the local user to execute remote calls."
			+ "The <b class=\"code\">relativeURL</b> is the restful function that you would like to execute on the remote instance of Photonic3d."
			+ "The following are the set of prerequisites that must first take place before this function can be successful."
			+ " <div class='code'>A user(local user) must exist on the local running instance of Photonic 3D.</div>"
			+ " <div class='code'>A different user(remote user) must exist on the remote instance of Photonic 3D.</div>"
			+ " <div class='code'>The local user must have been given the <b class=\"code\">login</b>, <b class=\"code\">userAdmin</b> & <b class=\"code\">remoteExecution</b> roles.</div>"
			+ " <div class='code'>The remote user must have been given the <b class=\"code\">login</b> & <b class=\"code\">userAdmin</b> roles.</div>"
			+ " <div class='code'>The local user must send a friend request using the \"" + PhotonicCrypto.FEATURE_NAME + "\" feature to the remote user.</div>"
			+ " <div class='code'>The remote user must accept the friend request from the local user.</div>"
			+ " <div class='code'>The remote user must must give the <b class=\"code\">login</b> role to their new friend(the local user).</div>"
			+ " <div class='code'>The remote user must must give any other role to their new friend(the local user) that coresponds with the restful function they would like to grant access.</div>"
			+ " <div class='code'>The local user must be logged in to their own instance of Photonic 3D to sign and encrypt the message(this restful function).</div>"
			+ " <div class='code'>The remote user must be logged in to their own instance of Photonic 3D in order to verify and decrypt all incoming messages(restful functions) from the local user.</div>"
			+ "";
    private static final Logger logger = LogManager.getLogger();
	public static RemoteService INSTANCE = new RemoteService();
	
	public URI getHostPort(UriInfo uriInfo) throws URISyntaxException {
		URI baseURI = uriInfo.getBaseUri();
		return new URI(baseURI.getScheme(), null, baseURI.getHost(), baseURI.getPort(), null, null, null);
	}
	
	public String buildRelativeURL(UriInfo uriInfo) {
		List<PathSegment> segments = uriInfo.getPathSegments();
		int relativeLength = uriInfo.getBaseUri().toString().length() + segments.get(0).getPath().length() + segments.get(1).getPath().length() + segments.get(2).getPath().length() + 3;
		return  uriInfo.getRequestUri().toString().substring(relativeLength);
	}
	
	public void setHeaders(HttpRequestBase requestToSend, HttpServletRequest request) {
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			Enumeration<String> values = request.getHeaders(name);
			while (values.hasMoreElements()) {
				requestToSend.addHeader(name, values.nextElement());
			}
		}
	}
	
	public Response genericExecute(HttpRequestBase requestToSend, HttpServletRequest request, UriInfo uriInfo, String toUserId) throws UserManagementException {
		PhotonicUser currentUser = (PhotonicUser)request.getUserPrincipal();
		if (currentUser == null) {
			throw new UserManagementException("You have to be logged in to execute remote functions.");
		}

		UUID toUser = UUID.fromString(toUserId);
		setHeaders(requestToSend, request);
		RendezvousClient client;
		try {
			client = RendezvousClient.getServer(getHostPort(uriInfo));
		} catch (Exception e) {
			logger.error("Couldn't connect to rendezvous server", e);
			throw new UserManagementException("Couldn't connect to rendezvous server");
		}
		
		try {
			HttpResponse response = client.sendRequestToRemote(currentUser.getUserId(), toUser, requestToSend, 20, TimeUnit.SECONDS);
			Headers<Object> map = new Headers<Object>();
			for (Header header : response.getAllHeaders()) {
				map.add(header.getName(), header.getValue());
			}
			return Response.status(response.getStatusLine().getStatusCode())
					.entity(response.getEntity().getContent())
					.replaceAll(map)
					.build();
		} catch (HttpException e) {
			logger.error("Couldn't connect to rendezvous server", e);
			throw new UserManagementException("Couldn't connect to rendezvous server", e);
		} catch (InterruptedException e) {
			logger.error("Interrupted while waiting for response from rendezvous server", e);
			throw new UserManagementException("Interrupted while waiting for response from rendezvous server", e);
		} catch (TimeoutException e) {
			logger.error("Photonic 3d stopped waiting for the remote system to respond.", e);
			throw new UserManagementException("Photonic 3d stopped waiting for the remote system to respond", e);
		} catch (InvalidKeyException | CertificateExpiredException | CertificateNotYetValidException
				| NoSuchPaddingException | SignatureException | InvalidNameException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
				| NoSuchAlgorithmException e) {
			logger.error("A signature or encryption problem occurred communicating with the remote user", e);
			throw new UserManagementException("A signature or encryption problem occurred communicating with the remote user", e);
		} catch (IOException e) {
			logger.error("An IO error occurred communicating with the other server", e);
			throw new UserManagementException("An IO error occurred communicating with the other server", e);
		}
	}
	
    @ApiOperation(value = API_DOC)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR),
    		@ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR)})
	@POST
	@Path("execute/{toRemoteUserId}/{relativeURL:.*}")
	public Response callPost(
			@PathParam("remoteUserId") String toRemoteUserId,
			@Context UriInfo uriInfo,
			@Context HttpServletRequest incomingRequest,
			InputStream istream) throws UserManagementException {
		HttpPost outgoingRequest = new HttpPost(buildRelativeURL(uriInfo));
		outgoingRequest.setEntity(new InputStreamEntity(istream));
		return genericExecute(outgoingRequest, incomingRequest, uriInfo, toRemoteUserId);
	}
	
    @ApiOperation(value = API_DOC)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR),
    		@ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR)})
	@PUT
	@Path("execute/{toRemoteUserId}/{relativeURL:.*}")
	public Response callPut(
			@PathParam("remoteUserId") String toRemoteUserId,
			@Context UriInfo uriInfo,
			@Context HttpServletRequest incomingRequest,
			InputStream istream) throws UserManagementException {
		HttpPut outgoingRequest = new HttpPut(buildRelativeURL(uriInfo));
		outgoingRequest.setEntity(new InputStreamEntity(istream));
		return genericExecute(outgoingRequest, incomingRequest, uriInfo, toRemoteUserId);
	}
	
    @ApiOperation(value = API_DOC)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR),
    		@ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR)})
	@DELETE
	@Path("execute/{toRemoteUserId}/{relativeURL:.*}")
	public Response callDelete(
			@PathParam("remoteUserId") String toRemoteUserId,
			@Context UriInfo uriInfo,
			@Context HttpServletRequest incomingRequest) throws UserManagementException {
		HttpDelete outgoingRequest = new HttpDelete(buildRelativeURL(uriInfo));
		return genericExecute(outgoingRequest, incomingRequest, uriInfo, toRemoteUserId);
	}

    @ApiOperation(value = API_DOC)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR),
    		@ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR)})
	@HEAD
	@Path("execute/{toRemoteUserId}/{relativeURL:.*}")
	public Response callHead(
			@PathParam("remoteUserId") String toRemoteUserId,
			@Context UriInfo uriInfo,
			@Context HttpServletRequest incomingRequest) throws UserManagementException {
		HttpHead outgoingRequest = new HttpHead(buildRelativeURL(uriInfo));
		return genericExecute(outgoingRequest, incomingRequest, uriInfo, toRemoteUserId);
	}
	
    @ApiOperation(value = API_DOC)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR),
    		@ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR)})
	@OPTIONS
	@Path("execute/{toRemoteUserId}/{relativeURL:.*}")
	public Response callOptions(
			@PathParam("remoteUserId") String toRemoteUserId,
			@Context UriInfo uriInfo,
			@Context HttpServletRequest incomingRequest) throws UserManagementException {
		HttpOptions outgoingRequest = new HttpOptions(buildRelativeURL(uriInfo));
		return genericExecute(outgoingRequest, incomingRequest, uriInfo, toRemoteUserId);
	}
	
    @ApiOperation(value = API_DOC)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR),
    		@ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR)})
	@GET
	@Path("execute/{toRemoteUserId}/{relativeURL:.*}")
	public Response callGet(
			@PathParam("remoteUserId") String toRemoteUserId,
			@Context UriInfo uriInfo,
			@Context HttpServletRequest incomingRequest) throws UserManagementException {
		HttpGet outgoingRequest = new HttpGet(buildRelativeURL(uriInfo));
		return genericExecute(outgoingRequest, incomingRequest, uriInfo, toRemoteUserId);
	}
}
