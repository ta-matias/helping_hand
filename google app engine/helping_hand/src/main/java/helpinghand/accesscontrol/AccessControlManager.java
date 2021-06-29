package helpinghand.accesscontrol;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Value;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;

import helpinghand.util.QueryUtils;
import helpinghand.util.account.AccountUtils;

import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.GeneralUtils.badPassword;

public class AccessControlManager {
	
	private static final String ACCOUNT_NOT_FOUND_ERROR = "Error in AccessControlManager: Account [%s] does not exist";
	private static final String ACCOUNT_INACTIVE_ERROR = "Error in AccessControlManager: Account [%s] is inactive";
	private static final String TOKEN_NOT_FOUND_ERROR = "Error in AccessControlManager: Token with id [%s] does not exist";
	private static final String TOKEN_EXPIRED_ERROR = "Error in AccessControlManager: Token [%s] has expired";
	private static final String WRONG_PASSWORD_ERROR = "Error in AccessControlManager: Wrong password for account [%s]";
	private static final String DATASTORE_EXCEPTION_ERROR = "Error in AccessControlManager: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error in AccessControlManager: Transaction was active";
	private static final String GENERIC_ERROR = "Error in AccessControlManager: Reached outside of try/catch block";
	private static final String SU_CHANGE_ERROR ="Error in AccessControlManager: There was an attempt to change role of SuperUser [%s]";
	private static final String RBAC_NOT_FOUND_ERROR = "Error in AccessControlManager: RBAC for operation [%s] does not exist";
	
	private static final long TOKEN_DURATION = 12;//12h
	
	public static final String RBAC_KIND = "RBACPolicy";
	public static final String RBAC_PERMISSION_PROPERTY = "permited";
	public static final String RBAC_ID_PROPERTY = "operation";
	
	public static final String TOKEN_ID_PARAM = "tokenId";
	
	public static final String TOKEN_KIND = "Token";
	public static final String TOKEN_ID_PROPERTY = "id";
	public static final String TOKEN_OWNER_PROPERTY = "account";
	public static final String TOKEN_ROLE_PROPERTY = "role";
	public static final String TOKEN_CREATION_PROPERTY = "creation";
	public static final String TOKEN_EXPIRATION_PROPERTY = "expiration";
	
	
	private static Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	private static Logger log = Logger.getLogger(AccessControlManager.class.getName());
	
	/**
	 * Checks if RBAC policy has been initialized, should be called before initilizeRBACPolicy()
	 * @return <b>true</b> if it is initialized, <b>false</b> if it is not
	 */
	public static boolean RBACPolicyIntitalized() {
		
		Entity check = QueryUtils.getEntityByProperty(RBAC_KIND, RBAC_ID_PROPERTY, RBACRule.CREATE_USER.operation);
		return check != null;
	}
	
	/**
	 * Stores initial RBAC policy in datastore if it does not exist yet
	 * @return <b>true</b> if successfully initialized or already initialized, <b>false</b> if there was an error initializing
	 */
	public static boolean intitializeRBACPolicy() {
		if(RBACPolicyIntitalized()) return true;
		
		List<Entity> rules = new LinkedList<>();
		
		for(RBACRule rule : RBACRule.values()) {
			Key ruleKey = datastore.allocateId(datastore.newKeyFactory().setKind(RBAC_KIND).newKey());
			Entity.Builder buider = Entity.newBuilder(ruleKey);
			rules.add(rule.getEntity(buider));
		}
		
		Entity[] rulesArray = new Entity[rules.size()];
		rules.toArray(rulesArray);
		
		
		Transaction txn = datastore.newTransaction();
		
		try {
			txn.add(rulesArray);
			txn.commit();
			return true;
		}
		catch(DatastoreException e) {
			log.info(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
			return false;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.info(TRANSACTION_ACTIVE_ERROR);
				return false;
			}
		}
		
	}
	
	/**
	 * Starts a session for an account
	 * @param id - id of the account
	 * @param password - password for the account
	 * @return LoginInfo with information about token, null if there is an error
	 */
	
	public static LoginInfo startSession(String id, String password) {
		if(badString(id) || badString(password))return null;
		
		Entity account = QueryUtils.getEntityByProperty(AccountUtils.ACCOUNT_KIND, AccountUtils.ACCOUNT_ID_PROPERTY, id);
		
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
			return null;
		}
		if(badPassword(account,password)) {
			log.warning(String.format(WRONG_PASSWORD_ERROR, id));
			return null;
		}
		
		
		

