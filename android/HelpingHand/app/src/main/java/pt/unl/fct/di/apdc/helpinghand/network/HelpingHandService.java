package pt.unl.fct.di.apdc.helpinghand.network;


import pt.unl.fct.di.apdc.helpinghand.data.model.InstitutionRegisterModel;
import pt.unl.fct.di.apdc.helpinghand.data.model.LoginCredentials;
import pt.unl.fct.di.apdc.helpinghand.data.model.UserRegisterModel;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryName;

public interface HelpingHandService {

    String REST = "rest/";
    String LOGIN = "login";
    String LOGOUT = "logout";
    String USER = "user/";
    String CREATE = "/";
    String INSTITUTION = "institution/";

    @POST(REST + USER + "{userId}/" + LOGIN)
    Call<String> authenticateUser(@Path("userId") String userId, @Body LoginCredentials user);

    @POST(REST + INSTITUTION + "{instId}/" + LOGIN )
    Call<String> authenticateInstitution(@Path("instId") String instId, @Body LoginCredentials user);

    @DELETE(REST + USER + "{userId}/" + LOGOUT)
    Call<Void> logoutUser(@Path("userId") String userId, String tokenId);

    @DELETE(REST + INSTITUTION + "{instId}/" + LOGOUT)
    Call<Void> logoutInst(@Path("instId") String instId, @QueryName String tokenId);

    @POST(REST + INSTITUTION)
    Call<Void> createInstitution(@Body InstitutionRegisterModel institutionRegisterModel);

    @POST(REST + USER)
    Call<Void> createUser(@Body UserRegisterModel userModel);

}
