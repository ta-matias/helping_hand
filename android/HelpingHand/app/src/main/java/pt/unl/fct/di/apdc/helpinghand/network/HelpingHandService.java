package pt.unl.fct.di.apdc.helpinghand.network;


import java.util.List;

import pt.unl.fct.di.apdc.helpinghand.data.model.Account;
import pt.unl.fct.di.apdc.helpinghand.data.model.AccountInfo;
import pt.unl.fct.di.apdc.helpinghand.data.model.ChangePass;
import pt.unl.fct.di.apdc.helpinghand.data.model.ChangeVisibility;
import pt.unl.fct.di.apdc.helpinghand.data.model.CreateEvent;
import pt.unl.fct.di.apdc.helpinghand.data.model.CreateHelp;
import pt.unl.fct.di.apdc.helpinghand.data.model.EventData;
import pt.unl.fct.di.apdc.helpinghand.data.model.HelpData;
import pt.unl.fct.di.apdc.helpinghand.data.model.HelperStats;
import pt.unl.fct.di.apdc.helpinghand.data.model.InstitutionRegisterModel;
import pt.unl.fct.di.apdc.helpinghand.data.model.LoginCredentials;
import pt.unl.fct.di.apdc.helpinghand.data.model.LoginInfo;
import pt.unl.fct.di.apdc.helpinghand.data.model.UpdateUserModel;
import pt.unl.fct.di.apdc.helpinghand.data.model.UserRegisterModel;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryName;

public interface HelpingHandService {

    String REST = "rest";
    String LOGIN = "/login";
    String LOGOUT = "/logout";
    String CHANGEPASS = "/password";
    String INFO = "/info";
    String USER = "/user";
    String INSTITUTION = "/institution";
    String EVENT = "/event";
    String EVENTS = "/events";
    String ACCOUNT = "/account";
    String VISIBILITY = "/visibility";
    String JOIN = "/join";
    String HELP = "/help";
    String OFFER = "/offer";
    String END = "/end";
    String FINISH = "/finish";
    String HELPER = "/helper";

    /**User**/

    @POST(REST + USER + "/{userId}" + LOGIN)
    Call<LoginInfo> authenticateUser(@Path("userId") String userId, @Body LoginCredentials user);

    @POST(REST + USER)
    Call<Void> createUser(@Body UserRegisterModel userModel);

    @DELETE(REST + USER + "/{userId}" + LOGOUT)
    Call<Void> logoutUser(@Path("userId") String userId, String tokenId);

    @PUT(REST + USER + "/{userId}" + CHANGEPASS)
    Call<Void> changePassUser(@Path("userId") String userId, @Body ChangePass data,
                              @Query("tokenId") String tokenId);

    @PUT(REST + USER + "/{userId}" + INFO)
    Call<Void> updateUserInfo(@Path("userId") String userId, @Body UpdateUserModel updateUserModel,
                              @Query("tokenId") String tokenId);

    @GET(REST + USER + "/{userId}" + INFO)
    Call<AccountInfo> getUserInfo(@Path("userId") String userId, @Query("tokenId") String tokenId);

    @GET(REST + USER + "/{userId}" + ACCOUNT)
    Call<Account> getUserAccount(@Path("userId") String userId, @Query("tokenId") String tokenId);

    @DELETE(REST + USER + "/{userId}")
    Call<Void> deleteUser(@Path("userId") String userId, @Query("tokenId") String tokenId);

    @DELETE(REST + USER + "/{instId}")
    Call<Void> deleteInst(@Path("instId") String instId, @Query("tokenId") String tokenId);

    @PUT(REST + USER + "/{userId}" + VISIBILITY)
    Call<Void> updateUserVisibility(@Path("userId") String userId,@Query("visibility") String visibility,
                                    @Query("tokenId") String tokenId);

    @GET(REST + USER + "/{userId}" + HELP)
    Call<List<HelpData>> getUserHelps(@Path("userId") String userId,
                                         @Query("tokenId") String tokenId);

    /**Institution**/

