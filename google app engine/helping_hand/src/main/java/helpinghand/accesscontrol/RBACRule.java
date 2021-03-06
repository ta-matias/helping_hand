/**
 * 
 */
package helpinghand.accesscontrol;

import static helpinghand.accesscontrol.Role.*;
import static helpinghand.accesscontrol.AccessControlManager.RBAC_ID_PROPERTY;
import static helpinghand.accesscontrol.AccessControlManager.RBAC_PERMISSION_PROPERTY;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.ListValue;
/**
 * @author PogChamp Software
 *
 */
public enum RBACRule {
	
	//UserResource
	GET_ALL_USERS("GET_user", new Role[] {USER,INSTITUTION,GBO}), //list all user accounts (role USER)
	CREATE_USER("POST_user", new Role[] {ALL}), //create a user account
	DELETE_USER("DELETE_user", new Role[] {USER,GBO}), //delete a user account
	GET_USER("GET_user_account", new Role[] {USER,GBO}), //get user account
	LOGIN_USER("POST_user_login", new Role[] {ALL}), //login as a user
	LOGOUT_USER("DELETE_user_logout", new Role[] {USER}), //logout as a user
	UPDATE_USER_ID("PUT_user_id", new Role[] {USER,GBO}), // update a user's id
	UPDATE_USER_PASSWORD("PUT_user_password", new Role[] {USER,GBO}),//update user's password
	UPDATE_USER_EMAIL("PUT_user_email", new Role[] {USER,GBO}), // update a user's email
	UPDATE_USER_STATUS("PUT_user_status", new Role[] {USER,GBO}), //update a user's status
	UPDATE_USER_VISIBILITY("PUT_user_visibility", new Role[] {USER,GBO}), // update a user's visibility
	UPDATE_USER_INFO("PUT_user_info", new Role[] {USER,GBO}), // update a user's account info
	GET_USER_INFO("GET_user_info", new Role[] {USER,INSTITUTION,GBO}), //get a user's account info
	UPDATE_USER_PROFILE("PUT_user_profile", new Role[] {USER,GBO}), // update a user's profile
	GET_USER_PROFILE("GET_user_profile", new Role[] {USER,INSTITUTION,GBO}), //get a user's profile
	GET_USER_FEED("GET_user_feed", new Role[] {USER}), // get all user feeds
	UPDATE_USER_FEED("PUT_user_feed", new Role[] {USER}), // update a user feed
	GET_USER_STATS("GET_user_stats",new Role[] {USER,INSTITUTION}), // get user's stats
	FOLLOW("POST_user_follow",new Role[] {USER}), // user follows another user
	UFOLLOW("DELETE_user_follow",new Role[] {USER}), // user unfollows another user
	GET_USER_EVENTS("GET_user_events",new Role[] {USER,INSTITUTION,GBO}), // list all events created by the user
	GET_USER_HELP("GET_user_help",new Role[] {USER,INSTITUTION,GBO}),  // list all help requests created by the user
	
	//Institution Resource
	GET_ALL_INSTITUTIONS("GET_institution", new Role[] {USER,INSTITUTION,GBO}), // list all institution accounts (role INSTITUTION)
	CREATE_INSTITUTION("POST_institution", new Role[] {ALL}), //create an institution account
	DELETE_INSTITUTION("DELETE_institution", new Role[] {INSTITUTION,GBO}), // delete an institution account
	GET_INSTITUTION("GET_institution_account", new Role[] {INSTITUTION,GBO}), //get user account
	LOGIN_INSTITUTION("POST_institution_login", new Role[] {ALL}), // login as an institution
	LOGOUT_INSTITUTION("DELETE_institution_logout", new Role[] {INSTITUTION}), // logout as an institution
	UPDATE_INSTITUTION_ID("PUT_institution_id", new Role[] {INSTITUTION,GBO}), // update an institution's id
	UPDATE_INSTITUTION_PASSWORD("PUT_institution_password", new Role[] {INSTITUTION,GBO}),// update an institution's password
	UPDATE_INSTITUTION_EMAIL("PUT_institution_email", new Role[] {INSTITUTION,GBO}),// update an institution's email
	UPDATE_INSTITUTION_STATUS("PUT_institution_status", new Role[] {INSTITUTION,GBO}),// update an institution's status
	UPDATE_INSTITUTION_VISIBILITY("PUT_institution_visibility", new Role[] {INSTITUTION,GBO}),// update an institution's visibility
	UPDATE_INSTITUTION_INFO("PUT_institution_info", new Role[] {INSTITUTION,GBO}),// update an institution's account info
	GET_INSTITUTION_INFO("GET_institution_info", new Role[] {USER,INSTITUTION,GBO}), // get an institution's account info
	UPDATE_INSTITUTION_PROFILE("PUT_institution_profile", new Role[] {INSTITUTION,GBO}),// update an institution's profile
	GET_INSTITUTION_PROFILE("GET_institution_profile", new Role[] {USER,INSTITUTION,GBO}),  // get an institution's profile
	GET_INSTITUTION_MEMBERS("GET_institution_members", new Role[] {USER,INSTITUTION,GBO}), //list an institution's members
	ADD_INSTITUTION_MEMBER("POST_institution_members", new Role[] {INSTITUTION}), // add a member to an institution 
	REMOVE_INSTITUTION_MEMBER("DELETE_institution_members", new Role[] {INSTITUTION}), // remove a member from an institution
	GET_INSTITUTION_EVENTS("GET_institution_events",new Role[] {USER,INSTITUTION,GBO}), // lists all events created by the institution
	GET_INSTITUTION_HELP("GET_institution_help",new Role[] {USER,INSTITUTION,GBO}), // lists all help requests created by the institution
	GET_INSTITUTION_FEED("GET_institution_feed", new Role[] {INSTITUTION}), // get all institution's feeds
	UPDATE_INSTITUTION_FEED("PUT_institution_feed", new Role[] {INSTITUTION}), // update a institution feed
	
