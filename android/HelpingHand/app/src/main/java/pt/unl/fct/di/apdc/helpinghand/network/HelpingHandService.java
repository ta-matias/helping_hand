package pt.unl.fct.di.apdc.helpinghand.network;


import pt.unl.fct.di.apdc.helpinghand.data.model.AccountInfo;
import pt.unl.fct.di.apdc.helpinghand.data.model.ChangePass;
import pt.unl.fct.di.apdc.helpinghand.data.model.CreateEvent;
import pt.unl.fct.di.apdc.helpinghand.data.model.InstitutionRegisterModel;
import pt.unl.fct.di.apdc.helpinghand.data.model.LoginCredentials;
import pt.unl.fct.di.apdc.helpinghand.data.model.LoginInfo;
import pt.unl.fct.di.apdc.helpinghand.data.model.UpdateInstModel;
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

    @POST(REST + USER + "/{userId}" + LOGIN)
    Call<LoginInfo> authenticateUser(@Path("userId") String userId, @Body LoginCredentials user);

    @POST(REST + INSTITUTION + "/{instId}" + LOGIN )
    Call<LoginInfo> authenticateInstitution(@Path("instId") String instId, @Body LoginCredentials user);

    @DELETE(REST + USER + "/{userId}" + LOGOUT)
    Call<Void> logoutUser(@Path("userId") String userId, String tokenId);

    @DELETE(REST + INSTITUTION + "/{instId}" + LOGOUT)
    Call<Void> logoutInst(@Path("instId") String instId, @QueryName String tokenId);

    @POST(REST + INSTITUTION)
    Call<Void> createInstitution(@Body InstitutionRegisterModel institutionRegisterModel);

    @POST(REST + USER)
    Call<Void> createUser(@Body UserRegisterModel userModel);

    @PUT(REST + INSTITUTION + "/{instId}" + CHANGEPASS)
    Call<Void> changePassInst(@Path("instId") String instId, @Body ChangePass data,
                              @Query("tokenId") String tokenId);

    @PUT(REST + USER + "/{userId}" + CHANGEPASS)
    Call<Void> changePassUser(@Path("userId") String userId, @Body ChangePass data,
                              @Query("tokenId") String tokenId);

    @PUT(REST + USER + "/{userId}" + INFO)
    Call<Void> updateUserInfo(@Path("userId") String userId, @Body UpdateUserModel updateUserModel,
                                @Query("tokenId") String tokenId);

    @PUT(REST + INSTITUTION + "/{instId}" + INFO)
    Call<Void> updateInstInfo(@Path("instId") String instId, @Body UpdateUserModel updateInstModel,
                                @Query("tokenId") String tokenId);

    @GET(REST + INSTITUTION + "/{instId}" + INFO)
    Call<AccountInfo> getInstInfo(@Path("instId") String instId, @Query("tokenId") String tokenId);

    @GET(REST + USER + "/{userId}" + INFO)
    Call<AccountInfo> getUserInfo(@Path("userId") String userId, @Query("tokenId") String tokenId);

    @DELETE(REST + USER + "/{userId}")
    Call<Void> deleteUser(@Path("userId") String userId, @Query("tokenId") String tokenId);

    @DELETE(REST + USER + "/{instId}")
    Call<Void> deleteInst(@Path("instId") String instId, @Query("tokenId") String tokenId);

    @POST(REST + EVENT)
    Call<String> createEvent(@Query("token") String token, CreateEvent data);

}
