/**
 * 
 */
package helpinghand.util.account;

import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.ProjectionEntity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Value;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;
import com.google.gson.Gson;

import helpinghand.accesscontrol.LoginInfo;
import helpinghand.accesscontrol.Role;
import helpinghand.util.event.EventData;
import helpinghand.util.help.HelpData;
import helpinghand.util.route.Route;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;

import static helpinghand.util.GeneralUtils.badPassword;
import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.GeneralUtils.TOKEN_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_OWNER_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_ACCESS_INSUFFICIENT_ERROR;
import static helpinghand.util.GeneralUtils.EMAIL_REGEX;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_KIND;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_OWNER_PROPERTY;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ROLE_PROPERTY;
import static helpinghand.accesscontrol.AccessControlManager.startSession;
import static helpinghand.accesscontrol.AccessControlManager.endSession;
import static helpinghand.resources.EventResource.EVENT_KIND;
import static helpinghand.resources.EventResource.EVENT_CREATOR_PROPERTY;
import static helpinghand.resources.EventResource.EVENT_STATUS_PROPERTY;
import static helpinghand.resources.EventResource.PARTICIPANT_KIND;
import static helpinghand.resources.EventResource.PARTICIPANT_ID_PROPERTY;
import static helpinghand.resources.EventResource.cancelEvent;
import static helpinghand.resources.HelpResource.HELP_KIND;
import static helpinghand.resources.HelpResource.HELP_CREATOR_PROPERTY;
import static helpinghand.resources.HelpResource.HELPER_KIND;
import static helpinghand.resources.HelpResource.HELPER_ID_PROPERTY;
import static helpinghand.resources.HelpResource.HELPER_CURRENT_PROPERTY;
import static helpinghand.resources.HelpResource.cancelHelp;
import static helpinghand.resources.RouteResource.ROUTE_KIND;
import static helpinghand.resources.RouteResource.ROUTE_CREATOR_PROPERTY;
/**
 * @author PogChamp Software
 *
 */
public class AccountUtils {

	protected static final String DATASTORE_EXCEPTION_ERROR = "Error in AccountUtils: %s";
	protected static final String TRANSACTION_ACTIVE_ERROR = "Error in AccountUtils: Transaction was active";
	protected static final String ACCOUNT_NOT_FOUND_ERROR_2 = "Account that owns token (%d) does not exist";
	private static final String PASSWORD_DENIED_ERROR = "[%s] is not the current password for the account [%s]";
	public static final String ACCOUNT_NOT_FOUND_ERROR = "Account [%s] does not exist";
	public static final String ACCOUNT_ID_CONFLICT_ERROR = "Multiple accounts [%s] registered";
	private static final String FEED_NOT_FOUND_ERROR = "User [%s] has no notification feeds";

	private static final String GET_ALL_START ="Attempting to get all [%s] accounts with token (%d)";
	private static final String GET_ALL_OK ="Successfuly got all [%s] accounts with token (%d)";
	private static final String GET_ALL_BAD_DATA_ERROR = "Get all accounts attempt failed due to bad input";
	
	protected static final String CREATE_START = "Attempting to create account with id [%s] and role [%s]";
	protected static final String CREATE_OK = "Successfuly created account [%s] and role [%s]";
	protected static final String CREATE_ID_CONFLICT_ERROR = "Account with id [%s] already exists";
	protected static final String CREATE_EMAIL_CONFLICT_ERROR = "Account with id [%s] already exists";
	protected static final String CREATE_BAD_DATA_ERROR = "Account creation attempt failed due to bad inputs";

	private static final String DELETE_START = "Attempting to delete account [%s] with token (%d)";
	private static final String DELETE_OK = "Successfully deleted account with token (%d)";
	private static final String DELETE_BAD_DATA_ERROR = "Delete attempt failed due to bad inputs";

	private static final String GET_ACCOUNT_START = "Attempting to get account [%s] with token (%d)";
	private static final String GET_ACCOUNT_OK = "Successfuly got account [%s] with token (%d)";
	private static final String GET_ACCOUNT_BAD_DATA_ERROR = "Get account attempt failed due to bad inputs";
	
	private static final String LOGIN_START = "Attempting to login into account [%s]";
	private static final String LOGIN_FAILED = "Login failed for account [%s]";
	private static final String LOGIN_OK = "Login successful for account [%s]";
	private static final String LOGIN_BAD_DATA_ERROR ="Login attempt failed due to bad inputs";

	private static final String LOGOUT_START = "Attempting to logout with token (%d)";
	private static final String LOGOUT_FAILED = "Logout failed for token (%d)";
	private static final String LOGOUT_OK = "Logout successful for token (%d)";
	private static final String LOGOUT_BAD_DATA_ERROR ="Logout attempt failed due to bad inputs";