	//BackofficeResource
	UPDATE_USER_ROLE("PUT_restricted_updateAccountRole", new Role[] {SU}), // update a user account's role
	UPDATE_TOKEN_ROLE("PUT_restricted_updateTokenRole", new Role[] {USER,GBO,SU}), // update a token's current role
	GET_USERS_ROLE("GET_restricted_listRole", new Role[] {GBO}), // get all accounts of a certain role
	GET_DAILY_STATS("GET_restricted_dailyUsers", new Role[] {GBO}),// get number of accounts created between 2 dates
	
	//EventResource
	GET_ALL_EVENTS("GET_event", new Role[] {USER,INSTITUTION,GBO}), // list all events
	CREATE_EVENT("POST_event", new Role[] {USER,INSTITUTION}), // create an event
	CANCEL_EVENT("DELETE_event", new Role[] {USER,INSTITUTION,GBO}), // cancel an event
	END_EVENT("PUT_event_end", new Role[] {USER,INSTITUTION}), // finish an event
	GET_EVENT("GET_event_get", new Role[] {USER,INSTITUTION,GBO}), // get the event's data
	UPDATE_EVENT("PUT_event", new Role[] {USER,INSTITUTION}), // update the event's data
	JOIN_EVENT("POST_event_join", new Role[] {USER}), // add a participant to the event
	LEAVE_EVENT("DELETE_event_leave", new Role[] {USER}), // remove a participant from the event
	LIST_EVENT_PARTICIPANTS("GET_event_list", new Role[] {USER,INSTITUTION,GBO}), // list all participants of the event
	GET_EVENT_STATUS("GET_event_status", new Role[] {USER,INSTITUTION,GBO}), // get the event's status
	UPDATE_EVENT_STATUS("PUT_event_status", new Role[] {USER,INSTITUTION,GBO}), // update the event's status
	
	//HelpResource
	GET_ALL_HELP("GET_help", new Role[] {USER,GBO}), // list all helps
	CREATE_HELP("POST_help", new Role[] {USER,INSTITUTION}), // create a help
	UPDATE_HELP("PUT_help", new Role[] {USER,INSTITUTION}), // update the help's data
	CANCEL_HELP("DELETE_help", new Role[] {USER,INSTITUTION,GBO}), // cancel a help
	FINISH_HELP("PUT_help_finish", new Role[] {USER,INSTITUTION}), // finish a help
	OFFER_HELP("POST_help_offer", new Role[] {USER}), // add a participant to help
	LEAVE_HELP("DELETE_help_leave", new Role[] {USER}), // remove a participant from help
	CHOOSE_HELPER("PUT_help_helper", new Role[] {USER,INSTITUTION}); // choose a helper.
	
	public String operation;
	public Role[] permitted;
	
	RBACRule(String operation,Role[] permitted){
		this.operation = operation;
		this.permitted = permitted;
	}
	
	public Entity getEntity(Entity.Builder builder) {
		ListValue.Builder permissionBuilder = ListValue.newBuilder();
		
		for(Role r: permitted)
			permissionBuilder.addValue(r.name());

		ListValue permission = permissionBuilder.build();
		builder.set(RBAC_ID_PROPERTY,this.operation);
		builder.set(RBAC_PERMISSION_PROPERTY,permission);
		
		return builder.build();
	}
	
}
