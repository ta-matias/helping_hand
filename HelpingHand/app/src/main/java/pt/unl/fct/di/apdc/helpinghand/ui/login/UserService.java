package pt.unl.fct.di.apdc.helpinghand.ui.login;



import pt.unl.fct.di.apdc.helpinghand.data.model.UserAuthenticated;
import pt.unl.fct.di.apdc.helpinghand.data.model.UserCredentials;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UserService {
    @POST("rest/login")
    Call<UserAuthenticated> authenticatedUser(@Body UserCredentials userCredentials);


}
