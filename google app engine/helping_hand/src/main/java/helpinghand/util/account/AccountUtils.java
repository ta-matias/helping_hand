package helpinghand.util.account;

import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import helpinghand.accesscontrol.AccessControlManager;
import helpinghand.accesscontrol.LoginInfo;
import helpinghand.accesscontrol.Role;
import helpinghand.util.QueryUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;

import static helpinghand.util.GeneralUtils.badPassword;
import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.GeneralUtils.TOKEN_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_ACCESS_INSUFFICIENT_ERROR;



public class AccountUtils {
	
	protected static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final Logger log = Logger.getLogger(AccountUtils.class.getName());
	protected static final Gson g = new Gson();
	
	protected static final String DATASTORE_EXCEPTION_ERROR = "Error in AccountUtils: %s";
	protected static final String TRANSACTION_ACTIVE_ERROR = "Error in AccountUtils: Transaction was active";
	protected static final String ACCOUNT_NOT_FOUND_ERROR_2 = "Account that owns token [%s] does not exist";
	private static final String PASSWORD_DENIED_ERROR = "[%s] is not the current password for the account [%s]";
	public static final String ACCOUNT_NOT_FOUND_ERROR = "Account [%s] does not exist";
	
	protected static final String CREATE_START = "Attempting to create account with id [%s] and role [%s]";
	protected static final String CREATE_OK = "Successfuly created account [%s] and role [%s]";
	protected static final String CREATE_ID_CONFLICT_ERROR = "Account with id [%s] already exists";
	protected static final String CREATE_EMAIL_CONFLICT_ERROR = "Account with id [%s] already exists";
	protected static final String CREATE_BAD_DATA_ERROR = "Account creation attempt failed due to bad inputs";
	
	private static final String DELETE_START = "Attempting to delete account [%s] with token [%s]";
	private static final String DELETE_OK = "Successfully deleted account with token [%s]";
	private static final String DELETE_BAD_DATA_ERROR = "Delete attempt failed due to bad inputs";
	
	private static final String LOGIN_START = "Attempting to login into account [%s]";
	private static final String LOGIN_FAILED = "Login failed for account [%s]";
	private static final String LOGIN_OK = "Login successful for account [%s]";
	private static final String LOGIN_BAD_DATA_ERROR ="Login attempt failed due to bad inputs";
	
	private static final String LOGOUT_START = "Attempting to logout with token [%s]";
	private static final String LOGOUT_FAILED = "Logout failed for token [%s]";
	private static final String LOGOUT_OK = "Logout successful for token [%s]";

	private static final String UPDATE_ID_START = "Attempting to update id with token [%s]";
	private static final String UPDATE_ID_OK = "Successfuly updated id from [%s] to [%s] with token [%s]";
	private static final String UPDATE_ID_BAD_DATA_ERROR ="Change id attempt failed due to bad inputs";
	private static final String UPDATE_ID_CONFLICT_ERROR = "There already exists an account with id [%s]";
	
	private static final String UPDATE_PASSWORD_START = "Attempting to update password with token [%s]";
	private static final String UPDATE_PASSWORD_OK = "Successfuly updated password from [%s] to [%s] with token [%s]";
	private static final String UPDATE_PASSWORD_BAD_DATA_ERROR ="Change password attempt failed due to bad inputs";
	
	private static final String UPDATE_EMAIL_START ="Attempting to update email with token [%s]";
	private static final String UPDATE_EMAIL_OK = "Successfuly update email to [%s] with token [%s]";
	private static final String UPDATE_EMAIL_BAD_DATA_ERROR = "Change email attempt failed due to bad inputs";
	private static final String UPDATE_EMAIL_CONFLICT_ERROR = "There already exists an account with email [%s]";
	
	private static final String UPDATE_STATUS_START ="Attempting to update status with token [%s]";
	private static final String UPDATE_STATUS_OK = "Successfuly updated status to [%s] with token [%s]";
	private static final String UPDATE_STATUS_BAD_DATA_ERROR = "Change status attempt failed due to bad inputs";
	
