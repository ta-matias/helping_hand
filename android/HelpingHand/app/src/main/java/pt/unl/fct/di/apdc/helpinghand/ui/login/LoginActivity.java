package pt.unl.fct.di.apdc.helpinghand.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.TokenModel;
import pt.unl.fct.di.apdc.helpinghand.data.model.UserAuthenticated;
import pt.unl.fct.di.apdc.helpinghand.data.model.LoginCredentials;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.ui.home.HomePageActivity;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    /**
     * Variables
     */

    /*Layout related variables*/

    //Spinner for user type
    Spinner sUserType;
    //Username's edit text
    EditText et_username;
    //Password's edit text
    EditText et_password;
    //Login button
    Button btn_sign_in;

    /*Login information variables*/
    String clientId;
    String password;

    /*Network related variables*/
    HelpingHandProvider mProvider;
    HelpingHandService mService;
    AppPreferenceTools mPreferences;

    /*Other variables*/
    String userType;
    LoginCredentials userCredentials;

    public void onCreate(Bundle savedInstance) {

        super.onCreate(savedInstance);

        setContentView(R.layout.activity_login);

        //Sets the provider and the service
        mProvider = new HelpingHandProvider();
        mService = mProvider.getMService();
        mPreferences = mProvider.getmAppPreferenceTools();

        //Sets the views to the correct ones
        sUserType = findViewById(R.id.user_type_login);
        et_username = findViewById(R.id.username);
        et_password = findViewById(R.id.password);
        btn_sign_in = findViewById(R.id.login);

        userCredentials = new LoginCredentials();

        //setup of the spinner
        setupSpinner();

        setupEditTexts();



    }

    private void setupEditTexts() {

        et_username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                clientId = s.toString();
                userCredentials.setClientId(clientId);
                if(password != null)
                    btn_sign_in.setEnabled(true);
            }
        });

        et_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                password = s.toString();
                userCredentials.setPassword(password);
                if(clientId != null)
                    btn_sign_in.setEnabled(true);

            }
        });


        btn_sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("USER LOGIN", "Trying to login user: " + clientId);
                Call<String> call;
                if(userType.equals("Utilizador")){
                    call = mService.authenticateUser(clientId, userCredentials);
                }else{
                    call= mService.authenticateInstitution(clientId, userCredentials);
                }

                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if(response.isSuccessful())
                        { //if response status is OK
                            Log.i("Success", "User logged in with success.");
                            String token = response.message();
                            UserAuthenticated user = new UserAuthenticated(clientId,
                                    new TokenModel(token, System.currentTimeMillis(), token),
                                    userType);
                            mPreferences.saveAuthenticatedInfo(user);

                            Intent home = new Intent(LoginActivity.this, HomePageActivity.class);
                            startActivity(home);
                            finish();
                        }
                        else if (response.code() == 403)
                        { // if response status is FORBIDDEN
                            Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_LONG).show();
                        }
                        else if (response.code() == 400)
                        { // if response status is BAD_REQUEST
                            Toast.makeText(LoginActivity.this, "Data is invalid", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {

                        Toast.makeText(LoginActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });

    }



    private void setupSpinner() {
        List<String> sUserType = Arrays.asList(getResources().getStringArray(R.array.usertype));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sUserType);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        this.sUserType.setAdapter(adapter);

        //item listener
        this.sUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                userType = sUserType.get(position);
                if(userType.equals("Utilizador") || userType.equals("Instituição")) {
                    //Makes view visible but button is not enabled until has correct data
                    et_username.setVisibility(View.VISIBLE);
                    et_password.setVisibility(View.VISIBLE);
                    btn_sign_in.setVisibility(View.VISIBLE);
                    btn_sign_in.setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
