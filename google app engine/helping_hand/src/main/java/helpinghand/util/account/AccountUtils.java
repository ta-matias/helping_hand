package helpinghand.util.account;

import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Value;
import com.google.gson.Gson;

import helpinghand.accesscontrol.AccessControlManager;
import helpinghand.accesscontrol.LoginInfo;
import helpinghand.accesscontrol.Role;
import helpinghand.util.QueryUtils;

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
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_KIND;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_OWNER_PROPERTY;
import static helpinghand.resources.EventResource.EVENT_KIND;
import static helpinghand.resources.EventResource.EVENT_STATUS_PROPERTY;
import static helpinghand.resources.EventResource.PARTICIPANT_KIND;
import static helpinghand.resources.EventResource.PARTICIPANT_ID_PROPERTY;
import static helpinghand.resources.HelpResource.HELP_KIND;
import static helpinghand.resources.HelpResource.HELPER_KIND;
import static helpinghand.resources.HelpResource.HELPER_ID_PROPERTY;
import static helpinghand.resources.HelpResource.HELPER_CURRENT_PROPERTY;



public class AccountUtils {
	
	protected static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final Logger log = Logger.getLogger(AccountUtils.class.getName());
	protected static final Gson g = new Gson();
	
	protected static final String DATASTORE_EXCEPTION_ERROR = "Error in AccountUtils: %s";
	protected static final String TRANSACTION_ACTIVE_ERROR = "Error in AccountUtils: Transaction was active";
	protected static final String ACCOUNT_NOT_FOUND_ERROR_2 = "Account that owns token (%d) does not exist";
	private static final String PASSWORD_DENIED_ERROR = "[%s] is not the current password for the account [%s]";
	public static final String ACCOUNT_NOT_FOUND_ERROR = "Account [%s] does not exist";
	private static final String MULTIPLE_FEED_ERROR = "User [%s] has multiple notification feeds";
	private static final String FEED_NOT_FOUND_ERROR = "User [%s] has no notification feeds";
	
	protected static final String CREATE_START = "Attempting to create account with id [%s] and role [%s]";
	protected static final String CREATE_OK = "Successfuly created account [%s] with role [%s]";
	protected static final String CREATE_ID_CONFLICT_ERROR = "Account with id [%s] already exists";
	protected static final String CREATE_EMAIL_CONFLICT_ERROR = "Account with id [%s] already exists";
	protected static final String CREATE_BAD_DATA_ERROR = "Account creation attempt failed due to bad inputs";
	
	private static final String DELETE_START = "Attempting to delete account [%s] with token (%d)";
	private static final String DELETE_OK = "Successfully deleted account with token (%d)";
	private static final String DELETE_BAD_DATA_ERROR = "Delete attempt failed due to bad inputs";
	
	private static final String LOGIN_START = "Attempting to login into account [%s]";
	private static final String LOGIN_FAILED = "Login failed for account [%s]";
	private static final String LOGIN_OK = "Login successful for account [%s]";
	private static final String LOGIN_BAD_DATA_ERROR ="Login attempt failed due to bad inputs";
	
	private static final String LOGOUT_START = "Attempting to logout with token (%d)";
	private static final String LOGOUT_FAILED = "Logout failed for token (%d)";
	private static final String LOGOUT_OK = "Logout successful for token (%d)";
	private static final String LOGOUT_BAD_DATA_ERROR ="LOgout attempt failed due to bad inputs";

	private static final String UPDATE_ID_START = "Attempting to update id with token (%d)";
	private static final String UPDATE_ID_OK = "Successfuly updated id from [%s] to [%s] with token (%d)";
	private static final String UPDATE_ID_BAD_DATA_ERROR ="Change id attempt failed due to bad inputs";
	private static final String UPDATE_ID_CONFLICT_ERROR = "There already exists an account with id [%s]";
	
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
	private static final String MULTIPLE_ACCOUNT_INFO_ERROR = "There are multiple account infos for the account [%s]";
	private static final String ACCOUNT_INFO_NOT_FOUND_ERROR = "There is no account info for the account [%s]";
	
