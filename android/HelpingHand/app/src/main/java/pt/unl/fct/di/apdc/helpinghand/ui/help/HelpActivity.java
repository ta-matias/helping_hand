package pt.unl.fct.di.apdc.helpinghand.ui.help;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.ui.home.HomePageActivity;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;

public class HelpActivity extends AppCompatActivity {

    private Button createBtn;
    private Button offerBtn;
    private Button mineBtn;

    private ImageButton backBtn;

    private HelpingHandProvider provider;
    private AppPreferenceTools preferences;

    protected void onCreate(Bundle savedInstance) {

        super.onCreate(savedInstance);

        setContentView(R.layout.activity_help);

        provider = HelpingHandProvider.getInstance();

        preferences = provider.getmAppPreferenceTools();

        setupButtons();
    }

    private void setupButtons(){

        createBtn = findViewById(R.id.create_help_btn);
        offerBtn = findViewById(R.id.offer_help_btn);
        mineBtn = findViewById(R.id.my_helps);

        backBtn = findViewById(R.id.help_back_btn);

        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent create = new Intent(HelpActivity.this, CreateHelpActivity.class);
                startActivity(create);
                finish();
            }
        });

        offerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent offer = new Intent( HelpActivity.this, OfferHelpActivity.class);
                startActivity(offer);
                finish();
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent back = new Intent(HelpActivity.this, HomePageActivity.class);
                startActivity(back);
                finish();
            }
        });

        mineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mine = new Intent(HelpActivity.this, MyHelpsActivity.class);
                startActivity(mine);
                finish();
            }
        });


    }
}
