package pt.unl.fct.di.apdc.helpinghand.ui.register;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;


import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.ErrorModel;
import pt.unl.fct.di.apdc.helpinghand.data.model.InstitutionRegisterModel;
import pt.unl.fct.di.apdc.helpinghand.data.model.UserRegisterModel;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.ui.loading.StartUserActivity;
import pt.unl.fct.di.apdc.helpinghand.utility.ErrorUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    /**
     * Layout element variables
     */

    //User type spinner to select user
    private Spinner userType;

    //String of type selected
    private String typeSelected;

    //Name of institution textView
    private TextView txt_name;

    //Name of institution editText
    private EditText et_name;

    //Username textView
    private TextView txt_username;

    //Username editText
    private EditText et_username;

    //Initials textView
    private TextView txt_initials;

    //Initials editText
    private EditText et_initials;

    //Email textView
    private TextView txt_email;

    //Email editText
    private EditText et_email;

    //Email info warning
    private TextView et_email_info;

    //Password textView
    private TextView txt_password;

    //Password editText
    private EditText et_password;

    //Password info warning
    private TextView et_password_info;

    //Confirm password textView
    private TextView txt_confirm_password;

    //Confirm password editText
    private EditText et_confirm_password;

    //Confirm Password info warning
    private TextView et_confirm_info;

    //Register button
    private Button btn_register;

    /**
     * Register Variables
     */
    String name = "";
    String username = "";
    String initials = "";
    String email = "";
    String password = "";
    String confirmPassword = "";

    /**
     * REST related variables
     */
    HelpingHandProvider helpingHandProvider;
    HelpingHandService service;

    private final Logger LOG = Logger.getAnonymousLogger();

    private final String EMAIL_REGEX = "^([_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)(\\.[a-zA-Z]{2,6}))?$";
    private static final Pattern PASSWORD_REGEX = Pattern.compile("^"
            + "(?=.*[0-9])"
            + "(?=.*[a-z])"
            + "(?=.*[A-Z])"
            + "(?=.*[!@#&()–{}:;',?/*~$^+=<>])"
            + ".{8,20}"
            + "$");

    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);

        setContentView(R.layout.activity_register);

        //Setup of all the text objects
        userType = findViewById(R.id.user_type);
        txt_name = findViewById(R.id.txt_name);
        et_name = findViewById(R.id.et_name);
        txt_username = findViewById(R.id.username_txt);
        et_username = findViewById(R.id.et_username);
        txt_initials = findViewById(R.id.txt_initials);
        et_initials = findViewById(R.id.et_initials);
        txt_email = findViewById(R.id.txt_email);
        et_email = findViewById(R.id.et_email);
        et_email_info = findViewById(R.id.et_email_info);
        txt_password = findViewById(R.id.txt_password);
        et_password = findViewById(R.id.et_password);
        et_password_info = findViewById(R.id.et_pass_info);
        txt_confirm_password = findViewById(R.id.txt_confirm_password);
        et_confirm_password = findViewById(R.id.et_confirm_password);
        et_confirm_info = findViewById(R.id.et_confirm_info);
        btn_register = findViewById(R.id.btn_register);

        btn_register.setEnabled(false);
        et_email_info.setVisibility(View.GONE);
        et_password_info.setVisibility(View.GONE);
        et_confirm_info.setVisibility(View.GONE);

        //Prepares the retrofit service and shared preferences
        helpingHandProvider = new HelpingHandProvider();
        service = helpingHandProvider.getMService();

        setupSpinner();

        userType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                typeSelected = (String) parent.getItemAtPosition(position);
                if(typeSelected.equals("Utilizador"))
                    registerUser();
                else if(typeSelected.equals("Instituição"))
                    registerInstitution();
                else{
                    txt_name.setVisibility(View.INVISIBLE);
                    et_name.setVisibility(View.INVISIBLE);
                    txt_username.setVisibility(View.INVISIBLE);
                    et_username.setVisibility(View.INVISIBLE);
                    txt_initials.setVisibility(View.INVISIBLE);
                    et_initials.setVisibility(View.INVISIBLE);
                    txt_email.setVisibility(View.INVISIBLE);
                    et_email.setVisibility(View.INVISIBLE);
                    txt_password.setVisibility(View.INVISIBLE);
                    et_password.setVisibility(View.INVISIBLE);
                    et_password_info.setVisibility(View.INVISIBLE);
                    txt_confirm_password.setVisibility(View.INVISIBLE);
                    et_confirm_password.setVisibility(View.INVISIBLE);
                    et_confirm_info.setVisibility(View.INVISIBLE);
                    btn_register.setVisibility(View.INVISIBLE);

                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (typeSelected.equals("Utilizador")) {
                    LOG.info("Trying to register user with username: " + username + ", email: " + email);
                    UserRegisterModel userRegisterModel = new UserRegisterModel(username, email, password, confirmPassword);
                    Call<Void> call = service.createUser(userRegisterModel);

                    call.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if(response.isSuccessful()){
                                LOG.info(response.message());
                                startActivity(new Intent(getBaseContext(), StartUserActivity.class));
                                finish();
                            }
                            else{
                                ErrorModel model = ErrorUtils.parseError(response);
                                Toast.makeText(getBaseContext(), "Error type is "
                                        + model.type + ", description " + model.description,
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(getBaseContext(), "Fail >>" + t.getMessage(), Toast.LENGTH_LONG).show();

                        }
                    });

                }else{
                    LOG.info("Trying to register institution");
                    InstitutionRegisterModel institutionRegisterModel = new InstitutionRegisterModel(name, username, initials, email, password, confirmPassword);
                    Call<Void> call = service.createInstitution(institutionRegisterModel);

                    call.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                           if(response.isSuccessful()){
                               LOG.info("Institution registered");
                               startActivity(new Intent(getBaseContext(), StartUserActivity.class));
                               finish();
                           }
                           else{
                               ErrorModel model = ErrorUtils.parseError(response);
                               Toast.makeText(getBaseContext(), "Error type is "
                                               + model.type + ", description " + model.description,
                                       Toast.LENGTH_LONG).show();
                           }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(getBaseContext(), "Fail >>" + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }


            }
        });

        et_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                name = s.toString();
                if (!name.equals("") && !username.equals("") && !initials.equals("") && !email.equals("") && !password.equals("") && !confirmPassword.equals("")) {
                    btn_register.setEnabled(true);
                }
            }
        });

        et_username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                username = s.toString();
                if(typeSelected.equals("Instituição")) {
                    if (!name.equals("") && !username.equals("") && !initials.equals("") && !email.equals("") && !password.equals("") && !confirmPassword.equals("")) {
                        btn_register.setEnabled(true);
                    }
                }else if(typeSelected.equals("Utilizador")){
                    if (!username.equals("")  && !email.equals("")
                            && !password.equals("")
                            && !confirmPassword.equals("")) {
                        btn_register.setEnabled(true);
                    }
                }
            }
        });

        et_initials.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                initials = s.toString();
                if (!name.equals("") && !username.equals("") && !initials.equals("") && !email.equals("") && !password.equals("") && !confirmPassword.equals("")) {
                    btn_register.setEnabled(true);
                }
            }
        });

        et_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(Patterns.EMAIL_ADDRESS.matcher(s.toString()).matches()) {
                    email = s.toString();
                    if(typeSelected.equals("Instituição")) {
                        if (!name.equals("") && !username.equals("") && !initials.equals("") && !email.equals("") && !password.equals("") && !confirmPassword.equals("")) {
                            btn_register.setEnabled(true);
                        }
                    }else if(typeSelected.equals("Utilizador")){
                        if (!username.equals("") && !email.equals("") && !password.equals("") && !confirmPassword.equals("")) {
                            btn_register.setEnabled(true);
                        }
                    }
                    et_email_info.setVisibility(View.GONE);
                }else{
                    et_email_info.setVisibility(View.VISIBLE);
                }

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
                if (PASSWORD_REGEX.matcher(s.toString()).matches()) {
                    password = s.toString();
                    if(typeSelected.equals("Instituição")) {
                        if (!name.equals("") && !username.equals("") && !initials.equals("") && !email.equals("") && !password.equals("") && !confirmPassword.equals("")) {
                            btn_register.setEnabled(true);
                        }
                    }else if(typeSelected.equals("Utilizador")){
                        if (!username.equals("") && !email.equals("") && !password.equals("") && !confirmPassword.equals("")) {
                            btn_register.setEnabled(true);
                        }
                    }
                    et_password_info.setVisibility(View.GONE);

                }else
                    et_password_info.setVisibility(View.VISIBLE);
            }
        });

        et_confirm_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals(password)) {
                    confirmPassword = s.toString();
                    if(typeSelected.equals("Instituição")) {
                        if (!name.equals("") && !username.equals("") && !initials.equals("") && !email.equals("") && !password.equals("") && !confirmPassword.equals("")) {
                            btn_register.setEnabled(true);
                        }
                    }else if(typeSelected.equals("Utilizador")){
                        if (!username.equals("") && !email.equals("") && !password.equals("") && !confirmPassword.equals("")) {
                            btn_register.setEnabled(true);
                        }
                    }
                    et_confirm_info.setVisibility(View.GONE);
                }else{
                    et_confirm_info.setVisibility(View.VISIBLE);
                }

            }
        });



    }

    private void setupSpinner(){
        List<String> sUserType = Arrays.asList(getResources().getStringArray(R.array.usertype));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sUserType);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userType.setAdapter(adapter);

    }

    private void registerUser(){
        //Sets the visibility of all elements to visible
        txt_username.setVisibility(View.VISIBLE);
        et_username.setVisibility(View.VISIBLE);
        txt_username.setText("Username");
        txt_email.setVisibility(View.VISIBLE);
        et_email.setVisibility(View.VISIBLE);
        txt_password.setVisibility(View.VISIBLE);
        et_password.setVisibility(View.VISIBLE);
        txt_confirm_password.setVisibility(View.VISIBLE);
        et_confirm_password.setVisibility(View.VISIBLE);
        btn_register.setVisibility(View.VISIBLE);


        //Sets visibility of the unnecessary elements gone
        txt_name.setVisibility(View.GONE);
        et_name.setVisibility(View.GONE);
        txt_initials.setVisibility(View.GONE);
        et_initials.setVisibility(View.GONE);
        et_email_info.setVisibility(View.GONE);
        et_password_info.setVisibility(View.GONE);
        et_confirm_info.setVisibility(View.GONE);

    }

    private void registerInstitution(){
        //Sets the visibility of all elements to visible
        txt_name.setVisibility(View.VISIBLE);
        et_name.setVisibility(View.VISIBLE);
        txt_username.setVisibility(View.VISIBLE);
        txt_username.setText("Id da Instituição");
        et_username.setVisibility(View.VISIBLE);
        txt_initials.setVisibility(View.VISIBLE);
        et_initials.setVisibility(View.VISIBLE);
        txt_email.setVisibility(View.VISIBLE);
        et_email.setVisibility(View.VISIBLE);
        txt_password.setVisibility(View.VISIBLE);
        et_password.setVisibility(View.VISIBLE);
        txt_confirm_password.setVisibility(View.VISIBLE);
        et_confirm_password.setVisibility(View.VISIBLE);
        btn_register.setVisibility(View.VISIBLE);

        //Sets visibility of the unnecessary elements gone
        et_email_info.setVisibility(View.GONE);
        et_password_info.setVisibility(View.GONE);
        et_confirm_info.setVisibility(View.GONE);
    }
}
