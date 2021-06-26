/**
 * @author PogChamp Software
 *
 */

package helpinghand.resources;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.*;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;
import com.google.gson.Gson;

import helpinghand.accesscontrol.AccessControlManager;
import helpinghand.accesscontrol.Role;
import helpinghand.util.QueryUtils;

import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PARAM;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_KIND;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PROPERTY;
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

@Path(BackOfficeResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class BackOfficeResource {

	
	private static final String DATASTORE_EXCEPTION_ERROR = "Error in BackOfficeResource: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error in BackOfficeResource: Transaction was active";
	
	private static final String TOKEN_JURISDICTION_ERROR = "Token [%s](access %d) cannot alter higher or same level account [%s](access %d)";
	private static final String TOKEN_POWER_ERROR = "Token [%s](access %d) cannot change an account's role to a higher level (access %d)";
	
	private static final String UPDATE_ROLE_START = "Attempting to update role of user [%s] to [%s]";
	private static final String UPDATE_ROLE_OK = "Successfulty to updated role of user [%s] to [%s]";
	private static final String UPDATE_ROLE_BAD_DATA_ERROR = "Update role attempt failed due to bad inputs";
	private static final String UPDATE_ROLE_UPDATE_ERROR = "Update role attempt failed while changing account's role";
	
	private static final String LIST_ROLE_START = "Attempting to get [%s] account with token [%s]";
	private static final String LIST_ROLE_OK = "Successfulty to got [%s] accounts with token [%s]";
	private static final String LIST_ROLE_BAD_DATA_ERROR = "List accounts by role attempt failed due to bad inputs";
	
	private static final String DAILY_STATS_START = "Attempting to get [%s] account with token [%s]";
	private static final String DAILY_STATS_OK = "Successfulty to got [%s] accounts with token [%s]";
	private static final String DAILY_STATS_BAD_DATA_ERROR = "List accounts by role attempt failed due to bad inputs";
	
	
	
	private static final String USER_ROLE_PARAM = "role";
	private static final String START_DATE_PARAM = "startDate";
	private static final String END_DATE_PARAM = "endDate";
	
	// Paths
	//TODO:fix paths
	public static final String PATH = "/restricted";
	private static final String CHANGE_ROLE_PATH = "/"+USER_ID_PARAM+"/role"; //PUT
	private static final String LIST_ROLE_PATH = "/role"; // GET
	private static final String DAILY_USERS_PATH = "/dailyUsers"; // GET
	

	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	// logger object
	private static final Logger log = Logger.getLogger(UserResource.class.getName());
	
	private final Gson g = new Gson();

	public BackOfficeResource() {}

	
	@PUT
	@Path(CHANGE_ROLE_PATH)
	public Response changeRoleAccount(@PathParam(USER_ID_PARAM) String id, @QueryParam(USER_ROLE_PARAM) String role, @QueryParam(TOKEN_ID_PARAM) String token) {
		log.info(String.format(UPDATE_ROLE_START, id, role));
		Role targetRole = Role.getRole(role);
		if(badString(id) || targetRole == null || badString(token)) {
			log.warning(UPDATE_ROLE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}	
		
		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.warning(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(TOKEN_KIND,TOKEN_ID_PROPERTY,token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR,token));
		}
		
		Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
		Role accountRole = Role.getRole(account.getString(ACCOUNT_ROLE_PROPERTY));
		
		if(tokenRole.getAccess() <= accountRole.getAccess()) {
			log.warning(String.format(TOKEN_JURISDICTION_ERROR,token,tokenRole.getAccess(),account,accountRole.getAccess()));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(tokenRole.getAccess() < targetRole.getAccess()) {
			log.warning(String.format(TOKEN_POWER_ERROR, token,tokenRole.getAccess(),targetRole.getAccess()));
		}
		
		if(AccessControlManager.changeRole(id,targetRole)) {
			log.info(String.format(UPDATE_ROLE_OK ,id,targetRole.name()));
			return Response.ok().build();
		}
		log.severe(UPDATE_ROLE_UPDATE_ERROR);
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}
	
	@GET
	@Path(LIST_ROLE_PATH)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response listAccountsByRole(@PathParam("role") String role, @QueryParam(TOKEN_ID_PARAM)String token) {
		
		Role roleParam = Role.getRole(role);
		if(roleParam == null || badString(token)) {
			log.warning(LIST_ROLE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		log.info(String.format(LIST_ROLE_START,roleParam.name(),token));
		
		List<Entity> entities = QueryUtils.getEntityListByProperty(ACCOUNT_KIND, ACCOUNT_ROLE_PROPERTY, roleParam.name());
		List<String[]> data = entities.stream().map(entity->new String[] {entity.getString(ACCOUNT_ID_PROPERTY),Boolean.toString(entity.getBoolean(ACCOUNT_STATUS_PROPERTY))}).collect(Collectors.toList());
		
		log.info(String.format(LIST_ROLE_OK, roleParam.name(),token));
		return Response.ok(g.toJson(data)).build();
	}

	@GET
	@Path(DAILY_USERS_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response dailyStatistics(@QueryParam(TOKEN_ID_PARAM) String token,@QueryParam(START_DATE_PARAM)String startDate,@QueryParam(END_DATE_PARAM)String endDate) {
		if(badString(token) || badString(startDate) || badString(endDate)) {
			log.warning(DAILY_STATS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(DAILY_STATS_START,startDate,endDate,token));
		
		
		Query<Entity> query = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).build();
		
		Map<LocalDate, Integer> map = initializeMap(startDate, endDate);
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			QueryResults<Entity> users = txn.run(query);
			txn.commit();
			while(users.hasNext()) {
				Entity user = users.next();
				
				String creation = user.getString(ACCOUNT_CREATION_PROPERTY);
				LocalDate creationDate = LocalDate.parse(creation);
				
				if(map.containsKey(creationDate)) {
					int counter = map.get(creationDate);
					counter++;
					map.put(creationDate, counter);
				}
			}
			
			log.info(String.format(DAILY_STATS_OK));
			return Response.ok(g.toJson(map)).build();
		}
		catch (DatastoreException e) {
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
	
	private Map<LocalDate, Integer> initializeMap(String startDate, String endDate) {
		
		Map<LocalDate, Integer> data = new HashMap<LocalDate, Integer>();
		
		LocalDate start = LocalDate.parse(startDate);
		LocalDate end = LocalDate.parse(endDate);
		
		long numOfDays = ChronoUnit.DAYS.between(start, end)+1;
		
		Stream.iterate(start, date -> date.plusDays(1)).limit(numOfDays).forEach(k -> data.put(k, 0));
		
		return data;
	}
}
