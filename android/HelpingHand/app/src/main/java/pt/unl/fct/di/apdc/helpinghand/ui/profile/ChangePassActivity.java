package pt.unl.fct.di.apdc.helpinghand.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.ChangePass;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePassActivity extends AppCompatActivity {

    private HelpingHandService mService;
    private AppPreferenceTools mPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_change_password);

        HelpingHandProvider mProvider = new HelpingHandProvider();

        mService = mProvider.getMService();

        mPreferences = mProvider.getmAppPreferenceTools();

        EditText old_pass_et = findViewById(R.id.old_pass_et);
        EditText new_pass_et = findViewById(R.id.new_pass_et);
        EditText conf_et = findViewById(R.id.conf_et);

        Button conf_btn = findViewById(R.id.conf_btn);

        ChangePass changePass = new ChangePass();

        conf_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Call<Void> call = null;
                if(mPreferences.getRole().equals("Utilizador")){
                    call = mService.changePassUser(mPreferences.getUsername(),
                            changePass, mPreferences.getAccessToken());
                }else{
                    call = mService.changePassInst(mPreferences.getUsername(),
                            changePass,
                            mPreferences.getAccessToken());
                }

                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if(response.code() == 400){// bad request

                        }else if(response.code() == 404){//Not found

                        }else if(response.code() == 403){//Forbbiden

                        }else if(response.code() == 500){//Server error
                            
                        }else if(response.isSuccessful()){ //response is successfull
                            Toast.makeText(ChangePassActivity.this,
                                    "Password alterada com sucesso.", Toast.LENGTH_SHORT)
                                    .show();
                            Intent passChanged = new Intent(ChangePassActivity.this, 
                                    ProfileActivity.class);
                            startActivity(passChanged);
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {

                    }
                });

            }
        });

        old_pass_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                changePass.oldPassword = s.toString();
            }
        });

        new_pass_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                changePass.newPassword = s.toString();
            }
        });

        conf_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                changePass.confirmation = s.toString();
            }
        });
    }
}
