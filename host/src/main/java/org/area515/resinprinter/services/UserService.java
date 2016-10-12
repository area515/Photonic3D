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
import java.util.concurrent.ConcurrentHashMap;

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
import org.area515.resinprinter.security.FriendshipFeature;
import org.area515.resinprinter.security.UserManagementException;
import org.area515.resinprinter.util.security.Friend;
import org.area515.resinprinter.util.security.PhotonicUser;

@Api(value="users")
@RolesAllowed({PhotonicUser.FULL_RIGHTS, PhotonicUser.USER_ADMIN})
@Path("")
public class UserService {
    private static final Logger logger = LogManager.getLogger();

	public static UserService INSTANCE = new UserService();
	private ConcurrentHashMap<UUID, List<Message>> transientMessages = new ConcurrentHashMap<>();
	
	public class Message {
		private PhotonicUser fromUser;
		private PhotonicUser toUser;
		private String message;
		
		public Message(PhotonicUser fromUser, PhotonicUser toUser, String message) {
			this.fromUser = fromUser;
			this.toUser = toUser;
			this.message = message;
		}

		public PhotonicUser getFromUser() {
			return fromUser;
		}
		public void setFromUser(PhotonicUser fromUser) {
			this.fromUser = fromUser;
		}

		public PhotonicUser getToUser() {
			return toUser;
		}
		public void setToUser(PhotonicUser toUser) {
			this.toUser = toUser;
		}

		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((fromUser == null) ? 0 : fromUser.hashCode());
			result = prime * result + ((message == null) ? 0 : message.hashCode());
			result = prime * result + ((toUser == null) ? 0 : toUser.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Message other = (Message) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (fromUser == null) {
				if (other.fromUser != null)
					return false;
			} else if (!fromUser.equals(other.fromUser))
				return false;
			if (message == null) {
				if (other.message != null)
					return false;
			} else if (!message.equals(other.message))
				return false;
			if (toUser == null) {
				if (other.toUser != null)
					return false;
			} else if (!toUser.equals(other.toUser))
				return false;
			return true;
		}

		private UserService getOuterType() {
			return UserService.this;
		}
	}
	
    @ApiOperation(value = "Gets all local users and trusted remote users(friends) of this Photonic3d installation.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("users/list")
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
	@Path("users")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public PhotonicUser createNewUser(PhotonicUser user) throws UserManagementException {
    	if (user == null) {
    		throw new UserManagementException("No user specified to save");
    	}
    	if (user.getName() == null) {
    		throw new UserManagementException("No username specified.");
    	}
    	if (user.getCredential() == null) {
    		throw new UserManagementException("No password specified.");
    	}
		return FeatureManager.getUserManagementFeature().update(user);
	}
    
    @ApiOperation(value = "Deletes the specified Photonic 3d user. This user can be a 'Friend' or a local user.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@DELETE
	@Path("users/{userId}")
	@Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@PathParam("userId")String userId) throws UserManagementException {
		FeatureManager.getUserManagementFeature().remove(FeatureManager.getUserManagementFeature().getUser(UUID.fromString(userId)));
		return Response.status(Status.OK).build();
    }
    
    @ApiOperation(value = "Trusts a new remote Photonic 3d user as a new friend. "
    		+ "A 'Friend' in Photonic3d is nothing more than a remote user that has been given rights to perform actions on your printer.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@POST
	@Path("friends/trust")
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
	@Path("friends/requestFriendship/{friendshipFeature}/{userIdToSendRequest}")
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
		return friendshipFeature.sendFriendRequest(currentUser, new PhotonicUser(null, null, UUID.fromString(userId), null, null, true));
    }
    
    @ApiOperation(value = "Gets all of the remote users that have asked us to be friends.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR),
            @ApiResponse(code = 500, message = SwaggerMetadata.UNEXPECTED_ERROR)})
	@GET
	@Path("friends/requests/list")
	@Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Friend> getPotentialFriends() throws UserManagementException {
		List<Friend> allFriends = new ArrayList<Friend>();
		for (FriendshipFeature feature : FeatureManager.getFriendshipFeatures().values()) {
			allFriends.addAll(feature.getFriendRequests());
		}
		
		return allFriends;
    }
    
    @RolesAllowed({PhotonicUser.FULL_RIGHTS, PhotonicUser.USER_ADMIN, PhotonicUser.CHAT})
    @ApiOperation(value = "Creates a transient P2P message that will be gone when Photonic 3d restarts.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR)})
	@GET
	@Path("messages/create/{toUserId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createMessage(
    		String messageString,
    		@PathParam("toUserId")
    		String toUserId,
    		@Context HttpServletRequest request) throws UserManagementException {
		PhotonicUser fromUser = (PhotonicUser)request.getUserPrincipal();
		if (fromUser == null) {
			throw new UserManagementException("You have to be logged in to chat.");
		}
		
		PhotonicUser toUser = FeatureManager.getUserManagementFeature().getUser(UUID.fromString(toUserId));
		if (toUser.isRemote()) {
			throw new UserManagementException("To send this message to a remote user, append 'services/remote/execute/" + toUserId + "/' to the front of your restful request.");
		}
		
		List<Message> messages = new ArrayList<Message>();
		List<Message> oldMessages = transientMessages.putIfAbsent(toUser.getUserId(), messages);
		if (oldMessages != null) {
			messages = oldMessages;
		}
		
		Message message = new Message(fromUser, toUser, messageString);
		messages.add(message);
    }
    
    @RolesAllowed({PhotonicUser.FULL_RIGHTS, PhotonicUser.USER_ADMIN, PhotonicUser.CHAT})
    @ApiOperation(value = "Removes a P2P message from the transient message store, making sure the caller was the original sender.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR)})
	@GET
	@Path("messages/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeMessage(
    		Message messageToRemove,
    		@Context HttpServletRequest request) throws UserManagementException {
    	
		PhotonicUser fromUser = (PhotonicUser)request.getUserPrincipal();
		if (fromUser == null) {
			throw new UserManagementException("You have to be logged in to chat.");
		}
		
		if (!fromUser.equals(messageToRemove.getFromUser())) {
			throw new UserManagementException("You can't remove a message that wasn't from you");
		}
		
		List<Message> messages = transientMessages.get(messageToRemove.getToUser().getUserId());
		if (messages == null) {
			return;
		}
		
		messages.remove(messageToRemove);
    }

    @RolesAllowed({PhotonicUser.FULL_RIGHTS, PhotonicUser.USER_ADMIN, PhotonicUser.CHAT})
    @ApiOperation(value = "Removes a P2P message from the transient message store, making sure the caller was the original sender.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerMetadata.SUCCESS),
            @ApiResponse(code = 400, message = SwaggerMetadata.USER_UNDERSTANDABLE_ERROR)})
	@GET
	@Path("messages/list")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Message> getMessages(@Context HttpServletRequest request) throws UserManagementException {
    	
		PhotonicUser user = (PhotonicUser)request.getUserPrincipal();
		if (user == null) {
			throw new UserManagementException("You have to be logged in to chat.");
		}

		return transientMessages.get(user.getUserId());
    }

}