	private static final String GET_ACCOUNT_INFO_START = "Attempting to get info of account [%s] with token (%d)";
	private static final String GET_ACCOUNT_INFO_OK = "Successfuly got account info of account [%s] with token (%d)";
	private static final String GET_ACCOUNT_INFO_BAD_DATA_ERROR = "Get account info attempt failed due to bad inputs";
	
	protected static final String UPDATE_PROFILE_START ="Attempting to update profile with token (%d)";
	protected static final String UPDATE_PROFILE_OK = "Successfuly updated prfile of account [%s] with token (%d)";
	protected static final String UPDATE_PROFILE_BAD_DATA_ERROR = "Change profile attempt failed due to bad inputs";
	protected static final String MULTIPLE_PROFILE_ERROR = "There are multiple account profiles for the account [%s]";
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
	
	private static final String GET_FEED_START ="Attempting to get notification with token (%d)";
	private static final String GET_FEED_OK ="Successfuly got notification list of [%s] with token (%d)";
	private static final String GET_FEED_BAD_DATA_ERROR = "Get notification feed failed due to bad input";
	
	private static final String UPDATE_FEED_START ="Attempting to update notification list with token (%d)";
	private static final String UPDATE_FEED_OK ="Successfuly updated notification feed  of [%s] with token (%d)";
	private static final String UPDATE_FEED_BAD_DATA_ERROR = "Update notification feed failed due to bad input";

	private static final String ADD_NOTIFICATION_FEED_START ="Attempting to add notification to [%s]'s feed";
	private static final String ADD_NOTIFICATION_FEED_OK ="Successfuly added notification to [%s]'s feed";
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
	
	
	protected static final boolean ACCOUNT_STATUS_DEFAULT_USER = true;
	protected static final boolean ACCOUNT_STATUS_DEFAULT_INSTITUTION = true;//false in final release
	protected static final boolean ACCOUNT_VISIBLE_DEFAULT = true;
	protected static final String DEFAULT_PROPERTY_VALUE_STRING = "";
	protected static final ListValue DEFAULT_PROPERTY_VALUE_STRINGLIST = ListValue.newBuilder().build();
	
	
	public AccountUtils() {}
	
