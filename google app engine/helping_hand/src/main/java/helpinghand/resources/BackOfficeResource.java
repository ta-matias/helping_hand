/**
 * @author PogChamp Software
 *
 */

package helpinghand.resources;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.*;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;
import com.google.gson.Gson;

import helpinghand.accesscontrol.Role;
import helpinghand.util.user.StatisticsInfo;
import helpinghand.util.user.UserBasicInfo;
import helpinghand.util.user.UserDetails;
import helpinghand.util.user.UserInfo;
import helpinghand.util.user.UserProfile;

@Path(BackOfficeResource.PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class BackOfficeResource {

	// Constants
	private static final String USER_INFO_FORMAT = "%s_info"; // used in checksum for authentication token
	private static final String USER_PROFILE_FORMAT = "%s_profile"; // used in checksum for authentication token
	private static final String USER_KIND = "User";
	private static final String USER_INFO_KIND = "UserInfo";
	private static final String USER_PROFILE_KIND = "UserProfile";

	// Paths
	public static final String PATH = "/restricted";
	private static final String DELETE_PATH = "/delete/{userId}"; // DELETE
	private static final String DISABLE_PATH = "/disable/{userId}"; // PUT
	private static final String CHANGE_ROLE_PATH = "/{userId}/{role}"; //PUT
	private static final String LIST_PATH = "/list"; // GET
	private static final String LIST_ROLE_PATH = "/role/{role}"; // GET
	private static final String DETAILS_PATH = "/{userId}/details"; // GET
	private static final String INFO_PATH = "/{userId}/info"; // GET
	private static final String PROFILE_PATH = "/{userId}/profile"; // GET
	private static final String DAILY_USERS_PATH = "/dailyUsers"; // GET
	

	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private KeyFactory userKeyFactory = datastore.newKeyFactory().setKind(USER_KIND);

	// logger object
	private static final Logger log = Logger.getLogger(UserResource.class.getName());
	
	private final Gson g = new Gson();

	public BackOfficeResource() {
	}

	@DELETE
	@Path(DELETE_PATH)
	public Response deleteAccount(@PathParam("userId") String userId, String tokenId) {
		log.info(String.format("Attempting to delete account for user {%s}", userId));
		
		//TODO: Verificar se o tokenId pertence a alguem do backoffice e se tem permissoes para apagar
		
		Key userKey = userKeyFactory.newKey(userId);
		Key userInfoKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", userId))
				.setKind(USER_INFO_KIND)
				.newKey(String.format(USER_INFO_FORMAT, userId));
		Key userProfileKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", userId))
				.setKind(USER_PROFILE_KIND)
				.newKey(String.format(USER_PROFILE_FORMAT, userId));
		
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity user = txn.get(userKey);
			
			if(user == null) {
				txn.rollback();
				String message = String.format("User with ID:{%s} does not exist", userId);
				log.warning(message);
				return Response.status(Status.FORBIDDEN).entity(message).build();
			}
			
			//TODO: Se estiver logado, terminar sessao da conta
			
			txn.delete(userInfoKey, userProfileKey, userKey);
			log.info("User " + userId + " has successfully been removed!");
			txn.commit();
			return Response.ok().build();
		}
		catch (DatastoreException e) {
			txn.rollback();
			String message = String.format("Datastore Exception on deleting account: %s", e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Internal server error
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after deleting account.");
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Transaction was active
			}
		}
	}
	
	@PUT
	@Path(DISABLE_PATH)
	public Response disableAccount(@PathParam("userId") String userId, String tokenId) {
		log.info(String.format("Attempting to delete account for user {%s}", userId));
		
		//TODO: Verificar se o tokenId pertence a alguem do backoffice e se tem permissoes para dar disable
		
		Key userKey = userKeyFactory.newKey(userId);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity user = txn.get(userKey);
			
			if(user == null) {
				txn.rollback();
				String message = String.format("User with ID:{%s} does not exist", userId);
				log.warning(message);
				return Response.status(Status.FORBIDDEN).entity(message).build();
			}
			
			//TODO: Se estiver logado, terminar sessao da conta
			
			Entity updatedUser = Entity.newBuilder(user).set("status", false).build();
			txn.update(updatedUser);
			
			log.info("User " + userId + " has successfully been disabled!");
			txn.commit();
			return Response.ok().build();
		}
		catch (DatastoreException e) {
			txn.rollback();
			String message = String.format("Datastore Exception on disabling account: %s", e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Internal server error
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after disabling account.");
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Transaction was active
			}
		}
	}
	
	@PUT
	@Path(CHANGE_ROLE_PATH)
	public Response changeRoleAccount(@PathParam("userId") String userId, @PathParam("role") String role, String tokenId) {
		log.info(String.format("Attempting to change role for user {%s} to role {%s}", userId, role));
		
		//TODO: Verificar se o tokenId pertence a alguem do backoffice e se tem permissoes para obter esta operacao
		
		Key userKey = userKeyFactory.newKey(userId);
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity user = txn.get(userKey);
			
			if(user == null) {
				txn.rollback();
				String message = String.format("User with ID:{%s} does not exist", userId);
				log.warning(message);
				return Response.status(Status.FORBIDDEN).entity(message).build();
			}
			
			//TODO: Verificar se o utilizador de backoffice nao esta a tentar mudar o role de um utilizador com role igual ou superior
			
			String role_name = role.trim().toUpperCase();
			Role new_role = Role.getRole(role_name);
			
			if(new_role == null) {
				txn.rollback();
				String message = String.format("Role {%s} is not valid", role);
				log.warning(message);
				return Response.status(Status.BAD_REQUEST).entity(message).build();
			}
			
			Entity changedUser =  Entity.newBuilder(user).set("role", new_role.name()).build();
			
			txn.update(changedUser);
			txn.commit();
			log.info(String.format("Changed role of user with ID:{%s}", userId));
			return Response.ok().build();
		}
		catch (DatastoreException e) {
			txn.rollback();
			String message = String.format("Datastore Exception on changing role: %s", e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Internal server error
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after changing role.");
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Transaction was active
			}
		}
	}
	
	@GET
	@Path(LIST_PATH)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response listAccounts(String tokenId) {
		log.info("Attempting to get list of users.");
		
		//TODO: Verificar se o tokenId pertence a alguem do backoffice e se tem permissoes para obter esta listagem
		
		Query<Entity> query = Query.newEntityQueryBuilder().setKind("User").build();
		List<UserBasicInfo> data = new LinkedList<UserBasicInfo>();
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			
			QueryResults<Entity> users = txn.run(query);
			
			while(users.hasNext()) {
				Entity user = users.next();
				data.add(new UserBasicInfo(user.getKey().getName(), user.getBoolean("status")));
			}
			
			log.info("Got list of users.");
			txn.commit();
			return Response.ok(g.toJson(data)).build();
		}
		catch (DatastoreException e) {
			txn.rollback();
			String message = String.format("Datastore Exception on listing users: %s", e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Internal server error
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after listing users.");
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Transaction was active
			}
		}
	}
	
	@GET
	@Path(LIST_ROLE_PATH)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response listAccountsRole(@PathParam("role") String role, String tokenId) {
		log.info(String.format("Attempting to get list of users of role {%s}", role));
		
		//TODO: Verificar se o tokenId pertence a alguem do backoffice e se tem permissoes para obter esta listagem
		
		Query<Entity> query = Query.newEntityQueryBuilder().setKind("User").build();
		List<UserBasicInfo> data = new LinkedList<UserBasicInfo>();
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			
			QueryResults<Entity> users = txn.run(query);
			String role_name = role.trim().toUpperCase();
			Role user_role = Role.getRole(role_name);
			
			while(users.hasNext()) {
				Entity user = users.next();
				String current_role_name = user.getString("role");
				Role current_user_role = Role.getRole(current_role_name);
				
				if(user_role.equals(current_user_role))
					data.add(new UserBasicInfo(user.getKey().getName(), user.getBoolean("status")));
			}
			
			log.info(String.format("Got list of users for role {%s}", role));
			txn.commit();
			return Response.ok(g.toJson(data)).build();
		}
		catch (DatastoreException e) {
			txn.rollback();
			String message = String.format("Datastore Exception on listing users for role {%s}: %s", role, e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Internal server error
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after listing users for role {%s}.", role);
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Transaction was active
			}
		}
	}
	
	@GET
	@Path(DETAILS_PATH)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response accountDetails(@PathParam("userId") String userId, String tokenId) {
		log.info(String.format("Attempting to get account details of user {%s}", userId));
		
		//TODO: Verificar se o tokenId pertence a alguem do backoffice e se tem permissoes para obter esta listagem
		
		Key userKey = userKeyFactory.newKey(userId);
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity user = txn.get(userKey);
			
			if(user == null) {
				txn.rollback();
				String message = String.format("User with ID:{%s} does not exist", userId);
				log.warning(message);
				return Response.status(Status.FORBIDDEN).entity(message).build();
			}
			
			UserDetails data = new UserDetails(user.getKey().getName(), user.getString("email"), user.getBoolean("status"), user.getString("role"),
					user.getTimestamp("creation").toString());
			
			log.info("Account details of user {" + userId + "} have been successfully retrieved.");
			txn.commit();
			return Response.ok(g.toJson(data)).build();
		}
		catch (DatastoreException e) {
			txn.rollback();
			String message = String.format("Datastore Exception on getting account details for user {%s}: %s", userId, e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Internal server error
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after getting account details for user {%s}.", userId);
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Transaction was active
			}
		}
	}
	
	@GET
	@Path(INFO_PATH)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response accountInfo(@PathParam("userId") String userId, String tokenId) {
		log.info(String.format("Attempting to get account info of user {%s}", userId));
		
		//TODO: Verificar se o tokenId pertence a alguem do backoffice e se tem permissoes para obter esta listagem
		
		Key userKey = userKeyFactory.newKey(userId);
		
		Key infoKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", userId))
				.setKind(USER_INFO_KIND)
				.newKey(String.format(USER_INFO_FORMAT, userId));
		
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity user = txn.get(userKey);
			
			if(user == null) {
				txn.rollback();
				String message = String.format("User with ID:{%s} does not exist", userId);
				log.warning(message);
				return Response.status(Status.FORBIDDEN).entity(message).build();
			}
			
			Entity info = txn.get(infoKey);

			UserInfo data = new UserInfo(info.getString("phone"),
					info.getString("address1"), 
					info.getString("address2"), 
					info.getString("city"),
					info.getString("zip"));
			
			log.info("Account info of user {" + userId + "} has been successfully retrieved.");
			txn.commit();
			return Response.ok(g.toJson(data)).build();
		}
		catch (DatastoreException e) {
			txn.rollback();
			String message = String.format("Datastore Exception on getting account info for user {%s}: %s", userId, e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Internal server error
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after getting info details for user {%s}.", userId);
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Transaction was active
			}
		}
	}
	
	@GET
	@Path(PROFILE_PATH)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response accountProfile(@PathParam("userId") String userId, String tokenId) {
		log.info(String.format("Attempting to get account info of user {%s}", userId));
		
		//TODO: Verificar se o tokenId pertence a alguem do backoffice e se tem permissoes para obter esta listagem
		
		Key userKey = userKeyFactory.newKey(userId);
		
		Key profileKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", userId))
				.setKind(USER_PROFILE_KIND)
				.newKey(String.format(USER_PROFILE_FORMAT, userId));
		
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity user = txn.get(userKey);
			
			if(user == null) {
				txn.rollback();
				String message = String.format("User with ID:{%s} does not exist", userId);
				log.warning(message);
				return Response.status(Status.FORBIDDEN).entity(message).build();
			}
			
			Entity profile = txn.get(profileKey);

			UserProfile data = new UserProfile(profile.getBoolean("public"),
					profile.getString("name"),
					profile.getString("bio"));
			
			log.info("Account profile of user {" + userId + "} has been successfully retrieved.");
			txn.commit();
			return Response.ok(g.toJson(data)).build();
		}
		catch (DatastoreException e) {
			txn.rollback();
			String message = String.format("Datastore Exception on getting profile info for user {%s}: %s", userId, e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Internal server error
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				String message = String.format("Transaction was active after getting profile details for user {%s}.", userId);
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Transaction was active
			}
		}
	}

	@GET
	@Path(DAILY_USERS_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response dailyUsers(StatisticsInfo data) {
		log.info(String.format("Attempting to get daily user statistics of user."));
		
		//TODO: Verificar se o data.tokenId pertence a alguem do backoffice e se tem permissoes para obter estas estatisticas
		
		Query<Entity> query = Query.newEntityQueryBuilder().setKind("User").build();
		
		Map<LocalDate, Integer> map = this.initializeMap(data.startDate, data.endDate);
		
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			
			QueryResults<Entity> users = txn.run(query);
			
			while(users.hasNext()) {
				Entity user = users.next();
				
				String creation = user.getString("creation");
				LocalDate creation_date = this.stampToDate(creation);
				
				if(map.containsKey(creation_date)) {
					int counter = map.get(creation_date);
					counter++;
					map.put(creation_date, counter);
				}
			}
			
			log.info(String.format("Got the mapping of users to day."));
			txn.commit();
			return Response.ok(g.toJson(map)).build();
		}
		catch (DatastoreException e) {
			txn.rollback();
			String message = String.format("Datastore Exception on mapping day to users: %s", e.toString());
			log.severe(message);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Internal server error
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				String message = "Transaction was active after mapping day to users.";
				log.severe(message);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build(); // Transaction was active
			}
		}
	}
	
	private LocalDate stampToDate(String timestamp) {
		String[] pre_tokens = timestamp.split("T");
		String[] tokens = pre_tokens[0].split("-");
		
		LocalDate date = LocalDate.of(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
		
		return date;
	}
	
	private Map<LocalDate, Integer> initializeMap(String startDate, String endDate) {
		
		Map<LocalDate, Integer> data = new HashMap<LocalDate, Integer>();
		
		LocalDate start = LocalDate.parse(startDate);
		LocalDate end = LocalDate.parse(endDate);
		
		long numOfDays = ChronoUnit.DAYS.between(start, end)+1;
		
		Stream.iterate(start, date -> date.plusDays(1)).limit(numOfDays).forEach(k -> data.put(k, 0));
		
		return data;
	}
}