	private static final String UPDATE_PASSWORD_START = "Attempting to update password with token (%d)";
	private static final String UPDATE_PASSWORD_OK = "Successfuly updated password from [%s] to [%s] with token (%d)";
	private static final String UPDATE_PASSWORD_BAD_DATA_ERROR ="Change password attempt failed due to bad inputs";

	private static final String UPDATE_EMAIL_START ="Attempting to update email with token (%d)";
	private static final String UPDATE_EMAIL_OK = "Successfuly update email to [%s] with token (%d)";
	private static final String UPDATE_EMAIL_BAD_DATA_ERROR = "Change email attempt failed due to bad inputs";
	private static final String UPDATE_EMAIL_CONFLICT_ERROR = "There already exists an account with email [%s]";

	private static final String UPDATE_STATUS_START ="Attempting to update status with token (%d)";
	private static final String UPDATE_STATUS_OK = "Successfuly updated status to [%s] with token (%d)";
	private static final String UPDATE_STATUS_BAD_DATA_ERROR = "Change status attempt failed due to bad inputs";

	private static final String UPDATE_VISIBILITY_START ="Attempting to update visibility with token (%d)";
	private static final String UPDATE_VISIBILITY_OK = "Successfuly updated visibility to [%s] with token (%d)";
	private static final String UPDATE_VISIBILITY_BAD_DATA_ERROR = "Change visibility attempt failed due to bad inputs";

	private static final String UPDATE_ACCOUNT_INFO_START ="Attempting to update account info with token (%d)";
	private static final String UPDATE_ACCOUNT_INFO_OK = "Successfuly updated account info of account [%s] with token (%d)";
	private static final String UPDATE_ACCOUNT_INFO_BAD_DATA_ERROR = "Change account info attempt failed due to bad inputs";
	private static final String ACCOUNT_INFO_NOT_FOUND_ERROR = "There is no account info for the account [%s]";

	private static final String GET_ACCOUNT_INFO_START = "Attempting to get info of account [%s] with token (%d)";
	private static final String GET_ACCOUNT_INFO_OK = "Successfuly got account info of account [%s] with token (%d)";
	private static final String GET_ACCOUNT_INFO_BAD_DATA_ERROR = "Get account info attempt failed due to bad inputs";

	protected static final String UPDATE_PROFILE_START ="Attempting to update profile with token (%d)";
	protected static final String UPDATE_PROFILE_OK = "Successfuly updated profile of account [%s] with token (%d)";
	protected static final String UPDATE_PROFILE_BAD_DATA_ERROR = "Change profile attempt failed due to bad inputs";
	protected static final String PROFILE_NOT_FOUND_ERROR = "There is no account profile for the account [%s]";

	protected static final String GET_PROFILE_START = "Attempting to get profile of account [%s] with token (%d)";
	protected static final String GET_PROFILE_OK = "Successfuly got profile of account [%s] with token (%d)";
	protected static final String GET_PROFILE_BAD_DATA_ERROR = "Get profile attempt failed due to bad inputs";

	private static final String GET_EVENTS_START  = "Attempting to get all events of account [%s] with token (%d)";
	private static final String GET_EVENTS_OK = "Successfuly got all events of account [%s] with token (%d)";
	private static final String GET_EVENTS_BAD_DATA_ERROR  = "Get all events of account attempt failed due to bad inputs";

	private static final String GET_HELP_START  = "Attempting to get all events of account [%s] with token (%d)";
	private static final String GET_HELP_OK = "Successfuly got all events of account [%s] with token (%d)";
	private static final String GET_HELP_BAD_DATA_ERROR  = "Get all events of account attempt failed due to bad inputs";

	private static final String GET_ROUTES_START  = "Attempting to get all routes of account [%s] with token (%d)";
	private static final String GET_ROUTES_OK = "Successfuly got all routes of account [%s] with token (%d)";
	private static final String GET_ROUTES_BAD_DATA_ERROR  = "Get all routes of account attempt failed due to bad inputs";
	
	private static final String GET_FEED_START ="Attempting to get notification with token (%d)";
	private static final String GET_FEED_OK ="Successfuly got notification list of [%s] with token (%d)";
	private static final String GET_FEED_BAD_DATA_ERROR = "Get notification feed failed due to bad input";

	private static final String UPDATE_FEED_START ="Attempting to update notification list with token (%d)";
	private static final String UPDATE_FEED_OK ="Successfuly updated notification feed  of [%s] with token (%d)";
	private static final String UPDATE_FEED_BAD_DATA_ERROR = "Update notification feed failed due to bad input";

