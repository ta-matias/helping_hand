package helpinghand.resources;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;
import com.google.gson.Gson;

import helpinghand.accesscontrol.Role;
import helpinghand.util.account.Account;
import helpinghand.util.backoffice.*;

import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PARAM;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_KIND;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ROLE_PROPERTY;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_OWNER_PROPERTY;
import static helpinghand.accesscontrol.AccessControlManager.updateTokenRole;
import static helpinghand.resources.UserResource.USER_ID_PARAM;
import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.GeneralUtils.TOKEN_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.APP_SECRET_KIND;
import static helpinghand.util.GeneralUtils.APP_SECRET_VALUE_PROPERTY;
import static helpinghand.util.GeneralUtils.AVATAR_0;
import static helpinghand.util.GeneralUtils.OUR_EMAIL;
import static helpinghand.util.GeneralUtils.NOTIFICATION_ERROR;
import static helpinghand.util.account.AccountUtils.ACCOUNT_NOT_FOUND_ERROR;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_CONFLICT_ERROR;
import static helpinghand.util.account.AccountUtils.ACCOUNT_KIND;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_EMAIL_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_PASSWORD_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ROLE_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_CREATION_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_VISIBILITY_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_STATUS_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_INFO_KIND;
import static helpinghand.util.account.AccountUtils.ACCOUNT_INFO_PHONE_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_INFO_ADDRESS_1_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_INFO_ADDRESS_2_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_INFO_CITY_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_INFO_ZIPCODE_PROPERTY;
import static helpinghand.resources.UserResource.USER_PROFILE_KIND;
import static helpinghand.util.account.AccountUtils.PROFILE_NAME_PROPERTY;
import static helpinghand.util.account.AccountUtils.PROFILE_BIO_PROPERTY;
import static helpinghand.util.account.AccountUtils.PROFILE_AVATAR_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_FEED_KIND;
import static helpinghand.util.account.AccountUtils.ACCOUNT_FEED_NOTIFICATIONS_PROPERTY;
import static helpinghand.resources.UserResource.USER_STATS_KIND;
import static helpinghand.resources.UserResource.USER_STATS_REQUESTS_PROMISED_PROPERTY;
import static helpinghand.resources.UserResource.USER_STATS_REQUESTS_DONE_PROPERTY;
import static helpinghand.resources.UserResource.USER_STATS_RATING_PROPERTY;
import static helpinghand.util.account.AccountUtils.CREATE_ID_CONFLICT_ERROR;
import static helpinghand.util.account.AccountUtils.CREATE_EMAIL_CONFLICT_ERROR;
import static helpinghand.util.account.AccountUtils.addNotificationToFeed;
import static helpinghand.util.account.AccountUtils.DEFAULT_PROPERTY_VALUE_STRING;
import static helpinghand.util.account.AccountUtils.DEFAULT_PROPERTY_VALUE_STRINGLIST;
import static helpinghand.resources.UserResource.USER_STATS_INITIAL_RATING;
import static helpinghand.resources.UserResource.USER_STATS_INITIAL_REQUESTS;

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
	private static final String ROLE_CHANGED_NOTIFICATION = "A role da sua conta foi alterada para %s";

	private static final String UPDATE_TOKEN_ROLE_START = "Attempting to update current role of token (%d) to [%s]";
	private static final String UPDATE_TOKEN_ROLE_OK = "Successfulty to updated current role of token (%d) to [%s]";
	private static final String UPDATE_TOKEN_ROLE_BAD_DATA_ERROR = "Update current token role attempt failed due to bad inputs";
	private static final String UPDATE_TOKEN_ROLE_UPDATE_ERROR = "Update current token role attempt failed while changing account's role";
	private static final String SYSADMIN_CHANGE_ERROR ="Error in AccessControlManager: There was an attempt to change role of Sysadmin [%s]";
	private static final String INSTITUTION_CHANGE_ERROR ="Error in AccessControlManager: There was an attempt to change role of an Institution [%s]";

	private static final String LIST_ROLE_START = "Attempting to get [%s] accounts with token (%d)";
	private static final String LIST_ROLE_OK = "Successfulty to got [%s] accounts with token (%d)";
	private static final String LIST_ROLE_BAD_DATA_ERROR = "List accounts by role attempt failed due to bad inputs";

	private static final String DAILY_STATS_START = "Attempting to get account creation stats from [%s] to [%s] with token (%d)";
	private static final String DAILY_STATS_OK = "Successfulty to got account creation stats from [%s] to [%s] with token (%d)";
	private static final String DAILY_STATS_BAD_DATA_ERROR = "Get daily stats attempt failed due to bad inputs";
	private static final String DAILY_STATS_BAD_DATA_ERROR_DATES = "Get daily stats attempt failed due to bad date inputs [%s] and [%s]";
	
	private static final String CREATE_REPORT_START = "Attempting to create report with token (%d)";
	private static final String CREATE_REPORT_OK = "Successfulty created report with token (%d)";
	private static final String CREATE_REPORT_BAD_DATA_ERROR = "Create report attempt failed due to bad inputs";
	
	private static final String GET_REPORT_START = "Attempting to get report (%d) with token (%d)";
	private static final String GET_REPORT_OK ="Successfulty got report (%d) with token (%d)";
	private static final String GET_REPORT_BAD_DATA_ERROR = "Get report attempt failed due to bad inputs";
	private static final String REPORT_NOT_FOUND_ERROR = "Report (%d) doesn't exist";
	
	private static final String LIST_REPORTS_START = "Attempting to list reports with token (%d)";
	private static final String LIST_REPORTS_OK ="Successfulty listed reports with token (%d)";
	private static final String LIST_REPORTS_BAD_DATA_ERROR = "List reports attempt failed due to bad inputs";

	private static final String RESPOND_REPORT_START = "Attempting to respond to report (%d) with token (%d)";
	private static final String RESPOND_REPORT_OK = "Successfulty responded to report (%d) with token (%d)";
	private static final String RESPOND_REPORT_BAD_DATA_ERROR = "Respond to report attempt failed due to bad inputs";
	private static final String RESPOND_REPORT_NOTIFICATION =" %s viu o seu report em %s e respondeu : %s";
	
	private static final String DELETE_REPORT_START = "Attempting to delete report (%d) with token (%d)";
	private static final String DELETE_REPORT_OK = "Successfulty deleted report (%d) with token (%d)";
	private static final String DELETE_REPORT_BAD_DATA_ERROR = "Delete report attempt failed due to bad inputs";
	
	private static final String CREATE_SYSADMIN_START = "Attempting to create SYSADMIN account with set credentials";
	private static final String CREATE_SYSADMIN_OK = "Successfully created SYSADMIN account with set credentials";
	private static final String CREATE_SYSADMIN_BAD_SECRET_ERROR = "SYSADMIN account creation failed due to bad secret input";
	private static final String CREATE_SYSADMIN_PASSWORD_NOT_SET_ERROR = "SYSADMIN account creation failed due to the secret password not being set";
	private static final String CREATE_SYSADMIN_WRONG_PASSWORD_ERROR = "SYSADMIN account creation failed due to wrong secret password";
	private static final String CREATE_SYSADMIN_CREDENTIALS_NOT_SET_ERROR = "SYSADMIN account creation failed due to id or password not being set";
	private static final String MULTIPLE_SYSADMIN_ERROR = "There are multiple SYSADMIN accounts";
	private static final String WRONG_SYSADMIN_ERROR = "The existing SYSADMIN account does not follow the set credentials";
	
	private static final String USER_ROLE_PARAM = "role";
	private static final String START_DATE_PARAM = "startDate";
	private static final String END_DATE_PARAM = "endDate";
	private static final String REPORT_ID_PARAM = "reportId";
	
	private static final String REPORT_KIND = "Report";
	public static final String REPORT_DATE_PROPERTY = "date";
	public static final String REPORT_CREATOR_PROPERTY = "creator";
	public static final String REPORT_SUBJECT_PROPERTY = "subject";
	public static final String REPORT_TEXT_PROPERTY = "text";
	
	private static final String SECRET_PASSWORD_PARAM = "secretPassword";
	private static final String SYSADMIN_ID_KEY = "SYSADMIN_ID";
	private static final String SYSADMIN_PASSWORD_KEY = "SYSADMIN_PASSWORD";
	private static final String CREATE_SYSADMIN_PASSWORD_KEY = "CREATE_SYSADMIN_PASSWORD";

	// Paths
	public static final String PATH = "/restricted";
	private static final String UPDATE_ACCOUNT_ROLE_PATH = "/updateAccountRole"; //PUT
	private static final String UPDATE_TOKEN_ROLE_PATH = "/updateTokenRole";//PUT
	private static final String LIST_ROLE_PATH = "/listRole"; // GET
	private static final String DAILY_USERS_PATH = "/dailyUsers"; // GET
	private static final String CREATE_REPORT_PATH = "/createReport"; // POST
	private static final String GET_REPORT_PATH = "/getReport"; // GET
	private static final String LIST_REPORTS_PATH = "/listReports"; // GET
	private static final String RESPOND_REPORT_PATH = "/respondReport"; // PUT
	private static final String DELETE_REPORT_PATH = "/deleteReport"; // DELETE
	private static final String CREATE_SYSADMIN_PATH = "/createSysadmin";//POST

	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final Logger log = Logger.getLogger(BackOfficeResource.class.getName());
	private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind(TOKEN_KIND);
	private static final KeyFactory reportKeyFactory = datastore.newKeyFactory().setKind(REPORT_KIND);
	
	private final Gson g = new Gson();

	public BackOfficeResource() {}

	/**
	 * Updates the role of the account.
	 * @param id - The identification of the account.
	 * @param role - The updated role for the account.
	 * @param token - The token of the account that is performing this operation.
	 * @return 200, if the update was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot alter higher or same level account or the token cannot change the role to a higher level
	 * 		   or the token does not exist or there was an attempt to change the role of SuperUser.
	 * 		   404, if the account does not exist.
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
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Query<Entity> accountQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		Query<Key> tokenQuery = Query.newKeyQueryBuilder().setKind(TOKEN_KIND).setFilter(PropertyFilter.eq(TOKEN_OWNER_PROPERTY, id)).build();
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity tokenEntity = txn.get(tokenKey);
			
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			QueryResults<Entity> accountList = txn.run(accountQuery);	
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Entity oldAccount = accountList.next();
	
			if(accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR, id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
	
			Role accountRole = Role.getRole(oldAccount.getString(ACCOUNT_ROLE_PROPERTY));
			
			if(accountRole.equals(Role.INSTITUTION)) {
				txn.rollback();
				log.severe(String.format(INSTITUTION_CHANGE_ERROR, id));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			if(accountRole.equals(Role.SYSADMIN)) {
				txn.rollback();
				log.severe(String.format(SYSADMIN_CHANGE_ERROR, id));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			Role tokenRole = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
	
			if(tokenRole.getAccess() <= accountRole.getAccess()) {
				txn.rollback();
				log.warning(String.format(TOKEN_JURISDICTION_ERROR,tokenId,tokenRole.getAccess(),oldAccount.getString(ACCOUNT_ID_PROPERTY),accountRole.getAccess()));
				return Response.status(Status.FORBIDDEN).build();
			}
	
			if(tokenRole.getAccess() < targetRole.getAccess()) {
				txn.rollback();
				log.warning(String.format(TOKEN_POWER_ERROR, tokenId,tokenRole.getAccess(),targetRole.getAccess()));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			Entity newAccount = Entity.newBuilder(oldAccount)
					.set("role", targetRole.name())
					.build();
				
			QueryResults<Key> tokenList = txn.run(tokenQuery);
			
			List<Key> tokenKeyList = new LinkedList<>();
			tokenList.forEachRemaining(key->tokenKeyList.add(key));
			Key[] tokenKeyArray = new Key[tokenKeyList.size()];
			tokenKeyList.toArray(tokenKeyArray);
			
			txn.update(newAccount);
			txn.delete(tokenKeyArray);
			txn.commit();
			
			String message = String.format(ROLE_CHANGED_NOTIFICATION, targetRole.name());
			addNotificationToFeed(newAccount.getKey().getId(),message);
			
			log.info(String.format(UPDATE_ACCOUNT_ROLE_OK ,id,targetRole.name()));
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

		if(updateTokenRole(tokenId, targetRole)) {
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
	 * 		   500, otherwise.
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
		
		Query<Entity> accountQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ROLE_PROPERTY, role)).build();
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			QueryResults<Entity> accounts = txn.run(accountQuery);
			txn.commit();
			List<Account> data = new LinkedList<>();
			accounts.forEachRemaining(account->data.add(new Account(account,true)));
	
			log.info(String.format(LIST_ROLE_OK, roleParam.name(),tokenId));
			return Response.ok(g.toJson(data)).build();
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

		Timestamp startTimestamp;
		Timestamp endTimestamp;
		
		try {
			startTimestamp = Timestamp.parseTimestamp(start);
			endTimestamp = Timestamp.parseTimestamp(end);
		} catch(Exception e) {
			log.warning(String.format(DAILY_STATS_BAD_DATA_ERROR_DATES,start,end));
			return Response.status(Status.BAD_REQUEST).build();
		}

		log.info(String.format(DAILY_STATS_START,start,end,tokenId));

		Query<Entity> query = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND)
				.setFilter(CompositeFilter.and(PropertyFilter.gt(ACCOUNT_CREATION_PROPERTY, startTimestamp),PropertyFilter.lt(ACCOUNT_CREATION_PROPERTY,endTimestamp))).build();

		Map<Instant,Integer> map = initializeMap(Instant.parse(start).truncatedTo(ChronoUnit.DAYS),Instant.parse(end).truncatedTo(ChronoUnit.DAYS));

		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			QueryResults<Entity> creationTimestamps = txn.run(query);
			txn.commit();
			creationTimestamps.forEachRemaining(projectionEntity -> {
				Timestamp creationTimestamp = projectionEntity.getTimestamp(ACCOUNT_CREATION_PROPERTY);

				Instant creation = creationTimestamp.toDate().toInstant().truncatedTo(ChronoUnit.DAYS);

				int counter = map.get(creation);
				counter++;
				map.put(creation, counter);
			});

			List<DailyStat> data = new LinkedList<>();
			map.keySet().forEach(key-> data.add(new DailyStat(key.toString().substring(0,10),map.get(key))));

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
	 * @return data with statistics.
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
	
	/**
	 * Creates a new report.
	 * @param token - The token of the account requesting this operation.
	 * @param data - The report creation data.
	 * @return 200, if the report was successfully created.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not exist.
	 * 		   500, otherwise.
	 */
	@POST
	@Path(CREATE_REPORT_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createReport(@QueryParam(TOKEN_ID_PARAM)String token, CreateReport data) {
		if(badString(token) || data.badData()) {
			log.warning(CREATE_REPORT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		log.info(String.format(CREATE_REPORT_START,tokenId));
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Key reportKey = datastore.allocateId(reportKeyFactory.newKey());
		
		Timestamp now = Timestamp.now();
		
		Transaction txn = datastore.newTransaction();

		try {
			Entity tokenEntity = txn.get(tokenKey);
			
			if(tokenEntity == null) {
				txn.rollback();
				log.warning(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			Entity reportEntity = Entity.newBuilder(reportKey)
					.set(REPORT_CREATOR_PROPERTY, tokenEntity.getString(TOKEN_OWNER_PROPERTY))
					.set(REPORT_DATE_PROPERTY,now)
					.set(REPORT_SUBJECT_PROPERTY,data.subject)
					.set(REPORT_TEXT_PROPERTY,data.text)
					.build();
			
			txn.add(reportEntity);
			txn.commit();
			
			log.info(String.format(CREATE_REPORT_OK,tokenId));
			return Response.ok().build();
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
	 * Obtains the report data.
	 * @param token - the token of the account requesting this operation.
	 * @param report - The identification of the report.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   404, if the report does not exist.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_REPORT_PATH)
	public Response getReport(@QueryParam(TOKEN_ID_PARAM)String token, @QueryParam(REPORT_ID_PARAM)String report) {
		if(badString(token) ||badString(report)) {
			log.warning(GET_REPORT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		long reportId = Long.parseLong(report);
		
		log.info(String.format(GET_REPORT_START, reportId,tokenId));
		
		Key reportKey = reportKeyFactory.newKey(reportId);
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			Entity reportEntity = txn.get(reportKey);
			txn.commit();
			
			if(reportEntity == null) {
				log.warning(String.format(REPORT_NOT_FOUND_ERROR, reportId));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Report data = new Report(reportEntity);

			log.info(String.format(GET_REPORT_OK,reportId,tokenId));
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
	 * Obtains the list of the reports created by the account.
	 * @param token - The token of the account requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(LIST_REPORTS_PATH)
	public Response listReports(@QueryParam(TOKEN_ID_PARAM)String token) {
		if(badString(token)) {
			log.warning(LIST_REPORTS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		log.info(String.format(LIST_REPORTS_START,tokenId));
		
		Query<Entity> reportQuery = Query.newEntityQueryBuilder().setKind(REPORT_KIND).addOrderBy(OrderBy.asc(REPORT_DATE_PROPERTY)).build();
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			QueryResults<Entity> reportList = txn.run(reportQuery);
			txn.commit();
			
			List<Report> data = new LinkedList<>();
			reportList.forEachRemaining(report->data.add(new Report(report)));
			
			log.info(String.format(LIST_REPORTS_OK,tokenId));
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
	 * Responds to a report.
	 * @param token - The token of the account requesting this operation.
	 * @param report - The report identification to be responded.
	 * @return 200, if the report was successfully responded to.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not exist.
	 * 		   404, if the report does not exist or the account does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(RESPOND_REPORT_PATH)
	public Response respondReport(@QueryParam(TOKEN_ID_PARAM)String token, @QueryParam(REPORT_ID_PARAM)String report, ReportResponse data) {
		if(badString(token) || badString(report) || data.badData()) {
			log.warning(RESPOND_REPORT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		long reportId = Long.parseLong(report);
		
		log.info(String.format(RESPOND_REPORT_START, reportId,tokenId));
		
		Key reportKey = reportKeyFactory.newKey(reportId);
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			Entity reportEntity = txn.get(reportKey);
			
			if(reportEntity == null) {
				txn.rollback();
				log.warning(String.format(REPORT_NOT_FOUND_ERROR, reportId));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Entity tokenEntity = txn.get(tokenKey);
			
			if(tokenEntity == null) {
				txn.rollback();
				log.warning(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
				return Response.status(Status.FORBIDDEN).build();
			}
			String creator = reportEntity.getString(REPORT_CREATOR_PROPERTY);
			
			Query<Key> accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY,creator)).build();
			QueryResults<Key> accountList = txn.run(accountQuery);
			txn.commit();
			
			if(!accountList.hasNext()) {
				log.warning(String.format(ACCOUNT_NOT_FOUND_ERROR,creator));
				return Response.status(Status.NOT_FOUND).build();
			}
			Key accountKey = accountList.next();
			if(accountList.hasNext()) {
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR, creator));
			}
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, dd MMMM yyyy hh:mm:ss");
			LocalDateTime now =  LocalDateTime.now();
			String date =formatter.format(now);
			String message = String.format(RESPOND_REPORT_NOTIFICATION,tokenEntity.getString(TOKEN_OWNER_PROPERTY),date,data.message);
			if(!addNotificationToFeed(accountKey.getId(),message)) {
				log.warning(String.format(NOTIFICATION_ERROR, accountKey.getId()));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			log.info(String.format(RESPOND_REPORT_OK,reportId,tokenId));
			return Response.ok().build();
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
	 * Deletes a report.
	 * @param token - The token of the account requesting this operation.
	 * @param report - The report identification to be deleted.
	 * @return 200, if the report was successfully deleted.
	 * 		   400, if the data is invalid.
	 * 		   404, if the report does not exist.
	 * 		   500, otherwise.
	 */
	@DELETE
	@Path(DELETE_REPORT_PATH)
	public Response deleteReport(@QueryParam(TOKEN_ID_PARAM)String token, @QueryParam(REPORT_ID_PARAM)String report) {
		if(badString(token) || badString(report)) {
			log.warning(DELETE_REPORT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		long reportId = Long.parseLong(report);
		
		log.info(String.format(DELETE_REPORT_START, reportId,tokenId));
		
		Key reportKey = reportKeyFactory.newKey(reportId);
		
		Transaction txn = datastore.newTransaction();

		try {
			Entity reportEntity = txn.get(reportKey);
			
			if(reportEntity == null) {
				txn.rollback();
				log.warning(String.format(REPORT_NOT_FOUND_ERROR, reportId));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			

			txn.delete(reportKey);
			txn.commit();
			
			log.info(String.format(DELETE_REPORT_OK,reportId,tokenId));
			return Response.ok().build();
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
	 * Creates a new administrator account.
	 * @param secret - The secret password of the administrator.
	 * @return 200, if the registration was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the secret password is wrong.
	 * 		   409, if the account with the id/email already exists.
	 * 		   500, otherwise.
	 */
	@POST
	@Path(CREATE_SYSADMIN_PATH)
	public Response createSysadmin(@QueryParam(SECRET_PASSWORD_PARAM)String secret) {
		if(badString( secret)) {
			log.warning(CREATE_SYSADMIN_BAD_SECRET_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		
		KeyFactory secretKeyFactory = datastore.newKeyFactory().setKind(APP_SECRET_KIND);
		Key suIdKey = secretKeyFactory.newKey(SYSADMIN_ID_KEY);
		Key suPasswordKey = secretKeyFactory.newKey(SYSADMIN_PASSWORD_KEY);
		Key createSuPasswordKey = secretKeyFactory.newKey(CREATE_SYSADMIN_PASSWORD_KEY);
		
		Key accountKey = datastore.allocateId(datastore.newKeyFactory().setKind(ACCOUNT_KIND).newKey());
		Key accountInfoKey = datastore.newKeyFactory().addAncestor(PathElement.of(ACCOUNT_KIND, accountKey.getId())).setKind(ACCOUNT_INFO_KIND).newKey(accountKey.getId());
		Key accountFeedKey = datastore.newKeyFactory().addAncestor(PathElement.of(ACCOUNT_KIND, accountKey.getId())).setKind(ACCOUNT_FEED_KIND).newKey(accountKey.getId());
		Key userProfileKey = datastore.newKeyFactory().addAncestor(PathElement.of(ACCOUNT_KIND, accountKey.getId())).setKind(USER_PROFILE_KIND).newKey(accountKey.getId());
		Key userStatsKey = datastore.newKeyFactory().addAncestor(PathElement.of(ACCOUNT_KIND, accountKey.getId())).setKind(USER_STATS_KIND).newKey(accountKey.getId());

		Timestamp now = Timestamp.now();

		Entity accountInfo = Entity.newBuilder(accountInfoKey)
				.set(ACCOUNT_INFO_PHONE_PROPERTY,StringValue.newBuilder(DEFAULT_PROPERTY_VALUE_STRING).setExcludeFromIndexes(true).build())
				.set(ACCOUNT_INFO_ADDRESS_1_PROPERTY,StringValue.newBuilder(DEFAULT_PROPERTY_VALUE_STRING).setExcludeFromIndexes(true).build())
				.set(ACCOUNT_INFO_ADDRESS_2_PROPERTY,StringValue.newBuilder(DEFAULT_PROPERTY_VALUE_STRING).setExcludeFromIndexes(true).build())
				.set(ACCOUNT_INFO_ZIPCODE_PROPERTY,DEFAULT_PROPERTY_VALUE_STRING)
				.set(ACCOUNT_INFO_CITY_PROPERTY,DEFAULT_PROPERTY_VALUE_STRING)
				.build();

		Entity userProfile = Entity.newBuilder(userProfileKey)
				.set(PROFILE_AVATAR_PROPERTY, AVATAR_0)
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
		
		
		Query<Key> emailQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_EMAIL_PROPERTY,OUR_EMAIL)).build();
		Query<Entity> suQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ROLE_PROPERTY, Role.SYSADMIN.name())).build();
		
		log.info(CREATE_SYSADMIN_START);
		
		Transaction txn =  datastore.newTransaction();
		try {
			Entity createSuPassword = txn.get(createSuPasswordKey);
			if(createSuPassword == null) {
				txn.rollback();
				log.severe(CREATE_SYSADMIN_PASSWORD_NOT_SET_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			if(!createSuPassword.getString(APP_SECRET_VALUE_PROPERTY).equals(DigestUtils.sha512Hex(secret))) {
				txn.rollback();
				log.warning(CREATE_SYSADMIN_WRONG_PASSWORD_ERROR);
				return Response.status(Status.FORBIDDEN).build();
			}
			
			Entity suId = txn.get(suIdKey);
			Entity suEncryptedPassword = txn.get(suPasswordKey);
			if(suId ==null || suEncryptedPassword == null) {
				txn.rollback();
				log.severe(CREATE_SYSADMIN_CREDENTIALS_NOT_SET_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			String id = suId.getString(APP_SECRET_VALUE_PROPERTY);
			String encryptedPassword = suEncryptedPassword.getString(APP_SECRET_VALUE_PROPERTY);
			
			Query<Key> idQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY,id)).build();
			Entity account = Entity.newBuilder(accountKey)
					.set(ACCOUNT_ID_PROPERTY, id)
					.set(ACCOUNT_EMAIL_PROPERTY,OUR_EMAIL)
					.set(ACCOUNT_PASSWORD_PROPERTY, StringValue.newBuilder(encryptedPassword).setExcludeFromIndexes(true).build())
					.set(ACCOUNT_ROLE_PROPERTY,Role.SYSADMIN.name())
					.set(ACCOUNT_CREATION_PROPERTY,now)
					.set(ACCOUNT_STATUS_PROPERTY,true)
					.set(ACCOUNT_VISIBILITY_PROPERTY, false)
					.build();
			
			QueryResults<Entity> suList = txn.run(suQuery);
			if(suList.hasNext()) {
				Entity suAccount = suList.next();
				if(suAccount.getString(ACCOUNT_ID_PROPERTY).equals(id)){
					if(suList.hasNext()) {
						txn.rollback();
						log.severe(MULTIPLE_SYSADMIN_ERROR);
						return Response.status(Status.INTERNAL_SERVER_ERROR).build();
					}
					txn.commit();
					return Response.ok().build();
				}
				txn.rollback();
				log.severe(WRONG_SYSADMIN_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			QueryResults<Key> idCheck = txn.run(idQuery);

			if(idCheck.hasNext()) {
				txn.rollback();
				log.warning(String.format(CREATE_ID_CONFLICT_ERROR,id));
				return Response.status(Status.CONFLICT).build();
			} 
			
			QueryResults<Key> emailCheck = txn.run(emailQuery);
			
			if(emailCheck.hasNext()) {
				txn.rollback();
				log.warning(String.format(CREATE_EMAIL_CONFLICT_ERROR,OUR_EMAIL));
				return Response.status(Status.CONFLICT).build();
			}
			
			txn.add(account,accountInfo,userProfile,accountFeed,userStats);
			
			txn.commit();
			log.info(CREATE_SYSADMIN_OK);
			
			return Response.ok().build();
			
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
	
}