    @POST(REST + INSTITUTION + "/{instId}" + LOGIN )
    Call<LoginInfo> authenticateInstitution(@Path("instId") String instId, @Body LoginCredentials user);

    @DELETE(REST + INSTITUTION + "/{instId}" + LOGOUT)
    Call<Void> logoutInst(@Path("instId") String instId, @QueryName String tokenId);

    @POST(REST + INSTITUTION)
    Call<Void> createInstitution(@Body InstitutionRegisterModel institutionRegisterModel);


    @PUT(REST + INSTITUTION + "/{instId}" + CHANGEPASS)
    Call<Void> changePassInst(@Path("instId") String instId, @Body ChangePass data,
                              @Query("tokenId") String tokenId);


    @PUT(REST + INSTITUTION + "/{instId}" + INFO)
    Call<Void> updateInstInfo(@Path("instId") String instId, @Body UpdateUserModel updateInstModel,
                                @Query("tokenId") String tokenId);

    @GET(REST + INSTITUTION + "/{instId}" + INFO)
    Call<AccountInfo> getInstInfo(@Path("instId") String instId, @Query("tokenId") String tokenId);

    @GET(REST + INSTITUTION + "{instId}" + ACCOUNT)
    Call<Account> getInstAccount(@Path("instId") String instId, @Query("tokenId") String tokenId);

    @PUT(REST + INSTITUTION + "/{instId}" + VISIBILITY)
    Call<Void> updateInstVisibility(@Path("instId") String instId,
                                    @Query("visibility") String visibility,
                                    @Query("tokenId") String tokenId);

    @GET(REST + INSTITUTION + "/{instId}" + EVENTS)
    Call<List<EventData>> getAccountEvents(@Path("instId") String instId,
                                           @Query("tokenId") String tokenId);

    @GET(REST + INSTITUTION + "/{instId}" + HELP)
    Call<List<HelpData>> getInstHelps(@Path("instId") String instId,
                                         @Query("tokenId") String tokenId);

    /**Events**/

    @POST(REST + EVENT)
    Call<String> createEvent(@Query("tokenId") String tokenId, @Body CreateEvent data);

    @GET(REST + EVENT)
    Call<List<EventData>> getEvents(@Query("tokenId") String tokenId);

    @POST(REST + EVENT + "/{eventId}" + JOIN)
    Call<Void> joinEvent(@Path("eventId") String eventId, @Query("tokenId") String tokenId);

    @PUT(REST + EVENT + "/{eventId}" + END)
    Call<Void> finishEvent(@Path("eventId") String eventId, @Query("tokenId") String tokenId);

    @DELETE(REST + EVENT + "/{eventId}")
    Call<Void> cancelEvent(@Path("eventId") String eventId, @Query("tokenId") String tokenId);

    /**Helps**/

    @POST(REST + HELP)
    Call<Void> createHelp(@Query("tokenId") String tokenId, @Body CreateHelp data);

    @GET(REST + HELP)
    Call<List<HelpData>> getHelps(@Query("tokenId") String tokenId);

    @POST(REST + HELP + "/{helpId}" + OFFER)
    Call<Void> offerHelp(@Path("helpId") String help, @Query("tokenId") String tokenId);

    @PUT(REST + HELP + "/{helpId}" + FINISH)
    Call<Void> finishHelp(@Path("helpId") String helpId, @Query("tokenId") String tokenId, @Query("rating") String rating);

    @DELETE(REST + HELP + "/{helpId}")
    Call<Void> cancelHelp(@Path("helpId") String helpId, @Query("tokenId") String tokenId);

    @PUT(REST + HELP + "/{helpId}" + HELPER)
    Call<Void> chooseHelper(@Path("helpId") String helpId, @Query("tokenId") String tokenId,
                            @Query("helperId") String helperId);

    @GET(REST + HELP + "/{helpId}" + HELPER)
    Call<List<HelperStats>> getHelpers(@Path("helpId") String helpId, @Query("tokenId") String tokenId);




}