	private static final String ADD_NOTIFICATION_FEED_START ="Attempting to add notification to (%d)'s feed";
	private static final String ADD_NOTIFICATION_FEED_OK ="Successfuly added notification to (%d)'s feed";
	private static final String ADD_NOTIFICATION_FEED_BAD_DATA_ERROR = "Add notification to feed failed due to bad input";

	public static final String ACCOUNT_KIND = "Account";
	public static final String ACCOUNT_ID_PROPERTY = "id";
	public static final String ACCOUNT_EMAIL_PROPERTY = "email";
	public static final String ACCOUNT_PASSWORD_PROPERTY = "password";
	public static final String ACCOUNT_ROLE_PROPERTY = "role";
	public static final String ACCOUNT_CREATION_PROPERTY = "created";
	public static final String ACCOUNT_STATUS_PROPERTY = "status";
	public static final String ACCOUNT_VISIBILITY_PROPERTY = "visibility";

	public static final String ACCOUNT_INFO_KIND ="AccountInfo";
	public static final String ACCOUNT_INFO_PHONE_PROPERTY = "phone";
	public static final String ACCOUNT_INFO_ADDRESS_1_PROPERTY = "address1";
	public static final String ACCOUNT_INFO_ADDRESS_2_PROPERTY = "address2";
	public static final String ACCOUNT_INFO_ZIPCODE_PROPERTY = "zipcode";
	public static final String ACCOUNT_INFO_CITY_PROPERTY = "city";

	public static final String USER_PROFILE_KIND = "UserProfile";
	public static final String INSTITUTION_PROFILE_KIND = "InstitutionProfile";
	public static final String PROFILE_NAME_PROPERTY = "name";
	public static final String PROFILE_BIO_PROPERTY = "bio";
	public static final String INSTITUTION_PROFILE_INITIALS_PROPERTY = "initials";
	public static final String INSTITUTION_PROFILE_CATEGORIES_PROPERTY = "categories";

	public static final String INSTITUTION_MEMBER_KIND = "InstMember";
	public static final String INSTITUTION_MEMBER_ID_PROPERTY = "id";

	public static final String FOLLOWER_KIND ="Follower";
	public static final String FOLLOWER_ID_PROPERTY = "id";

	public static final String ACCOUNT_FEED_KIND = "AccountFeed";
	public static final String ACCOUNT_FEED_NOTIFICATIONS_PROPERTY = "notifications";
	
	protected static final String VISIBILITY_PARAM = "visibility";
	protected static final String STATUS_PARAM = "status";
	protected static final String EMAIL_PARAM = "email";
	
	protected static final boolean ACCOUNT_STATUS_DEFAULT_USER = true;
	protected static final boolean ACCOUNT_STATUS_DEFAULT_INSTITUTION = true;//false in final release
	protected static final boolean ACCOUNT_VISIBLE_DEFAULT = true;
	protected static final String DEFAULT_PROPERTY_VALUE_STRING = "";
	protected static final ListValue DEFAULT_PROPERTY_VALUE_STRINGLIST = ListValue.newBuilder().build();

	protected static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final Logger log = Logger.getLogger(AccountUtils.class.getName());
	private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind(TOKEN_KIND);
	private static final KeyFactory accountKeyFactory = datastore.newKeyFactory().setKind(ACCOUNT_KIND);
	protected static final Gson g = new Gson();
	
	public AccountUtils() {}

	/**
	 * 
	 * @param token
	 * @param role
	 * @return
	 */
	protected Response listAll(String token, Role role) {
		if(badString(token)) {
			log.info(GET_ALL_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(GET_ALL_START,role.name(), tokenId));

		Query<Entity> accountQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ROLE_PROPERTY, role.name())).build();

		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			QueryResults<Entity> accountList= txn.run(accountQuery);	
			txn.commit();
			List<Account> data = new LinkedList<>();

			accountList.forEachRemaining(account->data.add(new Account(account,false)));

