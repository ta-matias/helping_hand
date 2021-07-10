package helpinghand.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;


import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.ProjectionEntity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;

import helpinghand.accesscontrol.Role;
import helpinghand.util.account.*;
import helpinghand.util.user.*;

import static helpinghand.accesscontrol.AccessControlManager.TOKEN_KIND;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PARAM;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_OWNER_PROPERTY;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ROLE_PROPERTY;
import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.GeneralUtils.TOKEN_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_ACCESS_INSUFFICIENT_ERROR;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
/**
 * @author PogChamp Software
 *
 */
@Path(UserResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class UserResource extends AccountUtils {

	private static final String DATASTORE_EXCEPTION_ERROR = "Error in UserResource: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error in UserResource: Transaction was active";
	private static final String FOLLOW_CONFLICT_ERROR = "[%s] is already registered as following [%s]";
	private static final String UNFOLLOW_NOT_FOUND_ERROR = "[%s] is not registered as following [%s]";
	private static final String REPEATED_FOLLOWER_ERROR = "[%s] is registered as following [%s] multiple times";

	private static final String GET_ALL_START ="Attempting to get all users with token (%d)";
	private static final String GET_ALL_OK ="Successfuly got all users with token (%d)";
	private static final String GET_ALL_BAD_DATA_ERROR = "Get all users attempt failed due to bad input";

	private static final String MULTIPLE_STATS_ERROR = "User [%s] has multiple stats";
	private static final String STATS_NOT_FOUND_ERROR = "User [%s] has no stats";

	private static final String GET_STATS_START ="Attempting to get stats of [%s] with token (%d)";
	private static final String	GET_STATS_OK ="Successfuly got stats of [%s] with token (%d)";
	private static final String GET_STATS_BAD_DATA_ERROR = "Get stats failed due to bad input";

	private static final String ADD_RATING_START ="Attempting to add rating to [%s]";
	private static final String	ADD_RATING_OK ="Successfuly added rating to [%s]";
	private static final String ADD_RATING_BAD_DATA_ERROR = "Add rating failed due to bad input";

	private static final String FOLLOW_START = "Attempting to follow account [%s] with token (%d)";
	private static final String FOLLOW_OK = "Successfuly followed account [%s] with token (%d)[%s]";
	private static final String FOLLOW_BAD_DATA_ERROR = "Follow attempt failed due to bad inputs";

	private static final String UNFOLLOW_START = "Attempting to unfollow account [%s] with token (%d)";
	private static final String UNFOLLOW_OK = "Successfuly unfollowed account [%s] with token (%d)[%s]";
	private static final String UNFOLLOW_BAD_DATA_ERROR = "Unfollow attempt failed due to bad inputs";

	public static final String USER_ID_PARAM = "userId";

	public static final String USER_STATS_KIND = "UserStats";
	public static final String USER_STATS_RATING_PROPERTY = "rating";
	public static final String USER_STATS_REQUESTS_DONE_PROPERTY = "done";
	public static final String USER_STATS_REQUESTS_PROMISED_PROPERTY = "promised";
	public static final double USER_STATS_INITIAL_RATING = 0.0;
	public static final int USER_STATS_INITIAL_REQUESTS = 0;

	//Paths
	public static final String PATH = "/user";
	private static final String GET_USERS_PATH = ""; //GET
	private static final String CREATE_PATH = "";//POST
	private static final String LOGIN_PATH = "/{" + USER_ID_PARAM + "}/login";//POST
	private static final String LOGOUT_PATH = "/{" + USER_ID_PARAM + "}/logout";//DELETE
	private static final String DELETE_PATH ="/{" + USER_ID_PARAM + "}";//DELETE
	private static final String GET_PATH ="/{" + USER_ID_PARAM + "}/account";//GET
	private static final String UPDATE_ID_PATH="/{"+USER_ID_PARAM+"}/id";//PUT
	private static final String UPDATE_PASSWORD_PATH ="/{" + USER_ID_PARAM + "}/password";//PUT
	private static final String UPDATE_EMAIL_PATH = "/{" + USER_ID_PARAM + "}/email";//PUT
	private static final String UPDATE_STATUS_PATH="/{"+USER_ID_PARAM+"}/status";//PUT
	private static final String UPDATE_VISIBILITY_PATH="/{"+USER_ID_PARAM+"}/visibility";//PUT
	private static final String UPDATE_INFO_PATH = "/{" + USER_ID_PARAM + "}/info";//PUT
	private static final String GET_INFO_PATH = "/{" + USER_ID_PARAM + "}/info";//GET
	private static final String GET_EVENTS_PATH = "/{" + USER_ID_PARAM + "}/events";//GET
	private static final String GET_HELP_PATH = "/{" + USER_ID_PARAM + "}/help";//GET
	private static final String UPDATE_PROFILE_PATH ="/{" + USER_ID_PARAM + "}/profile";//PUT
	private static final String GET_PROFILE_PATH = "/{" + USER_ID_PARAM + "}/profile";//GET
	private static final String GET_FEED_PATH = "/{" + USER_ID_PARAM + "}/feed";//GET
	private static final String UPDATE_FEED_PATH = "/{" + USER_ID_PARAM + "}/feed";//PUT
	private static final String GET_STATS_PATH = "/{" + USER_ID_PARAM + "}/stats";//GET
	private static final String FOLLOW_PATH = "/{" + USER_ID_PARAM + "}/follow";//POST
	private static final String UNFOLLOW_PATH = "/{" + USER_ID_PARAM + "}/follow";//DELETE

	
	private static final Logger log = Logger.getLogger(UserResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory tokenKeyFactory =datastore.newKeyFactory().setKind(TOKEN_KIND);

	public UserResource() {super();}

	/**
	 * Obtains a list with the id and current status of all users
	 * @param token - token of the user doing this operation
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_USERS_PATH)
	public Response getAll(@QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(token)) {
			log.info(GET_ALL_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(GET_ALL_START, tokenId));

		Query<Entity> query = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ROLE_PROPERTY, Role.USER.name())).build();

		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			QueryResults<Entity> results= txn.run(query);	
			txn.commit();
			List<String[]> institutions = new LinkedList<>();

			results.forEachRemaining(entity -> {
				institutions.add(new String[] {entity.getString(ACCOUNT_ID_PROPERTY),Boolean.toString(entity.getBoolean(ACCOUNT_STATUS_PROPERTY))});
			});

			log.info(String.format(GET_ALL_OK,tokenId));
			return Response.ok(g.toJson(institutions)).build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}

	}

	/**
	 * Creates a new user.
	 * @param data - The registration data that contains id, email, password and the confirmation of the password.
	 * @return 200, if the registration was successful.
	 * 		   400, if one of the registration parameters is invalid.
	 * 		   409, if the user with the id/email already exists.
	 * 		   500, otherwise.
	 */
	@POST
	@Path(CREATE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createAccount(UserCreationData data) {
		if(data.badData()) {
			log.warning(CREATE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		log.info(String.format(CREATE_START,data.id,Role.USER.name()));

		

		Key accountKey = datastore.allocateId(datastore.newKeyFactory().setKind(ACCOUNT_KIND).newKey());
		Key accountInfoKey = datastore.allocateId(datastore.newKeyFactory().addAncestor(PathElement.of(ACCOUNT_KIND, accountKey.getId())).setKind(ACCOUNT_INFO_KIND).newKey());
		Key accountFeedKey = datastore.allocateId(datastore.newKeyFactory().addAncestor(PathElement.of(ACCOUNT_KIND, accountKey.getId())).setKind(ACCOUNT_FEED_KIND).newKey());
		Key userProfileKey = datastore.allocateId(datastore.newKeyFactory().addAncestor(PathElement.of(ACCOUNT_KIND, accountKey.getId())).setKind(USER_PROFILE_KIND).newKey());
		Key userStatsKey = datastore.allocateId(datastore.newKeyFactory().addAncestor(PathElement.of(ACCOUNT_KIND, accountKey.getId())).setKind(USER_STATS_KIND).newKey());

		Timestamp now = Timestamp.now();

		Entity account = Entity.newBuilder(accountKey)
				.set(ACCOUNT_ID_PROPERTY, data.id)
				.set(ACCOUNT_EMAIL_PROPERTY,data.email)
				.set(ACCOUNT_PASSWORD_PROPERTY, StringValue.newBuilder(DigestUtils.sha512Hex(data.password)).setExcludeFromIndexes(true).build())
				.set(ACCOUNT_ROLE_PROPERTY,Role.USER.name())
				.set(ACCOUNT_CREATION_PROPERTY,now)
				.set(ACCOUNT_STATUS_PROPERTY,ACCOUNT_STATUS_DEFAULT_USER)
				.set(ACCOUNT_VISIBILITY_PROPERTY, ACCOUNT_VISIBLE_DEFAULT)
				.build();

		Entity accountInfo = Entity.newBuilder(accountInfoKey)
				.set(ACCOUNT_INFO_PHONE_PROPERTY,StringValue.newBuilder(DEFAULT_PROPERTY_VALUE_STRING).setExcludeFromIndexes(true).build())
				.set(ACCOUNT_INFO_ADDRESS_1_PROPERTY,StringValue.newBuilder(DEFAULT_PROPERTY_VALUE_STRING).setExcludeFromIndexes(true).build())
				.set(ACCOUNT_INFO_ADDRESS_2_PROPERTY,StringValue.newBuilder(DEFAULT_PROPERTY_VALUE_STRING).setExcludeFromIndexes(true).build())
				.set(ACCOUNT_INFO_ZIPCODE_PROPERTY,DEFAULT_PROPERTY_VALUE_STRING)
				.set(ACCOUNT_INFO_CITY_PROPERTY,DEFAULT_PROPERTY_VALUE_STRING)
				.build();

		Entity userProfile = Entity.newBuilder(userProfileKey)
				.set(PROFILE_NAME_PROPERTY, DEFAULT_PROPERTY_VALUE_STRING)
				.set(PROFILE_BIO_PROPERTY, StringValue.newBuilder(DEFAULT_PROPERTY_VALUE_STRING).setExcludeFromIndexes(true).build())
				.build();

		Entity accountFeed = Entity.newBuilder(accountFeedKey)
				.set(ACCOUNT_FEED_NOTIFICATIONS_PROPERTY, DEFAULT_PROPERTY_VALUE_STRINGLIST)
				.build();

		Entity userStats = Entity.newBuilder(userStatsKey)
				.set(USER_STATS_REQUESTS_PROMISED_PROPERTY,USER_STATS_INITIAL_REQUESTS)
				.set(USER_STATS_REQUESTS_DONE_PROPERTY,USER_STATS_INITIAL_REQUESTS)
				.set(USER_STATS_RATING_PROPERTY,USER_STATS_INITIAL_RATING)
				.build();
		
		Query<Key> idQuery  = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY,data.id)).build();
		Query<Key> emailQuery  = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_EMAIL_PROPERTY,data.email)).build();
		
		Transaction txn = datastore.newTransaction();

		try {
			
			QueryResults<Key> idCheck = txn.run(idQuery);

			if(idCheck.hasNext()) {
				txn.rollback();
				log.warning(String.format(CREATE_ID_CONFLICT_ERROR,data.id));
				return Response.status(Status.CONFLICT).build();
			} 
			
			QueryResults<Key> emailCheck = txn.run(emailQuery);
			
			if(emailCheck.hasNext()) {
				txn.rollback();
				log.warning(String.format(CREATE_EMAIL_CONFLICT_ERROR,data.email));
				return Response.status(Status.CONFLICT).build();
			}
			
			
			txn.add(account,accountInfo,userProfile,accountFeed,userStats);
			txn.commit();
			log.info(String.format(CREATE_OK, data.id, Role.USER.name()));
			return Response.ok().build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}

	}

	/**
	 * Deletes an existing user account
	 * @param id - The identification of the user to be deleted.
	 * @param token - The token of the account performing this operation.
	 * @return 200, if the account was successfully deleted.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@DELETE
	@Path(DELETE_PATH)
	public Response deleteAccount(@PathParam(USER_ID_PARAM) String id, @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.deleteAccount(id,token,Role.USER);
	}
	
	@GET
	@Path(GET_PATH)
	public Response getAccount(@PathParam(USER_ID_PARAM) String id, @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.getAccount(id, token);
	}

	/**
	 * It performs a login on the user account.
	 * @param data - The requested data to perform login.
	 * @return 200, if the login was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the login failed.
	 */
	@POST
	@Path(LOGIN_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response login(Login data) {
		return super.login(data);
	}

	/**
	 * It performs a logout on the user account.
	 * @param token - The token of the user requesting the logout.
	 * @return 200, if the logout was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the logout failed.
	 */
	@DELETE
	@Path(LOGOUT_PATH)
	public Response logout(@QueryParam(TOKEN_ID_PARAM) String token) {
		return super.logout(token);
	}

	/**
	 * Updates the identification of the user.
	 * @param id - The identification of the user to be updated
	 * @param data - The updated id data for the user.
	 * @param token - The token of the user requesting the change of the id.
	 * @return 200, if the identification change was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the password is not the current password for the account or the token cannot execute the operation
	 * 		   with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   409, if there is already an account with this id.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_ID_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateId(@PathParam(USER_ID_PARAM)String id, ChangeId data, @QueryParam(TOKEN_ID_PARAM)String token) {
		return super.updateId(id, data, token);
	}

	/**
	 * Updates the password of the user account.
	 * @param id - The identification of the user.
	 * @param data - The updated password data for the user account.
	 * @param token - The token that is used to perform the operation
	 * @return 200, if the password was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the password is not the current password for the account or the token cannot execute the operation
	 * 		   with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_PASSWORD_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updatePassword(@PathParam(USER_ID_PARAM)String id, ChangePassword data, @QueryParam(TOKEN_ID_PARAM)String token) {
		return super.updatePassword(id, data, token);
	}

	/**
	 * Updates the email of the user account.
	 * @param id - The identification of the user.
	 * @param data - The updated email data for user.
	 * @param token - The token of the account requesting this operation.
	 * @return 200, if the email was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the password is not the current for the account or the token cannot execute the operation
	 * 		   with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   409, if there is already an account with the email.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_EMAIL_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateEmail(@PathParam(USER_ID_PARAM)String id, ChangeEmail data, @QueryParam(TOKEN_ID_PARAM)String token) {
		return super.updateEmail(id,data, token);
	}

	/**
	 * Updates the status of the user account.
	 * @param id - The identification of the user.
	 * @param data - The updated status data for user.
	 * @param token - The token of the user requesting this operation.
	 * @return 200, if the status was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the password is not the current for the account or the token cannot execute the operation
	 * 		   with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_STATUS_PATH)
	public Response updateStatus(@PathParam(USER_ID_PARAM) String id, ChangeStatus data, @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.updateStatus(id, data, token);
	}

	/**
	 * Updates the visibility of the user account.
	 * @param id - The user identification.
	 * @param data - The updated visibility data for user.
	 * @param token - The token requesting this operation.
	 * @return 200, if the visibility was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the password is not the current for the account or the token cannot execute the operation
	 * 		   with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_VISIBILITY_PATH)
	public Response updateVisibility(@PathParam(USER_ID_PARAM) String id, ChangeVisibility data, @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.updateVisibility(id, data, token);
	}

	/**
	 * Obtains the account info of the user.
	 * @param id - The identification of the user.
	 * @param token - The token requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise
	 */
	@GET
	@Path(GET_INFO_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response getAccountInfo(@PathParam(USER_ID_PARAM)String id, @QueryParam(TOKEN_ID_PARAM)String token) {
		return super.getAccountInfo(id, token);
	}

	/**
	 * Updates the user account info.
	 * @param id - The identification of the user.
	 * @param data - The updated account info data for user.
	 * @param token - The token requesting this operation.
	 * @return 200, if the account info was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_INFO_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateAccountInfo(@PathParam(USER_ID_PARAM)String id, AccountInfo data, @QueryParam(TOKEN_ID_PARAM)String token) {
		return super.updateAccountInfo(id,data, token);
	}

	/**
	 * Obtains the list of the events created by the user.
	 * @param id - The identification of the user.
	 * @param token - The token of the user.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 */
	@GET
	@Path(GET_EVENTS_PATH)
	public Response getAccountEvents(@PathParam(USER_ID_PARAM)String id,@QueryParam(TOKEN_ID_PARAM)String token) {
		return super.getAccountEvents(id, token);
	}

	/**
	 * Obtains the list of help requests created by the user.
	 * @param id - The identification of the user.
	 * @param token - The token of the user.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the user account does not exist or the token does not exist.
	 */
	@GET
	@Path(GET_HELP_PATH)
	public Response getAccountHelpRequests(@PathParam(USER_ID_PARAM)String id,@QueryParam(TOKEN_ID_PARAM)String token) {
		return super.getAccountHelpRequests(id, token);
	}

	/**
	 * Obtains the profile of the user.
	 * @param id - The user identification to obtain the profile.
	 * @param token - The token of the user requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the user does not exist or the token does not exist
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_PROFILE_PATH)
	public Response getProfile(@PathParam(USER_ID_PARAM)String id, @QueryParam(TOKEN_ID_PARAM)String token) {
		if(badString(id) || badString(token)) {
			log.warning(GET_PROFILE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(GET_PROFILE_START,id,tokenId));

		Query<ProjectionEntity> accountQuery = Query.newProjectionEntityQueryBuilder().setProjection(ACCOUNT_ID_PROPERTY,ACCOUNT_VISIBILITY_PROPERTY)
				.setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		try {
		
			QueryResults<ProjectionEntity> accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			ProjectionEntity account = accountList.next();
			
			if(accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			Entity tokenEntity = txn.get(tokenKey);
	
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
				return Response.status(Status.NOT_FOUND).build();
			}
	
			if(!account.getBoolean(ACCOUNT_VISIBILITY_PROPERTY) &&!tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
				Role role  = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;//minimum access level required do execute this operation
				if(role.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}
			
			Query<Entity> profileQuery = Query.newEntityQueryBuilder().setKind(USER_PROFILE_KIND).setFilter(PropertyFilter.hasAncestor(account.getKey())).build();
			
			QueryResults<Entity> profileList = txn.run(profileQuery);
			txn.commit();
			
			if(!profileList.hasNext()) {
				log.severe(String.format(PROFILE_NOT_FOUND_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			Entity userProfile = profileList.next();
			
			if(profileList.hasNext()) {
				log.severe(String.format(MULTIPLE_PROFILE_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
	
	
	
			UserProfile profile = new UserProfile(
					userProfile.getString(PROFILE_NAME_PROPERTY),
					userProfile.getString(PROFILE_BIO_PROPERTY)
					);
			
			
			log.info(String.format(GET_PROFILE_OK,id,tokenId));
			return Response.ok(g.toJson(profile)).build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}

	/**
	 * Updates the profile of the user.
	 * @param id - The user identification who is going to update the profile.
	 * @param token - The authentication token of the user.
	 * @param data - The updated profile of the user.
	 * @return 200, if the update was successful.
	 * 		   400, if the updated profile is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_PROFILE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateProfile(@PathParam(USER_ID_PARAM)String id, @QueryParam(TOKEN_ID_PARAM)String token,UserProfile data) {
		if(data.badData() || badString(token) || badString(id)) {
			log.warning(UPDATE_PROFILE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(UPDATE_PROFILE_START,tokenId));

		Query<Key> accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction();
		try {
		
			QueryResults<Key> accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			Key accountKey = accountList.next();
			
			if(accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			Entity tokenEntity = txn.get(tokenKey);
	
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
				return Response.status(Status.NOT_FOUND).build();
			}
	
			if(!tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
				Role role  = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;//minimum access level required do execute this operation
				if(role.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}

			Query<Entity> profileQuery = Query.newEntityQueryBuilder().setKind(USER_PROFILE_KIND).setFilter(PropertyFilter.hasAncestor(accountKey)).build();
			
			QueryResults<Entity> profileList = txn.run(profileQuery);
	
			if(!profileList.hasNext()) {
				txn.rollback();
				log.severe(String.format(PROFILE_NOT_FOUND_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			Entity userProfile = profileList.next();
			
			if(profileList.hasNext()) {
				txn.rollback();
				log.severe(String.format(MULTIPLE_PROFILE_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
	
			Entity updatedUserProfile = Entity.newBuilder(userProfile)
					.set(PROFILE_NAME_PROPERTY, data.name)
					.set(PROFILE_BIO_PROPERTY,data.bio)
					.build();

		
			txn.update(updatedUserProfile);
			txn.commit();
			log.info(String.format(UPDATE_PROFILE_OK,id,tokenId));
			return Response.ok().build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}

	}

	/**
	 * Obtains the feed of the user account.
	 * @param id - The identification of the user.
	 * @param token - The token of the user requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not belong to the user account.
	 * 		   404, if the user account does not exist or the token does not exist. 
	 */
	@GET
	@Path(GET_FEED_PATH)
	public Response getFeed(@PathParam(USER_ID_PARAM) String id, @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.getFeed(id, token);
	}

	/**
	 * Updates the feed of the user account.
	 * @param token - The token of the user requesting this operation.
	 * @param id - The identification of the user account.
	 * @param data - The updated feed data.
	 * @return 200, if the feed was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not belong to the user account.
	 * 		   404, if the user account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_FEED_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateFeed(@PathParam(USER_ID_PARAM) String id,@QueryParam(TOKEN_ID_PARAM) String token, AccountFeed feed) {
		return super.updateFeed(id, token, feed);
	}

	/**
	 * Obtains the stats of the user account.
	 * @param id - The user identification that owns the stats.
	 * @param token - The token of the user requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   404, if the user account does not exist.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_STATS_PATH)
	public Response getStats(@PathParam(USER_ID_PARAM) String id, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(token) || badString(id)) {
			log.info(GET_STATS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(GET_STATS_START, id, tokenId));

		Query<Key> accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		try {
		
			QueryResults<Key> accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			Key accountKey = accountList.next();
			
			if(accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}


			Query<Entity> statsQuery = Query.newEntityQueryBuilder().setKind(USER_STATS_KIND).setFilter(PropertyFilter.hasAncestor(accountKey)).build();
			
			QueryResults<Entity> statsList = txn.run(statsQuery);
			txn.commit();
			
			if(!statsList.hasNext()) {
				txn.rollback();
				log.severe(String.format(PROFILE_NOT_FOUND_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			Entity statsEntity = statsList.next();
			
			if(statsList.hasNext()) {
				txn.rollback();
				log.severe(String.format(MULTIPLE_PROFILE_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
	
			double promised = Double.valueOf(statsEntity.getLong(USER_STATS_REQUESTS_PROMISED_PROPERTY));
			double done = Double.valueOf(statsEntity.getLong(USER_STATS_REQUESTS_DONE_PROPERTY));
			double reliability;
	
			if(promised == 0.0) {
				reliability = 0;
			}else {
				reliability = done/promised;
			}
			
			UserStats stats = new UserStats(statsEntity.getDouble(USER_STATS_RATING_PROPERTY),reliability);
	
			log.info(String.format(GET_STATS_OK,id,tokenId));
			return Response.ok(g.toJson(stats)).build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}

	/**
	 * The user follows another user.
	 * @param id - The user identification to be followed.
	 * @param token - The token of the user requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   404, if the user to be followed does not exist or the token does not exist.
	 * 		   409, if the user is already following another user.
	 * 		   500, otherwise
	 */
	@POST
	@Path(FOLLOW_PATH)
	public Response follow(@PathParam(USER_ID_PARAM) String id, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(id) || badString(token)) {
			log.warning(FOLLOW_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(FOLLOW_START, id,tokenId));

		Query<Key> accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction();
		try {
		
			QueryResults<Key> accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			Key accountKey = accountList.next();
			
			if(accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			Entity tokenEntity = txn.get(tokenKey);
	
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
				return Response.status(Status.NOT_FOUND).build();
			}
			String user = tokenEntity.getString(TOKEN_OWNER_PROPERTY);
			
			
			accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, user)).build();
			
			accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			accountKey = accountList.next();
			
			if(accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			Query<Key> followerQuery = Query.newKeyQueryBuilder().setKind(FOLLOWER_KIND)
					.setFilter(CompositeFilter.and(PropertyFilter.eq(FOLLOWER_ID_PROPERTY, user), PropertyFilter.hasAncestor(accountKey)))
					.build();
			
			QueryResults<Key> followerCheck = txn.run(followerQuery);

			if(followerCheck.hasNext()) {
				txn.rollback();
				log.warning(String.format(FOLLOW_CONFLICT_ERROR, user,id));
				return Response.status(Status.CONFLICT).build();
			}


			
			Key followerKey = datastore.allocateId(datastore.newKeyFactory().addAncestor(PathElement.of(ACCOUNT_KIND, accountKey.getId())).setKind(FOLLOWER_KIND).newKey());
	
			Entity follower = Entity.newBuilder(followerKey)
					.set(FOLLOWER_ID_PROPERTY,user)
					.build();

			txn.add(follower);
			txn.commit();
			log.info(String.format(FOLLOW_OK,id,tokenId,user));
			return Response.ok().build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}		

	}

	/**
	 * The user unfollows another user.
	 * @param id - The user identification to be unfollowed.
	 * @param token - The token of the user requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   404, if the user to be unfollowed does not exist or the user that owns the token does not exist
	 * 		   or the user is not following another user.
	 * 		   500, otherwise.
	 */
	//owner of token stops following account with id
	@DELETE
	@Path(UNFOLLOW_PATH)
	public Response unfollow(@PathParam(USER_ID_PARAM)String id, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(id) || badString(token)) {
			log.warning(UNFOLLOW_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(UNFOLLOW_START, id,tokenId));

		Query<Key> accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction();
		try {
		
			QueryResults<Key> accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			Key accountKey = accountList.next();
			
			if(accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			Entity tokenEntity = txn.get(tokenKey);
	
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
				return Response.status(Status.NOT_FOUND).build();
			}
			String user = tokenEntity.getString(TOKEN_OWNER_PROPERTY);
			
			
			accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, user)).build();
			
			accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			accountKey = accountList.next();
			
			if(accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			Query<Key> followerQuery = Query.newKeyQueryBuilder().setKind(FOLLOWER_KIND)
					.setFilter(CompositeFilter.and(PropertyFilter.eq(FOLLOWER_ID_PROPERTY, user), PropertyFilter.hasAncestor(accountKey)))
					.build();
			
			QueryResults<Key> followerList = txn.run(followerQuery);
			
			if(!followerList.hasNext()) {
				txn.rollback();
				log.warning(String.format(UNFOLLOW_NOT_FOUND_ERROR, user,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Key followerKey = followerList.next();
			
			if(followerList.hasNext()) {
				txn.rollback();
				log.warning(String.format(REPEATED_FOLLOWER_ERROR, user,id));
				return Response.status(Status.CONFLICT).build();
			}

			txn.delete(followerKey);
			txn.commit();
			log.info(String.format(UNFOLLOW_OK,id,tokenId,user));
			return Response.ok().build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}

	}

	/**
	 * Adds rating to stats.
	 * @param user - The user identification to update his stats.
	 * @param finished - The boolean saying if the event/help is finished or not. 
	 * @param rating - The rating for the user.
	 * @return true, if the rating was successfully added to the stats.
	 * 		   false, otherwise.
	 */
	public static boolean addRatingToStats(String user,boolean finished,int rating) {
		if(badString(user)|| rating < 0 || rating > 5) {
			log.warning(ADD_RATING_BAD_DATA_ERROR);
			return false;
		}

		log.info(String.format(ADD_RATING_START,user));
		Query<Key> accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, user)).build();
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		try {
		
			QueryResults<Key> accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,user));
				return false;
			}
			Key accountKey = accountList.next();
			
			if(accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR,user));
				return false;
			}

			Query<Entity> statsQuery = Query.newEntityQueryBuilder().setKind(USER_STATS_KIND).setFilter(PropertyFilter.hasAncestor(accountKey)).build();
			
			QueryResults<Entity> statsList = txn.run(statsQuery);
			
			if(!statsList.hasNext()) {
				txn.rollback();
				log.severe(String.format(STATS_NOT_FOUND_ERROR,user));
				return false;
			}
			Entity stats = statsList.next();
			if(statsList.hasNext()) {
				txn.rollback();
				log.severe(String.format(MULTIPLE_STATS_ERROR,user));
				return false;
			}
	
			double oldRating = stats.getDouble(USER_STATS_RATING_PROPERTY);
			double oldDone = Long.valueOf(stats.getLong(USER_STATS_REQUESTS_DONE_PROPERTY)).doubleValue();
			double oldPromised = Long.valueOf(stats.getLong(USER_STATS_REQUESTS_PROMISED_PROPERTY)).doubleValue();
	
			double newPromised = oldPromised++;
			double newDone = oldDone;
			double newRating = oldRating;
	
			if(finished) {
				newDone++;
				newRating = ((oldRating * oldDone)+rating)/newDone;
			}
	
			Entity updatedStats = Entity.newBuilder(stats)
					.set(USER_STATS_RATING_PROPERTY, newRating)
					.set(USER_STATS_REQUESTS_DONE_PROPERTY,Double.valueOf(newDone).longValue())
					.set(USER_STATS_REQUESTS_PROMISED_PROPERTY,Double.valueOf(newPromised).longValue())
					.build();

			txn.update(updatedStats);
			txn.commit();
			log.info(String.format(ADD_RATING_OK,user));
			return true;
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return false;
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return false;
			}
		}

	}

}