	private static final String UPDATE_VISIBILITY_START ="Attempting to update visibility with token [%s]";
	private static final String UPDATE_VISIBILITY_OK = "Successfuly updated visibility to [%s] with token [%s]";
	private static final String UPDATE_VISIBILITY_BAD_DATA_ERROR = "Change visibility attempt failed due to bad inputs";
	
	private static final String UPDATE_ACCOUNT_INFO_START ="Attempting to update account info with token [%s]";
	private static final String UPDATE_ACCOUNT_INFO_OK = "Successfuly updated account info of account [%s] with token [%s]";
	private static final String UPDATE_ACCOUNT_INFO_BAD_DATA_ERROR = "Change account info attempt failed due to bad inputs";
	private static final String MULTIPLE_ACCOUNT_INFO_ERROR = "There are multiple account infos for the account [%s]";
	private static final String ACCOUNT_INFO_NOT_FOUND_ERROR = "There is no account info for the account [%s]";
	
	private static final String GET_ACCOUNT_INFO_START = "Attempting to get info of account [%s] with token [%s]";
	private static final String GET_ACCOUNT_INFO_OK = "Successfuly got account info of account [%s] with token [%s]";
	private static final String GET_ACCOUNT_INFO_BAD_DATA_ERROR = "Get account info attempt failed due to bad inputs";
	
	
	protected static final String UPDATE_PROFILE_START ="Attempting to update profile with token [%s]";
	protected static final String UPDATE_PROFILE_OK = "Successfuly updated prfile of account [%s] with token [%s]";
	protected static final String UPDATE_PROFILE_BAD_DATA_ERROR = "Change profile attempt failed due to bad inputs";
	protected static final String MULTIPLE_PROFILE_ERROR = "There are multiple account profiles for the account [%s]";
	protected static final String PROFILE_NOT_FOUND_ERROR = "There is no account profile for the account [%s]";
	
	protected static final String GET_PROFILE_START = "Attempting to get profile of account [%s] with token [%s]";
	protected static final String GET_PROFILE_OK = "Successfuly got profile of account [%s] with token [%s]";
	protected static final String GET_PROFILE_BAD_DATA_ERROR = "Get profile attempt failed due to bad inputs";
	
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
	
	public static final String INSTITUTION_MEMBERS_KIND = "InstMember";
	public static final String INSTITUTION_MEMBERS_ID_PROPERTY = "id";
	
	protected static final boolean ACCOUNT_STATUS_DEFAULT_USER = true;
	protected static final boolean ACCOUNT_STATUS_DEFAULT_INSTITUTION = true;//false in final release
	protected static final boolean ACCOUNT_VISIBLE_DEFAULT = true;
	protected static final String DEFAULT_PROPERTY_VALUE_STRING = "";
	protected static final ListValue DEFAULT_PROPERTY_VALUE_STRINGLIST = ListValue.newBuilder().build();
	
	
	public AccountUtils() {}
	
