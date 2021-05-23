package pt.unl.fct.di.apdc.helpinghand.network;


import pt.unl.fct.di.apdc.helpinghand.data.model.InstitutionRegisterModel;
import pt.unl.fct.di.apdc.helpinghand.data.model.UserAuthenticated;
import pt.unl.fct.di.apdc.helpinghand.data.model.UserCredentials;
import pt.unl.fct.di.apdc.helpinghand.data.model.UserRegisterModel;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;

public interface HelpingHandService {

    String REST = "rest";
    String LOGIN = "login";
    String LOGOUT = "logout";
    String USER = "user";
    String CREATE = "/";
    String INSTITUTION = "institution";

    @POST(LOGIN)
    Call<UserAuthenticated> authenticateUser(@Body UserCredentials user);

    @DELETE(LOGOUT)
    Call<UserAuthenticated> logoutUser(@Body UserCredentials user);


    @POST(REST + CREATE + INSTITUTION + CREATE)
    Call<Void> createInstitution(@Body InstitutionRegisterModel institutionRegisterModel);

    @POST(REST + CREATE + USER + CREATE)
    Call<Void> createUser(@Body UserRegisterModel userModel);

}
