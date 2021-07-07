package pt.unl.fct.di.apdc.helpinghand.ui.home;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.logging.Logger;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.ui.events.EventsActivity;
import pt.unl.fct.di.apdc.helpinghand.ui.loading.StartUserActivity;
import pt.unl.fct.di.apdc.helpinghand.ui.maps.MapsActivity;
import pt.unl.fct.di.apdc.helpinghand.ui.profile.ProfileActivity;
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
            //Profile Button
            case R.id.home_profile:
                i = new Intent(HomePageActivity.this, ProfileActivity.class);
                startActivity(i);
                break;
            //Events Button
            case R.id.home_events:
                i = new Intent(HomePageActivity.this, EventsActivity.class);
                startActivity(i);
                break;

            //Map button
            case R.id.home_maps:
                i = new Intent(HomePageActivity.this, MapsActivity.class);
                startActivity(i);
                break;

            //Search button
            case R.id.home_search:
                if(mPreferences.getRole().equals("Utilizador")){

                }else{

                }
                break;

            //Logout Button
            case R.id.btn_logout:
                if(mPreferences.isAuthorized()){

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Tem a certeza que pretende sair?");
                    builder.setIcon(R.drawable.ic_baseline_warning_24);
                    builder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPreferences.removeAllPrefs();
                            Intent i = new Intent(HomePageActivity.this, StartUserActivity.class);
                            startActivity(i);

                        }
                    });
                    builder.setNeutralButton("Não", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                break;
        }

    }
}
