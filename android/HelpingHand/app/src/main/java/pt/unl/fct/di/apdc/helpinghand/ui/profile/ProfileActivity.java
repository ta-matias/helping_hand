package pt.unl.fct.di.apdc.helpinghand.ui.profile;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.util.logging.Logger;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.Account;
import pt.unl.fct.di.apdc.helpinghand.data.model.AccountInfo;
import pt.unl.fct.di.apdc.helpinghand.data.model.ChangeVisibility;
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

    SwitchCompat visibility;

    Account account = new Account();

    private HelpingHandProvider mProvider;
    private HelpingHandService mService;
    private AppPreferenceTools mPreferences;

    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        mProvider = HelpingHandProvider.getInstance();

        mService = mProvider.getMService();

        mPreferences = mProvider.getmAppPreferenceTools();

        if(mPreferences.getRole().equals("USER"))
            userProfile();
        else
            instProfile();

        //TODO: (possibly change to a fragment to add a bar for navigation at the bottom)

        username_txt.setText(mPreferences.getUsername());

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

        visibility = findViewById(R.id.inst_visibility_stc);

        change_pass_btn = findViewById(R.id.inst_change_pass_btn);

        zip_txt = findViewById(R.id.inst_zip_txt);

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

        Call<Account> accountCall = mService.getInstAccount(mPreferences.getUsername(),
                mPreferences.getAccessToken());

        accountCall.enqueue(new Callback<Account>() {
            @Override
            public void onResponse(Call<Account> call, Response<Account> response) {
                if(response.isSuccessful()){
                    Account account = response.body();
                    username_txt.setText(account.id);
                    email_txt.setText(account.email);
                }
            }

            @Override
            public void onFailure(Call<Account> call, Throwable t) {

            }
        });


        visibility.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked != account.visibility) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);

                    builder.setTitle("Tem a certeza que pretende alterar a visibilidade do perfil?");

                    builder.setNegativeButton("Não", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            visibility.setChecked(account.visibility);
                        }
                    });

                    builder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Logger.getAnonymousLogger().info(Boolean.toString(isChecked) + " " + Boolean.toString(account.visibility));
                            account.visibility = isChecked;

                            Call<Void> call = mService.updateInstVisibility(mPreferences.getUsername(),
                                    Boolean.toString(isChecked), mPreferences.getAccessToken());
                            call.enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        dialog.cancel();
                                    } else if (response.code() == 400) {
                                        Toast.makeText(ProfileActivity.this, "Bad request try again.", Toast.LENGTH_SHORT).show();
                                    } else if (response.code() == 404) {
                                        Toast.makeText(ProfileActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
                                    } else if (response.code() == 500) {
                                        Toast.makeText(ProfileActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {

                                }
                            });

                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();

                }
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

        visibility = findViewById(R.id.user_visibility_stc);



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

                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);

                builder.setTitle("Ocorreu um erro com a operação tente novamente.");

                builder.setNeutralButton("Ok!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();

            }
        });

        Call<Account> accountCall = mService.getUserAccount(mPreferences.getUsername(),
                mPreferences.getAccessToken());

        accountCall.enqueue(new Callback<Account>() {
            @Override
            public void onResponse(Call<Account> call, Response<Account> response) {
                if(response.isSuccessful()){
                    account = response.body();
                    username_txt.setText(account.id);
                    email_txt.setText(account.email);
                    visibility.setChecked(account.visibility);
                }
            }

            @Override
            public void onFailure(Call<Account> call, Throwable t) {

            }
        });

        visibility.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked != account.visibility) {


                    AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);

                    builder.setTitle("Tem a certeza que pretende alterar a visibilidade do perfil?");

                    builder.setNegativeButton("Não", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            visibility.setChecked(account.visibility);
                        }
                    });

                    builder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Logger.getAnonymousLogger().info(Boolean.toString(isChecked) + " " + Boolean.toString(account.visibility));
                            account.visibility = isChecked;

                            Call<Void> call = mService.updateUserVisibility(mPreferences.getUsername(),
                                    Boolean.toString(isChecked), mPreferences.getAccessToken());
                            call.enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        dialog.cancel();
                                    } else if (response.code() == 400) {
                                        Toast.makeText(ProfileActivity.this, "Bad request try again.", Toast.LENGTH_SHORT).show();
                                    } else if (response.code() == 404) {
                                        Toast.makeText(ProfileActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
                                    } else if (response.code() == 500) {
                                        Toast.makeText(ProfileActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {

                                }
                            });

                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                }
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
                        if(mPreferences.getRole().equals("USER"))
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
