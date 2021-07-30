package pt.unl.fct.di.apdc.helpinghand.ui.events;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.EventData;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JoinEventActivity extends AppCompatActivity implements OnMapReadyCallback{

    private SupportMapFragment mapFragment;

    private HelpingHandProvider mProvider;
    private HelpingHandService mService;
    private AppPreferenceTools mPreferences;

    private List<EventData> listOfEvents;

    private PopupWindow pw;

    ImageButton backBtn;

    protected void onCreate(Bundle savedInstance) {

        super.onCreate(savedInstance);

        setContentView(R.layout.activity_join_event);

        mProvider = HelpingHandProvider.getInstance();
        mService = mProvider.getMService();
        mPreferences = mProvider.getmAppPreferenceTools();

        backBtn = findViewById(R.id.je_back_btn);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent back = new Intent(JoinEventActivity.this, EventsActivity.class);
                startActivity(back);
                finish();
            }
        });


        Call<List<EventData>> call = mService.getEvents(mPreferences.getAccessToken());

            call.enqueue(new Callback<List<EventData>>() {
                @Override
                public void onResponse(Call<List<EventData>> call, Response<List<EventData>> response) {
                    if(response.isSuccessful()){
                        listOfEvents = response.body();
                        mapFragment = (SupportMapFragment) getSupportFragmentManager().
                                findFragmentById(R.id.join_event_map);
                        setupMap();
                        Toast.makeText(JoinEventActivity.this, "Eventos Obtidos com sucesso.", Toast.LENGTH_SHORT).show();
                    }
                    else if(response.code() == 400){
                        Toast.makeText(JoinEventActivity.this, "Bad input token try again", Toast.LENGTH_SHORT).show();
                    }
                    else if( response.code() == 500) {
                        Toast.makeText(JoinEventActivity.this, "Internal Server Error", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<List<EventData>> call, Throwable t) {

                }
            });

    }

    private void setupMap() {
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(@NonNull @NotNull GoogleMap googleMap) {

        LatLng lisboa = new LatLng(38.72686773847514, -9.138576068030861);
        
        googleMap.addMarker(new MarkerOptions().position(lisboa));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(lisboa));


        for (EventData event: listOfEvents
        ) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.draggable(false)
                    .title(String.valueOf(event.id))
                    .visible(true)
                    .snippet("Nome: " + event.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    .position(new LatLng(event.location[0], event.location[1]));
            googleMap.addMarker(markerOptions);

        }

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull @NotNull Marker marker) {
                marker.showInfoWindow();
                return true;
            }
        });

        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(@NonNull @NotNull Marker marker) {
                for (EventData event: listOfEvents) {
                    if (String.valueOf(event.id).equals(marker.getTitle())){
                        try {
                            LayoutInflater layoutInflater = (LayoutInflater) JoinEventActivity.this.
                                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            View layout = layoutInflater.inflate(R.layout.adapter_join_event,
                                    findViewById(R.id.adapter_layout));
                            pw = new PopupWindow(layout, 1000, 1200, true);

                            pw.showAtLocation(layout, Gravity.CENTER, 0, 0);

                            TextView eventName = layout.findViewById(R.id.name_adapter);
                            TextView eventCreator = layout.findViewById(R.id.creator_adapter);
                            TextView eventDesc = layout.findViewById(R.id.description_adapter);
                            TextView eventStart = layout.findViewById(R.id.start_adapter);
                            TextView eventEnd = layout.findViewById(R.id.end_adapter);
                            TextView eventLoc = layout.findViewById(R.id.location_adapter);

                            Button joinBtn = layout.findViewById(R.id.join_adapter_btn);

                            if (mPreferences.getRole().equals("USER")) {
                                joinBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Call<Void> call = mService.joinEvent(marker.getTitle(),
                                                mPreferences.getAccessToken());

                                        call.enqueue(new Callback<Void>() {
                                            @Override
                                            public void onResponse(Call<Void> call, Response<Void> response) {
                                                switch (response.code()) {
                                                    case 200:
                                                        pw.dismiss();
                                                        Toast.makeText(JoinEventActivity.this,
                                                                "Juntou-se a este evento",
                                                                Toast.LENGTH_SHORT).show();
                                                        break;

                                                    case 400:
                                                        Toast.makeText(JoinEventActivity.this,
                                                                "Ocorreu um erro no pedido",
                                                                Toast.LENGTH_SHORT).show();
                                                        break;

                                                    case 404:
                                                        Toast.makeText(JoinEventActivity.this,
                                                                "Evento não encontrado tente novamente"
                                                                , Toast.LENGTH_SHORT).show();
                                                        break;

                                                    case 403:
                                                        Toast.makeText(JoinEventActivity.this,
                                                                "Acção não permitida",
                                                                Toast.LENGTH_SHORT).show();
                                                        break;

                                                    case 409:
                                                        Toast.makeText(JoinEventActivity.this,
                                                                "Já está adicionado a este evento",
                                                                Toast.LENGTH_SHORT).show();
                                                        break;

                                                    case 500:
                                                        Toast.makeText(JoinEventActivity.this,
                                                                "Ocorreu um erro no servidor",
                                                                Toast.LENGTH_SHORT).show();
                                                        break;
                                                }

                                            }

                                            @Override
                                            public void onFailure(Call<Void> call, Throwable t) {

                                            }
                                        });
                                    }
                                });
                            }else{
                                joinBtn.setVisibility(View.GONE);
                            }

                            eventName.setText(event.name);
                            eventCreator.setText(event.creator);
                            eventDesc.setText(event.description);
                            eventStart.setText(event.start);
                            eventEnd.setText(event.end);
                            eventLoc.setText(getAddressFromLocation(event.location));
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }


    private String getAddressFromLocation(double[] location){
        Geocoder coder = new Geocoder(JoinEventActivity.this);
        List<Address> addresses;
        try {
            addresses = coder.getFromLocation(location[0], location[1], 1);
            return addresses.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


}
