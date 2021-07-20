package helpinghand.resources;

import static helpinghand.resources.EmailLinksResource.PATH;
import static helpinghand.util.account.AccountUtils.ACCOUNT_KIND;
import static helpinghand.util.account.AccountUtils.ACCOUNT_STATUS_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_EMAIL_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.GeneralUtils.APP_SECRET_KIND;
import static helpinghand.util.GeneralUtils.APP_SECRET_VALUE_PROPERTY;
import static helpinghand.util.GeneralUtils.EMAIL_API_KEY;
import static helpinghand.util.GeneralUtils.EMAIL_API_KEY_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.EMAIL_REGEX;
import static helpinghand.util.GeneralUtils.EMAIL_SENDING_ERROR;
import static helpinghand.util.GeneralUtils.EMAIL_SENDING_OK;
import static helpinghand.util.GeneralUtils.OUR_EMAIL;
import static helpinghand.util.GeneralUtils.OUR_REST_URL;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Transaction;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;


@Path(PATH)
@Produces(MediaType.APPLICATION_JSON+";charset=utf-8")
public class EmailLinksResource {
	
	public static final String PATH = "/links";
	
	private static final String DATASTORE_EXCEPTION_ERROR = "Error in EmailLinksResource: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error in EmailLinksResource: Transaction was active";
	private static final String SECRET_NOT_FOUND_ERROR = "Secret (%d) not found, it may have expired";
	private static final String WRONG_SECRET_ERROR = "Secret (%d) does not belong to (%d) or has expired";
	
	private static final String CONFIRM_EMAIL_UPDATE_START = "Starting email update for account(%d) with secret(%d)";
	private static final String CONFIRM_EMAIL_UPDATE_OK= "Email update successful";
	private static final String CONFIRM_EMAIL_UPDATE_BAD_DATA_ERROR = "Email update confirmation failed due to bad inputs";
	private static final String EMAIL_VERIFICATION_SUBJECT = "Email Verification";
	private static final String EMAIL_VERIFICATION_CONTENT = "%s, click <a href = %s >here</a> to verify this email and complete the email change.";
	
	private static final String CONFIRM_ACCOUNT_CREATION_START = "Confirming creation of account(%d) with secret(%d)";
	private static final String CONFIRM_ACCOUNT_CREATION_OK= "Account confirmation successful";
	private static final String CONFIRM_ACCOUNT_CREATION_BAD_DATA_ERROR = "Account creation confirmation failed due to bad inputs";
	private static final String ACCOUNT_VERIFICATION_SUBJECT = "Account creation verification";
	private static final String ACCOUNT_VERIFICATION_CONTENT = "%s, click <a href = %s >here</a> to verify this email and enable your account.";
	
	private static final String CONFIRM_EMAIL_UPDATE_PATH = "/email";
	private static final String CONFIRM_ACCOUNT_CREATION_PATH="/account";
	
	public static final String EMAIL_SECRET_KIND = "EmailSecret";
	public static final String EMAIL_SECRET_ACCOUNT_PROPERTY = "account";
	public static final String EMAIL_SECRET_EXPIRATION_PROPERTY = "expires";
	
	public static final String ACCOUNT_SECRET_KIND = "AccountSecret";
	public static final String ACCOUNT_SECRET_ACCOUNT_PROPERTY = "account";
	public static final String ACCOUNT_SECRET_EXPIRATION_PROPERTY = "expires";
	
	public static final String SECRET_PARAM = "secret";
	public static final String DATASTORE_ID_PARAM = "id";
	public static final String EMAIL_PARAM = "email";
	
	public static final String EMAIL_VERIFICATION_URL_FORMAT = OUR_REST_URL+PATH+CONFIRM_EMAIL_UPDATE_PATH+"?secret=%s&id=%s&email=%s";
	public static final String ACCOUNT_VERIFICATION_URL_FORMAT = OUR_REST_URL+PATH+CONFIRM_ACCOUNT_CREATION_PATH+"?secret=%s&id=%s";
	