			log.info(String.format(GET_ALL_OK,role.name(),tokenId));
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
	 * Deletes an existing account either user or institution.
	 * @param id - The identification of the user/institution to be deleted.
	 * @param token - The token of the account performing this operation.
	 * @param role - The role of the account.
	 * @return 200, if the account was successfully deleted.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	protected Response deleteAccount(String id,String token,Role role) {
		if(badString(id)||badString(token)) {
			log.warning(DELETE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(DELETE_START,id,tokenId));
		
		Query<Key> tokenQuery = Query.newKeyQueryBuilder().setKind(TOKEN_KIND).setFilter(PropertyFilter.eq(TOKEN_OWNER_PROPERTY, id)).build();
		Query<Key> accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Query<ProjectionEntity> eventQuery = Query.newProjectionEntityQueryBuilder().setKind(EVENT_KIND).setProjection(EVENT_STATUS_PROPERTY).setFilter(PropertyFilter.eq(EVENT_CREATOR_PROPERTY, id)).build();
		
		Query<Key> helpQuery = Query.newKeyQueryBuilder().setKind(HELP_KIND).setFilter(PropertyFilter.eq(HELP_CREATOR_PROPERTY, id)).build();
		
		Transaction txn = datastore.newTransaction();

		try {
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
			
			Query<Key> childrenQuery = Query.newKeyQueryBuilder().setFilter(PropertyFilter.hasAncestor(accountKey)).build();
	
			Entity tokenEntity = txn.get(tokenKey);
	
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
				return Response.status(Status.NOT_FOUND).build();
			}
	
			if(!tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
				Role tokenRole  = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;//minimum access level required do execute this operation
				if(tokenRole.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,tokenRole.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}
	
			List<Key> toDelete = new LinkedList<>();
			toDelete.add(accountKey);
	
			QueryResults<Key> childrenList = txn.run(childrenQuery);
			childrenList.forEachRemaining(child->toDelete.add(child));
			
			QueryResults<ProjectionEntity> eventList = txn.run(eventQuery);
			eventList.forEachRemaining(event->{
				if(event.getBoolean(EVENT_STATUS_PROPERTY))
					cancelEvent(event.getKey().getId());
			});
			
			QueryResults<Key> helpList = txn.run(helpQuery);
			helpList.forEachRemaining(help->cancelHelp(help.getId()));
			
			long keyId = accountKey.getId();
			
			Query<Key> participantQuery = Query.newKeyQueryBuilder().setKind(PARTICIPANT_KIND).setFilter(PropertyFilter.eq(PARTICIPANT_ID_PROPERTY, keyId)).build();
			Query<Key> memberQuery = Query.newKeyQueryBuilder().setKind(INSTITUTION_MEMBER_KIND).setFilter(PropertyFilter.eq(INSTITUTION_MEMBER_ID_PROPERTY, keyId)).build();
			Query<Key> followQuery = Query.newKeyQueryBuilder().setKind(FOLLOWER_KIND).setFilter(PropertyFilter.eq(FOLLOWER_ID_PROPERTY, keyId)).build();
			Query<ProjectionEntity> helperQuery = Query.newProjectionEntityQueryBuilder().setKind(HELPER_KIND).setProjection(HELPER_CURRENT_PROPERTY).setFilter(PropertyFilter.eq(HELPER_ID_PROPERTY, keyId)).build();
			
			if(!role.equals(Role.INSTITUTION)) {
				QueryResults<Key> memberList = txn.run(memberQuery);
				QueryResults<Key> participantList = txn.run(participantQuery);
				QueryResults<Key> followList = txn.run(followQuery);
				QueryResults<ProjectionEntity> helperList = txn.run(helperQuery);
				
				memberList.forEachRemaining(membership->toDelete.add(membership));
				participantList.forEachRemaining(participation->toDelete.add(participation));
				followList.forEachRemaining(follow->toDelete.add(follow));
				
				helperList.forEachRemaining(helper->{
					toDelete.add(helper.getKey());
					if(helper.getBoolean(HELPER_CURRENT_PROPERTY)) {
						//TODO:notify help creator
					}
				});
			}
			
			//end all account sessions(delete tokens)
			QueryResults<Key> tokenList = txn.run(tokenQuery);
			tokenList.forEachRemaining(key->toDelete.add(key));

			//convert list to Key array
			Key[] keys = new Key[toDelete.size()];
			toDelete.toArray(keys);
		
			txn.delete(keys);
			txn.commit();
			log.info(String.format(DELETE_OK,tokenId));
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
	 * Returns account data.
	 * @param id - id of the account.
	 * @param token - token performing request.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	protected Response getAccount(String id, String token) {
		if(badString(id) || badString(token)) {
			log.warning(GET_ACCOUNT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(GET_ACCOUNT_START,id,tokenId));

		Query<Entity> accountQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			Entity tokenEntity = txn.get(tokenKey);
			
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			QueryResults<Entity> accountList = txn.run(accountQuery);
			txn.commit();
			
			if(!accountList.hasNext()) {
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Entity account = accountList.next();
			
			if(accountList.hasNext()) {
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			if(!tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
				Role role  = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;//minimum access level required do execute this operation
				if(role.getAccess() < minAccess) {
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}

			Account info = new Account(account,true);
			
			log.info(String.format(GET_ACCOUNT_OK,id,tokenId));
			return Response.ok(g.toJson(info)).build();
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
	 * It performs a login on the user/institution account.
	 * @param data - The requested data to perform login.
	 * @return 200, if the login was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the login failed.
	 */
	protected Response login(Login data) {
		if(data.badData()) {
			log.warning(LOGIN_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		log.info(String.format(LOGIN_START,data.id));

		LoginInfo info = startSession(data.id,data.password);

		if(info == null) {
			log.warning(String.format(LOGIN_FAILED, data.id));
			return Response.status(Status.FORBIDDEN).build();
		}

		log.info(String.format(LOGIN_OK, data.id));
		return Response.ok(g.toJson(info)).build();
	}

	/**
	 * It performs a logout on the user/institution account.
	 * @param token - The token of the user/institution requesting the logout.
	 * @return 200, if the logout was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the logout failed.
	 */
	protected Response logout(String token) {
		if(badString(token)) {
			log.warning(LOGOUT_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(LOGOUT_START,tokenId));

		if(!endSession(tokenId)) {
			log.warning(String.format(LOGOUT_FAILED, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}

		log.info(String.format(LOGOUT_OK, tokenId));
		return Response.ok().build();
	}

	/**
	 * Updates the password of the user/institution account.
	 * @param id - The identification of the user/institution.
	 * @param data - The updated password data for the user/institution account.
	 * @param token - The token that is used to perform the operation
	 * @return 200, if the password was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the password is not the current password for the account or the token cannot execute the operation
	 * 		   with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	protected Response updatePassword(String id,ChangePassword data, String token) {
		if(data.badData()|| badString(id)||badString(token)) {
			log.warning(UPDATE_PASSWORD_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(UPDATE_PASSWORD_START,tokenId));
		
		Query<Entity> accountQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction();

		try {
			QueryResults<Entity> accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Entity account = accountList.next();
			
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
	
			if(badPassword(account,data.oldPassword)) {
				txn.rollback();
				log.severe(String.format(PASSWORD_DENIED_ERROR,data.oldPassword,id));
				return Response.status(Status.FORBIDDEN).build(); 
			}
			
			Entity updatedAccount = Entity.newBuilder(account)
					.set(ACCOUNT_PASSWORD_PROPERTY,DigestUtils.sha512Hex(data.newPassword))
					.build();
			
			txn.update(updatedAccount);
			txn.commit();
			log.info(String.format(UPDATE_PASSWORD_OK,data.oldPassword,data.newPassword,tokenId));
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
	 * Updates the email of the user/institution account.
	 * @param id - The identification of the user/identification.
	 * @param data - The updated email data for user/institution.
	 * @param token - The token of the account requesting this operation.
	 * @return 200, if the email was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the password is not the current for the account or the token cannot execute the operation
	 * 		   with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   409, if there is already an account with the email.
	 * 		   500, otherwise.
	 */
	protected Response updateEmail(String id, String email, String token) {
		if(!email.matches(EMAIL_REGEX) || badString(id) ||badString(token)) {
			log.warning(UPDATE_EMAIL_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(UPDATE_EMAIL_START,tokenId));

		Query<Entity> accountQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction();

		try {
			QueryResults<Entity> accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Entity account = accountList.next();
			
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
			
			Query<Key> checkQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_EMAIL_PROPERTY, email)).build();
			
			if(txn.run(checkQuery).hasNext()) {
				txn.rollback();
				log.warning(String.format(UPDATE_EMAIL_CONFLICT_ERROR,email));
				return Response.status(Status.CONFLICT).build(); 
			}

			Entity updatedAccount = Entity.newBuilder(account)
					.set(ACCOUNT_EMAIL_PROPERTY,email)
					.build();

			txn.update(updatedAccount);
			txn.commit();
			log.info(String.format(UPDATE_EMAIL_OK,email,tokenId));
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
	 * Updates the status of the user/institution account.
	 * @param id - The identification of the user/institution.
	 * @param data - The updated status data for user/institution.
	 * @param token - The token of the user/institution requesting this operation.
	 * @return 200, if the status was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the password is not the current for the account or the token cannot execute the operation
	 * 		   with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	protected Response updateStatus(String id, String status, String token) {
		if(badString(status)||badString(id) || badString(token)) {
			log.warning(UPDATE_STATUS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);
		
		boolean statusBoolean = Boolean.getBoolean(status);

		log.info(String.format(UPDATE_STATUS_START,tokenId));

		Query<Entity> accountQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction();

		try {
			QueryResults<Entity> accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Entity account = accountList.next();
			
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

			Entity updatedAccount = Entity.newBuilder(account)
					.set(ACCOUNT_STATUS_PROPERTY,statusBoolean)
					.build();

			txn.update(updatedAccount);
			txn.commit();
			log.info(String.format(UPDATE_STATUS_OK,statusBoolean,tokenId));
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
	 * Updates the visibility of the user/institution account.
	 * @param id - The user/institution identification.
	 * @param data - The updated visibility data for user/institution.
	 * @param token - The token requesting this operation.
	 * @return 200, if the visibility was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the password is not the current for the account or the token cannot execute the operation
	 * 		   with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	protected Response updateVisibility(String id, String visibility, String token) {
		if(badString(visibility)||badString(id) || badString(token)) {
			log.warning(UPDATE_VISIBILITY_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);
		
		boolean visibilityBoolean = Boolean.getBoolean(visibility);

		log.info(String.format(UPDATE_VISIBILITY_START,tokenId));

		Query<Entity> accountQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction();

		try {
			QueryResults<Entity> accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Entity account = accountList.next();
			
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

			Entity updatedAccount = Entity.newBuilder(account)
					.set(ACCOUNT_VISIBILITY_PROPERTY,visibilityBoolean)
					.build();

			txn.update(updatedAccount);
			txn.commit();
			log.info(String.format(UPDATE_VISIBILITY_OK,visibilityBoolean,tokenId));
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
	 * Obtains the account info of the user/institution.
	 * @param id - The identification of the user/institution.
	 * @param token - The token requesting this operation.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise
	 */
	protected Response getAccountInfo(String id,String token) {
		if(badString(id) || badString(token)) {
			log.warning(GET_ACCOUNT_INFO_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(GET_ACCOUNT_INFO_START,id,tokenId));

		Query<ProjectionEntity> accountQuery = Query.newProjectionEntityQueryBuilder().setProjection(ACCOUNT_VISIBILITY_PROPERTY)
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
	
			if(!account.getBoolean(ACCOUNT_VISIBILITY_PROPERTY) && !tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
				Role role  = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;//minimum access level required do execute this operation
				if(role.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}
			
			Key infoKey = datastore.newKeyFactory().setKind(ACCOUNT_INFO_KIND).addAncestor(PathElement.of(ACCOUNT_KIND, account.getKey().getId())).newKey(account.getKey().getId());
			Entity accountInfo = txn.get(infoKey);		
			txn.commit();

			if(accountInfo == null) {
				log.severe(String.format(ACCOUNT_INFO_NOT_FOUND_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			AccountInfo info = new AccountInfo(
					accountInfo.getString(ACCOUNT_INFO_PHONE_PROPERTY),
					accountInfo.getString(ACCOUNT_INFO_ADDRESS_1_PROPERTY),
					accountInfo.getString(ACCOUNT_INFO_ADDRESS_2_PROPERTY),
					accountInfo.getString(ACCOUNT_INFO_ZIPCODE_PROPERTY),
					accountInfo.getString(ACCOUNT_INFO_CITY_PROPERTY)
					);
			
			log.info(String.format(GET_ACCOUNT_INFO_OK,id,tokenId));
			return Response.ok(g.toJson(info)).build();
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
	 * Updates the user/institution account info.
	 * @param id - The identification of the user/institution.
	 * @param data - The updated account info data for user/institution.
	 * @param token - The token requesting this operation.
	 * @return 200, if the account info was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	protected Response updateAccountInfo(String id, AccountInfo data, String token) {
		if(data.badData() ||badString(id)|| badString(token)) {
			log.warning(UPDATE_ACCOUNT_INFO_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(UPDATE_ACCOUNT_INFO_START,tokenId));

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
			
			Key infoKey = datastore.newKeyFactory().setKind(ACCOUNT_INFO_KIND).addAncestor(PathElement.of(ACCOUNT_KIND, accountKey.getId())).newKey(accountKey.getId());
			Entity accountInfo = txn.get(infoKey);	

			if(accountInfo == null) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_INFO_NOT_FOUND_ERROR,id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
	
			Entity updatedAccountInfo = Entity.newBuilder(accountInfo)
					.set(ACCOUNT_INFO_PHONE_PROPERTY, data.phone)
					.set(ACCOUNT_INFO_ADDRESS_1_PROPERTY,data.address1)
					.set(ACCOUNT_INFO_ADDRESS_2_PROPERTY,data.address2)
					.set(ACCOUNT_INFO_ZIPCODE_PROPERTY,data.zipcode)
					.set(ACCOUNT_INFO_CITY_PROPERTY,data.city)
					.build();

			txn.update(updatedAccountInfo);
			txn.commit();
			log.info(String.format(UPDATE_ACCOUNT_INFO_OK,id,tokenId));
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
	 * Obtains the list of the events created by the user/institution.
	 * @param id - The identification of the user/institution.
	 * @param token - The token of the user/institution.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 */
	protected Response getAccountEvents(String id,String token) {
		if(badString(id)|| badString(token)) {
			log.info(GET_EVENTS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(GET_EVENTS_START,id,tokenId));
		
		Query<ProjectionEntity> accountQuery = Query.newProjectionEntityQueryBuilder().setProjection(ACCOUNT_VISIBILITY_PROPERTY).setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		Query<Entity> eventQuery = Query.newEntityQueryBuilder().setKind(EVENT_KIND).setFilter(PropertyFilter.eq(EVENT_CREATOR_PROPERTY,id)).build();
		
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
	
			if(!account.getBoolean(ACCOUNT_VISIBILITY_PROPERTY) && !tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
				Role role  = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;//minimum access level required do execute this operation
				if(role.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}
			
			QueryResults<Entity> eventList = txn.run(eventQuery);
			txn.commit();
			
			List<EventData> events = new LinkedList<>();
			eventList.forEachRemaining(event->events.add(new EventData(event)));
	
			log.info(String.format(GET_EVENTS_OK,id,tokenId));
			return Response.ok(g.toJson(events)).build();
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
	 * Obtains the list of help requests created by the user/institution.
	 * @param id - The identification of the user/institution.
	 * @param token - The token of the user/institution.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the user/institution account does not exist or the token does not exist.
	 */
	protected Response getAccountHelpRequests(String id,String token) {
		if(badString(id)|| badString(token)) {
			log.info(GET_HELP_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(GET_HELP_START,id,tokenId));

		Query<ProjectionEntity> accountQuery = Query.newProjectionEntityQueryBuilder().setProjection(ACCOUNT_VISIBILITY_PROPERTY).setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		Query<Entity> helpQuery = Query.newEntityQueryBuilder().setKind(HELP_KIND).setFilter(PropertyFilter.eq(HELP_CREATOR_PROPERTY,id)).build();
		
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
	
			if(!account.getBoolean(ACCOUNT_VISIBILITY_PROPERTY) && !tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
				Role role  = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;//minimum access level required do execute this operation
				if(role.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}
			
			QueryResults<Entity> helpList = txn.run(helpQuery);
			txn.commit();
			
			List<HelpData> helps = new LinkedList<>();
			helpList.forEachRemaining(help->helps.add(new HelpData(help)));
	
			log.info(String.format(GET_HELP_OK,id,tokenId));
			return Response.ok(g.toJson(helps)).build();
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
	 * 
	 * @param id
	 * @param token
	 * @return
	 */
	protected Response getAccountRoutes(String id,String token) {
		if(badString(id)|| badString(token)) {
			log.info(GET_ROUTES_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}

		long tokenId = Long.parseLong(token);

		log.info(String.format(GET_ROUTES_START,id,tokenId));

		Query<ProjectionEntity> accountQuery = Query.newProjectionEntityQueryBuilder().setProjection(ACCOUNT_VISIBILITY_PROPERTY).setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		Query<Entity> routeQuery = Query.newEntityQueryBuilder().setKind(ROUTE_KIND).setFilter(PropertyFilter.eq(ROUTE_CREATOR_PROPERTY,id)).build();
		
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
	
			if(!account.getBoolean(ACCOUNT_VISIBILITY_PROPERTY) && !tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
				Role role  = Role.getRole(tokenEntity.getString(TOKEN_ROLE_PROPERTY));
				int minAccess = 1;//minimum access level required do execute this operation
				if(role.getAccess() < minAccess) {
					txn.rollback();
					log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
					return Response.status(Status.FORBIDDEN).build();
				}
			}
			
			QueryResults<Entity> routeList = txn.run(routeQuery);
			txn.commit();
			
			List<Route> helps = new LinkedList<>();
			routeList.forEachRemaining(route->helps.add(new Route(route)));
	
			log.info(String.format(GET_ROUTES_OK,id,tokenId));
			return Response.ok(g.toJson(helps)).build();
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
	 * Obtains the feed of the user/institution account.
	 * @param id - The identification of the user/institution.
	 * @param token - The token of the user/institution.
	 * @return 200, if the operation was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not belong to the user/institution account.
	 * 		   404, if the user/institution account does not exist or the token does not exist. 
	 */
	protected Response getFeed(String id, String token) {
		if(badString(token) || badString(id)) {
			log.info(GET_FEED_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		log.info(String.format(GET_FEED_START, tokenId));

		Query<Key> accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
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
			
			Entity tokenEntity = txn.get(tokenKey);
	
			if(tokenEntity == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
				return Response.status(Status.NOT_FOUND).build();
			}
		
			if(!tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
				log.warning(String.format(TOKEN_OWNER_ERROR, tokenId,id));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			Key feedKey = datastore.newKeyFactory().setKind(ACCOUNT_FEED_KIND).addAncestor(PathElement.of(ACCOUNT_KIND, id)).newKey(accountKey.getId());
			Entity feed = txn.get(feedKey);
			txn.commit();
			
			if(feed == null) {
				log.severe(String.format(FEED_NOT_FOUND_ERROR, id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			} 
	
			List<Value<String>> notifications = feed.getList(ACCOUNT_FEED_NOTIFICATIONS_PROPERTY);
			List<String> notificationList = notifications.stream().map(notification->notification.get()).collect(Collectors.toList());
	
			log.info(String.format(GET_FEED_OK,id,tokenId));
			return Response.ok(g.toJson(notificationList)).build();
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
	 * Updates the feed of the user/institution account.
	 * @param token - The token of the user/institution.
	 * @param id - The identification of the user/institution account.
	 * @param data - The updated feed data.
	 * @return 200, if the feed was successfully updated.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token does not belong to the user/institution account.
	 * 		   404, if the user/institution account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	protected Response updateFeed(String id, String token, AccountFeed data) {
		if(badString(token) || badString(id) || data.badData()) {
			log.info(UPDATE_FEED_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		long tokenId = Long.parseLong(token);
		
		log.info(String.format(UPDATE_FEED_START, tokenId));

		Query<Key> accountQuery = Query.newKeyQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);

		ListValue.Builder feedBuilder = ListValue.newBuilder();

		for(String notification:data.feed)
			feedBuilder.addValue(StringValue.newBuilder(notification).setExcludeFromIndexes(true).build());

		ListValue completeFeed = feedBuilder.build(); 
		
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
				txn.rollback();
				log.warning(String.format(TOKEN_OWNER_ERROR, tokenId,id));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			Key feedKey = datastore.newKeyFactory().setKind(ACCOUNT_FEED_KIND).addAncestor(PathElement.of(ACCOUNT_KIND, id)).newKey(accountKey.getId());
			
			Entity feed = txn.get(feedKey);
			
			if(feed == null) {
				txn.rollback();
				log.severe(String.format(FEED_NOT_FOUND_ERROR, id));
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			} 
	
			Entity updatedFeed = Entity.newBuilder(feed)
					.set(ACCOUNT_FEED_NOTIFICATIONS_PROPERTY, completeFeed)
					.build();
		
			txn.update(updatedFeed);
			txn.commit();
			log.info(String.format(UPDATE_FEED_OK,id,tokenId));
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
	 * Adds a notification to feed of the user/institution.
	 * @param user - The identification of the user/institution.
	 * @param message - The message to be added to the feed.
	 * @return true, if the message was successfully added to the feed.
	 * 		   false, otherwise.
	 */
	public static boolean addNotificationToFeed(long datastoreId,String message) {
		if(badString(message)) {
			log.warning(ADD_NOTIFICATION_FEED_BAD_DATA_ERROR);
			return false;
		}

		log.info(String.format(ADD_NOTIFICATION_FEED_START,datastoreId));
		
		Key accountKey = accountKeyFactory.newKey(datastoreId);
		Key feedKey = datastore.newKeyFactory().setKind(ACCOUNT_FEED_KIND).addAncestor(PathElement.of(ACCOUNT_KIND,datastoreId)).newKey(datastoreId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity account = txn.get(accountKey);
			
			if(account == null) {
				txn.rollback();
				log.warning(String.format(ACCOUNT_NOT_FOUND_ERROR,Long.toString(datastoreId)));
				return false;
			}

			Entity feed = txn.get(feedKey);
			
			if(feed == null) {
				txn.rollback();
				log.warning(String.format(FEED_NOT_FOUND_ERROR,account.getString(ACCOUNT_ID_PROPERTY)));
				return false;
			}

			List<Value<String>> notifications = feed.getList(ACCOUNT_FEED_NOTIFICATIONS_PROPERTY);
	
			ListValue.Builder feedBuilder = ListValue.newBuilder();
	
			notifications.forEach(notification->{
				feedBuilder.addValue(StringValue.newBuilder(notification.get()).setExcludeFromIndexes(true).build());
			});
			
			feedBuilder.addValue(StringValue.newBuilder(message).setExcludeFromIndexes(true).build());
			
			ListValue completeFeed = feedBuilder.build();
	
			Entity updatedFeed = Entity.newBuilder(feed)
					.set(ACCOUNT_FEED_NOTIFICATIONS_PROPERTY, completeFeed)
					.build();

			txn.update(updatedFeed);
			txn.commit();
			log.info(String.format(ADD_NOTIFICATION_FEED_OK,datastoreId));
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
