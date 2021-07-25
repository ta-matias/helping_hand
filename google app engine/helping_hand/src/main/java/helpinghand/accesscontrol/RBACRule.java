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
	
	//Must Have
	m1CREATE_INSTITUTION("POST_institution", new Role[] {ALL}), //create an institution account M1
	m2LOGIN_INSTITUTION("POST_institution_login", new Role[] {ALL}), // login as an institution M2
	m3LOGOUT_INSTITUTION("DELETE_institution_logout", new Role[] {INSTITUTION}), // logout as an institution M3
	m4DELETE_INSTITUTION("DELETE_institution", new Role[] {INSTITUTION,GBO,SYSADMIN}), // delete an institution account M4
	m5GET_INSTITUTION("GET_institution_account", new Role[] {USER,INSTITUTION,GBO}), //get institution account
	m6GET_INSTITUTION_INFO("GET_institution_info", new Role[] {USER,INSTITUTION,GBO}), // get an institution's account info
	m7GET_INSTITUTION_PROFILE("GET_institution_profile", new Role[] {USER,INSTITUTION,GBO}),  // get an institution's profile
	m8CREATE_USER("POST_user", new Role[] {ALL}), //create a user account
	m9LOGIN_USER("POST_user_login", new Role[] {ALL}), //login as a user
	m10LOGOUT_USER("DELETE_user_logout", new Role[] {USER,GBO,SYSADMIN}), //logout as a user
	m11DELETE_USER("DELETE_user", new Role[] {USER,GBO,SYSADMIN}), //delete a user account
	m12GET_USER("GET_user_account", new Role[] {USER,INSTITUTION,GBO,SYSADMIN}), //get user account
	m13GET_USER_INFO("GET_user_info", new Role[] {USER,INSTITUTION,GBO,SYSADMIN}), //get a user's account info
	m14GET_USER_PROFILE("GET_user_profile", new Role[] {USER,INSTITUTION,GBO,SYSADMIN}), //get a user's profile
	
	//Should Have
	s1GET_ALL_INSTITUTIONS("GET_institution", new Role[] {USER,INSTITUTION,GBO}), // list all institution accounts (role INSTITUTION)
	s2UPDATE_INSTITUTION_PASSWORD("PUT_institution_password", new Role[] {INSTITUTION,GBO}),// update an institution's password
	s3UPDATE_INSTITUTION_EMAIL("PUT_institution_email", new Role[] {INSTITUTION,GBO}),// update an institution's email
	s4UPDATE_INSTITUTION_AVATAR("PUT_institution_avatar", new Role[] {INSTITUTION,GBO,SYSADMIN}), // update a user's avatar
	s5UPDATE_INSTITUTION_STATUS("PUT_institution_status", new Role[] {INSTITUTION,GBO,SYSADMIN}),// update an institution's status
	s6UPDATE_INSTITUTION_VISIBILITY("PUT_institution_visibility", new Role[] {INSTITUTION,GBO}),// update an institution's visibility
	s7UPDATE_INSTITUTION_INFO("PUT_institution_info", new Role[] {INSTITUTION,GBO}),// update an institution's account info
	s8UPDATE_INSTITUTION_PROFILE("PUT_institution_profile", new Role[] {INSTITUTION,GBO}),// update an institution's profile
	s9GET_INSTITUTION_HELP("GET_institution_help",new Role[] {USER,INSTITUTION,GBO}), // lists all help requests created by the institution
	s10GET_INSTITUTION_ROUTES("GET_institution_routes",new Role[] {USER,INSTITUTION,GBO}), // lists all routes created by the institution
	s11GET_ALL_USERS("GET_user", new Role[] {USER,INSTITUTION,GBO}), //list all user accounts (role USER)
	s12UPDATE_USER_PASSWORD("PUT_user_password", new Role[] {USER,GBO,SYSADMIN}),//update user's password
	s13UPDATE_USER_EMAIL("PUT_user_email", new Role[] {USER,INSTITUTION,GBO,SYSADMIN}), // update a user's email
	s14UPDATE_USER_AVATAR("PUT_user_avatar", new Role[] {USER,INSTITUTION,GBO,SYSADMIN}), // update a user's avatar
	s15UPDATE_USER_STATUS("PUT_user_status", new Role[] {USER,GBO,SYSADMIN}), //update a user's status
	s16UPDATE_USER_VISIBILITY("PUT_user_visibility", new Role[] {USER,GBO,SYSADMIN}), // update a user's visibility
	s17UPDATE_USER_INFO("PUT_user_info", new Role[] {USER,GBO,SYSADMIN}), // update a user's account info
	s18UPDATE_USER_PROFILE("PUT_user_profile", new Role[] {USER,GBO,SYSADMIN}), // update a user's profile
	s19GET_USER_HELP("GET_user_help",new Role[] {USER,INSTITUTION,GBO}),  // list all help requests created by the user
	s20GET_HELPING("GET_user_helping",new Role[] {USER,GBO,SYSADMIN}),//list all help requests where user is offering to help
	s21GET_USER_ROUTES("GET_user_routes",new Role[] {USER,INSTITUTION,GBO}),  // list all routes created by the user 
	s22GET_ALL_HELP("GET_help", new Role[] {USER,INSTITUTION,GBO,SYSADMIN}), // list all helps
	s23GET_HELP("GET_help_get", new Role[] {USER,INSTITUTION}), // update the help's data
	s24CREATE_HELP("POST_help", new Role[] {USER,INSTITUTION}), // create a help
	s25CANCEL_HELP("DELETE_help", new Role[] {USER,INSTITUTION,GBO}), // cancel a help
	s26FINISH_HELP("PUT_help_finish", new Role[] {USER,INSTITUTION}), // finish a help
	s27UPDATE_HELP("PUT_help", new Role[] {USER,INSTITUTION}), // update the help's data
	s28OFFER_HELP("POST_help_offer", new Role[] {USER}), // add a participant to help
	s29LEAVE_HELP("DELETE_help_leave", new Role[] {USER}), // remove a participant from help
	s30LIST_HELPERS("GET_help_helper", new Role[] {USER,INSTITUTION}), // choose a helper.
	s31CHOOSE_HELPER("PUT_help_helper", new Role[] {USER,INSTITUTION}), // choose a helper.	
	s32CREATE_ROUTE("POST_route", new Role[] {USER,INSTITUTION}),//create a route 
	s33DELETE_ROUTE("DELETE_route",new Role[] {USER,INSTITUTION,GBO}),//delete a route 
	s34UPDATE_ROUTE("PUT_route",new Role[] {USER,INSTITUTION}), //change route info 
	s35GET_ROUTE("GET_route_get", new Role[] {USER,INSTITUTION,GBO}),//get route info 
	s36LIST_ROUTES("GET_route",new Role[] {USER,INSTITUTION,GBO}), // list all route's info 
	
	//Could Have
	c1GET_INSTITUTION_MEMBERS("GET_institution_members", new Role[] {USER,INSTITUTION,GBO}), //list an institution's members
	c2ADD_INSTITUTION_MEMBER("POST_institution_members", new Role[] {INSTITUTION}), // add a member to an institution 
	c3REMOVE_INSTITUTION_MEMBER("DELETE_institution_members", new Role[] {INSTITUTION}), // remove a member from an institution
	c4UPDATE_USER_ROLE("PUT_restricted_updateAccountRole", new Role[] {SYSADMIN}), // update a user account's role
	c5UPDATE_TOKEN_ROLE("PUT_restricted_updateTokenRole", new Role[] {USER,GBO,SYSADMIN}), // update a token's current role
	c6GET_USERS_ROLE("GET_restricted_listRole", new Role[] {GBO}), // get all accounts of a certain role
	c7GET_DAILY_STATS("GET_restricted_dailyUsers", new Role[] {GBO}),// get number of accounts created between 2 dates
	c8CREATE_SYSADMIN("POST_restricted_createSysadmin",new Role[] {ALL}),//create SYSADMIN
	c9CONFIRM_EMAIL_UPDATE("GET_links_email",new Role[] {ALL}),
	c10CONFIRM_ACCOUNT_CREATION("GET_links_account",new Role[] {ALL}),
	c11SWEEP_TOKENS("GET_cron_tokens",new Role[] {ALL}),
	c12SWEEP_SECRETS("GET_cron_secrets",new Role[] {ALL}),
	
	//Highlights
	h1GET_INSTITUTION_EVENTS("GET_institution_events",new Role[] {USER,INSTITUTION,GBO}), // lists all events created by the institution
	h2GET_INSTITUTION_FEED("GET_institution_feed", new Role[] {INSTITUTION}), // get all institution's feeds
	h3UPDATE_INSTITUTION_FEED("PUT_institution_feed", new Role[] {INSTITUTION}), // update a institution feed
	h4FOLLOW("POST_user_follow",new Role[] {USER}), // user follows another user
	h5UNFOLLOW("DELETE_user_follow",new Role[] {USER}), // user unfollows another user
	h6GET_USER_EVENTS("GET_user_events",new Role[] {USER,INSTITUTION,GBO}), // list all events created by the user
	h7GET_PARTICIPATING("GET_user_participating", new Role[] {USER,GBO,SYSADMIN}),//list all events where user is participating
	h8GET_USER_FEED("GET_user_feed", new Role[] {USER,INSTITUTION}), // get all user feeds
	h9UPDATE_USER_FEED("PUT_user_feed", new Role[] {USER,INSTITUTION}), // update a user feed
	h10GET_USER_STATS("GET_user_stats",new Role[] {USER,INSTITUTION}), // get user's stats
	h11GET_ALL_EVENTS("GET_event", new Role[] {USER,INSTITUTION,GBO,SYSADMIN}), // list all events
	h12CREATE_EVENT("POST_event", new Role[] {INSTITUTION}), // create an event
	h13CANCEL_EVENT("DELETE_event", new Role[] {INSTITUTION,GBO}), // cancel an event
	h14END_EVENT("PUT_event_end", new Role[] {INSTITUTION}), // finish an event
	h15UPDATE_EVENT("PUT_event", new Role[] {INSTITUTION}), // update the event's data
	h16GET_EVENT("GET_event_get", new Role[] {USER,INSTITUTION,GBO}), // get the event's data
	h17JOIN_EVENT("POST_event_join", new Role[] {USER}), // add a participant to the event
	h18LEAVE_EVENT("DELETE_event_leave", new Role[] {USER}), // remove a participant from the event
	h19LIST_EVENT_PARTICIPANTS("GET_event_list", new Role[] {USER,INSTITUTION,GBO}), // list all participants of the event
	h20CREATE_REPORT("POST_restricted_createReport", new Role[] {USER,INSTITUTION,GBO}),// create a report
	h21GET_REPORT("GET_restricted_getReport", new Role[] {GBO}),//get a report
	h22LIST_REPORTS("GET_restricted_listReports", new Role[] {GBO}),// list all reports
	h23RESPOND_REPORT("PUT_restricted_respondReport", new Role[] {GBO}),// respond to a report
	h24DELETE_REPORT("DELETE_restricted_deleteReport", new Role[] {GBO}),// delete a report
	
	
	
	START_ROUTE("PUT_route_start",new Role[] {USER}), //start doing a route TODO
	END_ROUTE("PUT_route_end",new Role[] {USER}); //finish doing a route TODO
	
	
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
