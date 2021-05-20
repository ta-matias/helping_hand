/**
 * @author PogChamp Software
 *
 */

package helpinghand.resources;

import java.util.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.*;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;

import com.google.gson.Gson;

import helpinghand.accesscontrol.AccessControlManager;
import helpinghand.accesscontrol.Role;

import helpinghand.util.*;
import helpinghand.util.user.*;

@Path(UserResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class UserResource {
	
	//Constants
	private static final String USER_INFO_FORMAT = "%s_info"; // used in checksum for authentication token
	private static final String USER_PROFILE_FORMAT = "%s_profile"; // used in checksum for authentication token
	private static final String USER_KIND = "User";
	private static final String USER_INFO_KIND = "UserInfo";
	private static final String USER_PROFILE_KIND = "UserProfile";
	
	
	//Paths
	public static final String PATH = "/user";
	private static final String CREATE_PATH = "/";//POST
	private static final String LOGIN_PATH = "/{userId}/login";//POST
	private static final String LOGOUT_PATH = "/{userId}/logout";//DELETE
	private static final String DELETE_PATH ="/{userId}";//DELETE
	private static final String CHANGE_PASSWORD_PATH = "/{userId}/password";//PUT
	private static final String UPDATE_INFO_PATH = "{userId}/info";//PUT
	private static final String GET_INFO_PATH = "/{userId}/info";//GET
	private static final String UPDATE_PROFILE_PATH ="/{userId}/profile";//PUT
	private static final String GET_PROFILE_PATH = "/{userId}/profile";//GET
	
	
	
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private KeyFactory userKeyFactory = datastore.newKeyFactory().setKind(USER_KIND);
	
	// logger object
	private static final Logger log = Logger.getLogger(UserResource.class.getName());
	private final Gson g = new Gson();

	public UserResource() {}
	
	/*
	 * Creates a new user.
	 * @param data - The registration data that contains userId, email, password and the confirmation of the password.
	 * @return 200, if the registration was successful.
	 * 		   400, if one of the registration parameters is invalid.
	 * 		   409, if the user already exists.
	 * 		   500, otherwise.
	 */
	@POST
	@Path(CREATE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createUser(UserRegister data) {
		log.info(String.format("Attempting to create user with ID: {%s}", data.userId));

		if (!data.validate()) {
			String message = String.format("Data invalid to register user with ID: {%s}", data.userId);
			log.warning(message);
			return Response.status(Status.BAD_REQUEST).entity(String.format(message)).build();
		}

		Key userKey = userKeyFactory.newKey(data.userId);
		Entity newUser = Entity.newBuilder(userKey)
				.set("password",StringValue.newBuilder(DigestUtils.sha512Hex(data.password)).setExcludeFromIndexes(true).build())
				.set("email", data.email)
				.set("status", true)//active
				.set("role", Role.USER.name())
				.build();

		Key infoKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", data.userId))
				.setKind(USER_INFO_KIND)
				.newKey(String.format(USER_INFO_FORMAT, data.userId));
		
		Entity userInfo = Entity.newBuilder(infoKey)
				.set("phone", "")
				.set("address1", "")
				.set("address2", "")
				.set("city", "")
				.set("zip", "")
				.build();

		Key profileKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", data.userId))
				.setKind(USER_PROFILE_KIND)
				.newKey(String.format(USER_PROFILE_FORMAT, data.userId));
		
		Entity userProfile = Entity.newBuilder(profileKey)
				.set("public", true)//public profile by default
				.set("name", "")
				.set("bio", "")
				.build();

		Transaction txn = datastore.newTransaction();

		try {

			if (txn.get(userKey) != null) {
				txn.rollback();
				String message = String.format("User with ID: {%s} already exists.", data.userId);
				log.warning(message);
				return Response.status(Status.CONFLICT).entity(message).build(); // User already exists
			}

			txn.add(newUser, userInfo, userProfile);
			txn.commit();
			log.info(String.format("Created user with ID: {%s} and password: {%s}", data.userId,data.password));
			return Response.ok("Registration done.").build();
			
		} catch (DatastoreException e) {
			txn.rollback();
			String message = String.format("DatastoreException on registering user with ID: {%s}\n%s", data.userId,e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Internal server error
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after resgistering user with ID: {%s}",data.userId);
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Transaction was active
			}
		}
	}
	
   /**
	 * A user login is performed.
	 * @param userId - The user who is going to login.
	 * @param data - The login data that contains the clientId and password.
	 * @return 200, if the login was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the login was failed.
	 */
	@POST
	@Path(LOGIN_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response loginUser(@PathParam("userId") String userId, Login data) {
		log.info(String.format("Attempting to login user with ID: [%s]", data.clientId));

		if (!data.validate()) {
			String message = String.format("Data invalid to login user with ID: {%s}", data.clientId);
			log.warning(message);
			return Response.status(Status.BAD_REQUEST).entity(message).build(); // Data invalid
		}

		String token = AccessControlManager.startSessionUser(data.clientId, data.password);
		
		if(token == null) {
			return Response.status(Status.FORBIDDEN).entity("Login Failed").build();
		}
		
		return Response.ok(token).build();
		
	}
	
	/**
	 * A user logout is performed.
	 * @param userId - The user who is going to logout.
	 * @param tokenId - The authentication token from user that was logged in.
	 * @return 200, if the logout was successful.
	 * 		   403, if the logout was failed.
	 */
	@DELETE
	@Path(LOGOUT_PATH)
	public Response logoutUser(@PathParam("userId")String userId, @QueryParam("tokenId") String tokenId) {
		log.info(String.format("Attempting to logout user with ID: [%s]", userId));

		//ends a session, deleting token
		if(!AccessControlManager.endSession(tokenId)) return Response.status(Status.FORBIDDEN).entity("Logout failed").build();
		return Response.ok().build();
	}
	
	/**
	 * Deletes the user given its identification and the authentication token.
	 * @param userId - The user who is going to be deleted.
	 * @param tokenId - The authentication token from user that is going to be deleted.
	 * @return 200, if the deletion was successful.
	 * 		   404, if the user does not exist.
	 * 		   500, otherwise.
	 */
	@DELETE
	@Path(DELETE_PATH)
	public Response deleteUser(@PathParam("userId")String userId,@QueryParam("tokenId")String tokenId) {
		log.info(String.format("Attempting to delete account for user [%s]", userId));
		
		Key userKey = userKeyFactory.newKey(userId);
		Key userInfoKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", userId))
				.setKind(USER_INFO_KIND)
				.newKey(String.format(USER_INFO_FORMAT, userId));
		Key userProfileKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", userId))
				.setKind(USER_PROFILE_KIND)
				.newKey(String.format(USER_PROFILE_FORMAT, userId));
		
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity user = txn.get(userKey);
			
			if(user == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("User does not exist").build();
			}
			//TODO:change to endAllSessions(tokenId) in BETA
			if(!AccessControlManager.endSession(tokenId)) {
				//Logout failed, should never happen but can if there is an error in the datastore 
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Failed to end user session!").build();
			}
			txn.delete(userInfoKey,userProfileKey,userKey);
			log.info("User " + userId + " has successfully been removed!");
			txn.commit();
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
			}	
		}
	}
	
	/**
	 * Changes the password of the user.
	 * @param userId - The user who is going to change the password.
	 * @param tokenId - The authentication token from user that is going to change the password.
	 * @param data - The new password data.
	 * @return 200, if the change of password was successful.
	 * 		   400, if the password data is invalid.
	 * 		   403, if the password is not the same.
	 * 		   404, if the user does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(CHANGE_PASSWORD_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response changeUserPassword(@PathParam("userID")String userId, @QueryParam("tokenId")String tokenId , ChangePass data) {
		log.info(String.format("Attempting to change password of user with ID: {%s}", userId));

		
		if(!data.validate()) {
			return Response.status(Status.BAD_REQUEST).entity("Invalid data!").build();
		}
		Key userKey = userKeyFactory.newKey(userId);
		
		Transaction txn = datastore.newTransaction();
		
		
		try {
			
			Entity user = txn.get(userKey);
			
			if(user == null ) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("User does not exist").build();
			} 
				
				if(!user.getString("password").equals(DigestUtils.sha512Hex(data.oldPassword))) {
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity("Invalid Attributes!").build();
				}
					
				Entity newUser = Entity.newBuilder(user)
						.set("password", DigestUtils.sha512Hex(data.newPassword))
						.build();
				
				txn.update(newUser);
					
				log.info("User " + userId + " has successfully changed passwords!");
				txn.commit();
				return Response.ok().build();	
			
		} 
		catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
			}
		}
	}
	
	/**
	 * Obtains the information of the user.
	 * @param userId - The user who has information.
	 * @param tokenId - The authentication token of this user.
	 * @return 200, if the operation was successful.
	 * 		   404, if the user does not exist.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_INFO_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response getUserInfo(@PathParam("userId")String userId, @QueryParam("tokenId")String tokenId) {
		log.info(String.format("Attempting to get information of user with ID: {%s}", userId));

		Key userKey = userKeyFactory.newKey(userId);
		Key infoKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", userId))
				.setKind(USER_INFO_KIND)
				.newKey(String.format(USER_INFO_FORMAT, userId));
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		try {


			Entity user = txn.get(userKey);

			if (user == null) {
				txn.rollback();
				String message = String.format("User with ID: {%s} does not exist.", userId);
				log.warning(message);
				return Response.status(Status.NOT_FOUND).entity(message).build();
			}
			Entity storedInfo = txn.get(infoKey);

			UserInfo info = new UserInfo(storedInfo.getString("phone"),
					storedInfo.getString("address1"), 
					storedInfo.getString("address2"), 
					storedInfo.getString("city"),
					storedInfo.getString("zip"));

			txn.commit();
			log.info(String.format("Got information of user with ID: {%s}", userId));
			return Response.ok(g.toJson(info)).build();
		} catch (DatastoreException e) {
			
			txn.rollback();
			String message = String.format("DatastoreException on getting information of user with ID: {%s}\n%s",userId, e.toString());
			
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build();// Internal server error
			
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after getting information if user with ID: {%s}",userId);
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Transaction was active
			}
		}
	}
	
	/**
	 * Updates the information of the user.
	 * @param userId - The user who is going to update the information.
	 * @param tokenId - The authentication token of the user who is going to update the information.
	 * @param data - The updated information of the user.
	 * @return 200, if the update was successful.
	 * 		   400, if the new information of the user is invalid.
	 * 		   404, if the user does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_INFO_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateUserInfo(@PathParam("userId")String userId, @QueryParam("tokenId")String tokenID,UserInfo data) {
		log.info(String.format("Attempting to update information of user with ID: {%s}", userId));
		
		if(!data.validate()) {
			String message = String.format("New information for user with ID: {%s} is invalid.", userId);
			log.warning(message);
			return Response.status(Status.BAD_REQUEST).entity(message).build(); //Info Invalid
		}
		
		Key userKey = userKeyFactory.newKey(userId);
		Key infoKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", userId))
				.setKind(USER_INFO_KIND)
				.newKey(String.format(USER_INFO_FORMAT, userId));
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity user = txn.get(userKey);
			if(user == null) {
				txn.rollback();
				String message = String.format("User with ID: {%s} does not exist.", userId);
				log.warning(message);
				return Response.status(Status.NOT_FOUND).entity(message).build();
			}
				
			
			Entity info = txn.get(infoKey);
			
			Entity updatedInfo = Entity.newBuilder(info)
					.set("phone", data.phone)
					.set("address1",data.address1)
					.set("address2", data.address2)
					.set("city",data.city)
					.set("zip", data.zip)
					.build();
			
			txn.update(updatedInfo);
			txn.commit();
			String message = String.format("Updated information of user with ID: {%s}", userId);
			log.info(message);
			return Response.ok(message).build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			String message = String.format("DatastoreException on update information user with ID: %s\n%s",userId, e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build();//Internal server error
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after update information user with ID: {%s}",userId);
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); //Transaction was active
			}
		}
	}
	
	/**
	 * Obtains the profile of the user.
	 * @param userId - The user who has the profile.
	 * @param tokenId - The authentication token from user.
	 * @return 200, if the operation was successful.
	 * 		   403, if the user has a private profile.
	 * 		   404, if the user does not exist.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_PROFILE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response getUserProfile(@PathParam("userId")String userId, @QueryParam("tokenId")String tokenId) {
		log.info(String.format("Attempting to get profile of user with ID: {%s}", userId));

		Key userKey = userKeyFactory.newKey(userId);
		Key profileKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", userId))
				.setKind(USER_PROFILE_KIND)
				.newKey(String.format(USER_PROFILE_FORMAT, userId));
		
		Transaction txn = datastore
				.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		try {

			Entity user = txn.get(userKey);

			if (user == null) {
				txn.rollback();
				String message = String.format("User with ID: %s does not exist.",userId);
				log.warning(message);
				return Response.status(Status.NOT_FOUND).entity(message).build();
			}
			Entity storedProfile = txn.get(profileKey);
			
			//if profile is not public and requester is not the owner of the profile
			//in ALPHA this is not able to be called by any user besides the owner of the profile so there is no problem, but we must discuss this because of the filter
			if(!storedProfile.getBoolean("public") && !AccessControlManager.getOwner(tokenId).equals(userId)) {
				txn.rollback();
				String message = String.format("User with ID: %s has a private profile.",userId);
				log.warning(message);
				return Response.status(Status.FORBIDDEN).entity(message).build();
			}
			
			UserProfile profile = new UserProfile(storedProfile.getBoolean("public"),
					storedProfile.getString("name"),
					storedProfile.getString("bio"));
			

			txn.commit();
			log.info(String.format("Got profile of user with ID: %s", userId));
			return Response.ok(g.toJson(profile)).build();
		} catch (DatastoreException e) {
			txn.rollback();
			String message = String.format("DatastoreException on getting information of user with ID: {%s}\n%s",userId, e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build();// Internal server error
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after getting profile user with ID: {%s}",userId);
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Transaction was active
			}
		}
	}
	
	/**
	 * Updates the profile of the user.
	 * @param userId - The user who is going to update the profile.
	 * @param tokenId - The authentication token of the user.
	 * @param data - The updated profile of the user.
	 * @return 200, if the update was successful.
	 * 		   400, if the updated profile is invalid.
	 * 		   404, if the user does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_PROFILE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateUserProfile(@PathParam("userId")String userId, @QueryParam("tokenId")String tokenId,UserProfile data) {
		log.info(String.format("Attempting to update profile of user with ID: {%s}", userId));
		
		if(!data.validate()) {
			String message = String.format("New profile for user with ID: {%s} is invalid.", userId);
			log.warning(message);
			return Response.status(Status.BAD_REQUEST).entity(message).build(); //Info Invalid
		}
		
		
		Key userKey = userKeyFactory.newKey(userId);
		Key profileKey = datastore.newKeyFactory().addAncestor(PathElement.of("User", userId))
				.setKind(USER_PROFILE_KIND)
				.newKey(String.format(USER_PROFILE_FORMAT, userId));
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity user = txn.get(userKey);
			if(user == null) {
				txn.rollback();
				String message = String.format("User with ID: %s does not exist.",userId);
				log.warning(message);
				return Response.status(Status.NOT_FOUND).entity(message).build();
			}
				
			
			Entity profile = txn.get(profileKey);
			
			Entity updatedProfile = Entity.newBuilder(profile)
					.set("public", data.publicProfile)
					.set("name", data.name)
					.set("bio", data.bio)
					.build();
			
			txn.update(updatedProfile);
			txn.commit();
			String message = String.format("Updated profile of user with ID: {%s}", userId);
			log.info(message);
			return Response.ok(message).build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			String message = String.format("DatastoreException on update information user with ID: {%s}\n%s",userId, e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); //Internal server error
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after update profile user with ID: {%s}",userId);
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); //Transaction was active
			}
		}
	}
}