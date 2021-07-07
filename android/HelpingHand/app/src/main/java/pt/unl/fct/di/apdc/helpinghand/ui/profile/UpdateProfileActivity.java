package pt.unl.fct.di.apdc.helpinghand.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.UpdateInstModel;
import pt.unl.fct.di.apdc.helpinghand.data.model.UpdateUserModel;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateProfileActivity extends AppCompatActivity {

    EditText update_inst_name_et;
    EditText update_inst_init_et;
    EditText update_phone_et;
    EditText update_address1_et;
    EditText update_address2_et;
    EditText update_city_et;
    EditText update_zip_et;
    Button conf_update_btn;
    AppPreferenceTools mPreferences;
    HelpingHandService mService;
    HelpingHandProvider mProvider;
    UpdateUserModel userInfo;


    public void onCreate(Bundle savedInstance) {

        super.onCreate(savedInstance);

        setContentView(R.layout.activity_update_user_info);

        mProvider = HelpingHandProvider.getInstance();
        mService= mProvider.getMService();
        mPreferences = mProvider.getmAppPreferenceTools();

        updateAccount();



        userInfo = new UpdateUserModel(" ", " " , " ", " "," ");

        setupButton();



        update_phone_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                userInfo.phone = s.toString();
            }
        });

        update_address1_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                userInfo.address1 = s.toString();
            }
        });

        update_address2_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                userInfo.address2 = s.toString();
            }
        });

        update_city_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                userInfo.city = s.toString();
            }
        });

        update_zip_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                userInfo.zipcode = s.toString();

            }
        });



    }


    private void setupButton(){
        conf_update_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Call<Void> call = null;
                if(mPreferences.getRole().equals("Utilizador")){
                    call = mService.updateUserInfo(mPreferences.getUsername(), userInfo,
                            mPreferences.getAccessToken());
                }else{
                    call = mService.updateInstInfo(mPreferences.getUsername(), userInfo,
                            mPreferences.getAccessToken());

                }

                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if(response.isSuccessful()){
                            Toast.makeText(UpdateProfileActivity.this,
                                    "Update made successfully", Toast.LENGTH_LONG).show();
                            mPreferences.updateUserInfo(mPreferences.getUsername(),
                                    mPreferences.getAccessToken(), mPreferences.getRole(),
                                    mPreferences.getCreationDate(), mPreferences.getRefreshToken(),
                                    userInfo);

                            Intent profileUpdated = new Intent(UpdateProfileActivity.this, ProfileActivity.class);
                            startActivity(profileUpdated);
                            finish();
                        }
                        else if(response.code() == 400){
                            Toast.makeText(UpdateProfileActivity.this,
                                    "Bad Request check if data is correct",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else if(response.code() == 404) {
                            Toast.makeText(UpdateProfileActivity.this, "User not found",
                                    Toast.LENGTH_SHORT).show();
                        }else if(response.code() == 500) {
                            Toast.makeText(UpdateProfileActivity.this,
                                    "Internal server error", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {

                    }
                });
            }
        });
    }


    private void updateAccount() {
        setContentView(R.layout.activity_update_user_info);

        update_phone_et = findViewById(R.id.update_phone_et);
        update_address1_et = findViewById(R.id.update_address1_et);
        update_address2_et = findViewById(R.id.update_address2_et);
        update_city_et = findViewById(R.id.update_city_et);
        update_zip_et = findViewById(R.id.update_zip_et);

        conf_update_btn = findViewById(R.id.conf_update_btn);


    }
}
