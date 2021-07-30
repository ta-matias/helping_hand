package pt.unl.fct.di.apdc.helpinghand.ui.events;



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

public class EventsActivity extends AppCompatActivity {

    HelpingHandProvider provider;
    AppPreferenceTools preferences;

    Button create_event_btn;
    Button join_event_btn;
    Button mine_events_btn;

    ImageButton backBtn;

    public void onCreate(Bundle savedInstance) {

        super.onCreate(savedInstance);

        setContentView(R.layout.activity_events);

        provider = HelpingHandProvider.getInstance();
        preferences = provider.getmAppPreferenceTools();

        setupButtons();

        setupClickListeners();

        setupViews();
    }

    private void setupButtons(){

        create_event_btn = findViewById(R.id.create_event_btn);

        join_event_btn = findViewById(R.id.join_event_btn);

        mine_events_btn = findViewById(R.id.mine_events_btn);

        backBtn = findViewById(R.id.ev_back_btn);

    }

    private void setupClickListeners(){

        create_event_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent createEvent = new Intent(EventsActivity.this,
                        CreateEventActivity.class);
                startActivity(createEvent);
                finish();

            }
        });

        join_event_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent joinEvent = new Intent(EventsActivity.this,
                        JoinEventActivity.class);
                startActivity(joinEvent);
                finish();
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent back = new Intent(EventsActivity.this, HomePageActivity.class);
                startActivity(back);
                finish();
            }
        });

        mine_events_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mine = new Intent(EventsActivity.this, MyEventsActivity.class);
                startActivity(mine);
                finish();
            }
        });

       
    }

    private void setupViews(){
        if(preferences.getRole().equals("USER")){
            create_event_btn.setVisibility(View.GONE);
            join_event_btn.setVisibility(View.VISIBLE);
            mine_events_btn.setVisibility(View.GONE);
        }
        else{
            create_event_btn.setVisibility(View.VISIBLE);
            join_event_btn.setVisibility(View.VISIBLE);
            mine_events_btn.setVisibility(View.VISIBLE);
        }

    }



}
