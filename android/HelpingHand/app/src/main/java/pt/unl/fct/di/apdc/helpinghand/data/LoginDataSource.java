package pt.unl.fct.di.apdc.helpinghand.data;

import pt.unl.fct.di.apdc.helpinghand.data.model.LoggedInUser;
import pt.unl.fct.di.apdc.helpinghand.data.model.UserAuthenticated;
import pt.unl.fct.di.apdc.helpinghand.data.model.UserCredentials;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    private HelpingHandService service;

    public LoginDataSource(){

    }

    public Result<LoggedInUser> login(String username, String password) {

        Call<UserAuthenticated> userAuthenticationCall = service.authenticateUser(new UserCredentials(username, password));
        try {
            Response<UserAuthenticated> response = userAuthenticationCall.execute();
            if (response.isSuccessful()) {
                UserAuthenticated ua = response.body();
                return new Result.Success<>(new LoggedInUser(ua.getTokenID().getTokenId(),ua.getUsername()));
            }
            return new Result.Error(new Exception(response.errorBody().toString()));
        } catch (IOException e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }
}