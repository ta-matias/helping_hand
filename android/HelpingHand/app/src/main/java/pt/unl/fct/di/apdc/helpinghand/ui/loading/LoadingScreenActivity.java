package pt.unl.fct.di.apdc.helpinghand.ui.loading;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.ui.home.HomePageActivity;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;

public class LoadingScreenActivity extends AppCompatActivity {

    private final static int SPLASH_TIMEOUT = 5000;

    private final Configuration configuration = new Configuration();

    private HelpingHandProvider mProvider;
    private AppPreferenceTools mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_screen);

        mProvider = new HelpingHandProvider();
        mPreferences = mProvider.getmAppPreferenceTools();

        //Gets view for powered text
        TextView text = findViewById(R.id.powered);

        //Checks if its in dark mode
        int currentNightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch(currentNightMode){
            //Its in light mode
            case Configuration.UI_MODE_NIGHT_NO:
                //Sets text color to black
                text.setTextColor(0);
                break;
            //Its in dark mode
            case Configuration.UI_MODE_NIGHT_YES:
                //Sets text color to white
                text.setTextColor(111111);
                break;
        }

        new Handler().postDelayed(() ->{
            Intent start;
            if(!mPreferences.isAuthorized()) {
                start = new Intent(LoadingScreenActivity.this, StartUserActivity.class);
            }else{
                start = new Intent(LoadingScreenActivity.this, HomePageActivity.class);
            }
            startActivity(start);
            finish();
        }, SPLASH_TIMEOUT);


    }
}