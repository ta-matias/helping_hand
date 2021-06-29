/**
 * 
 */
package helpinghand.resources;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;

import helpinghand.util.QueryUtils;
import helpinghand.util.account.*;
import helpinghand.accesscontrol.AccessControlManager;
import helpinghand.accesscontrol.Role;

import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PARAM;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_OWNER_PROPERTY;
import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.GeneralUtils.TOKEN_NOT_FOUND_ERROR;
import static helpinghand.util.GeneralUtils.TOKEN_ACCESS_INSUFFICIENT_ERROR;

/**
 * @author PogChamp Software
 *
 */
@Path(InstitutionResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class InstitutionResource extends AccountUtils{

	private static final String DATASTORE_EXCEPTION_ERROR = "Error in InstitutionResource: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error in InstitutionResource: Transaction was active";
	private static final String REPEATED_MEMBER_ERROR = "The same user is registered in an institution repeatedly";
	
	private static final String GET_ALL_START ="Attempting to get all institutions with token [%s]";
	private static final String GET_ALL_OK ="Successfuly got all institutions with token [%s]";
	private static final String GET_ALL_BAD_DATA_ERROR = "Get all institutions attempt failed due to bad input";
	
	private static final String GET_MEMBERS_START ="Attempting to get all institution [%s] members with token [%s]";
	private static final String GET_MEMBERS_OK ="Successfuly got all institution [%s] members with token [%s]";
	private static final String GET_MEMBERS_BAD_DATA_ERROR = "Get institutions members attempt failed due to bad input";
	
	private static final String ADD_MEMBER_START ="Attempting to get add member [%s] to institution [%s] with token [%s]";
	private static final String ADD_MEMBER_OK ="Successfuly added member [%s] to institution [%s] with token [%s]";
	private static final String ADD_MEMBER_BAD_DATA_ERROR = "Add member to institution attempt failed due to bad input";
	private static final String ADD_MEMBER_CONFLICT_ERROR = "Account [%s] is already a member of institution [%s]";

	private static final String REMOVE_MEMBER_START ="Attempting to remove member [%s] from institution [%s] with token [%s]";
	private static final String REMOVE_MEMBER_OK ="Successfuly removed member [%s] from institution [%s] with token [%s]";
	private static final String REMOVE_MEMBER_BAD_DATA_ERROR = "Remove member from institution  attempt failed due to bad input";
	private static final String REMOVE_MEMBER_NOT_FOUND_ERROR = "Account [%s] is not a member of institution [%s]";
	
	private static final String INSTITUTION_ID_PARAM = "instId";
	private static final String INSTITUTION_MEMBER_ID_PARAM = "memberId";
	
	
	
	
	
	//Paths
	public static final String PATH = "/institution";
	private static final String GET_INSTS_PATH="";//GET
	private static final String CREATE_PATH=""; //POST
	private static final String DELETE_PATH="/{"+INSTITUTION_ID_PARAM+"}"; //DELETE
	private static final String LOGIN_PATH="/{"+INSTITUTION_ID_PARAM+"}/login"; //POST
	private static final String LOGOUT_PATH="/{"+INSTITUTION_ID_PARAM+"}/logout"; //POST
	private static final String UPDATE_ID_PATH="/{"+INSTITUTION_ID_PARAM+"}/id";//PUT
	private static final String UPDATE_PASSWORD_PATH="/{"+INSTITUTION_ID_PARAM+"}/password"; //PUT
	private static final String UPDATE_EMAIL_PATH="/{"+INSTITUTION_ID_PARAM+"}/email"; //PUT
	private static final String UPDATE_STATUS_PATH="/{"+INSTITUTION_ID_PARAM+"}/status"; //PUT
	private static final String UPDATE_VISIBILITY_PATH="/{"+INSTITUTION_ID_PARAM+"}/visibility";//PUT
	private static final String UPDATE_INFO_PATH="/{"+INSTITUTION_ID_PARAM+"}/info"; //PUT
	private static final String GET_INFO_PATH="/{"+INSTITUTION_ID_PARAM+"}/info"; //GET
	private static final String UPDATE_PROFILE_PATH="/{"+INSTITUTION_ID_PARAM+"}/profile"; //PUT
	private static final String GET_PROFILE_PATH="/{"+INSTITUTION_ID_PARAM+"}/profile"; //GET
	private static final String ADD_MEMBER_PATH="/{"+INSTITUTION_ID_PARAM+"}/members"; //PUT
	private static final String REMOVE_MEMBER_PATH="/{"+INSTITUTION_ID_PARAM+"}/members"; //PUT
	private static final String GET_MEMBERS_PATH="/{"+INSTITUTION_ID_PARAM+"}/members"; //GET

	
	private static final Logger log = Logger.getLogger(InstitutionResource.class.getName());
	
	public InstitutionResource() {super();}
	
	
	
	/**
	 * Obtains a list with the id and current status of all institutions
	 * @param token - token of the user doing this operation
	 * @return 200, if the operation was successful.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_INSTS_PATH)
	public Response getAll(@QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(token)) {
			log.info(GET_ALL_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(GET_ALL_START, token));
		Query<Entity> query = Query.newEntityQueryBuilder().setKind(ACCOUNT_KIND).setFilter(PropertyFilter.eq(ACCOUNT_ROLE_PROPERTY, Role.INSTITUTION.name())).build();
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		try {
			QueryResults<Entity> results= txn.run(query);	
			txn.commit();
			List<String[]> institutions = new LinkedList<>();
			
			results.forEachRemaining(entity -> {
				institutions.add(new String[] {entity.getString(ACCOUNT_ID_PROPERTY),Boolean.toString(entity.getBoolean(ACCOUNT_STATUS_PROPERTY))});
			});
			
			log.info(String.format(GET_ALL_OK,token));
			return Response.ok(g.toJson(institutions)).build();
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
	
	/**
	 * Creates a new institution.
	 * @param data - The institution data that contains the name, the initials, the instId,
	 * 		  the email, the password and the confirmation of the password.
	 * @return 200, if the registration was successful.
	 * 		   400, if the institution data has invalid attributes.
	 * 		   409, if the institution already exists.
	 * 		   500, otherwise.
	 */
	@POST
	@Path(CREATE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createAccount(InstitutionCreationData data) {
		if(data.badData()) {
			log.warning(CREATE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(CREATE_START,data.email,Role.USER.name()));
		
		Entity check1 = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, data.id);
		if(check1 != null) {
			log.warning(String.format(CREATE_ID_CONFLICT_ERROR,data.id));
			return Response.status(Status.CONFLICT).build();
		}
		
		Entity check2 = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_EMAIL_PROPERTY, data.email);
		if(check2 != null) {
			log.warning(String.format(CREATE_EMAIL_CONFLICT_ERROR,data.email));
			return Response.status(Status.CONFLICT).build();
		}
		
		Key accountKey = datastore.allocateId(datastore.newKeyFactory().setKind(ACCOUNT_KIND).newKey());
		Key accountInfoKey = datastore.allocateId(datastore.newKeyFactory().addAncestor(PathElement.of(ACCOUNT_KIND, accountKey.getId())).setKind(ACCOUNT_INFO_KIND).newKey());
		Key institutionProfileKey = datastore.allocateId(datastore.newKeyFactory().addAncestor(PathElement.of(ACCOUNT_KIND, accountKey.getId())).setKind(INSTITUTION_PROFILE_KIND).newKey());
		
		Timestamp now = Timestamp.now();
		
		Entity account = Entity.newBuilder(accountKey)
		.set(ACCOUNT_ID_PROPERTY, data.id)
		.set(ACCOUNT_EMAIL_PROPERTY,data.email)
		.set(ACCOUNT_PASSWORD_PROPERTY, StringValue.newBuilder(DigestUtils.sha512Hex(data.password)).setExcludeFromIndexes(true).build())
		.set(ACCOUNT_ROLE_PROPERTY,Role.INSTITUTION.name())
		.set(ACCOUNT_CREATION_PROPERTY, now)
		.set(ACCOUNT_STATUS_PROPERTY,ACCOUNT_STATUS_DEFAULT_INSTITUTION)
		.set(ACCOUNT_VISIBILITY_PROPERTY, ACCOUNT_VISIBLE_DEFAULT)
		.build();
		
		Entity accountInfo = Entity.newBuilder(accountInfoKey)
		.set(ACCOUNT_INFO_PHONE_PROPERTY,StringValue.newBuilder(DEFAULT_PROPERTY_VALUE_STRING).setExcludeFromIndexes(true).build())
		.set(ACCOUNT_INFO_ADDRESS_1_PROPERTY,StringValue.newBuilder(DEFAULT_PROPERTY_VALUE_STRING).setExcludeFromIndexes(true).build())
		.set(ACCOUNT_INFO_ADDRESS_2_PROPERTY,StringValue.newBuilder(DEFAULT_PROPERTY_VALUE_STRING).setExcludeFromIndexes(true).build())
		.set(ACCOUNT_INFO_ZIPCODE_PROPERTY,DEFAULT_PROPERTY_VALUE_STRING)
		.set(ACCOUNT_INFO_CITY_PROPERTY,DEFAULT_PROPERTY_VALUE_STRING)
		.build();
		
		Entity institutionProfile = Entity.newBuilder(institutionProfileKey)
		.set(PROFILE_NAME_PROPERTY, data.name)
		.set(INSTITUTION_PROFILE_INITIALS_PROPERTY, data.initials)
		.set(PROFILE_BIO_PROPERTY, StringValue.newBuilder(DEFAULT_PROPERTY_VALUE_STRING).setExcludeFromIndexes(true).build())
		.set(INSTITUTION_PROFILE_CATEGORIES_PROPERTY, DEFAULT_PROPERTY_VALUE_STRINGLIST)
		.build();
		
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.add(account,accountInfo,institutionProfile);
			txn.commit();
			log.info(String.format(CREATE_OK,data.id,Role.USER.name()));
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

	/**
	 * Deletes an institution given its institution identification and the token identification.
	 * @param instId - The institution identification that is going to be deleted.
	 * @param tokenId - The token identification from this institution.
	 * @return 200, if the deletion was successful.
	 * 		   404, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	@DELETE
	@Path(DELETE_PATH)
	public Response deleteAccount(@PathParam(INSTITUTION_ID_PARAM) String email, @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.deleteAccount(email, token);
	}
	
	/**
	 * A institution login is performed.
	 * @param instId - The institution id that is going to login.
	 * @param data - The login data of the institution.
	 * @return 200, if the login was successful.
	 * 		   400, if the login is invalid.
	 * 		   403, if the login was failed.
	 */
	@POST
	@Path(LOGIN_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response login(Login data) {
		return super.login(data);
	}
	
	/**
	 * A institution logout is performed.
	 * @param instId - The institution id that is going to logout.
	 * @param tokenId - The token id of the session of this institution.
	 * @return 200, if the logout was successful.
	 * 		   403, if the logout was failed.
	 */
	@DELETE
	@Path(LOGOUT_PATH)
	public Response logout(@QueryParam(TOKEN_ID_PARAM) String token) {
		return super.logout(token);
	}
	
	/**
	 * Changes the id of the institution.
	 * @param instId - The institution id that is going to change the password.
	 * @param tokenId - The token id from this institution.
	 * @param data - The new password data for this institution.
	 * @return 200, if the institution has successfully changed the password.
	 * 		   400, if the data to change password is invalid.
	 * 		   403, if the password is not the same.
	 * 		   404, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_ID_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateId(@PathParam(INSTITUTION_ID_PARAM) String id, ChangeId data, @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.updateId(id, data, token);
	}
	
	/**
	 * Changes the password of the institution.
	 * @param instId - The institution id that is going to change the password.
	 * @param tokenId - The token id from this institution.
	 * @param data - The new password data for this institution.
	 * @return 200, if the institution has successfully changed the password.
	 * 		   400, if the data to change password is invalid.
	 * 		   403, if the password is not the same.
	 * 		   404, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_PASSWORD_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updatePassword(@PathParam(INSTITUTION_ID_PARAM) String id, ChangePassword data, @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.updatePassword(id, data, token);
	}
	
	/**
	 * Changes the email of the institution.
	 * @param email - The institution that is going to change the password.
	 * @param token - The token id of who is changing the password.
	 * @param data - The new email data for this institution.
	 * @return 200, if the institution has successfully changed the email.
	 * 		   400, if the data to change email is invalid.
	 * 		   403, if the password is not the same.
	 * 		   404, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_EMAIL_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateEmail(@PathParam(INSTITUTION_ID_PARAM) String id, ChangeEmail data , @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.updateEmail(id, data, token);
	}
	
	/**
	 * Changes the status of the institution.
	 * @param email - The institution that is going to change the password.
	 * @param token - The token id of who is changing the password.
	 * @param data - The new status data for this institution.
	 * @return 200, if the institution has successfully changed the email.
	 * 		   400, if the data to change email is invalid.
	 * 		   403, if the password is not the same.
	 * 		   404, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_STATUS_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateStatus( @PathParam(INSTITUTION_ID_PARAM) String id, ChangeStatus data,  @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.updateStatus(id, data, token);
	}
	
	/**
	 * Changes the visibility of the institution.
	 * @param email - The institution that is going to change the password.
	 * @param token - The token id of who is changing the password.
	 * @param data - The new status data for this institution.
	 * @return 200, if the institution has successfully changed the email.
	 * 		   400, if the data to change email is invalid.
	 * 		   403, if the password is not the same.
	 * 		   404, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_VISIBILITY_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateVisibility( @PathParam(INSTITUTION_ID_PARAM) String id, ChangeVisibility data,  @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.updateVisibility(id, data, token);
	}
	
	/**
	 * Obtains the institution account info.
	 * @param instId - The institution id that is going to be used to obtain its data.
	 * @param tokenId - The token id from this institution.
	 * @return 200, if the operation was successful.
	 * 		   404, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_INFO_PATH)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAccountInfo(@PathParam(INSTITUTION_ID_PARAM) String id, @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.getAccountInfo(id,token);
	}
	
	/**
	 * Updates the institution account info.
	 * @param instId - The institution identification.
	 * @param tokenId - The token identification from this institution.
	 * @param data - The updated data for this institution.
	 * @return 200, if the institution has successfully updated its data.
	 * 		   400, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_INFO_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateAccountInfo(@PathParam(INSTITUTION_ID_PARAM) String id, AccountInfo data, @QueryParam(TOKEN_ID_PARAM) String token) {
		return super.updateAccountInfo(id, data, token);
	}

	/**
	 * Obtains the institution profile.
	 * @param instId - The institution id that is going to be used to obtain its data.
	 * @param tokenId - The token id from this institution.
	 * @return 200, if the operation was successful.
	 * 		   404, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_PROFILE_PATH)
	public Response getProfile(@PathParam(INSTITUTION_ID_PARAM) String id, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(id) || badString(token)) {
			log.warning(GET_PROFILE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(GET_PROFILE_START,token));
		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
			return Response.status(Status.NOT_FOUND).build();
		}
		Entity tokenEntity = QueryUtils.getEntityByProperty(AccessControlManager.TOKEN_KIND, AccessControlManager.TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		if( !account.getBoolean(ACCOUNT_VISIBILITY_PROPERTY) && !tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		List<Entity> lst = QueryUtils.getEntityChildrenByKind(account,INSTITUTION_PROFILE_KIND);
		if(lst.size() > 1) {
			log.severe(String.format(MULTIPLE_PROFILE_ERROR,id));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		if(lst.isEmpty()) {
			log.severe(String.format(PROFILE_NOT_FOUND_ERROR,id));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		Entity instProfile = lst.get(0);
		
		List<Value<String>>  categoriesListValues = instProfile.getList(INSTITUTION_PROFILE_CATEGORIES_PROPERTY);
		List<String> categoriesList = categoriesListValues.stream().map(value -> value.get()).collect(Collectors.toList());
		String[] categories = new String[categoriesList.size()];
		categoriesList.toArray(categories);
		
		
		InstitutionProfile profile = new InstitutionProfile(
		instProfile.getString(PROFILE_NAME_PROPERTY),
		instProfile.getString(INSTITUTION_PROFILE_INITIALS_PROPERTY),
		instProfile.getString(PROFILE_BIO_PROPERTY),
		categories
		);
		log.info(String.format(GET_PROFILE_OK,id,token));
		return Response.ok(g.toJson(profile)).build();
	}
	
	/**
	 * Updates the institution profile.
	 * @param instId - The institution identification.
	 * @param tokenId - The token identification from this institution.
	 * @param data - The updated data for this institution.
	 * @return 200, if the institution has successfully updated its data.
	 * 		   400, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_PROFILE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateProfile(@PathParam(INSTITUTION_ID_PARAM) String id, @QueryParam(TOKEN_ID_PARAM) String token, InstitutionProfile data) {
		if(data.badData() || badString(token) ||badString(id)) {
			log.warning(UPDATE_PROFILE_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(UPDATE_PROFILE_START,token));
		
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
		if( !tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(account.getString(ACCOUNT_EMAIL_PROPERTY))) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		List<Entity> lst = QueryUtils.getEntityChildrenByKind(account,INSTITUTION_PROFILE_KIND);
		if(lst.size() > 1) {
			log.severe(String.format(MULTIPLE_PROFILE_ERROR,id));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		if(lst.isEmpty()) {
			log.severe(String.format(PROFILE_NOT_FOUND_ERROR,id));
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		Entity instProfile = lst.get(0);
		
		ListValue.Builder listValueBuilder = ListValue.newBuilder();
		for(String category: data.categories) {
			listValueBuilder.addValue(category);
		}
		ListValue categories = listValueBuilder.build();
		
		Entity updatedInstProfile = Entity.newBuilder(instProfile)
		.set(PROFILE_NAME_PROPERTY, data.name)
		.set(INSTITUTION_PROFILE_INITIALS_PROPERTY, data.initials)
		.set(PROFILE_BIO_PROPERTY,data.bio)
		.set(INSTITUTION_PROFILE_CATEGORIES_PROPERTY,categories)
		.build();
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.update(updatedInstProfile);
			txn.commit();
			log.info(String.format(UPDATE_PROFILE_OK,account.getString(ACCOUNT_EMAIL_PROPERTY),token));
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
	
	/**
	 * Obtains the list of subscribers given the institution id.
	 * @param instId - The institution id which is going to be used to obtain the users.
	 * @param tokenId - The token id to see if it has privilege to execute this operation.
	 * @return 200, if the operation was successful.
	 * 		   404, if the institution does not exist or the institution does not have users.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_MEMBERS_PATH)
	public Response getMembers(@PathParam(INSTITUTION_ID_PARAM) String id, @QueryParam(INSTITUTION_MEMBER_ID_PARAM) String token) {
		
		if(badString(id) || badString(token)) {
			log.warning(GET_MEMBERS_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(GET_MEMBERS_START,id,token));
		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
			return Response.status(Status.NOT_FOUND).build();
		}
		Entity tokenEntity = QueryUtils.getEntityByProperty(AccessControlManager.TOKEN_KIND, AccessControlManager.TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		if( !account.getBoolean(ACCOUNT_VISIBILITY_PROPERTY) && !tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		List<Entity> entityList = QueryUtils.getEntityChildrenByKind(account, INSTITUTION_MEMBERS_KIND);
		List<String> memberList = entityList.stream().map(entity->entity.getString(INSTITUTION_MEMBERS_ID_PROPERTY)).collect(Collectors.toList());
		
		log.info(String.format(GET_MEMBERS_OK,id,token));
		return Response.ok(g.toJson(memberList)).build();
	}
	
	/**
	 * Adds a new user to the institution.
	 * @param instId - The institution id where the user is going to subscribe.
	 * @param subscriberId - The user id who is going to subscribe to the institution.
	 * @param tokenId - The token id of the user who is going to subscribe to the institution.
	 * @return 200, if the join was successful.
	 * 		   404, if the institution does not exist or the subscription already exists.
	 * 		   500, otherwise.
	 */
	@POST
	@Path(ADD_MEMBER_PATH)
	public Response addMember(@PathParam(INSTITUTION_ID_PARAM) String id, @QueryParam(INSTITUTION_MEMBER_ID_PARAM) String memberId, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(id) || badString(memberId)||badString(token)) {
			log.warning(ADD_MEMBER_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(ADD_MEMBER_START,memberId,id,token));
		
		Entity check = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, memberId);
		if(check == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
			return Response.status(Status.NOT_FOUND).build();
		}
		
		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
			return Response.status(Status.NOT_FOUND).build();
		}
		
		List<Entity> check2List = QueryUtils.getEntityChildrenByKindAndProperty(account, INSTITUTION_MEMBERS_KIND,INSTITUTION_MEMBERS_ID_PROPERTY,memberId);
		if(check2List.size() > 1) {
			log.severe(REPEATED_MEMBER_ERROR);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		if(check2List.size() == 1) {
			log.warning(String.format(ADD_MEMBER_CONFLICT_ERROR, memberId,id));
			return Response.status(Status.CONFLICT).build();
		}
		
		Entity tokenEntity = QueryUtils.getEntityByProperty(AccessControlManager.TOKEN_KIND, AccessControlManager.TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		Key memberKey = datastore.allocateId(datastore.newKeyFactory().addAncestor(PathElement.of(ACCOUNT_KIND, account.getKey().getId())).setKind(INSTITUTION_MEMBERS_KIND).newKey());
		
		Entity member = Entity.newBuilder(memberKey)
		.set(INSTITUTION_MEMBERS_ID_PROPERTY,memberId)
		.build();
	
		Transaction txn = datastore.newTransaction();
		try {
			txn.add(member);
			txn.commit();
			log.info(String.format(ADD_MEMBER_OK,memberId,id,token));
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
	
	/**
	 * Removes a user from the institution.
	 * @param instId - The institution id where the user is subscribed.
	 * @param subscriberId - The user id who is going to be removed from the institution.
	 * @param tokenId - The token id of the user who is going to be removed from the institution.
	 * @return 200, if the removal was successful.
	 * 		   404, if the institution does not exist or the subscription does not exist.
	 * 		   500, otherwise.
	 */
	@DELETE
	@Path(REMOVE_MEMBER_PATH)
	public Response removeMember(@PathParam(INSTITUTION_ID_PARAM) String id, @QueryParam(INSTITUTION_MEMBER_ID_PARAM) String memberId, @QueryParam(TOKEN_ID_PARAM) String token) {
		if(badString(id) || badString(memberId)||badString(token)) {
			log.warning(REMOVE_MEMBER_BAD_DATA_ERROR);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		log.info(String.format(REMOVE_MEMBER_START,memberId,id,token));
		
		Entity check = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, memberId);
		if(check == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
			return Response.status(Status.NOT_FOUND).build();
		}
		
		List<Entity> check2List = QueryUtils.getEntityChildrenByKindAndProperty(check, INSTITUTION_MEMBERS_KIND,INSTITUTION_MEMBERS_ID_PROPERTY,memberId);
		if(check2List.size() > 1) {
			log.severe(REPEATED_MEMBER_ERROR);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		if(check2List.isEmpty()) {
			log.severe(String.format(REMOVE_MEMBER_NOT_FOUND_ERROR, memberId,id));
			return Response.status(Status.NOT_FOUND).build();
		}
		Entity check2 = check2List.get(0);
		
		Entity account = QueryUtils.getEntityByProperty(ACCOUNT_KIND, ACCOUNT_ID_PROPERTY, id);
		if(account == null) {
			log.severe(String.format(ACCOUNT_NOT_FOUND_ERROR, id));
			return Response.status(Status.NOT_FOUND).build();
		}
		Entity tokenEntity = QueryUtils.getEntityByProperty(AccessControlManager.TOKEN_KIND, AccessControlManager.TOKEN_ID_PROPERTY, token);
		if(tokenEntity == null) {
			log.severe(String.format(TOKEN_NOT_FOUND_ERROR, token));
			return Response.status(Status.FORBIDDEN).build();
		}
		if(!tokenEntity.getString(TOKEN_OWNER_PROPERTY).equals(id)) {
			Role role  = Role.getRole(tokenEntity.getString(AccessControlManager.TOKEN_ROLE_PROPERTY));
			int minAccess = 1;//minimum access level required do execute this operation
			if(role.getAccess() < minAccess) {
				log.warning(String.format(TOKEN_ACCESS_INSUFFICIENT_ERROR,token,role.getAccess(),minAccess));
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		
		
		
		Transaction txn = datastore.newTransaction();
		try {
			txn.delete(check2.getKey());
			txn.commit();
			log.info(String.format(REMOVE_MEMBER_OK,memberId,id,token));
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
	
}