		String tokenId;
		do {
			tokenId = UUID.randomUUID().toString();
		}while(QueryUtils.getEntityByProperty(TOKEN_KIND,TOKEN_ID_PROPERTY,id) != null);
		
		String role = account.getString(AccountUtils.ACCOUNT_ROLE_PROPERTY).equals(Role.INSTITUTION.name())?Role.INSTITUTION.name():Role.USER.name();
		
		
		Timestamp creation = Timestamp.now();
		
		Instant creationInstant = creation.toDate().toInstant();
		
		Timestamp expiration = Timestamp.of(Date.from(creationInstant.plus(TOKEN_DURATION, ChronoUnit.HOURS)));
		
		Key tokenKey = datastore.allocateId(datastore.newKeyFactory().setKind(TOKEN_KIND).newKey());
		
		Entity token = Entity.newBuilder(tokenKey)
		.set(TOKEN_OWNER_PROPERTY, id)
		.set(TOKEN_CREATION_PROPERTY,creation)
		.set(TOKEN_EXPIRATION_PROPERTY,expiration)
		.set(TOKEN_ID_PROPERTY, tokenId)
		.set(TOKEN_ROLE_PROPERTY,role)
		.build();
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.add(token);
			txn.commit();
			
			return new LoginInfo(id,role,tokenId,expiration.toString());
			
		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return null;
			}
		}
		log.severe(GENERIC_ERROR);
		return null;
	}
	
	
	
	
	
	/**
	 * Ends the session of an account
	 * @param tokenId - token id given by the user or institution
	 * @return <b>true</b> if logout was successful, <b>false</b> in case it failed.
	 */
	public static boolean endSession(String tokenId) {
		if(badString(tokenId)) return false;
		
		boolean problem = false;
		
		Entity token = QueryUtils.getEntityByProperty(TOKEN_KIND, TOKEN_ID_PROPERTY, tokenId);
		if(token == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
			return false;
		}
		
		Transaction txn = datastore.newTransaction();
		try {

			
			txn.delete(token.getKey());
			txn.commit();
			return !problem;

		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return false;
		}
		finally{
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return false;
			}
		}

	}

	/**
	 * Ends all sessions of an account
	 * @param accountID - id of the account
	 * @return  <b>true</b> if logout was successful, <b>false</b> in case it failed.
	 */
	public static boolean endAllSessions(String accountId) {
		if(badString(accountId)) return false;

		List<Entity> tokens = QueryUtils.getEntityListByProperty(TOKEN_KIND, TOKEN_OWNER_PROPERTY, accountId);
		
		Key[] keys = new Key[tokens.size()]; 
		tokens.stream().map(token->token.getKey()).collect(Collectors.toList()).toArray(keys);
		
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.delete(keys);
			txn.commit();
			return true;

		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return false;
		}
		finally{
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return false;
			}
		}
	}
	
	
	
	/**
	 * Changes the role of a user (not institution)
	 * @param id - id of account
	 * @param new_role - role to be removed from the user
	 * @return <b>true</b> if change was successful, <b>false</b> if it failed
	 */
	public static boolean updateAccountRole(String id, Role newRole) {
		if(badString(id) || newRole == null) return false;
		
		Entity oldAccount = QueryUtils.getEntityByProperty(AccountUtils.ACCOUNT_KIND, AccountUtils.ACCOUNT_ID_PROPERTY, id);
		if(oldAccount == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
			return false;
		}
		
		//can't change SU
		if(oldAccount.getString("role").equals(Role.SU.name())) {
			log.severe(String.format(SU_CHANGE_ERROR, id));
			return false;
		}

		Entity newAccount = Entity.newBuilder(oldAccount)
		.set("role", newRole.name())
		.build();
		
		Transaction txn = datastore.newTransaction();
		try {
			
			//update user role
			txn.update(newAccount);
			txn.commit();
			
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

	/**
	 * Elevates a token to a new role (only works on user tokens since institutions only have one role)
	 * @param tokenId - id of the token to be elevated.
	 * @param targetRole - role the token is to be elevated to.
	 * @return <b>true</b> if successful, <b>false</b> if failed.
	 */
	public static boolean updateTokenRole(String tokenId,Role targetRole) {
		if(badString(tokenId) || targetRole == null) return false;
		
		
		Entity oldToken = QueryUtils.getEntityByProperty(TOKEN_KIND,TOKEN_ID_PROPERTY, tokenId);
		if(oldToken == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
			return false;
		}
		Entity account =QueryUtils.getEntityByProperty(AccountUtils.ACCOUNT_KIND, AccountUtils.ACCOUNT_ID_PROPERTY, oldToken.getString(TOKEN_OWNER_PROPERTY));
		//user was deleted
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,oldToken.getString(TOKEN_OWNER_PROPERTY)));
			Transaction txn = datastore.newTransaction();
			try {
				
				//update token with new role
				txn.delete(oldToken.getKey());
				txn.commit();
				return true;


			}
			catch(DatastoreException e) {
				txn.rollback();
				log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
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
		Role userRole = Role.getRole(account.getString("role"));
		
		//trying to elevate to role with more access than allowed
		if(targetRole.getAccess() > userRole.getAccess()) {
			return false;
		}
		
		Timestamp creation = Timestamp.now();
		
		Instant creationInstant = creation.toDate().toInstant();
		
		Timestamp expiration = Timestamp.of(Date.from(creationInstant.plus(TOKEN_DURATION, ChronoUnit.HOURS)));
		
		Entity newToken = Entity.newBuilder(oldToken)
		.set(TOKEN_ROLE_PROPERTY, targetRole.name())
		.set(TOKEN_CREATION_PROPERTY,creation)
		.set(TOKEN_EXPIRATION_PROPERTY,expiration)
		.build();
		
		
		
		Transaction txn = datastore.newTransaction();
		try {
			
			//update token with new role
			txn.update(newToken);
			txn.commit();
			return true;


		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
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
	
	
	/**
	 * Evaluates whether a token has permission to do an operation.NOTE: in the event that the tokenId is null or a blank String, access with no token will be tested
	 * @param tokenId - id of the token
	 * @param operationId - id of the operations
	 * @return true if it has permission , false if it doesn't have permission
	 */
	public static boolean hasAccess(String tokenId,String operationId) {
		if(badString(operationId)) return false;
		
		boolean hasToken = !badString(tokenId);
		
		String role = Role.ALL.name();
		if(hasToken) {
			Entity token =QueryUtils.getEntityByProperty(TOKEN_KIND, TOKEN_ID_PROPERTY, tokenId);
			if(token == null) {
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
				return false;
			}
			
			Timestamp now = Timestamp.now();
			Timestamp tokenExpiration =token.getTimestamp(TOKEN_EXPIRATION_PROPERTY);
			if(tokenExpiration.compareTo(now) < 0) {
				log.severe(String.format(TOKEN_EXPIRED_ERROR,tokenId));
				return false;
			}
			
			Entity account = QueryUtils.getEntityByProperty(AccountUtils.ACCOUNT_KIND,AccountUtils.ACCOUNT_ID_PROPERTY,token.getString(TOKEN_OWNER_PROPERTY));
			if(account == null) {
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, token.getString(TOKEN_OWNER_PROPERTY)));
				return false;
			}
			if(!account.getBoolean(AccountUtils.ACCOUNT_STATUS_PROPERTY)) {
				log.severe(String.format(ACCOUNT_INACTIVE_ERROR, token.getString(TOKEN_OWNER_PROPERTY)));
				return false;
			}
		
			role = token.getString(TOKEN_ROLE_PROPERTY);
		}
		
		Entity rbac = QueryUtils.getEntityByProperty(RBAC_KIND, RBAC_ID_PROPERTY, operationId);
		if(rbac == null){
			log.severe(String.format(RBAC_NOT_FOUND_ERROR,operationId));
			return false;
		}
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		try {
			
			List<Value<String>> lst = rbac.getList(RBAC_PERMISSION_PROPERTY);
			txn.commit();
			List<String> roles = lst.stream().map(value->value.get()).collect(Collectors.toList());
			
			if(roles.contains(role))return true;
			return false;

		}
		catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
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
	
	
	
	
	/**
	 * Returns the id of the owner of the provided tokenId
	 * @param tokenId - id of token
	 * @return userId of the owner of the token or null in case of error
	 */
	public static String getOwner(String tokenId) {
		if(badString(tokenId))return null;

		Entity token = QueryUtils.getEntityByProperty(TOKEN_KIND, TOKEN_ID_PROPERTY, tokenId);
		if(token == null){
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
			return null;
		}
		return token.getString(TOKEN_OWNER_PROPERTY);
	
	}
	
}
