package helpinghand.accesscontrol;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Value;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;

public class AccessControlManager {

	private static final long TOKEN_DURATION = 12*60*60*1000;//12h

	private static Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	private static KeyFactory instKeyFactory = datastore.newKeyFactory().setKind("Inst");
	private static KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");
	private static KeyFactory RBACKeyFactory = datastore.newKeyFactory().setKind("RBACPolicy");
	
	private static Logger log = Logger.getLogger(AccessControlManager.class.getName());
	
	/**
	 * Checks if RBAC policy has been initialized, should be called before initilizeRBACPolicy()
	 * @return <b>true</b> if it is initialized, <b>false</b> if it is not
	 */
	public static boolean RBACPolicyIntitalized() {
		
		Key createUserKey = RBACKeyFactory.newKey("POST_user");
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity check = txn.get(createUserKey);
			
			txn.commit();
			return (check != null);
		}
		catch(DatastoreException e) {
			log.info(String.format("Error initializing RBAC Policy : %s", e.toString()));
			return false;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.info("Error initializing RBAC Policy : Transaction was active");
				return false;
			}
		}
	}
	
	/**
	 * Stores initial RBAC policy in datastore if it does not exist yet
	 * @return <b>true</b> if successfully initialized or already initialized, <b>false</b> if there was an error initializing
	 */
	public static boolean intitializeRBACPolicy() {
		
		Key createUserKey = RBACKeyFactory.newKey("POST_user");
		Key deleteUserKey = RBACKeyFactory.newKey("DELETE_user");
		Key loginUserKey = RBACKeyFactory.newKey("POST_user_login");
		Key logoutUserKey = RBACKeyFactory.newKey("DELETE_user_logout");
		Key changeUserPasswordKey = RBACKeyFactory.newKey("PUT_user_password");
		Key getUserInfoKey = RBACKeyFactory.newKey("GET_user_info");
		Key updateUserInfoKey = RBACKeyFactory.newKey("PUT_user_info");
		Key getUserProfileKey = RBACKeyFactory.newKey("GET_user_profile");
		Key updateUserProfileKey = RBACKeyFactory.newKey("PUT_user_profile");
		Key createInstKey = RBACKeyFactory.newKey("POST_institution");
		Key deleteInstKey = RBACKeyFactory.newKey("DELETE_institution");
		Key loginInstKey = RBACKeyFactory.newKey("POST_institution_login");
		Key logoutInstKey = RBACKeyFactory.newKey("DELETE_institution_logout");
		Key changeInstPasswordKey = RBACKeyFactory.newKey("PUT_institution_password");
		Key getInstKey = RBACKeyFactory.newKey("GET_institution");
		Key updateInstKey = RBACKeyFactory.newKey("PUT_institution");
		
		Entity createUserRBAC = Entity.newBuilder(createUserKey).set("permited", ListValue.of(Role.ALL.name()) ).build();
		Entity deleteUserRBAC = Entity.newBuilder(deleteUserKey).set("permited", ListValue.of(Role.USER.name()) ).build();
		Entity loginUserRBAC = Entity.newBuilder(loginUserKey).set("permited", ListValue.of(Role.ALL.name()) ).build();
		Entity logoutUserRBAC = Entity.newBuilder(logoutUserKey).set("permited", ListValue.of(Role.USER.name()) ).build();
		Entity changUserPasswordRBAC = Entity.newBuilder(changeUserPasswordKey).set("permited", ListValue.of(Role.USER.name()) ).build();
		Entity getUserInfoRBAC = Entity.newBuilder(getUserInfoKey).set("permited", ListValue.of(Role.USER.name()) ).build();
		Entity updateUserInfoRBAC = Entity.newBuilder(updateUserInfoKey).set("permited", ListValue.of(Role.USER.name()) ).build();
		Entity getUserProfileRBAC = Entity.newBuilder(getUserProfileKey).set("permited", ListValue.of(Role.USER.name()) ).build();		
		Entity updateUserProfileRBAC = Entity.newBuilder(updateUserProfileKey).set("permited", ListValue.of(Role.USER.name()) ).build();
		Entity createInstRBAC = Entity.newBuilder(createInstKey).set("permited", ListValue.of(Role.ALL.name()) ).build();
		Entity deleteInstRBAC = Entity.newBuilder(deleteInstKey).set("permited", ListValue.of(Role.INSTITUTION.name()) ).build();
		Entity loginInstRBAC = Entity.newBuilder(loginInstKey).set("permited", ListValue.of(Role.ALL.name()) ).build();
		Entity logoutInstRBAC = Entity.newBuilder(logoutInstKey).set("permited", ListValue.of(Role.INSTITUTION.name()) ).build();
		Entity changeInstPasswordRBAC = Entity.newBuilder(changeInstPasswordKey).set("permited", ListValue.of(Role.INSTITUTION.name()) ).build();
		Entity getInstRBAC = Entity.newBuilder(getInstKey).set("permited", ListValue.of(Role.INSTITUTION.name()) ).build();
		Entity updateInstRBAC = Entity.newBuilder(updateInstKey).set("permited", ListValue.of(Role.INSTITUTION.name()) ).build();
		
		
		
		
		
		Transaction txn = datastore.newTransaction();
		
		try {
			if(txn.get(createUserKey) != null) {
				txn.rollback();
				return true;
			}
			
			txn.add(createUserRBAC,deleteUserRBAC,loginUserRBAC,
					logoutUserRBAC,changUserPasswordRBAC,getUserInfoRBAC,
					updateUserInfoRBAC,getUserProfileRBAC,updateUserProfileRBAC,
					createInstRBAC,deleteInstRBAC,loginInstRBAC,
					logoutInstRBAC,changeInstPasswordRBAC,getInstRBAC,
					updateInstRBAC);
			
			txn.commit();
			return true;
		}
		catch(DatastoreException e) {
			log.info(String.format("Error initializing RBAC Policy : %s", e.toString()));
			return false;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.info("Error initializing RBAC Policy : Transaction was active");
				return false;
			}
		}
		
	}
	
	/**
	 * Starts a session for a user
	 * @param userId - id of the user starting a session
	 * @param password - password of the user starting a session
	 * @return String with <b>tokenId</b> or null in case of failure.
	 */
	public static String startSessionUser(String userId,String password) {
		if(userId == null || password == null) return null;
		Key userKey = userKeyFactory.newKey(userId);
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			

			if(user == null) {
				txn.rollback();
				return null;
			}
			if(!user.getString("password").equals(DigestUtils.sha512Hex(password))) {
				txn.rollback();
				return null;
			}
			if(!user.getBoolean("status")){
				txn.rollback();
				return null;
			}

			String tokenId;
			Key tokenKey;
			do {
				tokenId = UUID.randomUUID().toString();
				tokenKey = tokenKeyFactory.newKey(tokenId);
			}while(txn.get(tokenKey) != null);

			long now = System.currentTimeMillis();

			Entity token = Entity.newBuilder(tokenKey)
					.set("userId", userId)//id
					.set("role", Role.USER.name())//always login with USER/INSTITUTION level privileges
					.set("created",now)//creation time
					.set("expires",now+TOKEN_DURATION)//expiration time
					.build();

			txn.add(token);
			txn.commit();
			return tokenId;
		}
		catch(DatastoreException e) {
			txn.rollback();
			return null;
		}
		finally {
			if(txn.isActive()) { 
				txn.rollback();
				return null;
			}
		}
	}
	
	/**
	 * Starts a session for an institution
	 * @param instId - id of the institution starting a session
	 * @param password - password of the user starting a session
	 * @return String with <b>tokenId</b> or null in case of failure.
	 */
	public static String startSessionInst(String instId,String password) {
		if(instId == null || password == null) return null;
		Key instKey = instKeyFactory.newKey(instId);
		Transaction txn = datastore.newTransaction();
		try {
			Entity inst = txn.get(instKey);

			if(inst == null) {
				txn.rollback();
				log.info("Inst not found");
				return null;
			}
			if(!inst.getString("password").equals(DigestUtils.sha512Hex(password))) {
				txn.rollback();
				log.info("Wrong password: "+password);
				return null;
			}

			if(!inst.getBoolean("status")){
				txn.rollback();
				return null;
			}
			
			String tokenId;
			Key tokenKey;
			do {
				tokenId = UUID.randomUUID().toString(); 
				tokenKey = tokenKeyFactory.newKey(tokenId);
			}while(txn.get(tokenKey) != null);

			long now = System.currentTimeMillis();

			Entity token = Entity.newBuilder(tokenKey)
					.set("userId", instId)//id
					.set("role", Role.INSTITUTION.name())//always login with USER/INSTITUTION level privileges
					.set("created",now)//creation time
					.set("expires",now+TOKEN_DURATION)//expiration time
					.build();

			txn.add(token);
			txn.commit();
			return tokenId;
		}
		catch(DatastoreException e) {
			txn.rollback();
			return null;
		}
		finally {
			if(txn.isActive()) { 
				txn.rollback();
				return null;
			}
		}
	}
	
	
	/**
	 * Ends the session of a user or institution
	 * @param tokenId - token id given by the user or institution
	 * @return <b>true</b> if logout was successful, <b>false</b> in case it failed.
	 */
	public static boolean endSession(String tokenId) {
		if(tokenId == null) return false;

		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		Transaction txn = datastore.newTransaction();
		try {

			Entity token = txn.get(tokenKey);
			if(token == null) {
				txn.rollback();
				return false;
			}
			//if token has expired, delete it and return false
			if(token.getLong("expires") < System.currentTimeMillis()) {
				txn.delete(tokenKey);
				txn.commit();
				return false;
			}
			txn.delete(tokenKey);
			txn.commit();
			return true;

		}
		catch(DatastoreException e) {
			txn.rollback();
			return false;
		}
		finally{
			if(txn.isActive()) {
				txn.rollback();
				return false;
			}
		}

	}

	/**
	 * Ends all sessions of the client that owns the token provided
	 * @param tokenId - token provided by user
	 * @return  <b>true</b> if logout was successful, <b>false</b> in case it failed.
	 */
	public static boolean endAllSessions(String tokenId) {
		//TODO:Implement this in BETA
		return true;
	}
	
	
	
	/**
	 * Changes the role of a user (not institution)
	 * @param userId - id of the user
	 * @param new_role - role to be removed from the user
	 * @return <b>true</b> if change was successful, <b>false</b> if it failed
	 */
	public static boolean changeRole(String userId, Role new_role) {
		if(userId == null || new_role == null) return false;
		Key userKey = userKeyFactory.newKey(userId);
		Transaction txn = datastore.newTransaction();
		try {
			Entity oldUser = txn.get(userKey);
			if(oldUser == null) {
				txn.rollback();
				return false;
			}
			
			//can't change SU
			if(oldUser.getString("role").equals(Role.SU.name())) {
				txn.rollback();
				return false;
			}
			
			Entity newUser = Entity.newBuilder(oldUser)
					.set("role", new_role.name())
					.build();
			
			//update user role
			txn.update(newUser);
			txn.commit();
			
			return true;
		}
		catch(DatastoreException e) {
			txn.rollback();
			return false;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return false;
			}
		}
	}

	/**
	 * Elevates a token to a new role (only works on user tokens since institutions only have one role)
	 * @param tokenId - id of the token to be elevated.
	 * @param target_role - role the token is to be elevated to.
	 * @return <b>true</b> if successful, <b>false</b> if failed.
	 */
	public static boolean elevate_token(String tokenId,Role targetRole) {
		if(tokenId == null || targetRole == null) return false;

		Transaction txn = datastore.newTransaction();
		try {
			Key tokenKey = tokenKeyFactory.newKey(tokenId);
			Entity old_token = txn.get(tokenKey);
			if(old_token == null) {
				txn.rollback();
				return false;
			}
			Key userKey =  userKeyFactory.newKey(old_token.getString("userId"));
			Entity user = txn.get(userKey);
			//user was deleted
			if(user == null) {
				txn.delete(tokenKey);
				txn.commit();
				return false;
			}
			Role userRole = Role.getRole(user.getString("role"));
			
			//trying to elevate to role with more access than allowed
			if(targetRole.getAccess() > userRole.getAccess()) {
				txn.rollback();
				return false;
			}
			
			Long now = System.currentTimeMillis();
			Entity newToken = Entity.newBuilder(old_token)
					.set("role", targetRole.name())
					.set("created",now)
					.set("expires",now+TOKEN_DURATION)
					.build();
			
			//update token with new role
			txn.update(newToken);
			txn.commit();
			return true;


		}
		catch(DatastoreException e) {
			txn.rollback();
			return false;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return false;
			}
		}
	}

	/**
	 * Checks if a token belongs to a client and has privileges to do an operation
	 * @param clientId - id of the client doing the operation
	 * @param tokenId - id of token provided by the client
	 * @param operationId - id of operation to be done
	 * @return <b>true</b> if it can, <b>false</b> if it can not
	 */
	public static boolean clientHasAccess(String clientId, String tokenId,String operationId) {
		if(clientId == null || tokenId == null || operationId == null) return false;
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		Key rbacKey = RBACKeyFactory.newKey(operationId);
		
		//read-only transaction
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			Entity token = txn.get(tokenKey);
			if(token == null) {
				txn.rollback();
				return false;
			}
			//verify token belongs to this user
			if(!token.getString("userId").equals(clientId)) return false;
			

			String tokenRole = token.getString("role");
			
			Entity rbac = txn.get(rbacKey);
			if(rbac == null){
				txn.rollback();
				return false;
			}
			log.info(String.format("Checking if [%s] with token [%s] using role [%s] has access to operation [%s]",clientId,tokenId,tokenRole,operationId));
			List<Value<String>> permitedRoles = rbac.getList("permited");
			Iterator<Value<String>> it = permitedRoles.iterator();
			while(it.hasNext()) {
				if(it.next().get().equals(tokenRole)) {
					txn.commit();
					return true;
				}
				
			}
			
			txn.commit();
			return false;

		}
		catch(DatastoreException e) {
			txn.rollback();
			return false;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return false;
			}
		}
	}

	/**
	 * Checks if a token  has privileges to do an operation
	 * @param tokenId - id of token provided by the client
	 * @param operationId - id of operation to be done
	 * @return <b>true</b> if it can, <b>false</b> if it can not
	 */
	public static boolean tokenHasAccess(String tokenId,String operationId) {
		if(tokenId == null || operationId == null) return false;
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		Key rbacKey = RBACKeyFactory.newKey(operationId);
		
		//read-only transaction
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			Entity token = txn.get(tokenKey);
			if(token == null) {
				txn.rollback();
				return false;
			}
			
			String tokenRole = token.getString("role");
			
			Entity rbac = txn.get(rbacKey);
			if(rbac == null){
				txn.rollback();
				return false;
			}
			List<Value<String>> permitedRoles = rbac.getList("permited");
			Iterator<Value<String>> it = permitedRoles.iterator();
			while(it.hasNext()) {
				if(it.next().get().equals(tokenRole)) {
					txn.commit();
					return true;
				}
				
			}
			
			txn.commit();
			return false;

		}
		catch(DatastoreException e) {
			txn.rollback();
			return false;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return false;
			}
		}
	}
	
	/**
	 * Checks if an operation can be done by a client without a token
	 * @param operationId - id of operation to be done
	 * @return <b>true</b> if it can, <b>false</b> if it can not
	 */
	public static boolean allHasAccess(String operationId) {
		if(operationId == null) return false;
		
		Key rbacKey = RBACKeyFactory.newKey(operationId);
		
		//read-only transaction
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			
			Entity rbac = txn.get(rbacKey);
			if(rbac == null){
				txn.rollback();
				return false;
			}
			List<Value<String>> permitedRoles = rbac.getList("permited");
			Iterator<Value<String>> it = permitedRoles.iterator();
			while(it.hasNext()) {
				if(it.next().get().equals("ALL")) {
					txn.commit();
					return true;
				}
				
			}
			txn.commit();
			return false;

		}
		catch(DatastoreException e) {
			txn.rollback();
			return false;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return false;
			}
		}
	}
	
	/**
	 * Returns the owner of the provided tokenId
	 * @param tokenId - id of token
	 * @return userId of the owner of the token or null in case of error
	 */
	public static String getOwner(String tokenId) {
		if(tokenId == null) return null;
		
		Key tokenKey = tokenKeyFactory.newKey(tokenId);
		
		//read-only transaction
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		try {
			
			Entity token = txn.get(tokenKey);
			if(token == null){
				txn.rollback();
				return null;
			}
			
			txn.commit();
			return token.getString("userId");

		}
		catch(DatastoreException e) {
			txn.rollback();
			return null;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return null;
			}
		}
	}
	
	
}
