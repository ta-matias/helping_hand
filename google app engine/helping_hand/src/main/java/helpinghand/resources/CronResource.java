package helpinghand.resources;

import static helpinghand.resources.CronResource.PATH;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static helpinghand.util.account.AccountUtils.ACCOUNT_KIND;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ROLE_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_INFO_KIND;
import static helpinghand.util.account.AccountUtils.ACCOUNT_FEED_KIND;
import static helpinghand.util.account.AccountUtils.USER_PROFILE_KIND;
import static helpinghand.resources.UserResource.USER_STATS_KIND;
import static helpinghand.util.account.AccountUtils.INSTITUTION_PROFILE_KIND;

import static helpinghand.accesscontrol.AccessControlManager.TOKEN_KIND;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_EXPIRATION_PROPERTY;

import static helpinghand.resources.EmailLinksResource.EMAIL_SECRET_KIND;
import static helpinghand.resources.EmailLinksResource.EMAIL_SECRET_EXPIRATION_PROPERTY;
import static helpinghand.resources.EmailLinksResource.ACCOUNT_SECRET_KIND;
import static helpinghand.resources.EmailLinksResource.ACCOUNT_SECRET_EXPIRATION_PROPERTY;
import static helpinghand.resources.EmailLinksResource.ACCOUNT_SECRET_ACCOUNT_PROPERTY;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;

import helpinghand.accesscontrol.Role;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;

@Path(PATH)
@Produces(MediaType.APPLICATION_JSON+ ";charset=utf-8")
public class CronResource {
	public static final String PATH = "/cron";
	private static final String SWEEP_TOKENS = "/tokens";
	private static final String SWEEP_SECRETS = "/secrets";
	
	
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final Logger log = Logger.getLogger(CronResource.class.getName());
	
	private static final String SWEEP_TOKENS_START = "Attempting to delete expired tokens";
	private static final String SWEEP_TOKENS_OK = "Successfuly deleted expired tokens";
	
	private static final String SWEEP_SECRETS_START = "Attempting to delete expired secrets";
	private static final String SWEEP_SECRETS_OK = "Successfuly deleted expired secrets";
	
	private static final String DATASTORE_EXCEPTION_ERROR = "Error in CronResource: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error in CronResource: Transaction was active";
	
	public CronResource() {}
	
	
	@GET
	@Path(SWEEP_TOKENS)
	public Response sweepTokens() {
		
		log.info(SWEEP_TOKENS_START);
		Query<Key> tokenQuery = Query.newKeyQueryBuilder().setKind(TOKEN_KIND).setFilter(PropertyFilter.le(TOKEN_EXPIRATION_PROPERTY, Timestamp.now())).build();
		
		Transaction txn = datastore.newTransaction();
		try {
			QueryResults<Key> tokenKeyList = txn.run(tokenQuery);
			List<Key> keyList = new LinkedList<>();
			tokenKeyList.forEachRemaining(tokenKey->keyList.add(tokenKey));
			Key[] keyArray = new Key[keyList.size()];
			keyList.toArray(keyArray);
			txn.delete(keyArray);
			txn.commit();
			log.info(SWEEP_TOKENS_OK);
			return Response.ok().build();
		}catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
		
		
		
		
	}
	
	@GET
	@Path(SWEEP_SECRETS)
	public Response sweepSecrets() {
		
		log.info(SWEEP_SECRETS_START);
		Query<Key> emailSecretsQuery = Query.newKeyQueryBuilder().setKind(EMAIL_SECRET_KIND).setFilter(PropertyFilter.le(EMAIL_SECRET_EXPIRATION_PROPERTY, Timestamp.now())).build();
		Query<Entity> accountSecretsQuery = Query.newEntityQueryBuilder().setKind(ACCOUNT_SECRET_KIND).setFilter(PropertyFilter.le(ACCOUNT_SECRET_EXPIRATION_PROPERTY, Timestamp.now())).build();
		KeyFactory accountKeyFactory = datastore.newKeyFactory().setKind(ACCOUNT_KIND);
		Transaction txn = datastore.newTransaction();
		try {
			QueryResults<Key> emailSecretsKeyList = txn.run(emailSecretsQuery);
			QueryResults<Entity> accountSecretsList = txn.run(accountSecretsQuery);
			List<Key> keyList = new LinkedList<>();
			List<Key> accountKeysList = new LinkedList<>();
			emailSecretsKeyList.forEachRemaining(emailSecretKey->keyList.add(emailSecretKey));
			accountSecretsList.forEachRemaining(accountSecret->{
				keyList.add(accountSecret.getKey());
				accountKeysList.add(accountKeyFactory.newKey(accountSecret.getLong(ACCOUNT_SECRET_ACCOUNT_PROPERTY)));
			});
			Key[] keyArray = new Key[keyList.size()];
			keyList.toArray(keyArray);
			txn.delete(keyArray);
			txn.commit();
			Key[] accountKeysArray = new Key[accountKeysList.size()];
			accountKeysList.toArray(accountKeysArray);
			if(!deleteAccounts(accountKeysArray)) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			log.info(SWEEP_SECRETS_OK);
			return Response.ok().build();
		}catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
			
	}
	
	private boolean deleteAccounts(Key... accountKeys) {
		if(accountKeys == null || accountKeys.length == 0)return true;
		
		Transaction txn = datastore.newTransaction();
		try {
			Iterator<Entity> accounts = txn.get(accountKeys);
			List<Key> toDeleteList = new LinkedList<>();
			
			accounts.forEachRemaining(account->{
				long datastoreId = account.getKey().getId();
				PathElement accountPath = PathElement.of(ACCOUNT_KIND, datastoreId);
				Key infoKey = datastore.newKeyFactory().setKind(ACCOUNT_INFO_KIND).addAncestor(accountPath).newKey(datastoreId);
				Key feedKey = datastore.newKeyFactory().setKind(ACCOUNT_FEED_KIND).addAncestor(accountPath).newKey(datastoreId);
				
				toDeleteList.add(account.getKey());
				toDeleteList.add(infoKey);
				toDeleteList.add(feedKey);
				
				if(account.getString(ACCOUNT_ROLE_PROPERTY).equals(Role.INSTITUTION.name())) {
					Key profileKey = datastore.newKeyFactory().setKind(INSTITUTION_PROFILE_KIND).addAncestor(accountPath).newKey(datastoreId);
					toDeleteList.add(profileKey);
				}else {
					Key profileKey = datastore.newKeyFactory().setKind(USER_PROFILE_KIND).addAncestor(accountPath).newKey(datastoreId);
					Key statsKey = datastore.newKeyFactory().setKind(USER_STATS_KIND).addAncestor(accountPath).newKey(datastoreId);
					toDeleteList.add(profileKey);
					toDeleteList.add(statsKey);
				}
			});
			
			Key[] toDeleteArray = new Key[toDeleteList.size()];
			toDeleteList.toArray(toDeleteArray);
			txn.delete(toDeleteArray);
			txn.commit();
			return true;
		}catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_EXCEPTION_ERROR, e.toString()));
			return false;
		}finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return false;
			}
		}
	}
	
}
