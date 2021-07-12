package helpinghand.accesscontrol;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;

import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_CONFLICT_ERROR;
import static helpinghand.util.account.AccountUtils.ACCOUNT_NOT_FOUND_ERROR;
import static helpinghand.util.account.AccountUtils.ACCOUNT_KIND;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_PASSWORD_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_STATUS_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ROLE_PROPERTY;
/**
 * @author PogChamp Software
 *
 */
public class AccessControlManager {

	private static final String ACCOUNT_INACTIVE_ERROR = "Error in AccessControlManager: Account [%s] is inactive";
	private static final String TOKEN_NOT_FOUND_ERROR = "Error in AccessControlManager: Token with id (%d) does not exist";
	private static final String TOKEN_EXPIRED_ERROR = "Error in AccessControlManager: Token (%d) has expired";
	private static final String WRONG_PASSWORD_ERROR = "Error in AccessControlManager: Wrong password for account [%s]";
	private static final String DATASTORE_EXCEPTION_ERROR = "Error in AccessControlManager: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error in AccessControlManager: Transaction was active";
	
	private static final String RBAC_NOT_FOUND_ERROR = "Error in AccessControlManager: RBAC for operation [%s] does not exist";
	private static final String RBAC_PARTIAL_INITIALIZATION_ERROR = "Warning in AccessControlManager: RBAC not fully initialized, some rules are missing from database";
	private static final String MULTIPLE_RBAC_OPERATION_ERROR = "Error in AccessControlManager: Multiple RBAC  rules for operation [%s] registered";
	private static final String USER_ACCESS_INSUFFICIENT_ERROR = "Error in AccessControlManager: Token of user [%s] cannot be elevate to [%s] due to low access level (%d) < (%d)";

	private static final long TOKEN_DURATION = 12;//12h

	public static final String RBAC_KIND = "RBACPolicy";
	public static final String RBAC_PERMISSION_PROPERTY = "permited";
	public static final String RBAC_ID_PROPERTY = "operation";

	public static final String TOKEN_ID_PARAM = "tokenId";

	public static final String TOKEN_KIND = "Token";
	public static final String TOKEN_OWNER_PROPERTY = "account";
	public static final String TOKEN_ROLE_PROPERTY = "role";
	public static final String TOKEN_CREATION_PROPERTY = "creation";
	public static final String TOKEN_EXPIRATION_PROPERTY = "expiration";

	private static Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind(TOKEN_KIND);
	
	private static Logger log = Logger.getLogger(AccessControlManager.class.getName());

	/**
	 * Checks if RBAC policy has been initialized, should be called before initilizeRBACPolicy()
	 * @return <b>true</b> if it is initialized, <b>false</b> if it is not
	 */
	public static boolean RBACPolicyIntitalized() {
		
		Query<Key> rbacQuery = Query.newKeyQueryBuilder().setKind(RBAC_KIND).build();
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());