	/**
	 * Deletes an existing account either user or institution.
	 * @param id - The identification of the user/institution to be deleted.
	 * @param token - The token of the account performing this operation
	 * @return 200, if the account was successfully deleted.
	 * 		   400, if the data is invalid.
	 * 		   403, if the token cannot execute the operation with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   500, otherwise.
	 */
	protected Response deleteAccount(String id,String token) {
		
		if(badString(id)||badString(token)) {
			log.warning(DELETE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		long tokenId = Long.parseLong(token);
		log.info(String.format(DELETE_START,id,tokenId));
		
		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND, tokenId);
		if(tokenEntity == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		List<Key> toDelete = new LinkedList<>();
		toDelete.add(account.getKey());
		QueryUtils.getEntityChildren(account).stream().forEach(c->{
			if(!c.getKey().getKind().equals(EVENT_KIND))toDelete.add(c.getKey());
		});
		
		
		
		if(!Role.getRole(account.getString(ACCOUNT_ROLE_PROPERTY)).equals(Role.INSTITUTION)) {
			
			QueryUtils.getEntityListByProperty(INSTITUTION_MEMBER_KIND, INSTITUTION_MEMBER_ID_PROPERTY, id).stream().forEach(p->toDelete.add(p.getKey()));
			QueryUtils.getEntityListByProperty(PARTICIPANT_KIND, PARTICIPANT_ID_PROPERTY, id).stream().forEach(p->toDelete.add(p.getKey()));
			QueryUtils.getEntityListByProperty(HELPER_KIND, HELPER_ID_PROPERTY, id).stream().forEach(h->{
				toDelete.add(h.getKey());
				if(h.getBoolean(HELPER_CURRENT_PROPERTY)) {
					//TODO:notify help creator
				}
			});
		}
		
		if(!AccessControlManager.endAllSessions(id)) {
			log.warning(String.format(LOGOUT_FAILED, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Key[] keys = new Key[toDelete.size()];
		toDelete.toArray(keys);
		
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.delete(keys);
			txn.commit();
			
			log.info(String.format(DELETE_OK,token));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
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
		
		LoginInfo info = AccessControlManager.startSession(data.id,data.password);
		
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
		
		if(!AccessControlManager.endSession(tokenId)) {
			log.warning(String.format(LOGOUT_FAILED, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		log.info(String.format(LOGOUT_OK, tokenId));
		return Response.ok().build();
	}

	/**
	 * Updates the identification of the user/institution.
	 * @param id - The identification of the user/institution to be updated
	 * @param data - The updated id data for the user/institution.
	 * @param token - The token of the user/institution requesting the change of the id.
	 * @return 200, if the identification change was successful.
	 * 		   400, if the data is invalid.
	 * 		   403, if the password is not the current password for the account or the token cannot execute the operation
	 * 		   with the current access level.
	 * 		   404, if the account does not exist or the token does not exist.
	 * 		   409, if there is already an account with this id.
	 * 		   500, otherwise.
	 */
	protected Response updateId(String id,ChangeId data, String token) {
		if(data.badData()|| badString(id)||badString(token)) {
			log.warning(UPDATE_ID_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		long tokenId = Long.parseLong(token);
		log.info(String.format(UPDATE_ID_START,tokenId));
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND, tokenId);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		
		if(badPassword(account,data.password)) {
			log.severe(String.format(PASSWORD_DENIED_ERROR,data.password,id));
			return Response.status(Status.FORBIDDEN).build(); 
		}
		
		if(QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, data.id) != null) {
			log.warning(String.format(UPDATE_ID_CONFLICT_ERROR,data.id));
		}
		
		Entity updatedAccount = Entity.newBuilder(account)
		.set(ACCOUNT_ID_PROPERTY,data.id)
		.build();
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.update(updatedAccount);
			txn.commit();
			log.info(String.format(UPDATE_ID_OK,id,data.id,tokenId));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
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
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND, tokenId);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		
		if(badPassword(account,data.oldPassword)) {
			log.severe(String.format(PASSWORD_DENIED_ERROR,data.oldPassword,id));
			return Response.status(Status.FORBIDDEN).build(); 
		}
		
		Entity updatedAccount = Entity.newBuilder(account)
		.set(ACCOUNT_PASSWORD_PROPERTY,DigestUtils.sha512Hex(data.newPassword))
		.build();
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.update(updatedAccount);
			txn.commit();
			log.info(String.format(UPDATE_PASSWORD_OK,data.oldPassword,data.newPassword,tokenId));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
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
	protected Response updateEmail(String id, ChangeEmail data, String token) {
		if(data.badData() || badString(id) ||badString(token)) {
			log.warning(UPDATE_EMAIL_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		long tokenId = Long.parseLong(token);
		log.info(String.format(UPDATE_EMAIL_START,tokenId));
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND, tokenId);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		if(badPassword(account,data.password)) {
			log.severe(String.format(PASSWORD_DENIED_ERROR,data.password,id));
			return Response.status(Status.FORBIDDEN).build(); 
		}
		
		if(QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_EMAIL_PROPERTY, data.email) != null) {
			log.warning(String.format(UPDATE_EMAIL_CONFLICT_ERROR,data.email));
		}
		
		Entity updatedAccount = Entity.newBuilder(account)
		.set(ACCOUNT_EMAIL_PROPERTY,data.email)
		.build();
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.update(updatedAccount);
			txn.commit();
			log.info(String.format(UPDATE_EMAIL_OK,data.email,tokenId));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
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
	protected Response updateStatus(String id, ChangeStatus data, String token) {
		if(data.badData()||badString(id) || badString(token)) {
			log.warning(UPDATE_STATUS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		long tokenId = Long.parseLong(token);
		log.info(String.format(UPDATE_STATUS_START,tokenId));
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND, tokenId);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		if(badPassword(account,data.password)) {
			log.severe(String.format(PASSWORD_DENIED_ERROR,data.password,id));
			return Response.status(Status.FORBIDDEN).build(); 
		}
		
		Entity updatedAccount = Entity.newBuilder(account)
		.set(ACCOUNT_STATUS_PROPERTY,data.status)
		.build();
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.update(updatedAccount);
			txn.commit();
			log.info(String.format(UPDATE_STATUS_OK,data.status,tokenId));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
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
	protected Response updateVisibility(String id, ChangeVisibility data, String token) {
		if(data.badData()||badString(id) || badString(token)) {
			log.warning(UPDATE_VISIBILITY_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		long tokenId = Long.parseLong(token);
		log.info(String.format(UPDATE_VISIBILITY_START,tokenId));
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND, tokenId);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		if(badPassword(account,data.password)) {
			log.severe(String.format(PASSWORD_DENIED_ERROR,data.password,id));
			return Response.status(Status.FORBIDDEN).build(); 
		}
		
		Entity updatedAccount = Entity.newBuilder(account)
		.set(ACCOUNT_VISIBILITY_PROPERTY,data.visibility)
		.build();
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.update(updatedAccount);
			txn.commit();
			log.info(String.format(UPDATE_VISIBILITY_OK,data.visibility,tokenId));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
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
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND, tokenId);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		List<Entity> lst = QueryUtils.getEntityChildrenByKind(account,ACCOUNT_INFO_KIND);
		if(lst.size() > 1) {
			log.severe(String.format(MULTIPLE_ACCOUNT_INFO_ERROR,id));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		if(lst.isEmpty()) {
			log.severe(String.format(ACCOUNT_INFO_NOT_FOUND_ERROR,id));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		Entity accountInfo = lst.get(0);
		
		Entity updatedAccountInfo = Entity.newBuilder(accountInfo)
		.set(ACCOUNT_INFO_PHONE_PROPERTY, data.phone)
		.set(ACCOUNT_INFO_ADDRESS_1_PROPERTY,data.address1)
		.set(ACCOUNT_INFO_ADDRESS_2_PROPERTY,data.address2)
		.set(ACCOUNT_INFO_ZIPCODE_PROPERTY,data.zipcode)
		.set(ACCOUNT_INFO_CITY_PROPERTY,data.city)
		.build();
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.update(updatedAccountInfo);
			txn.commit();
			log.info(String.format(UPDATE_ACCOUNT_INFO_OK,id,tokenId));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
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
		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
			return Response.status(Status.NOT_FOUND).build();
		}
		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND, tokenId);
		if(tokenEntity == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		
		List<Entity> lst = QueryUtils.getEntityChildrenByKind(account,ACCOUNT_INFO_KIND);
		if(lst.size() > 1) {
			log.severe(String.format(MULTIPLE_ACCOUNT_INFO_ERROR,id));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		if(lst.isEmpty()) {
			log.severe(String.format(ACCOUNT_INFO_NOT_FOUND_ERROR,id));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		Entity accountInfo = lst.get(0);
		
		AccountInfo info = new AccountInfo(
		accountInfo.getString(ACCOUNT_INFO_PHONE_PROPERTY),
		accountInfo.getString(ACCOUNT_INFO_ADDRESS_1_PROPERTY),
		accountInfo.getString(ACCOUNT_INFO_ADDRESS_2_PROPERTY),
		accountInfo.getString(ACCOUNT_INFO_ZIPCODE_PROPERTY),
		accountInfo.getString(ACCOUNT_INFO_CITY_PROPERTY)
		);
		log.info(String.format(GET_ACCOUNT_INFO_OK,id,tokenId));
		return Response.ok(g.toJson(info)).build();
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
		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
			return Response.status(Status.NOT_FOUND).build();
		}
		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND, tokenId);
		if(tokenEntity == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!account.getBoolean(ACCOUNT_VISIBILITY_PROPERTY)||!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		List<String[]> events = QueryUtils.getEntityChildrenByKind(account, EVENT_KIND).stream()
				.map(event->new String[] {Long.toString(event.getKey().getId()),Boolean.toString(event.getBoolean(EVENT_STATUS_PROPERTY))})
				.collect(Collectors.toList());
		log.info(String.format(GET_EVENTS_OK,id,token));
		return Response.ok(g.toJson(events)).build();
		
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
		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
			return Response.status(Status.NOT_FOUND).build();
		}
		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND, tokenId);
		if(tokenEntity == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!account.getBoolean(ACCOUNT_VISIBILITY_PROPERTY)||!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,tokenId,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		List<String> helps = QueryUtils.getEntityChildrenByKind(account, HELP_KIND).stream()
				.map(help->Long.toString(help.getKey().getId()))
				.collect(Collectors.toList());
		log.info(String.format(GET_HELP_OK,id,token));
		return Response.ok(g.toJson(helps)).build();
		
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
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND,tokenId);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
			log.warning(String.format(TOKEN_OWNER_ERROR, tokenId,id));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		List<Entity> feedList = QueryUtils.getEntityChildrenByKind(account, ACCOUNT_FEED_KIND);
		if(feedList.isEmpty()) {
			log.severe(String.format(FEED_NOT_FOUND_ERROR, id));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} 
		if( feedList.size() > 1) {
			log.severe(String.format(MULTIPLE_FEED_ERROR,id));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		Entity feed = feedList.get(0);
		
		List<Value<String>> notifications = feed.getList(ACCOUNT_FEED_NOTIFICATIONS_PROPERTY);
		List<String> notificationList = notifications.stream().map(notification->notification.get()).collect(Collectors.toList());
		
		log.info(String.format(GET_FEED_OK,id,tokenId));
		return Response.ok(g.toJson(notificationList)).build();
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
	protected Response updateFeed(String token, String id, AccountFeed data) {
		if(badString(token) || badString(id) || data.badData()) {
			log.info(UPDATE_FEED_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		long tokenId = Long.parseLong(token);
		log.info(String.format(UPDATE_FEED_START, tokenId));
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}

		Entity tokenEntity = QueryUtils.getEntityById(TOKEN_KIND,tokenId);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		if(!tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
			log.warning(String.format(TOKEN_OWNER_ERROR, tokenId,id));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		List<Entity> feedList = QueryUtils.getEntityChildrenByKind(account, ACCOUNT_FEED_KIND);
		if(feedList.isEmpty()) {
			log.severe(String.format(FEED_NOT_FOUND_ERROR, id));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} 
		if( feedList.size() > 1) {
			log.severe(String.format(MULTIPLE_FEED_ERROR,id));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		
		
		Entity feedEntity = feedList.get(0);
		
		ListValue.Builder feedBuilder = ListValue.newBuilder();
		
		for(String notification:data.feed){
			feedBuilder.addValue(StringValue.newBuilder(notification).setExcludeFromIndexes(true).build());
		}
		
		ListValue completeFeed = feedBuilder.build(); 
		
		Entity updatedFeed = Entity.newBuilder(feedEntity)
		.set(ACCOUNT_FEED_NOTIFICATIONS_PROPERTY, completeFeed)
		.build();
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.update(updatedFeed);
			txn.commit();
		
			log.info(String.format(UPDATE_FEED_OK,id,tokenId));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
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
	public static boolean addNotificationToFeed(String user,String message) {
		if(badString(user)||badString(message)) {
			log.warning(ADD_NOTIFICATION_FEED_BAD_DATA_ERROR);
			return false;
		}
		
		log.info(String.format(ADD_NOTIFICATION_FEED_START,user));
		
		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, user);
		if(account == null) {
			log.warning(String.format(ACCOUNT_NOT_FOUND_ERROR, user));
			return false;
		}
		
		List<Entity> feedList = QueryUtils.getEntityChildrenByKind(account, ACCOUNT_FEED_KIND);
		if(feedList.size() > 1) {
			log.severe(String.format(MULTIPLE_FEED_ERROR,user));
			return false;
		}
		if(feedList.isEmpty()) {
			log.severe(String.format(FEED_NOT_FOUND_ERROR,user));
			return false;
		}
		
		
		Entity feed = feedList.get(0);
		
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
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.update(updatedFeed);
			txn.commit();
			log.info(String.format(ADD_NOTIFICATION_FEED_OK,user));
			return true;
		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return false;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return false;
			}
		}
		
		
		
	}
	
	
}
