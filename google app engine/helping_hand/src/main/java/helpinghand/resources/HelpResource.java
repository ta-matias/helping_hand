/**
 * 
 */
package helpinghand.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import helpinghand.util.help.*;
/**
 * @author PogChamp Software
 *
 */
@Path(HelpResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class HelpResource {
	
	public static final String PATH = "/help";
	private static final String CREATE_PATH = "";
	private static final String FINISH_PATH = "/{helpId}/finish";
	private static final String CANCEL_PATH = "/{helpId}";
	private static final String CHOOSE_HELPER_PATH = "/{helpId}";
	
	private static final String HELPKIND = "Help";
	private static final String USERKIND = "User";
	private static final String INSTKIND = "Inst";
	
	private static final Logger LOG = Logger.getLogger(HelpResource.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	private final KeyFactory helpKeyFactory = datastore.newKeyFactory().setKind(HELPKIND);
	
	private final Gson g = new Gson();
	
	public HelpResource() {
		
	}
	
	@POST
	@Path(CREATE_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createHelp(@QueryParam("tokenId") String tokenId, HelpData data) {
		LOG.fine("The help creation attempted by: " + data.creatorId);
		
		if(!data.validate())
			return Response.status(Status.BAD_REQUEST).entity("Invalid attributes!").build();
		
		Key helpKey = helpKeyFactory.newKey(data.helpId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity help = txn.get(helpKey);
			
			if(help != null) {
				txn.rollback();
				return Response.status(Status.CONFLICT).entity("The help already exists!").build();
			}
			
			help = Entity.newBuilder(helpKey)
					.set("name", data.name)
					.set("helpId", data.helpId)
					.set("creatorId", data.creatorId)
					.set("helpDate", data.helpDate)
					.set("address", data.address)
					.set("location", data.location)
					.set("status", true)
					.build();
			
			txn.add(help);
			LOG.info("Help " + data.helpId + " has successfully been created!");
			txn.commit();
			return Response.ok("Help " + data.helpId + " has successfully been created!").build();
		} catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction as active!").build();
			}
		}
		
	}
	
	@PUT
	@Path(FINISH_PATH)
	public Response finishHelp(@PathParam("helpId") String helpId, @QueryParam("tokenId") String tokenId) {
		LOG.fine("Attempting to finish the help " + helpId);
		
		Key helpKey = helpKeyFactory.newKey(helpId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity help = txn.get(helpKey);
			
			if(help == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("The help " + helpId + " does not exist!").build();
			}
			
			Entity newHelp = Entity.newBuilder(help)
					.set("status", false)
					.build();
			
			txn.update(newHelp);
			LOG.info("Help " + helpId + " was successfully finished.");
			txn.commit();
			return Response.ok("Help " + helpId + " was successfully finished.").build();
		} catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction as active!").build();
			}
		}
		
	}
	
	@DELETE
	@Path(CANCEL_PATH)
	public Response cancelHelp(@PathParam("helpId") String helpId, @QueryParam("tokenId") String tokenId) {
		LOG.fine("Attempting to cancel help " + helpId);
		
		Key helpKey = helpKeyFactory.newKey(helpId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			
			Entity help = txn.get(helpKey);
			
			if(help == null) {
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("The help " + helpId + " does not exist!").build();
			}
			
			txn.delete(helpKey);
			LOG.info("Help " + helpId + " has successfully been canceled!");
			txn.commit();
			return Response.ok("The help was canceled.").build();
		} catch(DatastoreException e) {
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction as active!").build();
			}
		}
		
	}

}