		try {
			QueryResults<Key> rbacList = txn.run(rbacQuery);
			txn.commit();
			
			AtomicInteger numRules = new AtomicInteger();
			rbacList.forEachRemaining(rule->numRules.incrementAndGet());
			if(numRules.get() != RBACRule.values().length) {
				log.warning(RBAC_PARTIAL_INITIALIZATION_ERROR);
			}
			return numRules.get() > 0;
			
			
		}catch(DatastoreException e) {
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

	/**
	 * Stores initial RBAC policy in datastore if it does not exist yet
	 * @return <b>true</b> if successfully initialized or already initialized, <b>false</b> if there was an error initializing
	 */
	public static boolean intitializeRBACPolicy() {
		if(RBACPolicyIntitalized()) 
			return true;

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
		} catch(DatastoreException e) {
			txn.rollback();
			log.info(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
			return false;
		} finally {
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
		if(badString(id) || badString(password))
			return null;
		
		
		Timestamp creation = Timestamp.now();

		Instant creationInstant = creation.toDate().toInstant();

		Timestamp expiration = Timestamp.of(Date.from(creationInstant.plus(TOKEN_DURATION, ChronoUnit.HOURS)));

		Key tokenKey = datastore.allocateId(tokenKeyFactory.newKey());

		Query<Entity> accountQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
		
		Transaction txn = datastore.newTransaction();
		
		try {
		
			QueryResults<Entity> accountList = txn.run(accountQuery);	
			
			if(!accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
				return null;
			}
			
			Entity account = accountList.next();
	
			if(accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR, id));
				return null;
			}
			
			if(!account.getBoolean(ACCOUNT_STATUS_PROPERTY)) {
				txn.rollback();
				log.warning(String.format(ACCOUNT_INACTIVE_ERROR, id));
				return null;
			}
			
			if(!account.getString(ACCOUNT_PASSWORD_PROPERTY).equals(DigestUtils.sha512Hex(password))) {
				txn.rollback();
				log.warning(String.format(WRONG_PASSWORD_ERROR, id));
				return null;
			}
			
	
			String role = account.getString(ACCOUNT_ROLE_PROPERTY);
	
			
	
			Entity token = Entity.newBuilder(tokenKey)
					.set(TOKEN_OWNER_PROPERTY, id)
					.set(TOKEN_CREATION_PROPERTY,creation)
					.set(TOKEN_EXPIRATION_PROPERTY,expiration)
					.set(TOKEN_ROLE_PROPERTY,role)
					.build();

		
			txn.add(token);
			txn.commit();
			return new LoginInfo(id,role,Long.toString(token.getKey().getId()),expiration.toString());
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
			return null;
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return null;
			}
		}
	}

	/**
	 * Ends the session of an account
	 * @param tokenId - token id given by the user or institution
	 * @return <b>true</b> if logout was successful, <b>false</b> in case it failed.
	 */
	public static boolean endSession(long tokenId) {

		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity token = txn.get(tokenKey);
			if(token == null) {
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return false;
			}
			
			txn.delete(tokenKey);
			txn.commit();
			return true;
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR,e.toString()));
			return false;
		} finally{
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
	public static boolean updateTokenRole(long tokenId,Role targetRole) {
		if(targetRole == null) 
			return false;

		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		
		Transaction txn = datastore.newTransaction();
		try {
		
			Entity oldToken = txn.get(tokenKey);
			
			if(oldToken == null) {
				txn.rollback();
				log.severe(String.format(TOKEN_NOT_FOUND_ERROR,tokenId));
				return false;
			}
			
			String id = oldToken.getString(TOKEN_OWNER_PROPERTY);
			
			Query<Entity> accountQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
			
			QueryResults<Entity> accountList = txn.run(accountQuery);
			
			if(!accountList.hasNext()) {
				txn.delete(tokenKey);
				txn.commit();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
				return false;
			}
			
			Entity account =accountList.next();
	
			if(accountList.hasNext()) {
				txn.rollback();
				log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR, id));
				return false;
			}
			
			
	
			Role userRole = Role.getRole(account.getString("role"));
	
			//trying to elevate to role with more access than allowed
			if(targetRole.getAccess() > userRole.getAccess()) {
				txn.rollback();
				log.severe(String.format(USER_ACCESS_INSUFFICIENT_ERROR, id,targetRole.name(),userRole.getAccess(),targetRole.getAccess()));
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

		
			//update token with new role
			txn.update(newToken);
			txn.commit();
			return true;
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
			return false;
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return false;
			}
		}
		
	}

	/**
	 * Evaluates whether a token has permission to do an operation.
	 * NOTE: in the event that the tokenId is null or a blank String, access with no token will be tested
	 * @param tokenId - id of the token
	 * @param operationId - id of the operations
	 * @return true if it has permission , false if it doesn't have permission
	 */
	public static boolean hasAccess(long tokenId,String operationId) {
		if(badString(operationId)) 
			return false;

		boolean hasToken = tokenId<0?false:true;

		String role = Role.ALL.name();
		
		Query<Entity> rbacQuery = Query.newEntityQueryBuilder().setKind(RBAC_KIND).setFilter(PropertyFilter.eq(RBAC_ID_PROPERTY, operationId)).build(); 
		
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			
			if(hasToken) {
				
				Key tokenKey = tokenKeyFactory.newKey(tokenId);
				
				Entity token = txn.get(tokenKey);
				
				if(token == null) {
					txn.rollback();
					log.severe(String.format(TOKEN_NOT_FOUND_ERROR, tokenId));
					return false;
				}
	
				Timestamp now = Timestamp.now();
				Timestamp tokenExpiration = token.getTimestamp(TOKEN_EXPIRATION_PROPERTY);
				
				if(tokenExpiration.compareTo(now) < 0) {
					txn.delete(tokenKey);
					txn.commit();
					log.severe(String.format(TOKEN_EXPIRED_ERROR,tokenId));
					return false;
				}
				String id = token.getString(TOKEN_OWNER_PROPERTY);
				
				Query<Entity> accountQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ID_PROPERTY, id)).build();
				
				QueryResults<Entity> accountList = txn.run(accountQuery);
				
				if(!accountList.hasNext()) {
					txn.delete(tokenKey);
					txn.commit();
					log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
					return false;
				}
				Entity account = accountList.next();
				
				if(accountList.hasNext()) {
					txn.delete(tokenKey);
					txn.commit();
					log.severe(String.format(ACCOUNT_ID_CONFLICT_ERROR, id));
					return false;
				}
				
				if(!account.getBoolean(ACCOUNT_STATUS_PROPERTY)) {
					txn.delete(tokenKey);
					txn.commit();
					log.severe(String.format(ACCOUNT_INACTIVE_ERROR, id));
					return false;
				}
	
				role = token.getString(TOKEN_ROLE_PROPERTY);
			}
			
			QueryResults<Entity> rbacList = txn.run(rbacQuery);
			txn.commit();
			
			if(!rbacList.hasNext()) {
				log.severe(String.format(RBAC_NOT_FOUND_ERROR,operationId));
				return false;
			}
	
			Entity rbac = rbacList.next();
			
			if(rbacList.hasNext()) {
				log.severe(String.format(MULTIPLE_RBAC_OPERATION_ERROR,operationId));
				return false;
			}
		
			List<Value<String>> lst = rbac.getList(RBAC_PERMISSION_PROPERTY);
			List<String> roles = lst.stream().map(value->value.get()).collect(Collectors.toList());

			if(roles.contains(role))return true;
			return false;
		} catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
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
