package helpinghand.resources;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;
import com.google.gson.Gson;

import helpinghand.accesscontrol.AccessControlManager;
import helpinghand.accesscontrol.Role;
import helpinghand.util.QueryUtils;

import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PARAM;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_KIND;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ROLE_PROPERTY;
import static helpinghand.resources.UserResource.USER_ID_PARAM;
import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.GeneralUtils.TOKEN_NOT_FOUND_ERROR;
import static helpinghand.util.account.AccountUtils.ACCOUNT_KIND;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ROLE_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_STATUS_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_CREATION_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_NOT_FOUND_ERROR;
/**
 * @author PogChamp Software
 *
 */
@Path(BackOfficeResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class BackOfficeResource {

	private static final String DATASTORE_EXCEPTION_ERROR = "Error in BackOfficeResource: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error in BackOfficeResource: Transaction was active";

	private static final String TOKEN_JURISDICTION_ERROR = "Token (%d)(access %d) cannot alter higher or same level account [%s](access %d)";
	private static final String TOKEN_POWER_ERROR = "Token (%d)(access %d) cannot change an account's role to a higher level (access %d)";

	private static final String UPDATE_ACCOUNT_ROLE_START = "Attempting to update role of user [%s] to [%s]";
	private static final String UPDATE_ACCOUNT_ROLE_OK = "Successfulty to updated role of user [%s] to [%s]";
	private static final String UPDATE_ACCOUNT_ROLE_BAD_DATA_ERROR = "Update role attempt failed due to bad inputs";
	private static final String UPDATE_ACCOUNT_ROLE_UPDATE_ERROR = "Update account role attempt failed while changing account's role";

	private static final String UPDATE_TOKEN_ROLE_START = "Attempting to update current role of token (%d) to [%s]";
	private static final String UPDATE_TOKEN_ROLE_OK = "Successfulty to updated current role of token (%d) to [%s]";
	private static final String UPDATE_TOKEN_ROLE_BAD_DATA_ERROR = "Update current token role attempt failed due to bad inputs";
	private static final String UPDATE_TOKEN_ROLE_UPDATE_ERROR = "Update current token role attempt failed while changing account's role";

	private static final String LIST_ROLE_START = "Attempting to get [%s] accounts with token (%d)";
	private static final String LIST_ROLE_OK = "Successfulty to got [%s] accounts with token (%d)";
	private static final String LIST_ROLE_BAD_DATA_ERROR = "List accounts by role attempt failed due to bad inputs";

	private static final String DAILY_STATS_START = "Attempting to get account creation stats from [%s] to [%s] with token (%d)";
	private static final String DAILY_STATS_OK = "Successfulty to got account creation stats from [%s] to [%s] with token (%d)";
	private static final String DAILY_STATS_BAD_DATA_ERROR = "Get daily stats attempt failed due to bad inputs";
	private static final String DAILY_STATS_BAD_DATA_ERROR_DATES = "Get daily stats attempt failed due to bad date inputs [%s] and [%s]";

	private static final String USER_ROLE_PARAM = "role";
	private static final String START_DATE_PARAM = "startDate";
	private static final String END_DATE_PARAM = "endDate";

	// Paths
	public static final String PATH = "/restricted";
	private static final String UPDATE_ACCOUNT_ROLE_PATH = "/updateAccountRole"; //PUT
	private static final String UPDATE_TOKEN_ROLE_PATH = "/updateTokenRole";//PUT
	private static final String LIST_ROLE_PATH = "/listRole"; // GET
	private static final String DAILY_USERS_PATH = "/dailyUsers"; // GET

	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	// logger object
	private static final Logger log = Logger.getLogger(UserResource.class.getName());

	private final Gson g = new Gson();

	public BackOfficeResource() {}

	/**
	 * Updates the role of the account.
	 * @param id - The identification of the account.
	 * @param role - The updated role for the account.
	 * @param token - The token of the account that is performing this operation.
	 * @return 200, if the update was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot alter higher or same level account or the token cannot change the role to a higher level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_ACCOUNT_ROLE_PATH)
	public Response updateRoleAccount(@QueryParam(USER_ID_PARAM) String id, @QueryParam(USER_ROLE_PARAM) String role, @QueryParam(TOKEN_ID_PARAM) String token) {
		Role targetRole = Role.getRole(role);

		if(badString(id) || targetRole == null || badString(token)) {
			log.warning(UPDATE_ACCOUNT_ROLE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}	

		long tokenId = Long.parseLong(token);

		log.info(String.format(UPDATE_ACCOUNT_ROLE_START, id, role));

		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);

		if(account == null) {
			log.warning(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
			return Response.status(Status.NOT_FOUND).build();
		}

		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND,tokenId);

		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
			return Response.status(Status.NOT_FOUND).build();
		}

		Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
		Role accountRole = Role.getRole(account.getString(ACCOUNT_ROLE_PROPERTY));

		if(tokenRole.getAccess() <= accountRole.getAccess()) {
			log.warning(String.format(TOKEN_JURISDICTION_ERROR,tokenId,tokenRole.getAccess(),account,accountRole.getAccess()));
			return Response.status(Status.FORBIDDEN).build();
		}

		if(tokenRole.getAccess() < targetRole.getAccess()) {
			log.warning(String.format(TOKEN_POWER_ERROR, tokenId,tokenRole.getAccess(),targetRole.getAccess()));
			return Response.status(Status.FORBIDDEN).build();
		}

		if(AccessControlManager.updateAccountRole(id,targetRole)) {
			log.info(String.format(UPDATE_ACCOUNT_ROLE_OK ,id,targetRole.name()));
			return Response.ok().build();
		}

		log.severe(UPDATE_ACCOUNT_ROLE_UPDATE_ERROR);
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}

	/**
	 * Updates the role of token.
	 * @param role - The updated role for token.
	 * @param token - The token that is going to perform the update.
	 * @return 200, if the update was successful.
	 * 		   400, if the data is invalid.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_TOKEN_ROLE_PATH)
	public Response updateRoleToken(@QueryParam(USER_ROLE_PARAM) String role, @QueryParam(TOKEN_ID_PARAM) String token) {
		Role targetRole = Role.getRole(role);

		if(targetRole == null || badString(token)) {
			log.warning(UPDATE_TOKEN_ROLE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}	

		long tokenId = Long.parseLong(token);

		log.info(String.format(UPDATE_TOKEN_ROLE_START,tokenId, role));

		if(AccessControlManager.updateTokenRole(tokenId, targetRole)){
			log.info(String.format(UPDATE_TOKEN_ROLE_OK, tokenId,role));
			return Response.ok().build();
		}

		log.severe(UPDATE_TOKEN_ROLE_UPDATE_ERROR);
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}

	/**
	 * List all accounts given the role.
	 * @param role - The role that is used to list users.
	 * @param token - The token of the account that is performing the operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 */
	@GET
	@Path(LIST_ROLE_PATH)
	public Response listAccountsByRole(@QueryParam(USER_ROLE_PARAM) String role, @QueryParam(TOKEN_ID_PARAM)String token) {
		Role roleParam = Role.getRole(role);

		if(roleParam == null || badString(token)) {
			log.warning(LIST_ROLE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(LIST_ROLE_START,roleParam.name(),tokenId));

		List<Entity> entities = QueryUtils.getEntityListByProperty(ACCOUNT_KIND, ACCOUNT_ROLE_PROPERTY, roleParam.name());
		List<String[]> data = entities.stream().map(entity->new String[] {entity.getString(ACCOUNT_ID_PROPERTY),Boolean.toString(entity.getBoolean(ACCOUNT_STATUS_PROPERTY))}).collect(Collectors.toList());

		log.info(String.format(LIST_ROLE_OK, roleParam.name(),tokenId));
		return Response.ok(g.toJson(data)).build();
	}

	/**
	 * Lists the daily statistics.
	 * @param token - The token of the account that is performing this operation.
	 * @param startDate - The start date of the statistics.
	 * @param endDate - The end date of the statistics.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid or the date inputs are invalid.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(DAILY_USERS_PATH)
	public Response dailyStatistics(@QueryParam(TOKEN_ID_PARAM) String token,@QueryParam(START_DATE_PARAM)String start,@QueryParam(END_DATE_PARAM)String end) {
		if(badString(token) || badString(start) || badString(end)) {
			log.warning(DAILY_STATS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		Timestamp startTimestamp ;
		Timestamp endTimestamp;
		
		try {
			startTimestamp = Timestamp.parseTimestamp(start);
			endTimestamp = Timestamp.parseTimestamp(end);
		} catch(Exception e) {
			log.warning(String.format(DAILY_STATS_BAD_DATA_ERROR_DATES,start,end));
			return Response.status(Status.BAD_REQUEST).build();
		}

		log.info(String.format(DAILY_STATS_START,start,end,tokenId));

		Query<ProjectionEntity> query = Query.newProjectionEntityQueryBuilder().setKind(ACCOUNT_KIND).setProjection(ACCOUNT_CREATION_PROPERTY)
				.setFilter(CompositeFilter.and(PropertyFilter.gt(ACCOUNT_CREATION_PROPERTY, startTimestamp),PropertyFilter.lt(ACCOUNT_CREATION_PROPERTY,endTimestamp))).build();

		Map<Instant,Integer> map = initializeMap(Instant.parse(start).truncatedTo(ChronoUnit.DAYS),Instant.parse(end).truncatedTo(ChronoUnit.DAYS));

		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			QueryResults<ProjectionEntity> creationTimestamps = txn.run(query);
			txn.commit();
			creationTimestamps.forEachRemaining(projectionEntity -> {

				Timestamp creationTimestamp = projectionEntity.getTimestamp(ACCOUNT_CREATION_PROPERTY);

				Instant creation = creationTimestamp.toDate().toInstant().truncatedTo(ChronoUnit.DAYS);

				int counter = map.get(creation);
				counter++;
				map.put(creation, counter);
			});

			List<String[]> data = map.keySet().stream().map(key -> new String[] {key.toString().substring(0,10),map.get(key).toString()}).collect(Collectors.toList());

			log.info(String.format(DAILY_STATS_OK, start, end, tokenId));
			return Response.ok(g.toJson(data)).build();
		} catch (DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}

	}

	/**
	 * Initializes the map that is going to be used to list the daily statistics.
	 * @param start - The start date of the statistics.
	 * @param end - The end date of the statistics.
	 * @return data
	 */
	private Map<Instant, Integer> initializeMap(Instant start, Instant end) {
		Map<Instant, Integer> data = new HashMap<>();

		Instant current = Instant.from(start);

		do {
			data.put(current,0);
			current = current.plus(1,ChronoUnit.DAYS);
		} while(!current.isAfter(end));

		return data;
	}

}
