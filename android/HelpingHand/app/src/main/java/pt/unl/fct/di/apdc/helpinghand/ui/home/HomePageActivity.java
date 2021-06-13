package pt.unl.fct.di.apdc.helpinghand.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.logging.Logger;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.ui.loading.StartUserActivity;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;


public class HomePageActivity extends AppCompatActivity {


    private HelpingHandProvider mProvider;
    private HelpingHandService mService;
    private AppPreferenceTools mPreferences;

    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setContentView(R.layout.activity_home);

        Logger log = Logger.getAnonymousLogger();

        log.info("preparing provider");
        mProvider = new HelpingHandProvider();

        log.info("preparing service");
        mService = mProvider.getMService();

        log.info("preparing preferences");
        mPreferences = mProvider.getmAppPreferenceTools();

        TextView home_name_txt = findViewById(R.id.home_name_txt);

        home_name_txt.setText(mPreferences.getUsername() + "!");

    }

    public void onClick(View v){
        Intent i;
        switch(v.getId()) {
            case R.id.home_profile:
                if(mPreferences.getRole().equals("Utilizador")){

                }else{

                }
                break;

            case R.id.home_events:
                if(mPreferences.getRole().equals("Utilizador")){

                }else{

                }
                break;

            case R.id.home_maps:
                if(mPreferences.getRole().equals("Utilizador")){

                }else{

                }
                break;

            case R.id.home_search:
                if(mPreferences.getRole().equals("Utilizador")){

                }else{

                }
                break;

            case R.id.btn_logout:
                if(mPreferences.isAuthorized()){
                    mPreferences.removeAllPrefs();
                    i = new Intent(HomePageActivity.this, StartUserActivity.class);
                    startActivity(i);
                    finish();
                }
                break;
        }

    }
}
