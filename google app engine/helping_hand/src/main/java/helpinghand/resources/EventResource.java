/**
 * 
 */
package helpinghand.resources;

import java.util.ArrayList;
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
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
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
import static helpinghand.util.GeneralUtils.TOKEN_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_ACCESS_INSUFFICIENT_ERROR;
import static helpinghand.util.GeneralUtils.badString;

/**
 * @author PogChamp Software
 *
 */
@Path(EventResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class EventResource {
	
	private static final String DATASTORE_EXCEPTION_ERROR = "Error in EventResource: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error is EventResource: Transaction was active";
	private static final String EVENT_NOT_FOUND_ERROR = "There is no event with id [%d]";
	
	private static final String CREATE_EVENT_START  = "Attempting to create event with token [%s]";
	private static final String CREATE_EVENT_OK = "Successfuly created event [%s](%d) with token [%s]";
	private static final String CREATE_EVENT_BAD_DATA_ERROR  = "Create event attempt failed due to bad inputs";
	
	private static final String END_EVENT_START  = "Attempting to end event with token [%s]";
	private static final String END_EVENT_OK = "Successfuly ended event [%s](%d) with token [%s]";
	private static final String END_EVENT_BAD_DATA_ERROR  = "End event attempt failed due to bad inputs";
	
	private static final String CANCEL_EVENT_START  = "Attempting to cancel event with token [%s]";
	private static final String CANCEL_EVENT_OK = "Successfuly canceled event [%s](%d) with token [%s]";
	private static final String CANCEL_EVENT_BAD_DATA_ERROR  = "Cancel event attempt failed due to bad inputs";
	

	public static final String PATH = "/event";
	private static final String CREATE_PATH = ""; // POST
	private static final String LIST_PATH = ""; // GET
	private static final String END_PATH = "/{eventId}/end"; // PUT
	private static final String CANCEL_PATH = "/{eventId}"; // DELETE
	private static final String GET_PATH = "/{eventId}"; // GET
	private static final String UPDATE_PATH = "/{eventId}"; // PUT
	private static final String JOIN_PATH = "/{eventId}/join"; // POST
	private static final String LEAVE_PATH = "/{eventId}/leave"; // DELETE
	private static final String LIST_BY_EVENT_PATH = "/{eventId}/list"; // GET
	private static final String GET_STATUS_PATH = "/{eventId}/status";
	
	private static final String EVENT_KIND = "Event";
	private static final String EVENT_ID_PROPERTY ="id";
	private static final String EVENT_NAME_PROPERTY ="name";
	private static final String EVENT_CREATOR_PROPERTY ="creator";
	private static final String EVENT_DESCRIPTION ="description";
	private static final String EVENT_START_PROPERTY = "start";
	private static final String EVENT_END_PROPERTY = "end";
	private static final String EVENT_LOCATION_PROPERTY = "location";
	private static final String EVENT_STATUS_PROPERTY ="status";
	private static final boolean EVENT_STATUS_DEFAULT = true;
	
	private static final String PARTICIPANT_KIND = "Participant";
	private static final String PARTICIPANT_ID_PROPERTY = "id";
	
	
	private static final String EVENT_ID_PARAM = "eventId";
	
	private static final Logger log = Logger.getLogger(EventResource.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	private final KeyFactory eventKeyFactory = datastore.newKeyFactory().setKind(EVENT_KIND);

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
	public Response createEvent(@QueryParam(TOKEN_ID_PARAM) String token, Event data) {
		
		if(data.badData() || badString(token)) {
			log.warning(CREATE_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(CREATE_EVENT_START,token));
		
		LatLng location = LatLng.of(data.location[0],data.location[1]);
		
		Key eventKey = datastore.allocateId(datastore.newKeyFactory().setKind(EVENT_KIND).newKey());
		Entity event = Entity.newBuilder(eventKey)
		.set(EVENT_NAME_PROPERTY, data.name)
		.set(EVENT_CREATOR_PROPERTY, data.creator)
		.set(EVENT_DESCRIPTION, data.description)
		.set(EVENT_START_PROPERTY, data.start)
		.set(EVENT_END_PROPERTY, data.end)
		.set(EVENT_LOCATION_PROPERTY, location)
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
	public Response endEvent(@PathParam("eventId") long event, @QueryParam("tokenId") String token) {
		if(badString(token)) {
			log.warning(END_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		
		Key eventKey = eventKeyFactory.newKey(event);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity event = txn.get(eventKey);
			
			if(event == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("The event with id " + eventId + " does not exist!").build();
			}
			
			Entity newEvent = Entity.newBuilder(event)
					.set("status", false)
					.build();
			
			txn.update(newEvent);
			LOG.info("The event " + eventId + " is finished.");
			txn.commit();
			return Response.ok("The event " + eventId + " is finished.").build();
		} catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction as active!").build();
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
	public Response cancelEvent(@PathParam(EVENT_ID_PARAM) long event, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(token)) {
			log.warning(CANCEL_EVENT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(CANCEL_EVENT_START,token));
		
		Entity eventEntity = QueryUtils.getEntityById(EVENT_KIND,event);
		if(eventEntity == null) {
			log.warning(EVENT_NOT_FOUND_ERROR);
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
		
		List<Key> toDelete = QueryUtils.getEntityChildrenByKind(eventEntity, PARTICIPANT_KIND).stream().map(entity->entity.getKey()).collect(Collectors.toList());
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
	public Response updateEvent(@PathParam("eventId") String eventId, @QueryParam("tokenId") String tokenId, EventDataPublic data) {
		LOG.fine("Changing attributes attempt with the token: " + tokenId);
		
		Key eventKey = eventKeyFactory.newKey(eventId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity event = txn.get(eventKey);
			
			if(event == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("The event with the id " + eventId + " does not exist!").build();
			}
			
			Entity newEvent = Entity.newBuilder(event)
					.set("name", data.name)
					.set("beginDate", data.beginDate)
					.set("endDate", data.endDate)
					.set("description", data.description)
					.set("conditions", data.conditions)
					.set("address", data.address)
					.set("location", data.location)
					.build();
			
			txn.update(newEvent);
			LOG.info("Event " + eventId + " has successfully been updated.");
			txn.commit();
			return Response.ok("Event " + eventId + " has successfully been updated.").build();
		} catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction as active!").build();
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
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEvent(@PathParam("eventId") String eventId, @QueryParam("tokenId") String tokenId) {
		LOG.fine("Getting attributes attributes of the event " + eventId + " by the user with the token: " + tokenId);
		
		Key eventKey = eventKeyFactory.newKey(eventId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity event = txn.get(eventKey);
			
			if(event == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("The event with id " + eventId + " does not exist!").build();
			}
			
			EventDataPublic publicData = new EventDataPublic(
					event.getString("name"),
					event.getString("beginDate"),
					event.getString("endDate"),
					event.getString("description"),
					event.getString("conditions"),
					event.getString("address"),
					event.getString("location")
					);
			
			LOG.info("Event " + eventId + " has successfully got his own attributes!");
			txn.commit();
			return Response.ok(g.toJson(publicData)).build();
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
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response joinEvent(@PathParam("eventId") String eventId, @QueryParam("tokenId") String tokenId) {
		LOG.fine("Attempting to join the user with the token: " + tokenId + " to the event: " + eventId);
		
		Key eventKey = eventKeyFactory.newKey(eventId);
		String userId = AccessControlManager.getOwner(tokenId);
		Key userKey = datastore.newKeyFactory().addAncestor(PathElement.of(EVENT_KIND, eventId)).setKind(USERKIND).newKey(userId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity event = txn.get(eventKey);
			Entity user = txn.get(userKey);
			
			if(event == null || user != null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("The event does not exist or the user has already joined to the event!").build();
			}
			
			Entity toAdd = Entity.newBuilder(userKey)
					.set("userId", userId)
					.build();
			txn.add(toAdd);
			
			LOG.info("User " + userId + " has successfully joined to the event " + eventId + "!");
			txn.commit();
			return Response.ok("User " + userId + " has successfully joined to the event " + eventId + "!").build();
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
	public Response leaveEvent(@PathParam("eventId") String eventId, @QueryParam("tokenId") String tokenId) {
		LOG.fine("Attempting to remove the user with the token: " + tokenId + " from: " + eventId);
		
		Key eventKey = eventKeyFactory.newKey(eventId);
		String userId = AccessControlManager.getOwner(tokenId);
		Key userKey = datastore.newKeyFactory().addAncestor(PathElement.of(EVENTKIND, eventId)).setKind(USERKIND).newKey(userId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity event = txn.get(eventKey);
			Entity user = txn.get(userKey);
			
			if(event == null || user == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("The event does not exist or the user does not exist!").build();
			}
			
			txn.delete(userKey);
			LOG.info("The user " + userId + " has successfully left the event.");
			txn.commit();
			return Response.ok("The user " + userId + " has successfully left the event.").build();
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
	
	/**
	 * Obtains the list of the events given the tokenId.
	 * @param tokenId - The token id of the user or the institution requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   404, if the list is empty.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(LIST_PATH)
	public Response listAllEvents(@QueryParam("tokenId") String tokenId) {
		LOG.fine("Getting all events!");
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Query<Entity> query = Query.newEntityQueryBuilder()
					.setKind(EVENTKIND)
					.build();

			QueryResults<Entity> subs = txn.run(query);

			List<String[]> subList = new ArrayList<String[]>();	

			subs.forEachRemaining(sub -> {
				subList.add(new String[]{sub.getString("name"), Boolean.toString(sub.getBoolean("status"))});
			});

			if(subList.isEmpty()) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("No Events!").build();
			}

			LOG.info("Successfully got all events!");
			txn.commit();
			return Response.ok(g.toJson(subList)).build();
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
	public Response listUsersByEvent(@PathParam("eventId") String eventId, @QueryParam("tokenId") String tokenId) {
		LOG.fine("Getting users attempt with token: " + tokenId);
		
		Key eventKey = eventKeyFactory.newKey(eventId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity event = txn.get(eventKey);
			
			if(event == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("The event " + eventId + " does not exist!").build();
			}
			
			Query<Entity> query = Query.newEntityQueryBuilder()
					.setKind(USERKIND)
					.setFilter(PropertyFilter.hasAncestor(eventKey))
					.build();
			
			QueryResults<Entity> subs = txn.run(query);
			
			List<String> users = new ArrayList<>();
			
			subs.forEachRemaining(user ->
				users.add(user.getString("userId"))
			);
			
			LOG.info("Successfully got users subscribed to : " + eventId);
			txn.commit();
			return Response.ok(g.toJson(users)).build();	
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
	
	/**
	 * 
	 * @param eventId
	 * @param tokenId
	 * @return
	 */
	@GET
	@Path(GET_STATUS_PATH)
	public Response getStatus(@PathParam("eventId") String eventId, @QueryParam("tokenId") String tokenId) {
		LOG.fine("Getting status attempt to: " + eventId);
		
		Key eventKey = eventKeyFactory.newKey(eventId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity event = txn.get(eventKey);
			
			if(event == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("The event " + eventId + " does not exist!").build();
			}
			
			LOG.info("The " + eventId + " has this status!");
			txn.commit();
			return Response.ok(g.toJson(event.getBoolean("status"))).build();
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
