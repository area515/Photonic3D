package org.area515.resinprinter.services;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.Set;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.plugin.FeatureManager;
import org.area515.resinprinter.security.PhotonicUser;
import org.area515.resinprinter.security.UserManagementException;
import org.eclipse.jetty.util.security.Credential;

@Api(value="users")
@RolesAllowed(UserService.FULL_RIGHTS)
@Path("user")
public class UserService {
    private static final Logger logger = LogManager.getLogger();

	public static final String FULL_RIGHTS = "admin";
	public static UserService INSTANCE = new UserService();
	
    @ApiOperation(value = "Gets all known users to this Photonic installation.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	public Set<PhotonicUser> getKnownUsers() {
    	return FeatureManager.getUserManagementFeature().getUsers();
    }
    
    @ApiOperation(value = "Creates a new Photonic 3d user.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@POST
	@Path("create/{username}/{password}")
	@Produces(MediaType.APPLICATION_JSON)
	public PhotonicUser createNewUser(@PathParam("username")String username, @PathParam("password")String password) throws UserManagementException {
		return FeatureManager.getUserManagementFeature().update(username, Credential.getCredential(password), new String[]{UserService.FULL_RIGHTS});
	}
    
    @ApiOperation(value = "Deletes the specified Photonic 3d user.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@DELETE
	@Path("delete/{userId}")
	@Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@PathParam("userId")String userId) {
		try {
			FeatureManager.getUserManagementFeature().remove(new PhotonicUser(null, UUID.fromString(userId)));
			return Response.status(Status.OK).build();
		} catch (UserManagementException e) {
			logger.error(e);
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
    }
}
