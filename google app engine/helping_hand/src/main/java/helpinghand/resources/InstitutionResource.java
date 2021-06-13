/**
 * 
 */
package helpinghand.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;

import helpinghand.util.ChangePass;
import helpinghand.util.Login;
import helpinghand.util.inst.*;
import helpinghand.accesscontrol.AccessControlManager;
/**
 * @author PogChamp Software
 *
 */
@Path(InstitutionResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class InstitutionResource {

	//Paths
	public static final String PATH = "/institution";
	private static final String CREATE_PATH="/"; //POST
	private static final String LOGIN_PATH="/{instId}/login"; //POST
	private static final String LOGOUT_PATH="/{instId}/logout"; //POST
	private static final String DELETE_PATH="/{instId}"; //DELETE
	private static final String UPDATE_PATH="/{instId}"; //PUT
	private static final String GET_PATH="/{instId}"; //GET
	private static final String CHANGE_PASSWORD_PATH="/{instId}/password"; //PUT
	private static final String ADD_SUBSCRIBER_PATH="/{instId}/subscribers"; //POST
	private static final String REMOVE_SUBSCRIBER_PATH="/{instId}/subscribers"; //DELETE
	private static final String GET_SUBSCRIBERS_PATH="/{instId}/subscribers"; //GET
	private static final String GET_INSTS_PATH="/getInsts";//GET
	private static final String CHANGE_STATUS_PATH="/{instId}/changeStatus";
	private static final String GET_STATUS_PATH="/{instId}/getStatus";

	//Kinds
	private static final String INSTKIND = "Inst";
	private static final String USERINSTKIND = "Sub";

	private static final Logger LOG = Logger.getLogger(InstitutionResource.class.getName());
	private	final	Datastore datastore	= DatastoreOptions.getDefaultInstance().getService();

	//Key factories
	private final KeyFactory instKeyFactory = datastore.newKeyFactory().setKind(INSTKIND);

	private final Gson g = new Gson();

	public InstitutionResource() {}

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
	public Response createInst(InstData data) {
		LOG.fine("Registering attempt by: " + data.instId);

		//see if data provided is valid(non null and follows rules)
		if(!data.validate()) 
			return Response.status(Status.BAD_REQUEST).entity("Invalid Attributes!").build();

		Key instKey = instKeyFactory.newKey(data.instId);

		Transaction txn = datastore.newTransaction();

		try {

			Entity inst = txn.get(instKey);

			if(inst != null) {
				txn.rollback();
				return Response.status(Status.CONFLICT).entity("The institution already exists!").build();
			}

			inst = Entity.newBuilder(instKey)
					.set("instId", data.instId)
					.set("email", data.email)
					.set("password",StringValue.newBuilder(DigestUtils.sha512Hex(data.password)).setExcludeFromIndexes(true).build())
					.set("name", data.name)
					.set("initials", data.initials)
					.set("address", "")
					.set("addressComp", "")
					.set("location", "")
					.set("phone", "")
					.set("status", false) //inactive, pending verification
					.build();
			
			txn.add(inst);
			LOG.info("Institution " + data.instId + " has been successfully registered!");
			txn.commit();
			return Response.ok().build();	
		}
		catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction as active!").build();
			}
		}
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
	public Response loginInst(@PathParam("instId") String instId, Login data) {
		LOG.fine("Login attempt by: " + instId);

		if(!data.validate() || !instId.equals(data.clientId)) 
			return Response.status(Status.BAD_REQUEST).entity("Invalid Attributes!").build();

		String token = AccessControlManager.startSessionInst(data.clientId, data.password);

		if(token == null)
			return Response.status(Status.FORBIDDEN).entity("Login Failed").build();

		return Response.ok(token).build();
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
	public Response logoutInst(@PathParam("instId") String instId, @QueryParam("tokenId") String tokenId) {
		LOG.fine("Logout attempt by: " + instId);

		//ends a session, deleting token
		if(!AccessControlManager.endSession(tokenId)) 
			return Response.status(Status.FORBIDDEN).entity("Logout failed").build();

		return Response.ok().build();
	}

	/**
	 * Deletes an institution given its institution identification and the token identification.
	 * @param instId - The institution identification that is going to be deleted.
	 * @param tokenId - The token identification from this institution.
	 * @return 200, if the deletion was successful.
	 * 		   404, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	// TODO: Inst  (gbo & ga se token nao pertencer a instituicao a ser apagada) 
	@DELETE
	@Path(DELETE_PATH)
	public Response deleteInst(@PathParam("instId") String instId, @QueryParam("tokenId") String tokenId) {
		//login token handling needed here
		LOG.fine("Deleting attempt with token: " + tokenId);

		Key instKey = instKeyFactory.newKey(instId);

		Transaction txn = datastore.newTransaction();
		
		try {
			Entity inst = txn.get(instKey);

			if(inst == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("Inst does not exist!").build();
			}

			if(!AccessControlManager.endSession(tokenId)) {
				//Logout failed, should never happen but can if there is a datastore error or exception
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Failed to end user session!").build();
			}
			txn.delete(instKey);
			LOG.info("User " + instId + " has successfully been removed!");
			txn.commit();
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
			}	
		}
	}

	/**
	 * Updates the institution data of the instId.
	 * @param instId - The institution identification.
	 * @param tokenId - The token identification from this institution.
	 * @param data - The updated data for this institution.
	 * @return 200, if the institution has successfully updated its data.
	 * 		   400, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	@PUT
	@Path(UPDATE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateInst(@PathParam("instId") String instId, @QueryParam("tokenId") String tokenId, InstDataPublic data) {
		LOG.fine("Changing attributes attempt by: " + instId);

		Key instKey = instKeyFactory.newKey(instId);

		Transaction txn = datastore.newTransaction();

		try {

			Entity inst = txn.get(instKey);

			if(inst == null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("The institution does not exist!").build();
			} 

			//more readable this way
			Entity newInst = Entity.newBuilder(inst)
					.set("name", data.name)
					.set("initials", data.initials)
					.set("address", data.address)
					.set("addressComp", data.addressComp)
					.set("location", data.location)
					.set("phone", data.phone)
					.build();

			txn.update(newInst);//update fails if there is nothing to update, more secure

			LOG.info("User " + instId + " has successfully changed attributes!");
			txn.commit();
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
			}
		}		
	}

	/**
	 * Obtains the institution data.
	 * @param instId - The institution id that is going to be used to obtain its data.
	 * @param tokenId - The token id from this institution.
	 * @return 200, if the operation was successful.
	 * 		   404, if the institution does not exist.
	 * 		   500, otherwise.
	 */
	@GET
	@Path(GET_PATH)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInst(@PathParam("instId") String instId, @QueryParam("tokenId") String tokenId) {
		//login token handling needed here, maybe verification for confirmed status
		LOG.fine("Getting attributes attempt by: " + instId);

		Key instKey = instKeyFactory.newKey(instId);

		Transaction txn = datastore.newTransaction();

		try {
			Entity inst = txn.get(instKey);

			if(inst == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("Inst does not exist").build();
			}

			//just made it like this to read more easily
			InstDataPublic publicData = new InstDataPublic(
					inst.getString("name"),
					inst.getString("initials"),
					inst.getString("address1"), 
					inst.getString("address2"), 
					inst.getString("city"), 
					inst.getString("phone")
					);

			LOG.info("User " + instId + " has successfully got his own attributes!");
			txn.commit();
			return Response.ok(g.toJson(publicData)).build();
		} 
		catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
			}
		}
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
	@Path(CHANGE_PASSWORD_PATH)
	public Response changeInstPassword(@PathParam("instId") String instId, @QueryParam("tokenId") String tokenId, ChangePass data) {
		//login token handling needed here, maybe verification for confirmed status
		LOG.fine("Changing password attempt by: " + instId);

		if(!data.validate())
			return Response.status(Status.BAD_REQUEST).entity("Invalid data!").build();
		
		Key instKey = instKeyFactory.newKey(instId);

		Transaction txn = datastore.newTransaction();

		try {
			Entity inst = txn.get(instKey);

			if(inst == null ) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("The institution does not exist!").build();
			} 

			if(!inst.getString("password").equals(DigestUtils.sha512Hex(data.oldPassword))) {
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("Invalid Attributes!").build();
			}

			Entity newInst = Entity.newBuilder(inst)
					.set("password", DigestUtils.sha512Hex(data.newPassword))
					.build();

			txn.update(newInst);

			LOG.info("User " + instId + " has successfully changed passwords!");
			txn.commit();
			return Response.ok().build();	
		} 
		catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
			}
		}
	}

	/**
	 * Adds a new user to the institution.
	 * @param instId - The institution id where the user is going to subscribe.
	 * @param subscriberId - The user id who is going to subscribe to the institution.
	 * @param tokenId - The token id of the user who is going to subscribe to the institution.
	 * @return 200, if the subscription was successful.
	 * 		   404, if the institution does not exist or the subscription already exists.
	 * 		   500, otherwise.
	 */
	//maybe should be in UserResource?
	//TODO
	//CHANGE THIS
	@POST 
	@Path(ADD_SUBSCRIBER_PATH)
	public Response subscribe(@PathParam("instId") String instId, @QueryParam("subscriberId") String subscriberId, @QueryParam("tokenId") String tokenId) {
		LOG.fine("Attempting to subscribe " + subscriberId +  " to: " + instId);

		Key instKey = instKeyFactory.newKey(instId);
		Key subKey = datastore.newKeyFactory().addAncestors(PathElement.of(INSTKIND, instId)).setKind(USERINSTKIND).newKey(subscriberId);

		Transaction txn = datastore.newTransaction();

		try {
			
			Entity inst = txn.get(instKey);
			Entity sub = txn.get(subKey);

			if(inst == null || sub != null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("The institution does not exist or the subscription already exists!").build();
			}

			Entity toAdd = Entity.newBuilder(subKey)
					.set("userId", subscriberId)
					.build();
			txn.add(toAdd);

			LOG.info("User " + subscriberId + " has been successfully benn subscribed to instituition: " + instId +"!");
			txn.commit();
			return Response.ok().build();	
		} 
		catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
			}
		}

	} 

	/**
	 * Removes an user from the institution.
	 * @param instId - The institution id where the user is subscribed.
	 * @param subscriberId - The user id who is going to be removed from the institution.
	 * @param tokenId - The token id of the user who is going to be removed from the institution.
	 * @return 200, if the unsubscription was successful.
	 * 		   404, if the institution does not exist or the subscription does not exist.
	 * 		   500, otherwise.
	 */
	//maybe should be in UserResource?
	//TODO
	//CHANGE THIS to joinInst
	@DELETE
	@Path(REMOVE_SUBSCRIBER_PATH)
	public Response unsubscribe(@PathParam("instId") String instId, @QueryParam("subscriberId") String subscriberId, @QueryParam("tokenId") String tokenId) {
		LOG.fine("Attempting to unsubscribe "+ subscriberId +  " from: " + instId);

		Key instKey = datastore.newKeyFactory().setKind(INSTKIND).newKey(instId);

		Key subKey = datastore.newKeyFactory().addAncestors(PathElement.of(INSTKIND, instId)).setKind(USERINSTKIND).newKey(subscriberId);

		Transaction txn = datastore.newTransaction();

		try {

			Entity inst = txn.get(instKey);
			Entity sub = txn.get(subKey);

			if(inst == null || sub == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("Inst and/or Subscriber do not exist").build();
			}

			txn.delete(subKey);

			LOG.info("User " + subscriberId + " has been successfully unsubcribed from instituition: " + instId +"!");
			txn.commit();
			return Response.ok().build();	
		} 
		catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
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
	//TODO
	//CHANGE TO INST GETTING ITS OWN SUBS
	@GET
	@Path(GET_SUBSCRIBERS_PATH)
	public Response getSubscribers(@PathParam("instId") String instId, @QueryParam("tokenId") String tokenId) {
		LOG.fine("Getting subscribers attempt with token: " + tokenId);

		Key instKey = instKeyFactory.newKey(instId);

		Transaction txn = datastore.newTransaction();

		try {
			Entity inst = txn.get(instKey);

			if(inst == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("Isnt does no exist").build();
			}

			Query<Entity> query = Query.newEntityQueryBuilder()
					.setKind(USERINSTKIND)
					.setFilter(PropertyFilter.hasAncestor(instKey))
					.build();

			QueryResults<Entity> subs = txn.run(query);

			List<String> subList = new ArrayList<String>();	

			subs.forEachRemaining(sub -> {
				subList.add(sub.getString("userId"));
			});


			LOG.info("Successfully got users subscribed to : "+ instId);
			txn.commit();
			return Response.ok(g.toJson(subList)).build();	
		} 
		catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
			}
		}
	}
	
	
	//conta com login
	@GET
	@Path(GET_INSTS_PATH)
	public Response getInsts(@QueryParam("tokenId") String tokenId) {
		LOG.fine("Getting all institutions!");
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Query<Entity> query = Query.newEntityQueryBuilder()
					.setKind(INSTKIND)
					.build();

			QueryResults<Entity> subs = txn.run(query);

			List<String[]> subList = new ArrayList<String[]>();	
		
			
			subs.forEachRemaining(sub -> {
				subList.add(new String[]{sub.getString("name"),Boolean.toString(sub.getBoolean("status"))});
			});
			
			
			if(subList.isEmpty()) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("No Instituitions!").build();
			}
			
			LOG.info("Successfully got all institutions!");
			txn.commit();
			return Response.ok(g.toJson(subList)).build();
			
		}catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
			}
		}
	}
	
	//gbo & ga
	@POST
	@Path(CHANGE_STATUS_PATH)
	public Response changeStatus(@QueryParam("tokenId") String tokenId, @PathParam("instId") String instId, Boolean change) {
		LOG.fine("Changing status attempt to: " + instId);

		Key instKey = instKeyFactory.newKey(instId);

		Transaction txn = datastore.newTransaction();

		try {

			Entity inst = txn.get(instKey);

			if(inst == null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("The institution does not exist!").build();
			} 

			//more readable this way
			Entity newInst = Entity.newBuilder(inst)
					.set("status",change)
					.build();

			txn.update(newInst);//update fails if there is nothing to update, more secure

			LOG.info("The " + instId + " has successfully changed status!");
			txn.commit();
			return Response.ok().build();
		}
		catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
			}
		}		
	}
	
	
	//gbo & ga
	@GET
	@Path(GET_STATUS_PATH)
	public Response getStatus(@QueryParam("tokenId") String tokenId, @PathParam("instId") String instId) {
		
				LOG.fine("Getting status attempt to: " + instId);

				Key instKey = instKeyFactory.newKey(instId);

				Transaction txn = datastore.newTransaction();

				try {
					Entity inst = txn.get(instKey);

					if(inst == null) {
						txn.rollback();
						return Response.status(Status.NOT_FOUND).entity("Inst does not exist").build();
					}


					LOG.info("The " + instId + " has this status!");
					txn.commit();
					return Response.ok(g.toJson(inst.getBoolean("status"))).build();
				} 
				catch(DatastoreException e) {
					txn.rollback();
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
				}
				finally {
					if(txn.isActive()) {
						txn.rollback();
						return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction was active!").build();
					}
				}
	}
	

}