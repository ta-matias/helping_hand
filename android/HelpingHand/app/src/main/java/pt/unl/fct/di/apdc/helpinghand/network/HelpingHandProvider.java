package pt.unl.fct.di.apdc.helpinghand.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import pt.unl.fct.di.apdc.helpinghand.HelpingHandApp;
import pt.unl.fct.di.apdc.helpinghand.data.model.RefreshTokenRequestModel;
import pt.unl.fct.di.apdc.helpinghand.data.model.TokenModel;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;
import pt.unl.fct.di.apdc.helpinghand.utility.ClientConfigs;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HelpingHandProvider {

    private HelpingHandService mService;
    private AppPreferenceTools mAppPreferenceTools;
    private Retrofit mRetrofit;
    private static HelpingHandProvider instance = null;

    public HelpingHandProvider() {
        this.mAppPreferenceTools = new AppPreferenceTools(HelpingHandApp.applicationContext);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Log.i("request JSON", original.method());
                String originalPath = original.url().url().getPath();
                if(originalPath.endsWith("refreshToken"))
                    return chain.proceed(original);
                else{
                    Request.Builder requestBuilder = original.newBuilder();
                    requestBuilder.addHeader("Accept","application/json");
                    if(mAppPreferenceTools.isAuthorized())
                        requestBuilder.addHeader("Authorization", "bearer" +
                                mAppPreferenceTools.getAccesToken());
                    requestBuilder.method(original.method(), original.body());
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                }
            }
        });


        Gson gson = new GsonBuilder()
                .create();

        mRetrofit = new Retrofit.Builder()
                .baseUrl(ClientConfigs.REST_API_URL)
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        mService = mRetrofit.create(HelpingHandService.class);
    }

    public static HelpingHandProvider getInstance(){
        if(instance == null)
            instance = new HelpingHandProvider();
        return instance;
    }

    public Retrofit getMRetrofit() {
        return mRetrofit;
    }

    public HelpingHandService getMService() {
        return mService;
    }

    public AppPreferenceTools getmAppPreferenceTools(){
        return mAppPreferenceTools;
    }
}