	protected Response deleteAccount(String id,String token) {
		
		if(badString(id)||badString(token)) {
			log.warning(DELETE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(DELETE_START,id,token));
		
		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,id));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(AccessControlManager.TOKEN_KIND, AccessControlManager.TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
			}
		}
		
		List<Key> toDelete = new LinkedList<>();
		toDelete.add(account.getKey());
		QueryUtils.getEntityChildren(account).stream().forEach(c->toDelete.add(c.getKey()));
		if(!Role.getRole(account.getString(ACCOUNT_ROLE_PROPERTY)).equals(Role.INSTITUTION)) {
			QueryUtils.getEntityListByProperty(INSTITUTION_MEMBERS_KIND,INSTITUTION_MEMBERS_ID_PROPERTY,id).stream().forEach(m->toDelete.add(m.getKey()));
		}
		
		if(!AccessControlManager.endAllSessions(id)) {
			log.warning(String.format(LOGOUT_FAILED, token));
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
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
			if(txn.isActive()) {
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
		
	}
	
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
	
	protected Response logout(String token) {
		log.info(String.format(LOGOUT_START,token));
		
		if(!AccessControlManager.endSession(token)) {
			log.warning(String.format(LOGOUT_FAILED, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		log.info(String.format(LOGOUT_OK, token));
		return Response.ok().build();
	}

	protected Response updateId(String id,ChangeId data, String token) {
		if(data.badData()|| badString(id)||badString(token)) {
			log.warning(UPDATE_ID_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(UPDATE_ID_START,token));
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,token));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(AccessControlManager.TOKEN_KIND, AccessControlManager.TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
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
			log.info(String.format(UPDATE_ID_OK,id,data.id,token));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
			if(txn.isActive()) {
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
	
	protected Response updatePassword(String id,ChangePassword data, String token) {
		if(data.badData()|| badString(id)||badString(token)) {
			log.warning(UPDATE_PASSWORD_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(UPDATE_PASSWORD_START,token));
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,token));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(AccessControlManager.TOKEN_KIND, AccessControlManager.TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
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
			log.info(String.format(UPDATE_PASSWORD_OK,data.oldPassword,data.newPassword,token));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
			if(txn.isActive()) {
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
	
	protected Response updateEmail(String id, ChangeEmail data, String token) {
		if(data.badData() || badString(id) ||badString(token)) {
			log.warning(UPDATE_EMAIL_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(UPDATE_EMAIL_START,token));
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,token));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(AccessControlManager.TOKEN_KIND, AccessControlManager.TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
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
			log.info(String.format(UPDATE_EMAIL_OK,data.email,token));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
			if(txn.isActive()) {
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
	
	protected Response updateStatus(String id, ChangeStatus data, String token) {
		if(data.badData()||badString(id) || badString(token)) {
			log.warning(UPDATE_STATUS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(UPDATE_STATUS_START,token));
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,token));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(AccessControlManager.TOKEN_KIND, AccessControlManager.TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
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
			log.info(String.format(UPDATE_STATUS_OK,data.status,token));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
			if(txn.isActive()) {
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
	
	protected Response updateVisibility(String id, ChangeVisibility data, String token) {
		if(data.badData()||badString(id) || badString(token)) {
			log.warning(UPDATE_VISIBILITY_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(UPDATE_VISIBILITY_START,token));
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,token));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(AccessControlManager.TOKEN_KIND, AccessControlManager.TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
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
			log.info(String.format(UPDATE_VISIBILITY_OK,data.visibility,token));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
			if(txn.isActive()) {
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
	
	protected Response updateAccountInfo(String id, AccountInfo data, String token) {
		if(data.badData() ||badString(id)|| badString(token)) {
			log.warning(UPDATE_ACCOUNT_INFO_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(UPDATE_ACCOUNT_INFO_START,token));
		
		Entity account =  QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2,token));
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(AccessControlManager.TOKEN_KIND, AccessControlManager.TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
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
			log.info(String.format(UPDATE_ACCOUNT_INFO_OK,id,token));
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
			if(txn.isActive()) {
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
		
	}
	
	protected Response getAccountInfo(String id,String token) {
		if(badString(id) || badString(token)) {
			log.warning(GET_ACCOUNT_INFO_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(GET_ACCOUNT_INFO_START,id,token));
		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
			return Response.status(Status.NOT_FOUND).build();
		}
		Entity tokenEntity = QueryUtils.getEntityByProperty(AccessControlManager.TOKEN_KIND, AccessControlManager.TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR_2, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(AccessControlManager.TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
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
		log.info(String.format(GET_ACCOUNT_INFO_OK,id,token));
		return Response.ok(g.toJson(info)).build();
	}

}
