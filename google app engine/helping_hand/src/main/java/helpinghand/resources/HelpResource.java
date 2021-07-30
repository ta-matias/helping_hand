/**
 * 
 */
package helpinghand.resources;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

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
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;
import com.google.gson.Gson;

import helpinghand.accesscontrol.Role;
import helpinghand.util.help.*;

import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PARAM;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_KIND;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_OWNER_PROPERTY;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ROLE_PROPERTY;
import static helpinghand.util.GeneralUtils.TOKEN_ACCESS_INSUFFICIENT_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.NOTIFICATION_ERROR;
import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.account.AccountUtils.ACCOUNT_KIND;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_CONFLICT_ERROR;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_NOT_FOUND_ERROR;
import static helpinghand.util.account.AccountUtils.FOLLOWER_ID_PROPERTY;
import static helpinghand.util.account.AccountUtils.FOLLOWER_KIND;
import static helpinghand.util.account.AccountUtils.addNotificationToFeed;
import static helpinghand.resources.UserResource.USER_STATS_KIND;
import static helpinghand.resources.UserResource.USER_STATS_REQUESTS_PROMISED_PROPERTY;
import static helpinghand.resources.UserResource.addRatingToStats;
/**
 * @author PogChamp Software
 *
 */
@Path(HelpResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class HelpResource {

	public static final String CURRENT_HELPER_LEFT_NOTIFICATION = "O ajudante escolhido, %s, deixou de oferecer ajuda no seu pedido '%s'";
	private static final String HELP_CANCELED_NOTIFICATION = "O pedido de ajuda '%s' foi cancelado";
	private static final String HELP_CREATED_NOTIFICATION = "Pedido de ajuda '%s' criado por '%s'";
	private static final String CHOSEN_HELPER_NOTIFICATION = "'%s' escolheu-o para o ajudar no pedido '%s', deixar de oferecer ajuda agora irá afetar a sua fiabilidade";
	private static final String UNCHOSEN_HELPER_NOTIFICATION = "'%s' escolheu outra pessoa para o ajudar em '%s', deixar de oferecer ajuda já não afetará a sua fiabilidade";
	private static final String RATING_NOTIFICATION = "'%s' classificou-o com %d pela sua ajuda em '%s'(%d)";
	private static final String RATING_ERROR = "Error rating user (%d)";

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
	
	private static final String GET_HELP_START  = "Attempting to get help(%d) data with token (%d)";
	private static final String GET_HELP_OK = "Successfuly got help [%s](%d) data with token (%d)";
	private static final String GET_HELP_BAD_DATA_ERROR  = "Get help attempt failed due to bad inputs";

	private static final String CANCEL_HELP_START  = "Attempting to cancel help(%d) with token (%d)";
	private static final String CANCEL_HELP_OK = "Successfuly canceled help [%s](%d) with token (%d)";
	private static final String CANCEL_HELP_BAD_DATA_ERROR  = "Cancel help attempt failed due to bad inputs";

	private static final String FINISH_HELP_START  = "Attempting to finish help (%d) with token (%d)";
	private static final String FINISH_HELP_OK = "Successfuly finished help [%s](%d) with token (%d)";
	private static final String FINISH_HELP_BAD_DATA_ERROR  = "Finish help attempt failed due to bad inputs";

	private static final String OFFER_HELP_START  = "Attempting to offer to help in (%d) with token (%d)";
	private static final String OFFER_HELP_OK = "Successfuly offered to help in [%s](%d) with token (%d)";
	private static final String OFFER_HELP_BAD_DATA_ERROR  = "Offer help attempt failed due to bad inputs";
	private static final String SELF_HELP_ERROR = "A user [%s] tried to help itself";

	private static final String LEAVE_HELP_START  = "Attempting to leave help (%d) with token (%d)";
	private static final String LEAVE_HELP_OK = "Successfuly left help for [%s](%d) with token (%d)";
	private static final String LEAVE_HELP_BAD_DATA_ERROR  = "Leave help attempt failed due to bad inputs";

	private static final String CHOOSE_HELPER_START  = "Attempting to choose helper for (%d) with token (%d)";
	private static final String CHOOSE_HELPER_OK = "Successfuly chose helper for [%s](%d) with token (%d)";
	private static final String CHOOSE_HELPER_BAD_DATA_ERROR  = "Choose helper attempt failed due to bad inputs";
	private static final String CHOOSE_HELPER_CONFLICT = "User [%s] is already the current helper of (%d)";
	
	private static final String LIST_HELPERS_START  = "Attempting to get helpers for (%d) with token (%d)";
	private static final String LIST_HELPERS_OK = "Successfuly got helpers for [%s](%d) with token (%d)";
	private static final String LIST_HELPERS_BAD_DATA_ERROR  = "Get helpers attempt failed due to bad inputs";
	
	private static final String HELP_ID_PARAM = "helpId";
	private static final String RATING_PARAM = "rating";

	public static final String PATH = "/help";
	private static final String CREATE_PATH = "";//POST
	private static final String UPDATE_PATH = "/{"+HELP_ID_PARAM+"}";//PUT
	private static final String CANCEL_PATH = "/{"+HELP_ID_PARAM+"}";//DELETE
	private static final String FINISH_PATH = "/{"+HELP_ID_PARAM+"}/finish";//PUT
	private static final String GET_PATH = "/{"+HELP_ID_PARAM+"}/get";//GET
	private static final String LIST_PATH ="";//GET
	private static final String OFFER_HELP_PATH = "/{"+HELP_ID_PARAM+"}/offer";//PUT
	private static final String LEAVE_HELP_PATH = "/{"+HELP_ID_PARAM+"}/leave";//PUT
	private static final String LIST_HELPERS_PATH = "/{"+HELP_ID_PARAM+"}/helper";//GET
	private static final String CHOOSE_HELPER_PATH = "/{"+HELP_ID_PARAM+"}/helper";//PUT

	public static final String HELP_KIND = "Help";
	public static final String HELP_NAME_PROPERTY = "name";
	public static final String HELP_CREATOR_PROPERTY = "creator";
	public static final String HELP_DESCRIPTION_PROPERTY = "description";
	public static final String HELP_TIME_PROPERTY = "time";
	public static final String HELP_LOCATION_LATITUDE_PROPERTY = "location_latitude";
	public static final String HELP_LOCATION_LONGITUDE_PROPERTY = "location_longitude";
	
	private static final String HELPER_ID_PARAM ="helperId";
	public static final String HELPER_KIND ="Helper";
	public static final String HELPER_ID_PROPERTY = "id";
	public static final String HELPER_CURRENT_PROPERTY = "current";

	private static final Logger log = Logger.getLogger(HelpResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind(TOKEN_KIND);
	private static final KeyFactory helpKeyFactory = datastore.newKeyFactory().setKind(HELP_KIND);
	private static final KeyFactory accountKeyFactory = datastore.newKeyFactory().setKind(ACCOUNT_KIND);

	private final Gson g = new Gson();

	public HelpResource() {}

	/**
	 * Lists all of the help requests.
	 * @param token - The token of the account requesting this operation.
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

			List<HelpData> helpList = new LinkedList<>();	

			results.forEachRemaining(help ->helpList.add(new HelpData(help)));

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
	 * Creates a new help request.
	 * @param token - The token of the account requesting this operation.
	 * @param data - The creation data of the new help request.
	 * @return 200, if the help request was successfully created.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not exist.
	 * 		   404, if the creator does not exist.
	 * 		   500, otherwise.
	 */
	@POST
	@Path(CREATE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createHelp(@QueryParam(TOKEN_ID_PARAM) String token, CreateHelp data) {
		if(data.badData() || badString(token)) {
			log.warning(CREATE_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(CREATE_HELP_START, tokenId));
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Timestamp time = Timestamp.parseTimestamp(data.time);

		Key helpKey = datastore.allocateId(helpKeyFactory.newKey());
		
		Transaction txn = datastore.newTransaction();

		try {
			Entity tokenEntity = txn.get(tokenKey); 
			
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			String id = tokenEntity.getString(TOKEN_OWNER_PROPERTY);
			
			Query<Key> accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
			
			QueryResults<Key> keyList = txn.run(accountQuery);
			
			if(!keyList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Key accountKey = keyList.next();
			
			if(keyList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			Entity help = Entity.newBuilder(helpKey)
					.set(HELP_NAME_PROPERTY, data.name)
					.set(HELP_CREATOR_PROPERTY,id)
					.set(HELP_DESCRIPTION_PROPERTY, data.description)
					.set(HELP_TIME_PROPERTY,time)
					.set(HELP_LOCATION_LATITUDE_PROPERTY,data.location[0])
					.set(HELP_LOCATION_LONGITUDE_PROPERTY,data.location[1])
					.build();

			Query<Entity> followerQuery = Query.newEntityQueryBuilder().setKind(FOLLOWER_KIND).setFilter(PropertyFilter.hasAncestor(accountKey)).build();
			
			QueryResults<Entity> followerList = txn.run(followerQuery);
			
			txn.add(help);
			txn.commit();
			
			String message = String.format(HELP_CREATED_NOTIFICATION,data.name,id);
			followerList.forEachRemaining(follower->{
				if(!addNotificationToFeed(follower.getLong(FOLLOWER_ID_PROPERTY),message))
					log.warning(String.format(NOTIFICATION_ERROR,follower.getLong(FOLLOWER_ID_PROPERTY)));
			});
			
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
	 * Updates the help request data.
	 * @param help - The identification of the help request to be updated.
	 * @param token - The token of the account requesting this operation.
	 * @param data - The updated data for the help request.
	 * @return 200, if the help request was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level or the token does not exist.
	 * 		   404, if the help request does not exist.
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
		
		Timestamp time = Timestamp.parseTimestamp(data.time);

		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Key helpKey = helpKeyFactory.newKey(helpId);
		
		Transaction txn = datastore.newTransaction();

		try {
			Entity helpEntity = txn.get(helpKey);
	
			if(helpEntity == null) {
				txn.rollback();
				log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
				return Response.status(Status.NOT_FOUND).build();
			}
	
			Entity tokenEntity = txn.get(tokenKey);
	
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}
	
			if(!helpEntity.getString(HELP_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))) {
				Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;
				if(tokenRole.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,tokenRole.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}
	
			Entity updatedHelp = Entity.newBuilder(helpEntity)
					.set(HELP_NAME_PROPERTY, data.name)
					.set(HELP_DESCRIPTION_PROPERTY, data.description)
					.set(HELP_TIME_PROPERTY,time)
					.set(HELP_LOCATION_LATITUDE_PROPERTY,data.location[0])
					.set(HELP_LOCATION_LONGITUDE_PROPERTY,data.location[1])
					.build();
		
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
	 * Returns the help request data.
	 * @param help - The identification of the help request to obtain its data.
	 * @param token - The token of the account requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   404, if the help does not exist.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_PATH)
	public Response getHelp(@PathParam(HELP_ID_PARAM) String help, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(help) || badString (token)) {
			log.warning(GET_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long helpId = Long.parseLong(help);

		log.fine(String.format(GET_HELP_START,helpId,tokenId));

		Key helpKey = helpKeyFactory.newKey(helpId);

		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			Entity helpEntity = txn.get(helpKey);
			txn.commit();

			if(helpEntity == null) {
				log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
				return Response.status(Status.NOT_FOUND).build();
			}

			HelpData data = new HelpData(helpEntity);

			log.info(String.format(GET_HELP_OK,helpEntity.getString(HELP_NAME_PROPERTY),helpId,tokenId));
			return Response.ok(g.toJson(data)).build();
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
	 * Finishes the help request.
	 * @param help - The identification of the help request to be finished.
	 * @param token - The token of the account requesting this operation.
	 * @return 200, if the help request was successfully finished.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level or the token does not exist.
	 * 		   404, if the help does not exist.
	 * 		   409, if there is no current helper in this help request.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(FINISH_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response endHelp(@PathParam(HELP_ID_PARAM) String help, @QueryParam(TOKEN_ID_PARAM) String token, @QueryParam(RATING_PARAM) String rating) {
		if(badString(rating)) {
			log.warning(FINISH_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		int ratingValue = Integer.parseInt(rating);

		if(badString(help) || badString(token) || ratingValue < 1 || ratingValue > 5) {
			log.warning(FINISH_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long helpId = Long.parseLong(help);

		log.info(String.format(FINISH_HELP_START,helpId, tokenId));

		Key helpKey = helpKeyFactory.newKey(helpId);

		Key tokenKey = tokenKeyFactory.newKey(tokenId);

		Transaction txn = datastore.newTransaction();

		try {
			Entity helpEntity = txn.get(helpKey);

			if(helpEntity == null) {
				txn.rollback();
				log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
				return Response.status(Status.NOT_FOUND).build();
			}

			Entity tokenEntity = txn.get(tokenKey);

			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}

			if(!helpEntity.getString(HELP_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))) {
				Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;
				if(tokenRole.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,tokenRole.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}

			List<Key> toDelete = new LinkedList<>();
			toDelete.add(helpEntity.getKey());

			Query<Entity> helperQuery = Query.newEntityQueryBuilder().setKind(HELPER_KIND).setFilter(PropertyFilter.hasAncestor(helpKey)).build();

			QueryResults<Entity> helperList = txn.run(helperQuery);

			List<Entity> currentHelper = new LinkedList<>();

			helperList.forEachRemaining(helper->{
				toDelete.add(helper.getKey());
				if(helper.getBoolean(HELPER_CURRENT_PROPERTY))
					currentHelper.add(helper);
			});

			if(currentHelper.size() > 1) {
				txn.rollback();
				log.severe(String.format(MULTIPLE_CURRENT_HELPER_ERROR, helpId));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			if(currentHelper.isEmpty()) {
				txn.rollback();
				log.warning(NO_CURRENT_HELPER_ERROR);
				return Response.status(Status.CONFLICT).build();
			}

			Key[] keys =  new Key[toDelete.size()];
			toDelete.toArray(keys);

			txn.delete(keys);
			txn.commit();

			long helperId = currentHelper.get(0).getLong(HELPER_ID_PROPERTY);

			if(!addRatingToStats(helperId,ratingValue)) {
				log.severe(String.format(RATING_ERROR, helperId));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			String message = String.format(RATING_NOTIFICATION,tokenEntity.getString(TOKEN_OWNER_PROPERTY),ratingValue,helpEntity.getString(HELP_NAME_PROPERTY),helpId);

			if(!addNotificationToFeed(helperId,message)) {
				log.severe(String.format(NOTIFICATION_ERROR, helperId));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			log.info(String.format(FINISH_HELP_OK,helpEntity.getString(HELP_NAME_PROPERTY), helpId,tokenId));
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
	 * Cancels the help request.
	 * @param help - The identification of the help request to be canceled.
	 * @param token - The token of the account requesting this operation.
	 * @return 200, if the help request was successfully canceled.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level or the token does not exist.
	 * 		   404, if the help does not exist.
	 * 		   500, otherwise.
	 */
	@DELETE
	@Path(CANCEL_PATH)
	public Response cancel(@PathParam(HELP_ID_PARAM) String help, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(help) || badString(token)) {
			log.warning(CANCEL_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long helpId = Long.parseLong(help);

		log.info(String.format(CANCEL_HELP_START, helpId,tokenId));

		Key helpKey = helpKeyFactory.newKey(helpId);

		Key tokenKey = tokenKeyFactory.newKey(tokenId);

		Transaction txn = datastore.newTransaction();

		try {
			Entity helpEntity = txn.get(helpKey);

			if(helpEntity == null) {
				txn.rollback();
				log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
				return Response.status(Status.NOT_FOUND).build();
			}

			Entity tokenEntity = txn.get(tokenKey);

			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}

			if(!helpEntity.getString(HELP_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))){
				Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;
				if(tokenRole.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,tokenRole.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}

			List<Key> toDelete = new LinkedList<>();
			toDelete.add(helpKey);

			Query<Entity> helperQuery = Query.newEntityQueryBuilder().setKind(HELPER_KIND).setFilter(PropertyFilter.hasAncestor(helpKey)).build();

			QueryResults<Entity> helperList = txn.run(helperQuery);

			List<Long> toNotify = new LinkedList<>();

			AtomicLong currentHelperId = new AtomicLong(-1);

			helperList.forEachRemaining(helper->{
				long datastoreId = helper.getLong(HELPER_ID_PROPERTY);
				toDelete.add(helper.getKey());
				toNotify.add(datastoreId);
				if(helper.getBoolean(HELPER_CURRENT_PROPERTY))
					currentHelperId.set(datastoreId);
			});

			if(currentHelperId.get() != -1) {
				Key helperStatsKey = datastore.newKeyFactory().setKind(USER_STATS_KIND)
						.addAncestor(PathElement.of(ACCOUNT_KIND, currentHelperId.get())).newKey(currentHelperId.get());
				Entity helperStats = txn.get(helperStatsKey);
				Entity updatedHelperStats = Entity.newBuilder(helperStats)
						.set(USER_STATS_REQUESTS_PROMISED_PROPERTY, helperStats.getLong(USER_STATS_REQUESTS_PROMISED_PROPERTY)-1)
						.build();
				txn.update(updatedHelperStats);
			}

			Key[] keys =  new Key[toDelete.size()];
			toDelete.toArray(keys);

			txn.delete(keys);
			txn.commit();

			String message = String.format(HELP_CANCELED_NOTIFICATION,helpEntity.getString(HELP_NAME_PROPERTY));
			toNotify.forEach(id->{
				if(!addNotificationToFeed(id,message))
					log.warning(String.format(NOTIFICATION_ERROR,id));
			});

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
	 * Chooses an user to join the help request.
	 * @param help - The identification of the help request.
	 * @param token - The token of the creator of the help request.
	 * @param helper - The identification of the helper who will be chosen to join the help request.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level or the token does not exist.
	 * 		   404, if the help does not exist or the user is not offering to help or the helper does not exist.
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

		Key helpKey = helpKeyFactory.newKey(helpId);
		Key tokenKey = tokenKeyFactory.newKey(tokenId);

		Query<Key> helperAccountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, helper)).build();

		Query<Entity> currentHelperQuery = Query.newEntityQueryBuilder().setKind(HELPER_KIND)
				.setFilter(CompositeFilter.and(PropertyFilter.hasAncestor(helpKey),PropertyFilter.eq(HELPER_CURRENT_PROPERTY,true))).build();

		Transaction txn = datastore.newTransaction();

		try {
			Entity helpEntity = txn.get(helpKey);

			if(helpEntity == null) {
				txn.rollback();
				log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
				return Response.status(Status.NOT_FOUND).build();
			}

			Entity tokenEntity = txn.get(tokenKey);

			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}

			if(!helpEntity.getString(HELP_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))) {
				Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;
				if(tokenRole.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,tokenRole.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}

			QueryResults<Key> helperAccountList = txn.run(helperAccountQuery);

			if(!helperAccountList.hasNext()) {
				txn.rollback();
				log.warning(String.format(ACCOUNT_NOT_FOUND_ERROR, helper));
				return Response.status(Status.NOT_FOUND).build();
			}

			Key helperAccountKey = helperAccountList.next();

			if(helperAccountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR, helper));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			Query<Entity> helperQuery = Query.newEntityQueryBuilder().setKind(HELPER_KIND).setFilter(CompositeFilter.and(
					PropertyFilter.eq(HELPER_ID_PROPERTY, helperAccountKey.getId()),PropertyFilter.hasAncestor(helpKey))).build();

			QueryResults<Entity> helperList = txn.run(helperQuery);

			if(!helperList.hasNext()) {
				txn.rollback();
				log.warning(String.format(HELPER_NOT_FOUND_ERROR, helper,helpId));
				return Response.status(Status.NOT_FOUND).build();
			}

			Entity helperEntity = helperList.next();

			if(helperList.hasNext()) {
				txn.rollback();
				log.severe(MULTIPLE_HELPER_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			if(helperEntity.getBoolean(HELPER_CURRENT_PROPERTY)) {
				txn.rollback();
				log.warning(String.format(CHOOSE_HELPER_CONFLICT,helper,helpId));
				return Response.status(Status.CONFLICT).build();
			}

			Entity updatedHelper = Entity.newBuilder(helperEntity)
					.set(HELPER_CURRENT_PROPERTY, true)
					.build();

			Key helperStatsKey = datastore.newKeyFactory().setKind(USER_STATS_KIND)
					.addAncestor(PathElement.of(ACCOUNT_KIND, helperAccountKey.getId())).newKey(helperAccountKey.getId());

			Entity helperStats = txn.get(helperStatsKey);

			//increase amount of promised requests for helper that becomes current helper
			Entity updatedHelperStats = Entity.newBuilder(helperStats)
					.set(USER_STATS_REQUESTS_PROMISED_PROPERTY, helperStats.getLong(USER_STATS_REQUESTS_PROMISED_PROPERTY)+1)
					.build();

			Entity[] toUpdate = new Entity[] {updatedHelper,updatedHelperStats};

			QueryResults<Entity> currentHelperList = txn.run(currentHelperQuery);

			Entity currentHelper = null;

			if(currentHelperList.hasNext()) {
				//if there was already a current helper, make him not the current  helper and reduce the amount of promised requests
				currentHelper = currentHelperList.next();

				Key currentHelperStatsKey = datastore.newKeyFactory().setKind(USER_STATS_KIND)
						.addAncestor(PathElement.of(ACCOUNT_KIND, currentHelper.getLong(HELPER_ID_PROPERTY))).newKey(currentHelper.getLong(HELPER_ID_PROPERTY));
				
				Entity currentHelperStats = txn.get(currentHelperStatsKey);

				Entity updatedCurrentHelperStats = Entity.newBuilder(currentHelperStats)
						.set(USER_STATS_REQUESTS_PROMISED_PROPERTY, helperStats.getLong(USER_STATS_REQUESTS_PROMISED_PROPERTY)-1)
						.build();

				Entity updatedCurrentHelper = Entity.newBuilder(currentHelper)
						.set(HELPER_CURRENT_PROPERTY, false)
						.build();

				toUpdate = new Entity[] {updatedHelper,updatedHelperStats,updatedCurrentHelper,updatedCurrentHelperStats};
			}

			if(currentHelperList.hasNext()) {
				txn.rollback();
				log.severe(String.format(MULTIPLE_CURRENT_HELPER_ERROR,helpId));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			txn.update(toUpdate);
			txn.commit();

			String message = String.format(CHOSEN_HELPER_NOTIFICATION,helpEntity.getString(HELP_CREATOR_PROPERTY),helpEntity.getString(HELP_NAME_PROPERTY));

			if(!addNotificationToFeed(helperEntity.getLong(HELPER_ID_PROPERTY),message))
				log.warning(String.format(NOTIFICATION_ERROR, helperEntity.getLong(HELPER_ID_PROPERTY)));

			if(currentHelper != null) {
				String message2 = String.format(UNCHOSEN_HELPER_NOTIFICATION,helpEntity.getString(HELP_CREATOR_PROPERTY),helpEntity.getString(HELP_NAME_PROPERTY));
				if(!addNotificationToFeed(currentHelper.getLong(HELPER_ID_PROPERTY),message2))
					log.warning(String.format(NOTIFICATION_ERROR, currentHelper.getLong(HELPER_ID_PROPERTY)));
			}

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
	 * Obtains the list of helpers of the help request.
	 * @param help - The identification of the help request to obtain its helpers.
	 * @param token - The token of the account requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level or the token does not exist.
	 * 		   404, if the help does not exist.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(LIST_HELPERS_PATH)
	public Response getHelpers(@PathParam(HELP_ID_PARAM)String help,@QueryParam(TOKEN_ID_PARAM)String token) {
		if(badString(help) || badString(token)) {
			log.warning(LIST_HELPERS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long helpId = Long.parseLong(help);

		log.info(String.format(LIST_HELPERS_START, helpId,tokenId));

		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		Key helpKey = helpKeyFactory.newKey(helpId);

		Query<Entity> helperQuery = Query.newEntityQueryBuilder().setKind(HELPER_KIND)
				.setFilter(PropertyFilter.hasAncestor(helpKey)).build();

		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			Entity helpEntity = txn.get(helpKey);

			if(helpEntity == null) {
				txn.rollback();
				log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
				return Response.status(Status.NOT_FOUND).build();
			}

			Entity tokenEntity = txn.get(tokenKey);

			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}

			if(!helpEntity.getString(HELP_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))) {
				Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;
				if(tokenRole.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,tokenRole.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}
			
			QueryResults<Entity> helperList = txn.run(helperQuery);
			List<Key> helperKeys = new LinkedList<>();

			helperList.forEachRemaining(helperKey -> {
				long datastoreId = helperKey.getLong(HELPER_ID_PROPERTY);
				helperKeys.add(accountKeyFactory.newKey(datastoreId));
				helperKeys.add(datastore.newKeyFactory().setKind(USER_STATS_KIND).addAncestor(PathElement.of(ACCOUNT_KIND,datastoreId)).newKey(datastoreId));
			});
			
			Key[] keyArray = new Key[helperKeys.size()];
			helperKeys.toArray(keyArray);

			Iterator<Entity> helperStats = txn.get(keyArray);
			txn.commit();

			List<HelperStats> statsList = new LinkedList<>();	
			
			while(helperStats.hasNext()) {
				Entity account = helperStats.next();
				Entity stats = helperStats.next();
				statsList.add(new HelperStats(account,stats));
			}
			log.info(String.format(LIST_HELPERS_OK,helpEntity.getString(HELP_NAME_PROPERTY),helpId,tokenId));
			return Response.ok(g.toJson(statsList)).build();
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
	 * The user offers to join the help request.
	 * @param help - The identification of the help request.
	 * @param token - The token of the user who wants to join to the help request.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not exist or the user tried to help itself.
	 * 		   404, if the help request does not exist or the user does not exist.
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

		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		Key helpKey = helpKeyFactory.newKey(helpId);
		Key helperKey = datastore.allocateId(datastore.newKeyFactory().setKind(HELPER_KIND).addAncestor(PathElement.of(HELP_KIND, helpId)).newKey());

		Transaction txn = datastore.newTransaction();

		try {
			Entity helpEntity = txn.get(helpKey);

			if(helpEntity == null) {
				txn.rollback();
				log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
				return Response.status(Status.NOT_FOUND).build();
			}

			
			Entity tokenEntity = txn.get(tokenKey);

			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}

			String user = tokenEntity.getString(TOKEN_OWNER_PROPERTY);
			if(helpEntity.getString(HELP_CREATOR_PROPERTY).equals(user)) {
				txn.rollback();
				log.warning(String.format(SELF_HELP_ERROR, user));
				return Response.status(Status.FORBIDDEN).build();
			}

			Query<Key> accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, user)).build();

			QueryResults<Key> accountList = txn.run(accountQuery);

			if(!accountList.hasNext()) {
				txn.rollback();
				log.warning(String.format(ACCOUNT_NOT_FOUND_ERROR, user));
				return Response.status(Status.NOT_FOUND).build();
			}

			Key accountKey = accountList.next();

			if(accountList.hasNext()) {
				txn.rollback();
				log.warning(String.format(ACCOUNT_ID_CONFLICT_ERROR, user));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			Query<Key> helperQuery = Query.newKeyQueryBuilder().setKind(HELPER_KIND)
					.setFilter(CompositeFilter.and(PropertyFilter.hasAncestor(helpKey),PropertyFilter.eq(HELPER_ID_PROPERTY, accountKey.getId()))).build();

			QueryResults<Key> helperList = txn.run(helperQuery);

			if(helperList.hasNext()) {
				txn.rollback();
				log.warning(String.format(HELPER_CONFLICT_ERROR, user,helpId));
				return Response.status(Status.CONFLICT).build();
			}

			Entity helper = Entity.newBuilder(helperKey)
					.set(HELPER_ID_PROPERTY,accountKey.getId())
					.set(HELPER_CURRENT_PROPERTY, false)
					.build();

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
	 * The user leaves the help request.
	 * @param help - The identification of the help request.
	 * @param token - The token of the user who wants to leave the help request.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not exist.
	 * 		   404, if the help request does not exist or the user is not offering to help
	 * 		   or the user does not exist.
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

		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		Key helpKey = helpKeyFactory.newKey(helpId);

		Transaction txn = datastore.newTransaction();

		try {
			Entity helpEntity = txn.get(helpKey);

			if(helpEntity == null) {
				txn.rollback();
				log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
				return Response.status(Status.NOT_FOUND).build();
			}

			Query<Key> creatorQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, helpEntity.getString(HELP_CREATOR_PROPERTY))).build();

			QueryResults<Key> creatorList = txn.run(creatorQuery);

			if(!creatorList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,helpEntity.getString(HELP_CREATOR_PROPERTY)));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			Key creatorKey = creatorList.next();

			if(creatorList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR,helpEntity.getString(HELP_CREATOR_PROPERTY)));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}


			Entity tokenEntity = txn.get(tokenKey);

			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}

			String user = tokenEntity.getString(TOKEN_OWNER_PROPERTY);

			Query<Key> accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, user)).build();

			QueryResults<Key> accountList = txn.run(accountQuery);

			if(!accountList.hasNext()) {
				txn.rollback();
				log.warning(String.format(ACCOUNT_NOT_FOUND_ERROR, user));
				return Response.status(Status.NOT_FOUND).build();
			}

			Key accountKey = accountList.next();

			if(accountList.hasNext()) {
				txn.rollback();
				log.warning(String.format(ACCOUNT_ID_CONFLICT_ERROR, user));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			Query<Entity> helperQuery = Query.newEntityQueryBuilder().setKind(HELPER_KIND)
					.setFilter(CompositeFilter.and(PropertyFilter.hasAncestor(helpKey),PropertyFilter.eq(HELPER_ID_PROPERTY, accountKey.getId()))).build();

			QueryResults<Entity> helperList = txn.run(helperQuery);

			if(!helperList.hasNext()) {
				txn.rollback();
				log.warning(String.format(HELPER_NOT_FOUND_ERROR, user,helpId));
				return Response.status(Status.NOT_FOUND).build();
			}

			Entity helperEntity = helperList.next();

			if(helperList.hasNext()) {
				txn.rollback();
				log.warning(MULTIPLE_HELPER_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			if(helperEntity.getBoolean(HELPER_CURRENT_PROPERTY)) {
				if(!addNotificationToFeed(creatorKey.getId(),String.format(CURRENT_HELPER_LEFT_NOTIFICATION, user,helpEntity.getString(HELP_NAME_PROPERTY)))) {
					txn.rollback();
					log.severe(String.format(NOTIFICATION_ERROR, creatorKey.getId()));
					return Response.status(Status.INTERNAL_SERVER_ERROR).build();
				}
			}

			txn.delete(helperEntity.getKey());
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

	/**
	 * Cancels the help request.
	 * @param helpId - The identification of the help request to be canceled.
	 * @return true, if the help was successfully canceled.
	 * 		   false, otherwise.
	 */
	public static boolean cancelHelp(long helpId) {
		Key helpKey = helpKeyFactory.newKey(helpId);

		Transaction txn = datastore.newTransaction();

		try {
			Entity helpEntity = txn.get(helpKey);

			if(helpEntity == null) {
				txn.rollback();
				log.warning(String.format(HELP_NOT_FOUND_ERROR, helpId));
				return false;
			}

			List<Key> toDelete = new LinkedList<>();
			toDelete.add(helpKey);

			Query<Entity> helperQuery = Query.newEntityQueryBuilder().setKind(HELPER_KIND).setFilter(PropertyFilter.hasAncestor(helpKey)).build();

			QueryResults<Entity> helperList = txn.run(helperQuery);

			List<Long> toNotify = new LinkedList<>();

			helperList.forEachRemaining(helper->{
				toDelete.add(helper.getKey());
				toNotify.add(helper.getLong(HELPER_ID_PROPERTY));
			});

			Key[] keys =  new Key[toDelete.size()];
			toDelete.toArray(keys);

			txn.delete(keys);
			txn.commit();

			String message = String.format(HELP_CANCELED_NOTIFICATION,helpEntity.getString(HELP_NAME_PROPERTY));
			toNotify.forEach(id->{
				if(!addNotificationToFeed(id,message))
					log.warning(String.format(NOTIFICATION_ERROR,id));
			});

			return true;
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
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
