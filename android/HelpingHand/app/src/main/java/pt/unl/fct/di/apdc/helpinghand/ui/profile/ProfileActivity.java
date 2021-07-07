package pt.unl.fct.di.apdc.helpinghand.ui.profile;

import android.accounts.Account;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.AccountInfo;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.ui.loading.StartUserActivity;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private Button change_pass_btn;
    private Button update_btn;
    private Button delete_acc_btn;

    private TextView username_txt;
    private TextView email_txt;
    private TextView phone_txt;
    private TextView address_txt;
    private TextView address2_txt;
    private TextView city_txt;
    private TextView zip_txt;

    private TextView inst_name_txt;
    private TextView inst_init_txt;
    private TextView inst_stat_txt;

    private HelpingHandProvider mProvider;
    private HelpingHandService mService;
    private AppPreferenceTools mPreferences;

    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        mProvider = HelpingHandProvider.getInstance();

        mService = mProvider.getMService();

        mPreferences = mProvider.getmAppPreferenceTools();

        if(mPreferences.getRole().equals("Utilizador"))
            userProfile();
        else
            instProfile();

        //TODO: (possibly change to a fragment to add a bar for navigation at the bottom)



        setClickListeners();
    }

    private void instProfile() {

        setContentView(R.layout.activity_profile_institution);

        username_txt = findViewById(R.id.inst_id_txt);

        email_txt = findViewById(R.id.inst_email_txt);

        inst_name_txt = findViewById(R.id.inst_name_txt);

        inst_init_txt = findViewById(R.id.inst_init_txt);

        address_txt = findViewById(R.id.inst_addr_txt);

        address2_txt = findViewById(R.id.inst_addr2_txt);

        city_txt = findViewById(R.id.inst_city_txt);

        phone_txt = findViewById(R.id.inst_phone_txt);

        inst_stat_txt = findViewById(R.id.inst_status_txt);

        change_pass_btn = findViewById(R.id.inst_change_pass_btn);

        update_btn = findViewById(R.id.inst_update_btn);

        delete_acc_btn = findViewById(R.id.delete_inst_acc_btn);

        Call<AccountInfo> call = mService.getInstInfo(mPreferences.getUsername(),
                mPreferences.getAccessToken());

        call.enqueue(new Callback<AccountInfo>() {
            @Override
            public void onResponse(Call<AccountInfo> call, Response<AccountInfo> response) {
                if(response.isSuccessful()){
                    AccountInfo info = response.body();
                    phone_txt.setText(info.phone);
                    address_txt.setText(info.address1);
                    address2_txt.setText(info.address2);
                    city_txt.setText(info.city);
                    zip_txt.setText(info.zipcode);
                }
            }

            @Override
            public void onFailure(Call<AccountInfo> call, Throwable t) {

            }
        });
    }

    private void userProfile() {

        setContentView(R.layout.activity_profile_user);

        username_txt = findViewById(R.id.user_username_txt);

        email_txt = findViewById(R.id.user_email_txt);

        phone_txt = findViewById(R.id.user_phone_txt);

        address_txt = findViewById(R.id.user_address_txt);

        address2_txt = findViewById(R.id.user_address2_txt);

        city_txt = findViewById(R.id.user_city_txt);

        zip_txt = findViewById(R.id.user_zip_txt);

        change_pass_btn = findViewById(R.id.user_change_pass_btn);

        update_btn = findViewById(R.id.user_update_btn);

        delete_acc_btn = findViewById(R.id.delete_user_acc_btn);

        Call<AccountInfo> call = mService.getUserInfo(mPreferences.getUsername(),
                mPreferences.getAccessToken());

        call.enqueue(new Callback<AccountInfo>() {
            @Override
            public void onResponse(Call<AccountInfo> call, Response<AccountInfo> response) {
                if(response.isSuccessful()){
                    AccountInfo info = response.body();
                    phone_txt.setText(info.phone);
                    address_txt.setText(info.address1);
                    address2_txt.setText(info.address2);
                    city_txt.setText(info.city);
                    zip_txt.setText(info.zipcode);
                }
            }

            @Override
            public void onFailure(Call<AccountInfo> call, Throwable t) {

            }
        });

    }

    private void setClickListeners() {

        change_pass_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent changePassword = new Intent(ProfileActivity.this, ChangePassActivity.class);
                startActivity(changePassword);
                finish();
            }
        });

        update_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent updateUser = new Intent(ProfileActivity.this, UpdateProfileActivity.class);
                startActivity(updateUser);
                finish();
            }
        });

        delete_acc_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);

                builder.setTitle("Deseja mesmo apagar a conta?");

                builder.setIcon(R.drawable.ic_delete);

                builder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Call<Void> call = null;
                        if(mPreferences.getRole().equals("Utilizador"))
                            call = mService.deleteUser(mPreferences.getUsername(),
                                    mPreferences.getAccessToken());
                        else
                            call = mService.deleteInst(mPreferences.getUsername(),
                                    mPreferences.getAccessToken());

                        call.enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if(response.isSuccessful()){
                                    mPreferences.removeAllPrefs();
                                    Intent deleted = new Intent(ProfileActivity.this,
                                            StartUserActivity.class);
                                    startActivity(deleted);
                                    finish();
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {

                            }
                        });
                    }
                });

                builder.setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog alert = builder.create();

                alert.show();
            }
        });


    }
}
