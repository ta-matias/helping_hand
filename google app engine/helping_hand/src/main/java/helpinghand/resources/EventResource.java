package helpinghand.resources;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;
import com.google.gson.Gson;

import helpinghand.accesscontrol.Role;
import helpinghand.util.account.Account;
import helpinghand.util.event.*;

import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PARAM;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_KIND;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ROLE_PROPERTY;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_OWNER_PROPERTY;
import static helpinghand.util.GeneralUtils.TOKEN_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_ACCESS_INSUFFICIENT_ERROR;
import static helpinghand.util.GeneralUtils.NOTIFICATION_ERROR;
import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_KIND;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_CONFLICT_ERROR;
import static helpinghand.util.account.AccountUtils.ACCOUNT_NOT_FOUND_ERROR;
import static helpinghand.util.account.AccountUtils.FOLLOWER_ID_PROPERTY;
import static helpinghand.util.account.AccountUtils.FOLLOWER_KIND;
import static helpinghand.util.account.AccountUtils.addNotificationToFeed;
/**
 * @author PogChamp Software
 *
 */
@Path(EventResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class EventResource {

	private static final String EVENT_CREATED_NOTIFICATION = "Event '%s' has been created by '%s'";
	private static final String EVENT_CANCELED_NOTIFICATION = "Event '%s' has been canceled";

	private static final String DATASTORE_EXCEPTION_ERROR = "Error in EventResource: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error is EventResource: Transaction was active";
	private static final String EVENT_NOT_FOUND_ERROR = "There is no event with id (%d)";
	private static final String MULTIPLE_PARTICIPANT_ERROR = "Participant is registed multiple times in the event";
	private static final String EVENT_ENDED_ERROR = "Event [%s](%d) already ended";

	private static final String CREATE_EVENT_START  = "Attempting to create event with token (%d)";
	private static final String CREATE_EVENT_OK = "Successfuly created event [%s](%d) with token (%d)";
	private static final String CREATE_EVENT_BAD_DATA_ERROR  = "Create event attempt failed due to bad inputs";

	private static final String END_EVENT_START  = "Attempting to end event (%d) with token (%d)";
	private static final String END_EVENT_OK = "Successfuly ended event [%s](%d) with token (%d)";
	private static final String END_EVENT_BAD_DATA_ERROR  = "End event attempt failed due to bad inputs";

	private static final String CANCEL_EVENT_START  = "Attempting to cancel event (%d) with token (%d)";
	private static final String CANCEL_EVENT_OK = "Successfuly canceled event [%s](%d) with token (%d)";
	private static final String CANCEL_EVENT_BAD_DATA_ERROR  = "Cancel event attempt failed due to bad inputs";

	private static final String UPDATE_EVENT_START  = "Attempting to update event (%d) with token (%d)";
	private static final String UPDATE_EVENT_OK = "Successfuly updated event [%s](%d) with token (%d)";
	private static final String UPDATE_EVENT_BAD_DATA_ERROR  = "Update event attempt failed due to bad inputs";

	private static final String GET_EVENT_START  = "Attempting to get event (%d) with token (%d)";
	private static final String GET_EVENT_OK = "Successfuly got event [%s](%d) with token (%d)";
	private static final String GET_EVENT_BAD_DATA_ERROR  = "Get event attempt failed due to bad inputs";

	private static final String JOIN_EVENT_START  = "Attempting to join event (%d) event with token (%d)";
	private static final String JOIN_EVENT_OK = "Successfuly joined event [%s](%d) with token (%d)";
	private static final String JOIN_EVENT_BAD_DATA_ERROR  = "Join event attempt failed due to bad inputs";
	private static final String JOIN_EVENT_CONFLICT_ERROR  = "User [%s] already joined event (%d)";

	private static final String LEAVE_EVENT_START  = "Attempting to leave event (%d) with token (%d)";
	private static final String LEAVE_EVENT_OK = "Successfuly left event [%s](%d) with token (%d)";
	private static final String LEAVE_EVENT_BAD_DATA_ERROR  = "Leave event attempt failed due to bad inputs";
	private static final String LEAVE_EVENT_NOT_FOUND_ERROR  = "User [%s] has not joined event (%d)";

	private static final String LIST_EVENTS_START  = "Attempting to get all events with token (%d)";
	private static final String LIST_EVENTS_OK = "Successfuly got all events with token (%d)";
	private static final String LIST_EVENTS_BAD_DATA_ERROR  = "Get all events attempt failed due to bad inputs";

	private static final String LIST_EVENT_PARTICIPANTS_START  = "Attempting to get event (%d) participants with token (%d)";
	private static final String LIST_EVENT_PARTICIPANTS_OK = "Successfuly got event [%s](%d) participants with token (%d)";
	private static final String LIST_EVENT_PARTICIPANTS_BAD_DATA_ERROR  = "Get event participants attempt failed due to bad inputs";

	private static final String EVENT_ID_PARAM = "eventId";

	public static final String PATH = "/event";
	private static final String LIST_PATH = ""; // GET
	private static final String LIST_BY_EVENT_PATH = "/{"+EVENT_ID_PARAM+"}/list"; // GET
	private static final String CREATE_PATH = ""; // POST
	private static final String UPDATE_PATH = "/{"+EVENT_ID_PARAM+"}"; // PUT
	private static final String CANCEL_PATH = "/{"+EVENT_ID_PARAM+"}"; // DELETE
	private static final String END_PATH = "/{"+EVENT_ID_PARAM+"}/end"; // PUT
	private static final String GET_PATH = "/{"+EVENT_ID_PARAM+"}/get"; // GET
	private static final String JOIN_PATH = "/{"+EVENT_ID_PARAM+"}/join"; // POST
	private static final String LEAVE_PATH = "/{"+EVENT_ID_PARAM+"}/leave"; // DELETE

	public static final String EVENT_KIND = "Event";
	public static final String EVENT_NAME_PROPERTY ="name";
	public static final String EVENT_CREATOR_PROPERTY ="creator";
	public static final String EVENT_DESCRIPTION_PROPERTY ="description";
	public static final String EVENT_START_PROPERTY = "start";
	public static final String EVENT_END_PROPERTY = "end";
	public static final String EVENT_LOCATION_PROPERTY = "location";
	public static final String EVENT_STATUS_PROPERTY ="status";
	private static final boolean EVENT_STATUS_DEFAULT = true;
	private static final boolean EVENT_FINISHED_STATUS = false;

	public static final String PARTICIPANT_KIND = "Participant";
	public static final String PARTICIPANT_ID_PROPERTY = "id";

	private static final Logger log = Logger.getLogger(EventResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind(TOKEN_KIND); 
	private static final KeyFactory accountKeyFactory = datastore.newKeyFactory().setKind(ACCOUNT_KIND); 
	private static final KeyFactory eventKeyFactory = datastore.newKeyFactory().setKind(EVENT_KIND); 

	private final Gson g = new Gson();

	public EventResource() {}

	/**
	 * Creates a new event.
	 * @param token - The token id from the user or the institution.
	 * @param data - The creation data of the new event.
	 * @return 200, if the creation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not belong to the creator.
	 * 		   404, if the creator does not exist.
	 * 		   500, otherwise.
	 */
	@POST
	@Path(CREATE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createEvent(@QueryParam(TOKEN_ID_PARAM) String token, CreateEvent data) {
		if(data.badData() || badString(token)) {
			log.warning(CREATE_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		log.info(String.format(CREATE_EVENT_START,tokenId));

		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		LatLng location = LatLng.of(data.location[0],data.location[1]);
		
		Timestamp start = Timestamp.parseTimestamp(data.start);
		Timestamp end = Timestamp.parseTimestamp(data.end);

		Key eventKey = datastore.allocateId(eventKeyFactory.newKey());
	
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
			
			Entity event = Entity.newBuilder(eventKey)
					.set(EVENT_NAME_PROPERTY, data.name)
					.set(EVENT_CREATOR_PROPERTY, id)
					.set(EVENT_DESCRIPTION_PROPERTY, data.description)
					.set(EVENT_START_PROPERTY,start)
					.set(EVENT_END_PROPERTY, end)
					.set(EVENT_LOCATION_PROPERTY, location)
					.set(EVENT_STATUS_PROPERTY, EVENT_STATUS_DEFAULT)
					.build();
			
			Query<Entity> followerQuery = Query.newEntityQueryBuilder().setKind(FOLLOWER_KIND)
					.setFilter(PropertyFilter.hasAncestor(accountKey)).build();
			
			QueryResults<Entity> followerList = txn.run(followerQuery);
			
			txn.add(event);
			txn.commit();
			
			String message = String.format(EVENT_CREATED_NOTIFICATION,data.name,id);
			followerList.forEachRemaining(follower->{
				if(addNotificationToFeed(follower.getLong(FOLLOWER_ID_PROPERTY),message))
					log.warning(String.format(NOTIFICATION_ERROR,follower.getString(FOLLOWER_ID_PROPERTY)));
			});

			log.info(String.format(CREATE_EVENT_OK, data.name,eventKey.getId(),tokenId));
			return Response.ok(eventKey.getId()).build();
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
	 * Ends the event.
	 * @param event - The identification of the event to be finished.
	 * @param token - The token id of the user or the institution who is going to finish the event.
	 * @return 200, if the event was finished with success.
	 * 		   400, if the data is invalid.
	 * 		   403, the token cannot execute the operation with the current access level.
	 * 		   404, if the event does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(END_PATH)
	public Response endEvent(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(event)|| badString(token)) {
			log.warning(END_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long eventId = Long.parseLong(event);

		log.info(String.format(END_EVENT_START, eventId, tokenId));
		
		Key eventKey = eventKeyFactory.newKey(eventId);
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction();

		try {
			Entity eventEntity = txn.get(eventKey);
	
			if(eventEntity == null) {
				txn.rollback();
				log.warning(String.format(EVENT_NOT_FOUND_ERROR, eventId));
				return Response.status(Status.NOT_FOUND).build();
			}
	
			Entity tokenEntity = txn.get(tokenKey);
	
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}
	
			if(!eventEntity.getString(EVENT_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))){
				Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;
				if(tokenRole.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,tokenRole.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}
	
			Entity updatedEvent = Entity.newBuilder(eventEntity)
					.set(EVENT_STATUS_PROPERTY, EVENT_FINISHED_STATUS)
					.build();
	
			//TODO:Do something to participants?

			txn.update(updatedEvent);
			txn.commit();
			log.info(String.format(END_EVENT_OK, updatedEvent.getString(EVENT_NAME_PROPERTY),eventId,tokenId));
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
	 * Cancels the event and sends a notification to users when the event is canceled.
	 * @param event - The identification of the event to be canceled.
	 * @param token - The token id of the user or the institution who is going to cancel the event.
	 * @return 200, if the cancellation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, the token cannot execute the operation with the current access level.
	 * 		   404, if the event does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@DELETE
	@Path(CANCEL_PATH)
	public Response cancel(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(event)||badString(token)) {
			log.warning(CANCEL_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long eventId = Long.parseLong(event);

		log.info(String.format(CANCEL_EVENT_START, eventId, tokenId));
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Key eventKey = eventKeyFactory.newKey(eventId);
		
		Query<Entity> participantQuery = Query.newEntityQueryBuilder().setKind(PARTICIPANT_KIND).setFilter(PropertyFilter.hasAncestor(eventKey)).build();
		
		Transaction txn = datastore.newTransaction();

		try {
			Entity eventEntity = txn.get(eventKey);
	
			if(eventEntity == null) {
				txn.rollback();
				log.warning(String.format(EVENT_NOT_FOUND_ERROR, eventId));
				return Response.status(Status.NOT_FOUND).build();
			}
	
			Entity tokenEntity = txn.get(tokenKey);
	
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}
	
			if(!eventEntity.getString(EVENT_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))){
				Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;
				if(tokenRole.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,tokenRole.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}
			
			List<Key> toDelete = new LinkedList<>();
			List<Long> toNotify = new LinkedList<>();
			toDelete.add(eventKey);
	
			QueryResults<Entity> participantList = txn.run(participantQuery);
			
			participantList.forEachRemaining(participant->{
				toDelete.add(participant.getKey());
				toNotify.add(participant.getLong(PARTICIPANT_ID_PROPERTY));
			});
			
			Key[] keys = new Key[toDelete.size()];
			toDelete.toArray(keys);
			txn.delete(keys);
			txn.commit();
		
			String message = String.format(EVENT_CANCELED_NOTIFICATION, eventEntity.getString(EVENT_NAME_PROPERTY));
			toNotify.forEach(id->{
				if(!addNotificationToFeed(id,message))
					log.warning(String.format(NOTIFICATION_ERROR,id));
			});
			
			log.info(String.format(CANCEL_EVENT_OK,eventEntity.getString(EVENT_NAME_PROPERTY),eventId,tokenId));
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
	 * Updates the event data of the event.
	 * @param event - The event identification that is going to be updated.
	 * @param token - The token id of the user or the institution who is going to update the event.
	 * @param data - The updated data for this event.
	 * @return 200, if the update was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the event does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateEvent(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token, CreateEvent data) {
		if(badString(event) || badString(token)|| data.badData()) {
			log.warning(UPDATE_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long eventId = Long.parseLong(event);

		log.info(String.format(UPDATE_EVENT_START, eventId,tokenId));
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Key eventKey = eventKeyFactory.newKey(eventId);
		
		Transaction txn = datastore.newTransaction();

		try {
			Entity eventEntity = txn.get(eventKey);
	
			if(eventEntity == null) {
				txn.rollback();
				log.warning(String.format(EVENT_NOT_FOUND_ERROR, eventId));
				return Response.status(Status.NOT_FOUND).build();
			}
	
			Entity tokenEntity = txn.get(tokenKey);
	
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}
	
			if(!eventEntity.getString(EVENT_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))) {
				Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;
				if(tokenRole.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR, tokenId,tokenRole.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}
	
			LatLng location = LatLng.of(data.location[0],data.location[1]);
	
			Timestamp start = Timestamp.parseTimestamp(data.start);
			Timestamp end = Timestamp.parseTimestamp(data.end);
	
			Entity updatedEvent = Entity.newBuilder(eventEntity)
					.set(EVENT_NAME_PROPERTY, data.name)
					.set(EVENT_DESCRIPTION_PROPERTY, data.description)
					.set(EVENT_START_PROPERTY, start)
					.set(EVENT_END_PROPERTY, end)
					.set(EVENT_LOCATION_PROPERTY, location)
					.build();
		
			txn.update(updatedEvent);
			txn.commit();
			log.info(String.format(UPDATE_EVENT_OK, data.name,updatedEvent.getKey().getId(),tokenId));
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
	 * Obtains the event data.
	 * @param event - The event identification that is going to be used to obtain its data.
	 * @param token - The token id of the user or the institution who is performing this request.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_PATH)
	public Response getEvent(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(event) || badString (token)) {
			log.warning(GET_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long eventId = Long.parseLong(event);

		log.fine(String.format(GET_EVENT_START,eventId,tokenId));

		Key eventKey = eventKeyFactory.newKey(eventId);
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			Entity eventEntity = txn.get(eventKey);
			txn.commit();
			
			if(eventEntity == null) {
				txn.rollback();
				log.warning(String.format(EVENT_NOT_FOUND_ERROR, eventId));
				return Response.status(Status.NOT_FOUND).build();
			}
	
			EventData data = new EventData(eventEntity);
	
			log.info(String.format(GET_EVENT_OK,eventEntity.getString(EVENT_NAME_PROPERTY),eventId,tokenId));
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
	 * The user joins to the event.
	 * @param event - The identification of the event where the user is going to join.
	 * @param token - The token id of the user who is going to join to the event.
	 * @return 200, if the user has successfully joined the event.
	 * 		   400, if the data is invalid.
	 * 		   403, if the event already ended.
	 * 		   404, if the event does not exist or the token does not exist.
	 * 		   409, if the user has already joined to the event.
	 * 		   500, otherwise.
	 */
	@POST
	@Path(JOIN_PATH)
	public Response joinEvent(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(event) || badString(token)) {
			log.warning(JOIN_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long eventId = Long.parseLong(event);

		log.info(String.format(JOIN_EVENT_START, eventId,tokenId));

		Key eventKey = eventKeyFactory.newKey(eventId);
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity eventEntity = txn.get(eventKey);
	
			if(eventEntity == null) {
				txn.rollback();
				log.warning(String.format(EVENT_NOT_FOUND_ERROR, eventId));
				return Response.status(Status.NOT_FOUND).build();
			}
	
			if(!eventEntity.getBoolean(EVENT_STATUS_PROPERTY) || Timestamp.now().compareTo(eventEntity.getTimestamp(EVENT_END_PROPERTY)) >= 0) {
				txn.rollback();
				log.warning(String.format(EVENT_ENDED_ERROR,eventEntity.getString(EVENT_NAME_PROPERTY),eventId));
				return Response.status(Status.FORBIDDEN).build();
			}
	
			Entity tokenEntity = txn.get(tokenKey);
	
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
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
			
			Query<Key> participantQuery = Query.newKeyQueryBuilder().setKind(PARTICIPANT_KIND)
					.setFilter(CompositeFilter.and(PropertyFilter.hasAncestor(eventKey),PropertyFilter.eq(PARTICIPANT_ID_PROPERTY, accountKey.getId()))).build();
	
			QueryResults<Key> participantList = txn.run(participantQuery);
			
			if(participantList.hasNext()) {
				txn.rollback();
				log.warning(String.format(JOIN_EVENT_CONFLICT_ERROR,user,eventId));
				return Response.status(Status.CONFLICT).build();
			}
	
			Key participantKey = datastore.allocateId(datastore.newKeyFactory().setKind(PARTICIPANT_KIND).addAncestor(PathElement.of(EVENT_KIND, eventId)).newKey());
	
			Entity participant = Entity.newBuilder(participantKey)
					.set(PARTICIPANT_ID_PROPERTY, accountKey.getId())
					.build();
		
			txn.add(participant);
			txn.commit();
			log.info(String.format(JOIN_EVENT_OK,eventEntity.getString(EVENT_NAME_PROPERTY), eventId,tokenId));
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
	 * Removes the user from the event.
	 * @param event - The event identification that the user has joined.
	 * @param token - The token id of the user who wants to leave the event.
	 * @return 200, if the user has successfully left the event.
	 * 		   400, if the data is invalid.
	 * 		   403, if the event already ended.
	 * 		   404, if the event does not exist or the user has not joined to the event or the token does not exist.
	 * 		   500, otherwise.
	 */
	@DELETE
	@Path(LEAVE_PATH)
	public Response leaveEvent(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(event) || badString(token)) {
			log.warning(LEAVE_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long eventId = Long.parseLong(event);

		log.info(String.format(LEAVE_EVENT_START, eventId,tokenId));
		
		Transaction txn = datastore.newTransaction();
		
		Key eventKey = eventKeyFactory.newKey(eventId);
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		try {
			Entity eventEntity =txn.get(eventKey);
	
			if(eventEntity == null) {
				txn.rollback();
				log.warning(String.format(EVENT_NOT_FOUND_ERROR, eventId));
				return Response.status(Status.NOT_FOUND).build();
			}
	
			if(!eventEntity.getBoolean(EVENT_STATUS_PROPERTY) || Timestamp.now().compareTo(eventEntity.getTimestamp(EVENT_END_PROPERTY)) >= 0) {
				txn.rollback();
				log.warning(String.format(EVENT_ENDED_ERROR,eventEntity.getString(EVENT_NAME_PROPERTY),eventId));
				return Response.status(Status.FORBIDDEN).build();
			}
	
			Entity tokenEntity = txn.get(tokenKey);
			
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
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
			
			Query<Key> participantQuery = Query.newKeyQueryBuilder().setKind(PARTICIPANT_KIND)
					.setFilter(CompositeFilter.and(PropertyFilter.hasAncestor(eventKey),PropertyFilter.eq(PARTICIPANT_ID_PROPERTY, accountKey.getId()))).build();
	
			QueryResults<Key> participantList = txn.run(participantQuery);
			
			if(!participantList.hasNext()) {
				txn.rollback();
				log.warning(String.format(LEAVE_EVENT_NOT_FOUND_ERROR,user,eventId));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Key participantKey = participantList.next();
			
			if(participantList.hasNext()) {
				txn.rollback();
				log.warning(MULTIPLE_PARTICIPANT_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		
			txn.delete(participantKey);
			txn.commit();
			log.info(String.format(LEAVE_EVENT_OK, eventEntity.getString(EVENT_NAME_PROPERTY),eventId,tokenId));
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
	 * Obtains the list of the events given the token.
	 * @param token - The token id of the user or the institution requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(LIST_PATH)
	public Response listAllEvents(@QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(token)) {
			log.warning(LIST_EVENTS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(LIST_EVENTS_START,tokenId));

		Query<Entity> query = Query.newEntityQueryBuilder().setKind(EVENT_KIND).build();

		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			QueryResults<Entity> events = txn.run(query);
			txn.commit();
			List<EventData> eventList = new LinkedList<>();	

			events.forEachRemaining(event ->eventList.add(new EventData(event)));

			log.info(String.format(LIST_EVENTS_OK, tokenId));
			return Response.ok(g.toJson(eventList)).build();
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
	 * Obtains the list of user given the event id.
	 * @param event - The event identification which is going to be used to obtain the users.
	 * @param token - The token id of the user or the institution requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   404, if the event does not exist.
	 */
	@GET
	@Path(LIST_BY_EVENT_PATH)
	public Response listUsersByEvent(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(event) || badString(token)) {
			log.warning(LIST_EVENT_PARTICIPANTS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		long eventId = Long.parseLong(event);

		log.info(String.format(LIST_EVENT_PARTICIPANTS_START, eventId,tokenId));

		Key eventKey = eventKeyFactory.newKey(eventId);
		
		Query<Entity> participantQuery = Query.newEntityQueryBuilder().setKind(PARTICIPANT_KIND)
				.setFilter(PropertyFilter.hasAncestor(eventKey)).build();
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			Entity eventEntity = txn.get(eventKey);
	
			if(eventEntity == null) {
				txn.rollback();
				log.warning(String.format(EVENT_NOT_FOUND_ERROR, eventId));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			QueryResults<Entity> participantList = txn.run(participantQuery);
			List<Key> participantKeys = new LinkedList<>();
			participantList.forEachRemaining(participant->participantKeys.add(accountKeyFactory.newKey(participant.getLong(PARTICIPANT_ID_PROPERTY))));
			
			Key[] keyArray = new Key[participantKeys.size()];
			participantKeys.toArray(keyArray);
			
			Iterator<Entity> participantEntities = txn.get(keyArray);
			txn.commit();
			
			List<Account>participantAccounts = new LinkedList<>();
			participantEntities.forEachRemaining(account->participantAccounts.add(new Account(account,false)));
			
			
			log.info(String.format(LIST_EVENT_PARTICIPANTS_OK, eventEntity.getString(EVENT_NAME_PROPERTY),eventId,tokenId));
			return Response.ok(g.toJson(participantAccounts)).build();
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
	 * Cancels the event eventId.
	 * @param eventId - The event identification to be canceled.
	 * @return true, if the event was successfully canceled.
	 * 		   false, otherwise.
	 */
	public static boolean  cancelEvent(long eventId) {
		Key eventKey = eventKeyFactory.newKey(eventId);
		
		Query<Entity> participantQuery = Query.newEntityQueryBuilder().setFilter(PropertyFilter.hasAncestor(eventKey)).build();
		
		Transaction txn = datastore.newTransaction();

		try {
			Entity eventEntity = txn.get(eventKey);
	
			if(eventEntity == null) {
				txn.rollback();
				return false;
			}
	
			List<Key> toDelete = new LinkedList<>();
			List<Long> toNotify = new LinkedList<>();
			toDelete.add(eventKey);
	
			QueryResults<Entity> participantList = txn.run(participantQuery);
			
			participantList.forEachRemaining(participant->{
				toDelete.add(participant.getKey());
				toNotify.add(participant.getLong(PARTICIPANT_ID_PROPERTY));
			});
			
			Key[] keys = new Key[toDelete.size()];
			toDelete.toArray(keys);
			txn.delete(keys);
			txn.commit();
		
			String message = String.format(EVENT_CANCELED_NOTIFICATION, eventEntity.getString(EVENT_NAME_PROPERTY));
			toNotify.forEach(id->{
				if(!addNotificationToFeed(id,message))
					log.warning(String.format(NOTIFICATION_ERROR,id));
			});
			
			return true;
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return false;
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return true;
			}
		}
		
	}

}
