package org.area515.resinprinter.services;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.plugin.FeatureManager;
import org.area515.resinprinter.security.Friend;
import org.area515.resinprinter.security.FriendshipFeature;
import org.area515.resinprinter.security.PhotonicUser;
import org.area515.resinprinter.security.UserManagementException;

@Api(value="users")
@RolesAllowed({PhotonicUser.FULL_RIGHTS, PhotonicUser.USER_ADMIN})
@Path("user")
public class UserService {
    private static final Logger logger = LogManager.getLogger();

	public static UserService INSTANCE = new UserService();
	
    @ApiOperation(value = "Gets all local users and trusted remote users(friends) of this Photonic3d installation.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	public Set<PhotonicUser> getKnownUsers() {
    	return FeatureManager.getUserManagementFeature().getUsers();
    }
    
    @ApiOperation(value = "Creates a new local Photonic 3d user.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@POST
	@Path("create")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public PhotonicUser createNewUser(PhotonicUser user) throws UserManagementException {
		return FeatureManager.getUserManagementFeature().update(user);
	}
    
    @ApiOperation(value = "Deletes the specified Photonic 3d user. This user can be a 'Friend' or a local user.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@DELETE
	@Path("delete/{userId}")
	@Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@PathParam("userId")String userId) {
		try {
			FeatureManager.getUserManagementFeature().remove(new PhotonicUser(null, null, UUID.fromString(userId), null, null));
			return Response.status(Status.OK).build();
		} catch (UserManagementException e) {
			logger.error(e);
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
    }
    
    @ApiOperation(value = "Creates(trusts) a new remote Photonic 3d user. "
    		+ "A 'Friend' in Photonic3d is nothing more than a remote user that has been given rights to perform actions on your printer.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@POST
	@Path("friend")
	@Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response trustUser(Friend friend) {
		try {
			FeatureManager.getUserManagementFeature().trustNewFriend(friend);
			FeatureManager.getFriendshipFeatures().get(friend.getFriendshipFeature()).acceptFriendRequest(friend);
			return Response.status(Status.OK).build();
		} catch (UserManagementException e) {
			logger.error(e);
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
    }
    
    @ApiOperation(value = "Sends a friend request to the specified friendship feature and waits for an immediate response from the remote user.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@POST
	@Path("requestFriendshipAndWait/{friendshipFeature}/{userIdToSendRequest}")
	@Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Friend sendTrustRequest(
		@PathParam("friendshipFeature")String friendshipFeatureName, 
		@PathParam("userIdToSendRequest")String userId,
		@Context HttpServletRequest request) throws UserManagementException {
		PhotonicUser currentUser = (PhotonicUser)request.getUserPrincipal();
		if (currentUser == null) {
			throw new UserManagementException("You have to be logged in to friend someone.");
		}

		Map<String, FriendshipFeature> features = FeatureManager.getFriendshipFeatures();
		FriendshipFeature friendshipFeature = features.get(friendshipFeatureName);
		return friendshipFeature.sendFriendRequest(currentUser, new PhotonicUser(null, null, UUID.fromString(userId), null, null));
    }
    
    @ApiOperation(value = "Gets all of the remote users that have asked us to be friends.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("waitingFriendRequests")
	@Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Friend> getPotentialFriends() throws UserManagementException {
		List<Friend> allFriends = new ArrayList<Friend>();
		for (FriendshipFeature feature : FeatureManager.getFriendshipFeatures().values()) {
			allFriends.addAll(feature.getFriendRequests());
		}
		
		return allFriends;
    }
}
