package pt.unl.fct.di.apdc.helpinghand.ui.loading;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.ui.login.LoginActivity;
import pt.unl.fct.di.apdc.helpinghand.ui.register.RegisterActivity;

public class StartUserActivity extends AppCompatActivity {

    private Button bt_login, bt_register, bt_about;

    public void onCreate(Bundle saved) {
        super.onCreate(saved);

        setContentView(R.layout.activity_start);

        //Sets buttons
        bt_login = findViewById(R.id.bt_login);
        bt_register = findViewById(R.id.bt_register);
        bt_about = findViewById(R.id.bt_about);

        //About button click listener
        bt_about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StartUserActivity.this, "Developed by PogChampSoftware", Toast.LENGTH_LONG).show();
            }
        });

        //Register button click listener
        bt_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent register = new Intent(StartUserActivity.this, RegisterActivity.class);
                startActivity(register);
            }
        });

        //Login button click listener
        bt_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent login = new Intent(StartUserActivity.this, LoginActivity.class);
                startActivity(login);
            }
        });

    }
}
