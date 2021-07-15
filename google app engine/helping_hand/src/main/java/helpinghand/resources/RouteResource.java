package helpinghand.resources;

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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;

import helpinghand.util.route.*;

import static helpinghand.resources.RouteResource.BASE_PATH;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PARAM;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_KIND;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_OWNER_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_CONFLICT_ERROR;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_KIND;
import static helpinghand.util.account.AccountUtils.ACCOUNT_NOT_FOUND_ERROR;
import static helpinghand.util.account.AccountUtils.FOLLOWER_ID_PROPERTY;
import static helpinghand.util.account.AccountUtils.FOLLOWER_KIND;
import static helpinghand.util.account.AccountUtils.addNotificationToFeed;
import static helpinghand.util.GeneralUtils.NOTIFICATION_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.badString;
/**
 * 
 * @author PogChamp Software
 *
 */
@Path(BASE_PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RouteResource {
	
	private static final String ROUTE_CREATED_NOTIFICATION = "Route '%s' as been created by '%s'";
	
	private static final String DATASTORE_EXCEPTION_ERROR = "Error in RouteResource: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error is RouteResource: Transaction was active";
	private static final String ROUTE_NOT_FOUND_ERROR = "Route (%d) doesn't exist";
	
	private static final String CREATE_ROUTE_START = "Attempting to create route [%s] with token (%d)";
	private static final String CREATE_ROUTE_OK = "Successfuly created route [%s] with token (%d)";
	private static final String CREATE_ROUTE_BAD_DATA_ERROR = "Create route attempt failed due to bad inputs";
	
	private static final String DELETE_ROUTE_START = "Attempting to delete route (%d) with token (%d)";
	private static final String DELETE_ROUTE_OK = "Successfuly deleted route (%d) with token (%d)";
	private static final String DELETE_ROUTE_BAD_DATA_ERROR = "Delete route attempt failed due to bad inputs";
	
	private static final String UPDATE_ROUTE_START = "Attempting to update route (%d) with token (%d)";
	private static final String UPDATE_ROUTE_OK = "Successfuly update route (%d) with token (%d)";
	private static final String UPDATE_ROUTE_BAD_DATA_ERROR = "Update route attempt failed due to bad inputs";
	private static final String ROUTE_CREATOR_ERROR = "Route (%d)  cannot be updated by user [%s]";
	
	private static final String GET_ROUTE_START = "Attempting to get route (%d) with token (%d)";
	private static final String GET_ROUTE_OK = "Successfuly got route (%d) with token (%d)";
	private static final String GET_ROUTE_BAD_DATA_ERROR = "Get route attempt failed due to bad inputs";
	
	private static final String LIST_ROUTES_START = "Attempting to list routes with token (%d)";
	private static final String LIST_ROUTES_OK = "Successfuly listed routes with token (%d)";
	private static final String LIST_ROUTES_BAD_DATA_ERROR = "List routes attempt failed due to bad inputs";
	
	private static final String START_ROUTE_START = "Attempting to start route (%d) with token (%d)";
	private static final String START_ROUTE_OK = "Successfuly started route (%d) with token (%d)";
	private static final String START_ROUTE_BAD_DATA_ERROR = "Start route attempt failed due to bad inputs";
	
	private static final String END_ROUTE_START = "Attempting to end route (%d) with token (%d)";
	private static final String END_ROUTE_OK = "Successfuly ended route (%d) with token (%d)";
	private static final String END_ROUTE_BAD_DATA_ERROR = "End route attempt failed due to bad inputs";
	
	private static final String ROUTE_ID_PARAM = "routeId";
	public static final String ROUTE_KIND = "Route";
	public static final String ROUTE_CREATOR_PROPERTY = "creator";
	public static final String ROUTE_NAME_PROPERTY = "name";
	public static final String ROUTE_DESCRIPTION_PROPERTY = "description";
	public static final String ROUTE_POINTS_PROPERTY = "points";
	public static final String ROUTE_CATEGORIES_PROPERTY = "categories";
	
	public static final String BASE_PATH = "/route";
	private static final String CREATE_PATH = "";//POST
	private static final String UPDATE_PATH = "/{"+ROUTE_ID_PARAM+"}";//PUT
	private static final String DELETE_PATH = "/{"+ROUTE_ID_PARAM+"}";//DELETE
	private static final String GET_PATH = "/{"+ROUTE_ID_PARAM+"}/get";//GET
	private static final String LIST_PATH = "";//GET
	private static final String START_PATH = "/{"+ROUTE_ID_PARAM+"}/start";//PUT
	private static final String END_PATH = "/{"+ROUTE_ID_PARAM+"}/end";//PUT
	
	private static final Logger log  = Logger.getLogger(RouteResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private final Gson g = new Gson();
	private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind(TOKEN_KIND);
	private static final KeyFactory accountKeyFactory = datastore.newKeyFactory().setKind(ACCOUNT_KIND);
	private static final KeyFactory routeKeyFactory = datastore.newKeyFactory().setKind(ROUTE_KIND);
	
	/**
	 * 
	 * @param token
	 * @param data
	 * @return
	 */
	@POST
	@Path(CREATE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createRoute(@QueryParam(TOKEN_ID_PARAM)String token, CreateRoute data) {
		if(badString(token) || data.badData()) {
			log.warning(CREATE_ROUTE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		log.info(String.format(CREATE_ROUTE_START,data.name,tokenId));
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Key routeKey = datastore.allocateId(routeKeyFactory.newKey());
		
		ListValue.Builder pointsBuilder = ListValue.newBuilder();
		data.points.forEach(point->pointsBuilder.addValue(LatLng.of(point[0], point[1])));
		ListValue points = pointsBuilder.build();
		
		ListValue.Builder categoriesBuilder = ListValue.newBuilder();
		
		for(String category: data.categories)
			categoriesBuilder.addValue(StringValue.of(category));

		ListValue categories = categoriesBuilder.build();
		
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity tokenEntity = txn.get(tokenKey);
		
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			String id = tokenEntity.getString(TOKEN_OWNER_PROPERTY);
			
			Entity routeEntity = Entity.newBuilder(routeKey)
					.set(ROUTE_CREATOR_PROPERTY,id)
					.set(ROUTE_NAME_PROPERTY,data.name)
					.set(ROUTE_DESCRIPTION_PROPERTY,data.description)
					.set(ROUTE_POINTS_PROPERTY,points)
					.set(ROUTE_CATEGORIES_PROPERTY,categories)
					.build();
			
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
			
			Query<Entity> followerQuery = Query.newEntityQueryBuilder().setKind(FOLLOWER_KIND).setFilter(PropertyFilter.hasAncestor(accountKey)).build();
			QueryResults<Entity> followerList = txn.run(followerQuery);
			
			txn.add(routeEntity);
			txn.commit();
			
			String message = String.format(ROUTE_CREATED_NOTIFICATION,data.name,id);
			followerList.forEachRemaining(follower->{
				if(addNotificationToFeed(follower.getLong(FOLLOWER_ID_PROPERTY),message))
					log.warning(String.format(NOTIFICATION_ERROR,follower.getString(FOLLOWER_ID_PROPERTY)));
			});
			
			log.info(String.format(CREATE_ROUTE_OK,data.name,tokenId));
			return Response.ok(routeKey.getId()).build();
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
	 * 
	 * @param route
	 * @param token
	 * @param data
	 * @return
	 */
	@PUT
	@Path(UPDATE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateRoute(@PathParam(ROUTE_ID_PARAM)String route,@QueryParam(TOKEN_ID_PARAM)String token, CreateRoute data) {
		if(badString(route) || badString(token) || data.badData()) {
			log.warning(UPDATE_ROUTE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		long routeId = Long.parseLong(route);
		
		log.info(String.format(UPDATE_ROUTE_START,routeId,tokenId));
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Key routeKey = routeKeyFactory.newKey(routeId);
		
		ListValue.Builder pointsBuilder = ListValue.newBuilder();
		data.points.forEach(point->pointsBuilder.addValue(LatLng.of(point[0], point[1])));
		ListValue points = pointsBuilder.build();
		
		ListValue.Builder categoriesBuilder = ListValue.newBuilder();
		
		for(String category: data.categories)
			categoriesBuilder.addValue(StringValue.of(category));

		ListValue categories = categoriesBuilder.build();
		
		Transaction txn = datastore.newTransaction();

		try {
			Entity routeEntity = txn.get(routeKey);
			
			if(routeEntity == null) {
				txn.rollback();
				log.warning(String.format(ROUTE_NOT_FOUND_ERROR,routeId));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Entity tokenEntity = txn.get(tokenKey);
		
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			if(!routeEntity.getString(ROUTE_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))) {
				txn.rollback();
				log.warning(String.format(ROUTE_CREATOR_ERROR,routeId,tokenEntity.getString(TOKEN_OWNER_PROPERTY)));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			String id = tokenEntity.getString(TOKEN_OWNER_PROPERTY);
		
			Entity updatedRouteEntity = Entity.newBuilder(routeEntity)
					.set(ROUTE_CREATOR_PROPERTY,id)
					.set(ROUTE_NAME_PROPERTY,data.name)
					.set(ROUTE_DESCRIPTION_PROPERTY,data.description)
					.set(ROUTE_POINTS_PROPERTY,points)
					.set(ROUTE_CATEGORIES_PROPERTY,categories)
					.build();
			
			txn.update(updatedRouteEntity);
			txn.commit();
			log.info(String.format(UPDATE_ROUTE_OK,routeId,tokenId));
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
	 * 
	 * @param route
	 * @param token
	 * @return
	 */
	@DELETE
	@Path(DELETE_PATH)
	public Response deleteRoute(@PathParam(ROUTE_ID_PARAM)String route,@QueryParam(TOKEN_ID_PARAM)String token) {
		if(badString(route) || badString(token)) {
			log.warning(DELETE_ROUTE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		long routeId = Long.parseLong(route);
		
		log.info(String.format(DELETE_ROUTE_START,routeId,tokenId));
		
		Key routeKey = routeKeyFactory.newKey(routeId);
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity routeEntity = txn.get(routeKey);
			
			if(routeEntity == null) {
				txn.rollback();
				log.warning(String.format(ROUTE_NOT_FOUND_ERROR,routeId));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Entity tokenEntity = txn.get(tokenKey);
			
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			if(!routeEntity.getString(ROUTE_CREATOR_PROPERTY).equals(tokenEntity.getString(TOKEN_OWNER_PROPERTY))) {
				txn.rollback();
				log.warning(String.format(ROUTE_CREATOR_ERROR,routeId,tokenEntity.getString(TOKEN_OWNER_PROPERTY)));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			txn.delete(routeKey);
			txn.commit();
			log.info(String.format(DELETE_ROUTE_OK,routeId,tokenId));
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
	 * 
	 * @param route
	 * @param token
	 * @return
	 */
	@GET
	@Path(GET_PATH)
	public Response getRoute(@PathParam(ROUTE_ID_PARAM)String route,@QueryParam(TOKEN_ID_PARAM)String token) {
		if(badString(route) || badString(token)) {
			log.warning(GET_ROUTE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		long routeId = Long.parseLong(route);
		
		log.info(String.format(GET_ROUTE_START,routeId,tokenId));
		
		Key routeKey = routeKeyFactory.newKey(routeId);
		
		Transaction txn = datastore.newTransaction();
	
		try {
			Entity routeEntity = txn.get(routeKey);
			txn.commit();
			
			if(routeEntity == null) {
				txn.rollback();
				log.warning(String.format(ROUTE_NOT_FOUND_ERROR,routeId));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Route data = new Route(routeEntity);
			
			log.info(String.format(GET_ROUTE_OK,routeId,tokenId));
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
	 * 
	 * @param token
	 * @return
	 */
	@GET
	@Path(LIST_PATH)
	public Response listRoutes(@QueryParam(TOKEN_ID_PARAM)String token) {
		if(badString(token)) {
			log.warning(LIST_ROUTES_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		log.info(String.format(LIST_ROUTES_START,tokenId));
		
		Query<Entity> routeQuery = Query.newEntityQueryBuilder().setKind(ROUTE_KIND).build();
		
		Transaction txn = datastore.newTransaction();
		
		try {
			QueryResults<Entity> routeList = txn.run(routeQuery);
			txn.commit();
			List<Route> data = new LinkedList<>();
			routeList.forEachRemaining(route->data.add(new Route(route)));
			
			log.info(String.format(LIST_ROUTES_OK,tokenId));
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
	 * 
	 * @param route
	 * @param token
	 * @return
	 */
	@PUT
	@Path(START_PATH)
	public Response startRoute(@PathParam(ROUTE_ID_PARAM)String route,@QueryParam(TOKEN_ID_PARAM)String token) {
		return null;
	}
	
	/**
	 * 
	 * @param route
	 * @param token
	 * @return
	 */
	@PUT
	@Path(END_PATH)
	public Response endRoute(@PathParam(ROUTE_ID_PARAM)String route,@QueryParam(TOKEN_ID_PARAM)String token) {
		return null;
	}
	
}
