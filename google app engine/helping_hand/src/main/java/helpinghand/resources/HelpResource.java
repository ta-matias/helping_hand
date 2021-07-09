/**
 * 
 */
package helpinghand.resources;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.Transaction;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;
import com.google.gson.Gson;

import helpinghand.accesscontrol.AccessControlManager;
import helpinghand.accesscontrol.Role;
import helpinghand.util.QueryUtils;
import helpinghand.util.help.*;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PARAM;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_KIND;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_OWNER_PROPERTY;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ROLE_PROPERTY;
import static helpinghand.util.GeneralUtils.TOKEN_ACCESS_INSUFFICIENT_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_OWNER_ERROR;
import static helpinghand.util.GeneralUtils.NOTIFICATION_ERROR;
import static helpinghand.util.GeneralUtils.RATING_ERROR;
import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.account.AccountUtils.ACCOUNT_KIND;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_NOT_FOUND_ERROR;
import static helpinghand.util.account.AccountUtils.addNotificationToFeed;
import static helpinghand.resources.UserResource.FOLLOWER_KIND;
import static helpinghand.resources.UserResource.FOLLOWER_ID_PROPERTY;
import static helpinghand.resources.UserResource.addRatingToStats;
/**
 * @author PogChamp Software
 *
 */
@Path(HelpResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class HelpResource {

	private static final String CURRENT_HELPER_LEFT_NOTIFICATION = "The helper you chose, [%s], has left the help request";
	private static final String HELP_CANCELED_NOTIFICATION = "Help request '%s' has been canceled";
	private static final String HELP_CREATED_NOTIFICATION = "Help request '%s' has been created by '%s'";

	private static final String DATASTORE_EXCEPTION_ERROR = "Error in HelpResource: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error is HelpResource: Transaction was active";
	private static final String HELP_NOT_FOUND_ERROR = "There is no help with id (%d)";
	private static final String MULTIPLE_HELPER_ERROR = "The user is registered as offering help multiple times";
	private static final String HELPER_NOT_FOUND_ERROR = "The user [%s] is not offering to help in (%d)";
	private static final String HELPER_CONFLICT_ERROR = "The user [%s] is already offering to help in (%d)";
	private static final String NO_CURRENT_HELPER_ERROR = "There is no current helper is this help request";
	private static final String MULTIPLE_CURRENT_HELPER_ERROR = "There are multiple current helpers in this help(%d) request";

	private static final String LIST_HELP_START  = "Attempting to get all help requests with token (%d)";
	private static final String LIST_HELP_OK = "Successfuly got all help requests with token (%d)";
	private static final String LIST_HELP_BAD_DATA_ERROR  = "Get all help requests attempt failed due to bad inputs";

	private static final String CREATE_HELP_START  = "Attempting to create help with token (%d)";
	private static final String CREATE_HELP_OK = "Successfuly created help [%s](%d) with token (%d)";
	private static final String CREATE_HELP_BAD_DATA_ERROR  = "Create help attempt failed due to bad inputs";

	private static final String UPDATE_HELP_START  = "Attempting to update help(%d) with token (%d)";
	private static final String UPDATE_HELP_OK = "Successfuly updated help [%s](%d) with token (%d)";
	private static final String UPDATE_HELP_BAD_DATA_ERROR  = "Update help attempt failed due to bad inputs";

	private static final String CANCEL_HELP_START  = "Attempting to cancel help(%d) with token (%d)";
	private static final String CANCEL_HELP_OK = "Successfuly canceled help [%s](%d) with token (%d)";
	private static final String CANCEL_HELP_BAD_DATA_ERROR  = "Cancel help attempt failed due to bad inputs";

	private static final String FINISH_HELP_START  = "Attempting to finish help (%d) with token (%d)";
	private static final String FINISH_HELP_OK = "Successfuly finished help [%s](%d) with token (%d)";
	private static final String FINISH_HELP_BAD_DATA_ERROR  = "Finish help attempt failed due to bad inputs";

	private static final String OFFER_HELP_START  = "Attempting to offer to help in (%d) with token (%d)";
	private static final String OFFER_HELP_OK = "Successfuly offered to help in [%s](%d) with token (%d)";
	private static final String OFFER_HELP_BAD_DATA_ERROR  = "Offer help attempt failed due to bad inputs";

	private static final String LEAVE_HELP_START  = "Attempting to leave help (%d) with token (%d)";
	private static final String LEAVE_HELP_OK = "Successfuly left help for [%s](%d) with token (%d)";
	private static final String LEAVE_HELP_BAD_DATA_ERROR  = "Leave help attempt failed due to bad inputs";

	private static final String CHOOSE_HELPER_START  = "Attempting to choose helper for (%d) with token (%d)";
	private static final String CHOOSE_HELPER_OK = "Successfuly chose helper for [%s](%d) with token (%d)";
	private static final String CHOOSE_HELPER_BAD_DATA_ERROR  = "Choose helper attempt failed due to bad inputs";
	private static final String CHOOSE_HELPER_CONFLICT = "User [%s] is already the current helper of (%d)";

	public static final String PATH = "/help";
	private static final String LIST_PATH ="";//GET
	private static final String CREATE_PATH = "";//POST
	private static final String UPDATE_PATH = "/{helpId}";//PUT
	private static final String CANCEL_PATH = "/{helpId}";//DELETE
	private static final String FINISH_PATH = "/{helpId}/finish";//PUT
	private static final String CHOOSE_HELPER_PATH = "/{helpId}/helper";//PUT
	private static final String OFFER_HELP_PATH = "/{helpId}/offer";//PUT
	private static final String LEAVE_HELP_PATH = "/{helpId}/leave";//PUT

	private static final String HELP_ID_PARAM = "helpId";
	private static final String RATING_PARAM = "rating";

	public static final String HELP_KIND = "Help";
	public static final String HELP_NAME_PROPERTY = "name";
	public static final String HELP_CREATOR_PROPERTY = "creator";
	private static final String HELP_DESCRIPTION_PROPERTY = "description";
	private static final String HELP_TIME_PROPERTY = "time";
	private static final String HELP_PERMANENT_PROPERTY = "permanent";
	private static final String HELP_LOCATION_PROPERTY = "location";
	private static final String HELP_STATUS_PROPERTY = "status";
	private static final String HELP_CONDITIONS_PROPERTY = "conditions";
	private static final boolean HELP_STATUS_INITIAL = true;

	private static final String HELPER_ID_PARAM ="helperId";
	public static final String HELPER_KIND ="Helper";
	public static final String HELPER_ID_PROPERTY = "id";
	public static final String HELPER_CURRENT_PROPERTY = "current";

	private static final Logger log = Logger.getLogger(HelpResource.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();

	public HelpResource() {}

	/**
	 * Lists all of the help requests.
	 * @param token - The token requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(LIST_PATH)
	public Response listHelp(@QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(token)) {
			log.warning(LIST_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(LIST_HELP_START, tokenId));

		Query<Entity> query = Query.newEntityQueryBuilder().setKind(HELP_KIND).build();

		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			QueryResults<Entity> results = txn.run(query);
			txn.commit();

			List<String[]> helpList = new LinkedList<>();	

			results.forEachRemaining(help ->helpList.add(new String[] {Long.toString(help.getKey().getId()),help.getString(HELP_NAME_PROPERTY)
					,Boolean.toString(help.getBoolean(HELP_STATUS_PROPERTY)),Boolean.toString(help.getBoolean(HELP_PERMANENT_PROPERTY))}));

			log.info(String.format(LIST_HELP_OK,tokenId));
			return Response.ok(g.toJson(helpList)).build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
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
	 * Creates a new help.
	 * @param token - The token requesting this operation.
	 * @param data - The data for the help creation.
	 * @return 200, if the help was successfully created.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not belong to the account.
	 * 		   404, if the creator does not exist.
	 * 		   500, otherwise.
	 */
	@POST
	@Path(CREATE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createHelp(@QueryParam(TOKEN_ID_PARAM) String token, CreateHelp data) {
		if(data.badData() || badString(token)) {
			log.warning(CREATE_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).entity("Invalid attributes!").build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(CREATE_HELP_START, tokenId));

		Entity creator = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, AccessControlManager.getOwner(tokenId));

		if(creator == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,data.creator));
			return Response.status(Status.NOT_FOUND).build();
		}

		if(!creator.getString(ACCOUNT_ID_PROPERTY).equals(data.creator)) {
			log.warning(String.format(TOKEN_OWNER_ERROR, tokenId,data.creator));
			return Response.status(Status.FORBIDDEN).build();
		}

		LatLng location = LatLng.of(data.location[0],data.location[1]);
		ListValue.Builder builder = ListValue.newBuilder();

		for(String condition: data.conditions)
			builder.addValue(condition);

		ListValue conditions = builder.build();

		Timestamp time = Timestamp.parseTimestamp(data.time);

		Key helpKey = datastore.allocateId(datastore.newKeyFactory().setKind(HELP_KIND).newKey());

		Entity help = Entity.newBuilder(helpKey)
				.set(HELP_NAME_PROPERTY, data.name)
				.set(HELP_CREATOR_PROPERTY,data.creator)
				.set(HELP_DESCRIPTION_PROPERTY, data.description)
				.set(HELP_TIME_PROPERTY,time)
				.set(HELP_PERMANENT_PROPERTY, data.permanent)
				.set(HELP_LOCATION_PROPERTY,location)
				.set(HELP_CONDITIONS_PROPERTY,conditions)
				.set(HELP_STATUS_PROPERTY, HELP_STATUS_INITIAL)
				.build();

		Transaction txn = datastore.newTransaction();

		try {
			txn.add(help);
			txn.commit();
			List<String> followers = QueryUtils.getEntityChildrenByKind(creator, FOLLOWER_KIND).stream().map(entity->entity.getString(FOLLOWER_ID_PROPERTY)).collect(Collectors.toList());
			String message = String.format(HELP_CREATED_NOTIFICATION,data.name,data.creator);
			followers.forEach(id->addNotificationToFeed(id,message));

			log.info(String.format(CREATE_HELP_OK, data.name, helpKey.getId(), tokenId));
			return Response.ok().build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
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
	 * Updates the help data.
	 * @param help - The identification of the help to be updated.
	 * @param token - The token requesting this operation.
	 * @param data - The updated data for the help.
	 * @return 200, if the help was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the help does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_PATH)
	public Response updateHelp(@PathParam(HELP_ID_PARAM) String help, @QueryParam(TOKEN_ID_PARAM) String token, CreateHelp data) {
		if(badString(help) || badString(token) || data.badData()) {
			log.warning(UPDATE_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long helpId = Long.parseLong(help);

		log.info(String.format(UPDATE_HELP_START,helpId, tokenId));

		Entity helpEntity = QueryUtils.getEntityById(HELP_KIND,helpId);

		if(helpEntity == null) {
			log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
			return Response.status(Status.NOT_FOUND).build();
		}

		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND,tokenId);

		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
			return Response.status(Status.NOT_FOUND).build();
		}

		if(!helpEntity.getString(HELP_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))) {
			Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
			int minAccess = 1;
			if(tokenRole.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,tokenRole.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}

		LatLng location = LatLng.of(data.location[0],data.location[1]);
		ListValue.Builder builder = ListValue.newBuilder();
		
		for(String condition: data.conditions)
			builder.addValue(condition);
		
		ListValue conditions = builder.build();

		Timestamp time = Timestamp.parseTimestamp(data.time);

		Entity updatedHelp = Entity.newBuilder(helpEntity)
				.set(HELP_NAME_PROPERTY, data.name)
				.set(HELP_CREATOR_PROPERTY,data.creator)
				.set(HELP_DESCRIPTION_PROPERTY, data.description)
				.set(HELP_TIME_PROPERTY,time)
				.set(HELP_PERMANENT_PROPERTY, data.permanent)
				.set(HELP_LOCATION_PROPERTY,location)
				.set(HELP_CONDITIONS_PROPERTY,conditions)
				.set(HELP_STATUS_PROPERTY, HELP_STATUS_INITIAL)
				.build();

		Transaction txn = datastore.newTransaction();

		try {
			txn.update(updatedHelp);
			txn.commit();
			log.info(String.format(UPDATE_HELP_OK, data.name,helpId,tokenId));
			return Response.ok().build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
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
	 * Finishes the help.
	 * @param help - The help identification to be finished.
	 * @param token - The token of the user/institution executing this operation.
	 * @return 200, if the help was successfully finished.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level or the rating error occurred.
	 * 		   404, if the help does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(FINISH_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response finishHelp(@PathParam(HELP_ID_PARAM) String help, @QueryParam(TOKEN_ID_PARAM) String token, @QueryParam(RATING_PARAM) String rating) {
		if(badString(rating)) {
			log.warning(FINISH_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		int ratingValue = Integer.parseInt(rating);

		if(badString(help) || badString(token) || ratingValue < 0 || ratingValue > 5) {
			log.warning(FINISH_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long helpId = Long.parseLong(help);

		log.info(String.format(FINISH_HELP_START,helpId, tokenId));

		Entity helpEntity = QueryUtils.getEntityById(HELP_KIND, helpId);

		if(helpEntity == null) {
			log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
			return Response.status(Status.NOT_FOUND).build();
		}

		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND,tokenId);

		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
			return Response.status(Status.NOT_FOUND).build();
		}

		if(!helpEntity.getString(HELP_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))) {
			Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
			int minAccess = 1;
			if(tokenRole.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,tokenRole.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}

		List<Entity> helperList = QueryUtils.getEntityChildrenByKindAndProperty(helpEntity, HELPER_KIND, HELPER_CURRENT_PROPERTY, true);

		if(helperList.isEmpty()) {
			log.severe(NO_CURRENT_HELPER_ERROR);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		if(helperList.size() > 1) {
			log.severe(String.format(MULTIPLE_CURRENT_HELPER_ERROR, helpId));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		Entity currentHelper = helperList.get(0);

		if(!addRatingToStats(currentHelper.getString(HELPER_ID_PROPERTY),true,ratingValue)) {
			log.severe(String.format(RATING_ERROR, currentHelper.getString(HELPER_ID_PROPERTY)));
			return Response.status(Status.FORBIDDEN).build();
		}

		if(helpEntity.getBoolean(HELP_PERMANENT_PROPERTY)) {
			log.info(String.format(FINISH_HELP_OK, helpEntity.getString(HELP_NAME_PROPERTY),helpId,tokenId));
			return Response.ok().build();
		}

		List<Key> toDelete = QueryUtils.getEntityChildrenByKind(helpEntity, HELPER_KIND).stream().map(helper->helper.getKey()).collect(Collectors.toList());
		toDelete.add(helpEntity.getKey());

		Key[] keys =  new Key[toDelete.size()];
		toDelete.toArray(keys);

		Transaction txn = datastore.newTransaction();

		try {
			txn.delete(keys);
			txn.commit();
			log.info(String.format(CANCEL_HELP_OK,helpEntity.getString(HELP_NAME_PROPERTY), helpId,tokenId));
			return Response.ok().build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
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
	 * Cancels the help.
	 * @param help - The identification of the help to be canceled.
	 * @param token - The token requesting this operation.
	 * @return 200, if the help was successfully canceled.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the help does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@DELETE
	@Path(CANCEL_PATH)
	public Response cancelHelp(@PathParam(HELP_ID_PARAM) String help, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(help) || badString(token)) {
			log.warning(CANCEL_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long helpId = Long.parseLong(help);

		log.info(String.format(CANCEL_HELP_START, helpId,tokenId));

		Entity helpEntity = QueryUtils.getEntityById(HELP_KIND, helpId);

		if(helpEntity == null) {
			log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
			return Response.status(Status.NOT_FOUND).build();
		}

		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND,tokenId);

		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
			return Response.status(Status.NOT_FOUND).build();
		}

		if(!helpEntity.getString(HELP_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))){
			Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
			int minAccess = 1;
			if(tokenRole.getAccess()< minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,tokenRole.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}

		List<Entity> helpers = QueryUtils.getEntityChildrenByKind(helpEntity, HELPER_KIND);

		for(Entity helper: helpers) {
			if(!addNotificationToFeed(helper.getString(HELPER_ID_PROPERTY),String.format(HELP_CANCELED_NOTIFICATION, helpEntity.getString(HELP_NAME_PROPERTY)))) {
				log.severe(String.format(NOTIFICATION_ERROR, helper.getString(HELPER_ID_PROPERTY)));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}

		List<Key> toDelete = helpers.stream().map(entity->entity.getKey()).collect(Collectors.toList());
		toDelete.add(helpEntity.getKey());

		Key[] keys =  new Key[toDelete.size()];
		toDelete.toArray(keys);

		Transaction txn = datastore.newTransaction();

		try {
			txn.delete(keys);
			txn.commit();
			log.info(String.format(CANCEL_HELP_OK,helpEntity.getString(HELP_NAME_PROPERTY), helpId,tokenId));
			return Response.ok().build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
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
	 * Chooses an user to join the help.
	 * @param help - The identification of the help.
	 * @param token - The token requesting this operation.
	 * @param helper - The helper  that will be chosen to join the help.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not exist or the token cannot execute the operation with the current access level.
	 * 		   404, if the help does not exist or the user is not offering to help or the token does not exist.
	 * 		   409, if the user is already the current helper.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(CHOOSE_HELPER_PATH)
	public Response chooseHelper(@PathParam(HELP_ID_PARAM) String help, @QueryParam(TOKEN_ID_PARAM) String token,@QueryParam(HELPER_ID_PARAM) String helper) {
		if(badString(help) || badString(token) || badString(helper)) {
			log.warning(CHOOSE_HELPER_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long helpId = Long.parseLong(help);

		log.info(String.format(CHOOSE_HELPER_START,helpId, tokenId));

		Entity helpEntity = QueryUtils.getEntityById(HELP_KIND, helpId);

		if(helpEntity == null) {
			log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
			return Response.status(Status.NOT_FOUND).build();
		}

		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND,tokenId);

		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
			return Response.status(Status.NOT_FOUND).build();
		}

		if(!helpEntity.getString(HELP_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))) {
			Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
			int minAccess = 1;
			if(tokenRole.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,tokenRole.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}

		List<Entity> checkList = QueryUtils.getEntityChildrenByKindAndProperty(helpEntity, HELPER_KIND, HELPER_ID_PROPERTY, helper);

		if(checkList.size() > 1) {
			log.severe(MULTIPLE_HELPER_ERROR);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		if(checkList.isEmpty()) {
			log.warning(String.format(HELPER_NOT_FOUND_ERROR, helper,helpId));
			return Response.status(Status.NOT_FOUND).build();
		}

		Entity helperEntity = checkList.get(0);

		if(helperEntity.getBoolean(HELPER_CURRENT_PROPERTY)) {
			log.warning(String.format(CHOOSE_HELPER_CONFLICT,helper,helpId));
			return Response.status(Status.CONFLICT).build();
		}

		Entity updatedHelper = Entity.newBuilder(helperEntity)
				.set(HELPER_CURRENT_PROPERTY, true)
				.build();

		List<Entity> currentHelperList = QueryUtils.getEntityChildrenByKindAndProperty(helpEntity, HELPER_KIND,HELPER_CURRENT_PROPERTY ,true);

		if(currentHelperList.size() > 1) {
			log.severe(String.format(MULTIPLE_CURRENT_HELPER_ERROR,helpId));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		Entity[] toUpdate = new Entity[] {updatedHelper};

		if(!currentHelperList.isEmpty()) {
			Entity currentHelper = currentHelperList.get(0);

			Entity updatedCurrentHelper = Entity.newBuilder(currentHelper)
					.set(HELPER_CURRENT_PROPERTY, false)
					.build();

			toUpdate = new Entity[] {updatedHelper,updatedCurrentHelper};
		}

		Transaction txn = datastore.newTransaction();

		try {
			txn.update(toUpdate);
			txn.commit();
			log.info(String.format(CHOOSE_HELPER_OK, helpEntity.getString(HELP_NAME_PROPERTY),helpId,tokenId));
			return Response.ok().build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
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
	 * The user offers to help.
	 * @param help - The identification of the help.
	 * @param token - The token requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not exist.
	 * 		   404, if the help does not exist or the token does not exist.
	 * 		   409, if the user has already offered to help.
	 * 		   500, otherwise.
	 */
	@POST
	@Path(OFFER_HELP_PATH)
	public Response offerHelp(@PathParam(HELP_ID_PARAM) String help, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(help) || badString(token)) {
			log.warning(OFFER_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long helpId = Long.parseLong(help);

		log.info(String.format(OFFER_HELP_START, helpId,tokenId));

		Entity helpEntity = QueryUtils.getEntityById(HELP_KIND, helpId);

		if(helpEntity == null) {
			log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
			return Response.status(Status.NOT_FOUND).build();
		}

		String user = AccessControlManager.getOwner(tokenId);

		if(user == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
			return Response.status(Status.NOT_FOUND).build();
		}

		List<Entity> checkList = QueryUtils.getEntityChildrenByKindAndProperty(helpEntity, HELPER_KIND,HELPER_ID_PROPERTY, user);

		if(checkList.size() > 1) {
			log.severe(MULTIPLE_HELPER_ERROR);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		if(!checkList.isEmpty()) {
			log.warning(String.format(HELPER_CONFLICT_ERROR, user,helpId));
			return Response.status(Status.CONFLICT).build();
		}

		Key helperKey = datastore.allocateId(datastore.newKeyFactory().addAncestor(PathElement.of(HELP_KIND, helpEntity.getKey().getId())).setKind(HELPER_KIND).newKey());

		Entity helper = Entity.newBuilder(helperKey)
				.set(HELPER_ID_PROPERTY,user)
				.set(HELPER_CURRENT_PROPERTY, false)
				.build();

		Transaction txn = datastore.newTransaction();

		try {
			txn.add(helper);
			txn.commit();
			log.info(String.format(OFFER_HELP_OK, helpEntity.getString(HELP_NAME_PROPERTY),helpId,tokenId));
			return Response.ok().build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
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
	 * The user leaves the help.
	 * @param help - The identification of the help.
	 * @param token - The token requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   404, if the help does not exist or the user is not offering to help or the token does not exist.
	 * 		   500, otherwise.
	 */
	@DELETE
	@Path(LEAVE_HELP_PATH)
	public Response leaveHelp(@PathParam(HELP_ID_PARAM) String help, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(help) || badString(token)) {
			log.warning(LEAVE_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long helpId = Long.parseLong(help);

		log.info(String.format(LEAVE_HELP_START,helpId, tokenId));

		Entity helpEntity = QueryUtils.getEntityById(HELP_KIND, helpId);

		if(helpEntity == null) {
			log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
			return Response.status(Status.NOT_FOUND).build();
		}

		String user = AccessControlManager.getOwner(tokenId);

		if(user == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
			return Response.status(Status.NOT_FOUND).build();
		}

		List<Entity> checkList = QueryUtils.getEntityChildrenByKindAndProperty(helpEntity, HELPER_KIND,HELPER_ID_PROPERTY, user);

		if(checkList.size() > 1) {
			log.severe(MULTIPLE_HELPER_ERROR);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		if(checkList.isEmpty()) {
			log.warning(String.format(HELPER_NOT_FOUND_ERROR, user,helpId));
			return Response.status(Status.NOT_FOUND).build();
		}

		Entity helper = checkList.get(0);

		if(helper.getBoolean(HELPER_CURRENT_PROPERTY)) {
			if(!addNotificationToFeed(helpEntity.getString(HELP_CREATOR_PROPERTY),String.format(CURRENT_HELPER_LEFT_NOTIFICATION, user))) {
				log.severe(String.format(NOTIFICATION_ERROR, helpEntity.getString(HELP_CREATOR_PROPERTY)));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			//TODO:This should be optional?

			if(!addRatingToStats(user,false,0)) {
				log.severe(String.format(RATING_ERROR, user));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}

		Transaction txn = datastore.newTransaction();

		try {
			txn.delete(helper.getKey());
			txn.commit();
			log.info(String.format(LEAVE_HELP_OK,helpEntity.getString(HELP_NAME_PROPERTY),helpId,tokenId));
			return Response.ok().build();
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}

	}

}
