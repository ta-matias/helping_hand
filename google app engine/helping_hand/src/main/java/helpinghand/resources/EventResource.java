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
import helpinghand.util.event.*;

import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PARAM;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_KIND;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PROPERTY;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ROLE_PROPERTY;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_OWNER_PROPERTY;
import static helpinghand.resources.UserResource.addNotificationToFeed;
import static helpinghand.util.GeneralUtils.TOKEN_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_ACCESS_INSUFFICIENT_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_OWNER_ERROR;
import static helpinghand.util.GeneralUtils.NOTIFICATION_ERROR;
import static helpinghand.util.GeneralUtils.badString;

/**
 * @author PogChamp Software
 *
 */
@Path(EventResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class EventResource {
	
	private static final String EVENT_CANCELED_NOTIFICATION = "Event '%s' has been canceled";
	
	private static final String DATASTORE_EXCEPTION_ERROR = "Error in EventResource: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error is EventResource: Transaction was active";
	private static final String EVENT_NOT_FOUND_ERROR = "There is no event with id (%s)";
	private static final String MULTIPLE_PARTICIPANT_ERROR = "Participant is registed multiple times in the event";
	private static final String EVENT_ENDED_ERROR = "Event [%s](%s) already ended";
	
	private static final String CREATE_EVENT_START  = "Attempting to create event with token [%s]";
	private static final String CREATE_EVENT_OK = "Successfuly created event [%s](%d) with token [%s]";
	private static final String CREATE_EVENT_BAD_DATA_ERROR  = "Create event attempt failed due to bad inputs";
	
	private static final String END_EVENT_START  = "Attempting to end event (%d) with token [%s]";
	private static final String END_EVENT_OK = "Successfuly ended event [%s](%d) with token [%s]";
	private static final String END_EVENT_BAD_DATA_ERROR  = "End event attempt failed due to bad inputs";
	
	private static final String CANCEL_EVENT_START  = "Attempting to cancel event (%d) with token [%s]";
	private static final String CANCEL_EVENT_OK = "Successfuly canceled event [%s](%d) with token [%s]";
	private static final String CANCEL_EVENT_BAD_DATA_ERROR  = "Cancel event attempt failed due to bad inputs";
	
	private static final String UPDATE_EVENT_START  = "Attempting to update event (%d) with token [%s]";
	private static final String UPDATE_EVENT_OK = "Successfuly updated event [%s](%d) with token [%s]";
	private static final String UPDATE_EVENT_BAD_DATA_ERROR  = "Update event attempt failed due to bad inputs";
	
	private static final String GET_EVENT_START  = "Attempting to get event (%d) with token [%s]";
	private static final String GET_EVENT_OK = "Successfuly got event [%s](%d) with token [%s]";
	private static final String GET_EVENT_BAD_DATA_ERROR  = "Get event attempt failed due to bad inputs";
	
	private static final String JOIN_EVENT_START  = "Attempting to join event (%d) event with token [%s]";
	private static final String JOIN_EVENT_OK = "Successfuly joined event [%s](%d) with token [%s]";
	private static final String JOIN_EVENT_BAD_DATA_ERROR  = "Join event attempt failed due to bad inputs";
	private static final String JOIN_EVENT_CONFLICT_ERROR  = "User [%s] already joined event (%d)";
	
	private static final String LEAVE_EVENT_START  = "Attempting to leave event (%d) with token [%s]";
	private static final String LEAVE_EVENT_OK = "Successfuly left event [%s](%d) with token [%s]";
	private static final String LEAVE_EVENT_BAD_DATA_ERROR  = "Leave event attempt failed due to bad inputs";
	private static final String LEAVE_EVENT_NOT_FOUND_ERROR  = "User [%s] has not joined event (%d)";
	
	private static final String LIST_EVENTS_START  = "Attempting to get all events with token [%s]";
	private static final String LIST_EVENTS_OK = "Successfuly got all events with token [%s]";
	private static final String LIST_EVENTS_BAD_DATA_ERROR  = "Get all events attempt failed due to bad inputs";
	
	private static final String LIST_EVENT_PARTICIPANTS_START  = "Attempting to get event (%d) participants with token [%s]";
	private static final String LIST_EVENT_PARTICIPANTS_OK = "Successfuly got event [%s](%d) participants with token [%s]";
	private static final String LIST_EVENT_PARTICIPANTS_BAD_DATA_ERROR  = "Get event participants attempt failed due to bad inputs";
	
	private static final String GET_EVENT_STATUS_START  = "Attempting to get event (%d) status with token [%s]";
	private static final String GET_EVENT_STATUS_OK = "Successfuly got event [%s](%d) status with token [%s]";
	private static final String GET_EVENT_STATUS_BAD_DATA_ERROR  = "Get event status attempt failed due to bad inputs";
	
	private static final String UPDATE_EVENT_STATUS_START  = "Attempting to update event (%d) status with token [%s]";
	private static final String UPDATE_EVENT_STATUS_OK = "Successfuly updated event [%s](%d) status with token [%s]";
	private static final String UPDATE_EVENT_STATUS_BAD_DATA_ERROR  = "Updates event status attempt failed due to bad inputs";
	
	private static final String EVENT_ID_PARAM = "eventId";
	private static final String STATUS_PARAM = "status";
	
	public static final String PATH = "/event";
	private static final String LIST_PATH = ""; // GET
	private static final String CREATE_PATH = ""; // POST
	private static final String UPDATE_PATH = "/{"+EVENT_ID_PARAM+"}"; // PUT
	private static final String CANCEL_PATH = "/{"+EVENT_ID_PARAM+"}"; // DELETE
	private static final String END_PATH = "/{"+EVENT_ID_PARAM+"}/end"; // PUT
	private static final String GET_PATH = "/{"+EVENT_ID_PARAM+"}/get"; // GET
	private static final String JOIN_PATH = "/{"+EVENT_ID_PARAM+"}/join"; // POST
	private static final String LEAVE_PATH = "/{"+EVENT_ID_PARAM+"}/leave"; // DELETE
	private static final String LIST_BY_EVENT_PATH = "/{"+EVENT_ID_PARAM+"}/list"; // GET
	private static final String GET_STATUS_PATH = "/{"+EVENT_ID_PARAM+"}/status";//GET
	private static final String UPDATE_STATUS_PATH = "/{"+EVENT_ID_PARAM+"}/status";//PUT
	
	private static final String EVENT_KIND = "Event";
	private static final String EVENT_NAME_PROPERTY ="name";
	private static final String EVENT_CREATOR_PROPERTY ="creator";
	private static final String EVENT_DESCRIPTION_PROPERTY ="description";
	private static final String EVENT_START_PROPERTY = "start";
	private static final String EVENT_END_PROPERTY = "end";
	private static final String EVENT_LOCATION_PROPERTY = "location";
	private static final String EVENT_CONDITIONS_PROPERTY ="conditions";
	private static final String EVENT_STATUS_PROPERTY ="status";
	private static final boolean EVENT_STATUS_DEFAULT = true;
	private static final boolean EVENT_FINISHED_STATUS = false;
	
	private static final String PARTICIPANT_KIND = "Participant";
	private static final String PARTICIPANT_ID_PROPERTY = "id";
	
	
	
	
	
	
	private static final Logger log = Logger.getLogger(EventResource.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();
	
	public EventResource() {}
	
	/**
	 * Creates a new event.
	 * @param tokenId - The token id from the user or the institution.
	 * @param data - The event data that contains the name, the eventId, the creatorId,
	 * 		  the beginDate, the endDate, the description and the conditions.
	 * @return 200, if the creation was successful.
	 * 		   400, if the attributes are invalid.
	 * 		   409, if the event already exists.
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
		
		log.info(String.format(CREATE_EVENT_START,token));
		
		if(!AccessControlManager.getOwner(token).equals(data.creator)) {
			log.warning(String.format(TOKEN_OWNER_ERROR, token,data.creator));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		LatLng location = LatLng.of(data.location[0],data.location[1]);
		ListValue.Builder builder = ListValue.newBuilder();
		for(String condition: data.conditions) {
			builder.addValue(condition);
		}
		ListValue conditions = builder.build();
		
		Key eventKey = datastore.allocateId(datastore.newKeyFactory().setKind(EVENT_KIND).newKey());
		Entity event = Entity.newBuilder(eventKey)
		.set(EVENT_NAME_PROPERTY, data.name)
		.set(EVENT_CREATOR_PROPERTY, data.creator)
		.set(EVENT_DESCRIPTION_PROPERTY, data.description)
		.set(EVENT_START_PROPERTY, data.start)
		.set(EVENT_END_PROPERTY, data.end)
		.set(EVENT_LOCATION_PROPERTY, location)
		.set(EVENT_CONDITIONS_PROPERTY,conditions)
		.set(EVENT_STATUS_PROPERTY, EVENT_STATUS_DEFAULT)
		.build();
		
		Transaction txn = datastore.newTransaction();
			
		try {
			
			txn.add(event);
			txn.commit();
			log.info(String.format(CREATE_EVENT_OK, data.name,eventKey.getId(),token));
			
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
	 * Finishes the event.
	 * @param eventId - The identification of the event to be finished.
	 * @param tokenId - The token id of the user or the institution who is going to finish the event.
	 * @return 200, if the event was finished with success.
	 * 		   404, if the event does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(END_PATH)
	public Response endEvent(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(token)) {
			log.warning(END_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		log.info(String.format(END_EVENT_START, event,token));
		
		Entity eventEntity = QueryUtils.getEntityById(EVENT_KIND,Long.parseLong(event));
		if(eventEntity == null) {
			log.warning(String.format(EVENT_NOT_FOUND_ERROR, event));
			return Response.status(Status.NOT_FOUND).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(TOKEN_KIND, TOKEN_ID_PROPERTY, token);
		if(token == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR,token));
		}
		
		if(!eventEntity.getString(EVENT_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))){
			Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
			int minAccess = 1;
			if(tokenRole.getAccess()< minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,tokenRole.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		Entity updatedEvent = Entity.newBuilder(eventEntity)
		.set(EVENT_STATUS_PROPERTY, EVENT_FINISHED_STATUS)
		.build();
		
		//TODO:Do something to participants?
		
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			txn.update(updatedEvent);
			log.info(String.format(END_EVENT_OK, updatedEvent.getString(EVENT_NAME_PROPERTY),event,token));
			txn.commit();
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
	 * Cancels the event.
	 * @param eventId - The identification of the event to be canceled.
	 * @param tokenId - The token id of the user or the institution who is going to cancel the event.
	 * @return 200, if the cancellation was successful.
	 * 		   404, if the event does not exist.
	 * 		   500, otherwise.
	 */
	@DELETE
	@Path(CANCEL_PATH)
	public Response cancelEvent(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(event)||badString(token)) {
			log.warning(CANCEL_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(CANCEL_EVENT_START,token));
		
		Entity eventEntity = QueryUtils.getEntityById(EVENT_KIND,Long.parseLong(event));
		if(eventEntity == null) {
			log.warning(String.format(EVENT_NOT_FOUND_ERROR, event));
			return Response.status(Status.NOT_FOUND).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(TOKEN_KIND, TOKEN_ID_PROPERTY, token);
		if(token == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR,token));
		}
		
		if(!eventEntity.getString(EVENT_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))){
			Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
			int minAccess = 1;
			if(tokenRole.getAccess()< minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,tokenRole.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		List<Entity> participants = QueryUtils.getEntityChildrenByKind(eventEntity, PARTICIPANT_KIND);
		for(Entity participant: participants) {
			if(!addNotificationToFeed(participant.getString(PARTICIPANT_ID_PROPERTY),String.format(EVENT_CANCELED_NOTIFICATION, eventEntity.getString(EVENT_NAME_PROPERTY)))) {
				log.severe(NOTIFICATION_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
		List<Key> toDelete = participants.stream().map(entity->entity.getKey()).collect(Collectors.toList());
		toDelete.add(eventEntity.getKey());
		
		Key[] keys = new Key[toDelete.size()];
		toDelete.toArray(keys);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			txn.delete(keys);
			txn.commit();
			log.info(String.format(CANCEL_EVENT_OK,eventEntity.getString(EVENT_NAME_PROPERTY),eventEntity.getKey().getId(),token));
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
	 * Updates the event data of the eventId.
	 * @param eventId - The event identification that is going to be updated.
	 * @param tokenId - The token id of the user or the institution who is going to update the event.
	 * @param data - The updated data for this event.
	 * @return 200, if the update was successful.
	 * 		   404, if the event does not exist.
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
		
		log.info(String.format(UPDATE_EVENT_START, event,token));
		
		Entity eventEntity = QueryUtils.getEntityById(EVENT_KIND, Long.parseLong(event));
		if(eventEntity == null) {
			log.warning(String.format(EVENT_NOT_FOUND_ERROR, event));
			return Response.status(Status.NOT_FOUND).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(TOKEN_KIND, TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		if(!eventEntity.getString(EVENT_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))) {
			Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
			int minAccess = 1;
			
			if(tokenRole.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR, token,tokenRole.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		LatLng location = LatLng.of(data.location[0],data.location[1]);
		ListValue.Builder builder = ListValue.newBuilder();
		for(String condition: data.conditions) {
			builder.addValue(condition);
		}
		ListValue conditions = builder.build();
		
		Entity updatedEvent = Entity.newBuilder(eventEntity)
		.set(EVENT_NAME_PROPERTY, data.name)
		.set(EVENT_CREATOR_PROPERTY, data.creator)
		.set(EVENT_DESCRIPTION_PROPERTY, data.description)
		.set(EVENT_START_PROPERTY, data.start)
		.set(EVENT_END_PROPERTY, data.end)
		.set(EVENT_LOCATION_PROPERTY, location)
		.set(EVENT_CONDITIONS_PROPERTY,conditions)
		.build();
		Transaction txn = datastore.newTransaction();
		
		try {
			
			txn.update(updatedEvent);
			txn.commit();
			log.info(String.format(UPDATE_EVENT_OK, data.name,updatedEvent.getKey().getId(),token));
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
	 * @param eventId - The event identification that is going to be used to obtain its data.
	 * @param tokenId - The token id of the user or the institution who is performing this request.
	 * @return 200, if the operation was successful.
	 * 		   404, if the event does not exist.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_PATH)
	public Response getEvent(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(event) || badString (token)) {
			log.warning(GET_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.fine(String.format(GET_EVENT_START,event,token));
		
		Entity eventEntity = QueryUtils.getEntityById(EVENT_KIND, Long.parseLong(event));
		
		LatLng location = eventEntity.getLatLng(EVENT_LOCATION_PROPERTY);
		List<String> conditionsList = eventEntity.getList(EVENT_CONDITIONS_PROPERTY).stream().map(value -> ((String)value.get())).collect(Collectors.toList());
		String[] conditions = new String[conditionsList.size()];
		conditionsList.toArray();
		
		EventData data = new EventData(
		eventEntity.getKey().getId(),
		eventEntity.getString(EVENT_NAME_PROPERTY),
		eventEntity.getString(EVENT_CREATOR_PROPERTY),
		eventEntity.getString(EVENT_DESCRIPTION_PROPERTY),
		eventEntity.getString(EVENT_START_PROPERTY),
		eventEntity.getString(EVENT_END_PROPERTY),
		new double[] {location.getLatitude(),location.getLongitude()},
		conditions
		);
		
		log.info(String.format(GET_EVENT_OK,eventEntity.getString(EVENT_NAME_PROPERTY),event,token));
		return Response.ok(g.toJson(data)).build();
		
	}
	
	/**
	 * Joins the user to the event.
	 * @param eventId - The identification of the event where the user is going to join.
	 * @param tokenId - The token id of the user who is going to join to the event.
	 * @return 200, if the user has successfully joined the event.
	 * 		   404, if the event does not exist or the user has already joined to the event.
	 * 		   500, otherwise.
	 */
	@POST
	@Path(JOIN_PATH)
	public Response joinEvent(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(event) || badString(token)) {
			log.warning(JOIN_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		log.info(String.format(JOIN_EVENT_START, event,token));
		
		Entity eventEntity = QueryUtils.getEntityById(EVENT_KIND, Long.parseLong(event));
		if(eventEntity == null) {
			log.warning(String.format(EVENT_NOT_FOUND_ERROR, event));
			return Response.status(Status.NOT_FOUND).build();
		}
		
		if(!eventEntity.getBoolean(EVENT_STATUS_PROPERTY)) {
			log.warning(String.format(EVENT_ENDED_ERROR,eventEntity.getString(EVENT_NAME_PROPERTY),event));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		String user = AccessControlManager.getOwner(token);
		if(user == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		
		List<Entity> checkList = QueryUtils.getEntityChildrenByKindAndProperty(eventEntity, PARTICIPANT_KIND, PARTICIPANT_ID_PROPERTY, user);
		if(checkList.size()>1) {
			log.severe(MULTIPLE_PARTICIPANT_ERROR);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		if(checkList.size() == 1) {
			log.warning(String.format(JOIN_EVENT_CONFLICT_ERROR, user,event));
			return Response.status(Status.CONFLICT).build();
		}
		
		
		Key participantKey = datastore.allocateId(datastore.newKeyFactory().setKind(PARTICIPANT_KIND).addAncestor(PathElement.of(EVENT_KIND, eventEntity.getKey().getId())).newKey());
		Entity participant = Entity.newBuilder(participantKey)
		.set(PARTICIPANT_ID_PROPERTY, user)
		.build();
		
		Transaction txn = datastore.newTransaction();
		
		try {
			txn.add(participant);
			txn.commit();
			log.info(String.format(JOIN_EVENT_OK,eventEntity.getString(EVENT_NAME_PROPERTY), event,token));
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
	 * @param eventId - The event identification that the user has joined.
	 * @param tokenId - The token id of the user who wants to leave the event.
	 * @return 200, if the user has successfully left the event.
	 * 		   404, if the event does not exist or the user does not exist.
	 * 		   500, otherwise.
	 */
	@DELETE
	@Path(LEAVE_PATH)
	public Response leaveEvent(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(event) || badString(token)) {
			log.warning(LEAVE_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		log.info(String.format(LEAVE_EVENT_START, event,token));
		
		Entity eventEntity = QueryUtils.getEntityById(EVENT_KIND, Long.parseLong(event));
		if(eventEntity == null) {
			log.warning(String.format(EVENT_NOT_FOUND_ERROR, event));
			return Response.status(Status.NOT_FOUND).build();
		}
		if(!eventEntity.getBoolean(EVENT_STATUS_PROPERTY)) {
			log.warning(String.format(EVENT_ENDED_ERROR,eventEntity.getString(EVENT_NAME_PROPERTY),event));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		String user = AccessControlManager.getOwner(token);
		if(user == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		
		List<Entity> checkList = QueryUtils.getEntityChildrenByKindAndProperty(eventEntity, PARTICIPANT_KIND,PARTICIPANT_ID_PROPERTY, user);
		
		if(checkList.size()>1) {
			log.severe(MULTIPLE_PARTICIPANT_ERROR);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		if(checkList.isEmpty()) {
			log.warning(String.format(LEAVE_EVENT_NOT_FOUND_ERROR, user,event));
			return Response.status(Status.NOT_FOUND).build();
		}
		Entity check = checkList.get(0);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			txn.delete(check.getKey());
			txn.commit();
			log.info(String.format(LEAVE_EVENT_OK, eventEntity.getString(EVENT_NAME_PROPERTY),event,token));
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
	 * Obtains the list of the events given the tokenId.
	 * @param tokenId - The token id of the user or the institution requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(LIST_PATH)
	public Response listAllEvents(@QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(token)) {
			log.warning(LIST_EVENTS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		log.info(String.format(LIST_EVENTS_START,token));
		
		Query<Entity> query = Query.newEntityQueryBuilder().setKind(EVENT_KIND).build();
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			QueryResults<Entity> events = txn.run(query);
			txn.commit();
			List<String[]> eventList = new LinkedList<>();	

			events.forEachRemaining(event ->eventList.add(new String[] {Long.toString(event.getKey().getId()),event.getString(EVENT_NAME_PROPERTY),Boolean.toString(event.getBoolean(EVENT_STATUS_PROPERTY))}));

			log.info(String.format(LIST_EVENTS_OK, token));
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
	 * @param eventId - The event identification which is going to be used to obtain the users.
	 * @param tokenId - The token id of the user or the institution requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   404, if the event does not exist.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(LIST_BY_EVENT_PATH)
	public Response listUsersByEvent(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(event) || badString(token)) {
			log.warning(LIST_EVENT_PARTICIPANTS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		log.info(String.format(LIST_EVENT_PARTICIPANTS_START, event,token));
		
		Entity eventEntity = QueryUtils.getEntityById(EVENT_KIND,Long.parseLong(event));
		if(eventEntity == null) {
			log.warning(String.format(EVENT_NOT_FOUND_ERROR, event));
			return Response.status(Status.NOT_FOUND).build();
		}
		
		List<String> participants = QueryUtils.getEntityChildrenByKind(eventEntity, PARTICIPANT_KIND).stream().map(entity->entity.getString(PARTICIPANT_ID_PROPERTY)).collect(Collectors.toList());
		
		log.info(String.format(LIST_EVENT_PARTICIPANTS_OK, eventEntity.getString(EVENT_NAME_PROPERTY),event,token));
		return Response.ok(g.toJson(participants)).build();
	}
	
	/**
	 * Returns current status of event
	 * @param event
	 * @param token
	 * @return
	 */
	@GET
	@Path(GET_STATUS_PATH)
	public Response getStatus(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(event) || badString(token)) {
			log.warning(GET_EVENT_STATUS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		log.info(String.format(GET_EVENT_STATUS_START, event,token));
		
		Entity eventEntity = QueryUtils.getEntityById(EVENT_KIND,Long.parseLong(event));
		if(eventEntity == null) {
			log.warning(String.format(EVENT_NAME_PROPERTY, event));
			return Response.status(Status.NOT_FOUND).build();
		}
		
		log.info(String.format(GET_EVENT_STATUS_OK, eventEntity.getString(EVENT_NAME_PROPERTY),event,token));
		return Response.ok(Boolean.toString(eventEntity.getBoolean(EVENT_STATUS_PROPERTY))).build();
		
	}
	
	/**
	 * Updates current status of event
	 * @param event
	 * @param token
	 * @param status
	 * @return
	 */
	@PUT
	@Path(UPDATE_STATUS_PATH)
	public Response updateStatus(@PathParam(EVENT_ID_PARAM) String event, @QueryParam(TOKEN_ID_PARAM) String token,@QueryParam(STATUS_PARAM)String status) {
		if(badString(event) || badString(token) || badString(status)) {
			log.warning(UPDATE_EVENT_STATUS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(UPDATE_EVENT_STATUS_START, event,token));
		
		Entity eventEntity = QueryUtils.getEntityById(EVENT_KIND,Long.parseLong(event));
		if(eventEntity == null) {
			log.warning(String.format(EVENT_NAME_PROPERTY, event));
			return Response.status(Status.NOT_FOUND).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(TOKEN_KIND, TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		if(!eventEntity.getString(EVENT_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))) {
			Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
			int minAccess = 1;
			
			if(tokenRole.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR, token,tokenRole.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		Entity updatedEvent = Entity.newBuilder(eventEntity)
		.set(EVENT_STATUS_PROPERTY, Boolean.parseBoolean(status))
		.build();
		
		Transaction txn = datastore.newTransaction();
		
		try {
			txn.update(updatedEvent);
			txn.commit();
			log.info(String.format(UPDATE_EVENT_STATUS_OK, updatedEvent.getString(EVENT_NAME_PROPERTY),event,token));
			return Response.ok().build();
		} catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
			}
		}
		
	}
	
}