	private static final long SECRET_DURATION = 1;//1h
	
	
	private static final Logger log = Logger.getLogger(EmailLinksResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory accountKeyFactory = datastore.newKeyFactory().setKind(ACCOUNT_KIND);
	private static final KeyFactory emailSecretKeyFactory = datastore.newKeyFactory().setKind(EMAIL_SECRET_KIND);
	private static final KeyFactory accountSecretKeyFactory = datastore.newKeyFactory().setKind(ACCOUNT_SECRET_KIND);
	
	public EmailLinksResource() {}
	
	
	@PUT
	@Path(CONFIRM_EMAIL_UPDATE_PATH)
	public Response confirmEmailUpdate(@QueryParam(SECRET_PARAM)String secret, 
			@QueryParam(DATASTORE_ID_PARAM)String datastoreIdString, @QueryParam(EMAIL_PARAM)String email) {
			
		if(badString(secret) || badString(datastoreIdString) || !email.matches(EMAIL_REGEX)) {
			log.severe(CONFIRM_EMAIL_UPDATE_BAD_DATA_ERROR);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		long secretId = Long.parseLong(secret);
		long datastoreId = Long.parseLong(datastoreIdString);
		log.info(String.format(CONFIRM_EMAIL_UPDATE_START, datastoreId,secretId));
		
		Key accountKey = accountKeyFactory.newKey(datastoreId);
		Key secretKey = emailSecretKeyFactory.newKey(secretId);
		
		
		Transaction txn = datastore.newTransaction();
		try {
			Entity secretEntity = txn.get(secretKey);
			if(secretEntity == null) {
				txn.rollback();
				log.warning(String.format(SECRET_NOT_FOUND_ERROR,secretId));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			if(secretEntity.getLong(EMAIL_SECRET_ACCOUNT_PROPERTY) != datastoreId || 
					Timestamp.now().compareTo(secretEntity.getTimestamp(EMAIL_SECRET_EXPIRATION_PROPERTY)) >= 0) {
				
				txn.delete(secretKey);
				txn.commit();
				log.severe(String.format(WRONG_SECRET_ERROR,datastoreId));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			
			Entity accountEntity  = txn.get(accountKey);
			
			if(accountEntity == null) {
				txn.delete(secretKey);
				txn.commit();
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,Long.toString(datastoreId)));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Entity updatedAccount = Entity.newBuilder(accountEntity)
					.set(ACCOUNT_EMAIL_PROPERTY, email)
					.build();
			
			txn.update(updatedAccount);
			txn.delete(secretKey);
			txn.commit();
			
			log.info(CONFIRM_EMAIL_UPDATE_OK);
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
	
	@PUT
	@Path(CONFIRM_ACCOUNT_CREATION_PATH)
	public Response confirmAccountCreation(@QueryParam(SECRET_PARAM)String secret, 
			@QueryParam(DATASTORE_ID_PARAM)String datastoreIdString) {
		
		if(badString(secret) || badString(datastoreIdString)) {
			log.severe(CONFIRM_ACCOUNT_CREATION_BAD_DATA_ERROR);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		long secretId = Long.parseLong(secret);
		long datastoreId = Long.parseLong(datastoreIdString);
		log.info(String.format(CONFIRM_ACCOUNT_CREATION_START, datastoreId,secretId));
		
		Key accountKey = accountKeyFactory.newKey(datastoreId);
		Key secretKey = accountSecretKeyFactory.newKey(secretId);
		
		
		Transaction txn = datastore.newTransaction();
		try {
			Entity secretEntity = txn.get(secretKey);
			if(secretEntity == null) {
				
				txn.rollback();
			
				log.warning(String.format(SECRET_NOT_FOUND_ERROR,secretId));
				return Response.status(Status.FORBIDDEN).build();
			}
		
			if(secretEntity.getLong(ACCOUNT_SECRET_ACCOUNT_PROPERTY) != datastoreId || 
					Timestamp.now().compareTo(secretEntity.getTimestamp(ACCOUNT_SECRET_EXPIRATION_PROPERTY)) >= 0) {
				
				txn.delete(secretKey);
				
				txn.commit();
				
				log.severe(String.format(WRONG_SECRET_ERROR,datastoreId));
				return Response.status(Status.FORBIDDEN).build();
			}
			
			
			Entity accountEntity  = txn.get(accountKey);
			
			if(accountEntity == null) {
				
				txn.delete(secretKey);
				
				txn.commit();
				
				log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR,Long.toString(datastoreId)));
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Entity updatedAccount = Entity.newBuilder(accountEntity)
					.set(ACCOUNT_STATUS_PROPERTY, true)
					.build();
			
			txn.update(updatedAccount);
			
			txn.delete(secretKey);
			
			txn.commit();
			
			log.info(CONFIRM_ACCOUNT_CREATION_OK);
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
	
	public static boolean sendEmailVerification(long datastoreId,String id,String email) {
		
		Key emailSecretKey = datastore.allocateId(emailSecretKeyFactory.newKey());
		
		Instant expirationInstant = Instant.now().plus(SECRET_DURATION,ChronoUnit.HOURS);
		Timestamp expiration = Timestamp.parseTimestamp(expirationInstant.toString());
		
		Entity emailSecret = Entity.newBuilder(emailSecretKey)
				.set(EMAIL_SECRET_ACCOUNT_PROPERTY, datastoreId)
				.set(EMAIL_SECRET_EXPIRATION_PROPERTY, expiration)
				.build();
		
		Transaction txn =  datastore.newTransaction();
		try {
			
			Entity apiKey = txn.get(datastore.newKeyFactory().setKind(APP_SECRET_KIND).newKey(EMAIL_API_KEY));
			
			if(apiKey == null) {
				txn.rollback();
				log.severe(EMAIL_API_KEY_NOT_FOUND_ERROR);
				return false;
			}
			
			String secret = Long.toString(emailSecretKey.getId());
			
			String verificationUrl = String.format(EMAIL_VERIFICATION_URL_FORMAT, secret,Long.toString(datastoreId),email);
			
			Email from  = new Email(OUR_EMAIL);
			Email to = new Email(email);
			Content content = new Content("text/html",String.format(EMAIL_VERIFICATION_CONTENT, id,verificationUrl));
			Mail mail = new Mail(from,EMAIL_VERIFICATION_SUBJECT,to,content);
			
			SendGrid sg = new SendGrid(apiKey.getString(APP_SECRET_VALUE_PROPERTY));
		    Request request = new Request();
		    try {
		    	request.setMethod(Method.POST);
		        request.setEndpoint("mail/send");
		        request.setBody(mail.build());
		        com.sendgrid.Response response = sg.api(request);
		        if(response.getStatusCode()> 300 ||response.getStatusCode()<200) {
		        	log.severe(String.format(EMAIL_SENDING_ERROR, email));
			    	return false;
		        }
		        
		    }catch(IOException e) {
		    	log.severe(String.format(EMAIL_SENDING_ERROR, email));
		    	return false;
		    }
		    
		    txn.add(emailSecret);
		    txn.commit();
		    log.info(String.format(EMAIL_SENDING_OK, email));
			return true;
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
	
	public static boolean sendAccountVerification(long datastoreId,String id, String email) {
		
		Key accountSecretKey = datastore.allocateId(accountSecretKeyFactory.newKey());
		
		Instant expirationInstant = Instant.now().plus(SECRET_DURATION,ChronoUnit.HOURS);
		Timestamp expiration = Timestamp.parseTimestamp(expirationInstant.toString());
		
		Entity accountSecret = Entity.newBuilder(accountSecretKey)
				.set(ACCOUNT_SECRET_ACCOUNT_PROPERTY, datastoreId)
				.set(ACCOUNT_SECRET_EXPIRATION_PROPERTY, expiration)
				.build();
		
		Transaction txn =  datastore.newTransaction();
		try {
			Entity apiKey = txn.get(datastore.newKeyFactory().setKind(APP_SECRET_KIND).newKey(EMAIL_API_KEY));
			
			
			if(apiKey == null) {
				txn.rollback();
				log.severe(EMAIL_API_KEY_NOT_FOUND_ERROR);
				return false;
			}
			
			String secret = Long.toString(accountSecretKey.getId());
			
			String verificationUrl = String.format(ACCOUNT_VERIFICATION_URL_FORMAT, secret,Long.toString(datastoreId));
			
			Email from  = new Email(OUR_EMAIL);
			Email to = new Email(email);
			Content content = new Content("text/html",String.format(ACCOUNT_VERIFICATION_CONTENT, id,verificationUrl));
			Mail mail = new Mail(from,ACCOUNT_VERIFICATION_SUBJECT,to,content);
			
			SendGrid sg = new SendGrid(apiKey.getString(APP_SECRET_VALUE_PROPERTY));
		    Request request = new Request();
		    try {
		    	request.setMethod(Method.POST);
		        request.setEndpoint("mail/send");
		        request.setBody(mail.build());
		        com.sendgrid.Response response = sg.api(request);
		        if(response.getStatusCode()> 300 ||response.getStatusCode()<200) {
		        	log.severe(String.format(EMAIL_SENDING_ERROR, email));
			    	return false;
		        }
		        
		    }catch(IOException e) {
		    	log.severe(String.format(EMAIL_SENDING_ERROR, email));
		    	return false;
		    }
		    txn.add(accountSecret);
		    txn.commit();
		    log.info(String.format(EMAIL_SENDING_OK, email));
			return true;
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
	
	/*TODO:Peridodicaly query all EMAIL_SECRET_KIND and ACCOUNT_SECRET_KIND entities and 
	delete expired ones, if they are ACCOUNT_SECRET_KIND entities, delete the associated
	account(including child entities) from the datastore*/
}
