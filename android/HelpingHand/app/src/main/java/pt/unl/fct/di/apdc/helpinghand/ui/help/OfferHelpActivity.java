package pt.unl.fct.di.apdc.helpinghand.ui.help;

import android.content.Context;
import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import pt.unl.fct.di.apdc.helpinghand.data.model.HelpData;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.ui.events.JoinEventActivity;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OfferHelpActivity extends AppCompatActivity implements OnMapReadyCallback {

    HelpingHandProvider provider;
    HelpingHandService service;
    AppPreferenceTools preferenceTools;

    List<HelpData> listOfHelps;

    SupportMapFragment mapFragment;

    ImageButton backBtn;

    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);

        setContentView(R.layout.activity_offer_help);

        provider = HelpingHandProvider.getInstance();
        service = provider.getMService();
        preferenceTools = provider.getmAppPreferenceTools();

        backBtn = findViewById(R.id.oh_back_btn);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent back = new Intent(OfferHelpActivity.this, HelpActivity.class);
                startActivity(back);
                finish();
            }
        });

        Call<List<HelpData>> call = service.getHelps(preferenceTools.getAccessToken());

        call.enqueue(new Callback<List<HelpData>>() {
            @Override
            public void onResponse(Call<List<HelpData>> call, Response<List<HelpData>> response) {
                switch (response.code()){
                    case 200:
                        Toast.makeText(OfferHelpActivity.this,
                                "Ajudas obtidas com sucesso",
                                Toast.LENGTH_SHORT).show();
                        listOfHelps = response.body();
                        mapFragment = (SupportMapFragment) getSupportFragmentManager().
                                findFragmentById(R.id.offer_help_map);
                        mapFragment.getMapAsync(OfferHelpActivity.this);
                        break;

                    case 400:
                        Toast.makeText(OfferHelpActivity.this,
                                "Ajudas não foram obtidas devido a erro no token.",
                                Toast.LENGTH_SHORT).show();
                        break;

                    case 500:
                        Toast.makeText(OfferHelpActivity.this,
                                "Ajudas não foram obtidas devido a um erro no servidor",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void onFailure(Call<List<HelpData>> call, Throwable t) {

            }
        });
    }

    @Override
    public void onMapReady(@NonNull @NotNull GoogleMap googleMap) {
        for(HelpData help: listOfHelps){
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.draggable(false)
                    .title(String.valueOf(help.id))
                    .visible(true)
                    .snippet(help.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .position(new LatLng(help.location[0], help.location[1]));
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

                for(HelpData data: listOfHelps){
                    if(String.valueOf(data.id).equals(marker.getTitle())){
                        try{
                            LayoutInflater inflater = (LayoutInflater) OfferHelpActivity.this.
                                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            View layout = inflater.inflate(R.layout.popup_help_info,
                                    findViewById(R.id.offer_help_popup));

                            PopupWindow pw = new PopupWindow(layout, 1000,1200,
                                    true);

                            pw.showAtLocation(layout, Gravity.CENTER, 0,0);

                            TextView helpName = layout.findViewById(R.id.help_name);
                            TextView helpCreator = layout.findViewById(R.id.help_creator);
                            TextView helpDesc = layout.findViewById(R.id.help_desc);
                            TextView helpStart = layout.findViewById(R.id.help_time);
                            TextView helpLoc = layout.findViewById(R.id.help_loc);

                            Button offerHelp = layout.findViewById(R.id.pop_offer_help_btn);

                            helpName.setText(data.name);
                            helpCreator.setText(data.creator);
                            helpDesc.setText(data.description);
                            helpStart.setText(data.time);
                            helpLoc.setText((getAddressFromLocation(data.location)));

                            offerHelp.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Call<Void> call = service.offerHelp(marker.getTitle(),
                                            preferenceTools.getAccessToken());

                                    call.enqueue(new Callback<Void>() {
                                        @Override
                                        public void onResponse(Call<Void> call,
                                                               Response<Void> response) {
                                            switch (response.code()){
                                                case 200:
                                                    AlertDialog.Builder builder =
                                                            new AlertDialog.Builder(
                                                                    OfferHelpActivity.this);
                                                    builder.setTitle("Ajuda oferecida com sucesso")
                                                            .setNeutralButton("OK!", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    dialog.cancel();
                                                                    pw.dismiss();
                                                                }
                                                            });
                                                    AlertDialog dialog = builder.create();
                                                    dialog.show();
                                                    break;

                                                case 400:
                                                    Toast.makeText(OfferHelpActivity.this,
                                                            "Ocorreu um erro ao efetuar o pedido",
                                                            Toast.LENGTH_SHORT)
                                                            .show();
                                                    break;

                                                case 404:
                                                    Toast.makeText(OfferHelpActivity.this,
                                                            "A ajuda que escolheu ou a conta utilizada não foi encontrada",
                                                            Toast.LENGTH_SHORT)
                                                            .show();
                                                    break;

                                                case 403:
                                                    Toast.makeText(OfferHelpActivity.this,
                                                            "Não está autorizado a efetuar essa operação",
                                                            Toast.LENGTH_SHORT).show();
                                                    break;

                                                case 500:
                                                    Toast.makeText(OfferHelpActivity.this,
                                                            "Ocorreu um erro no servidor por favor tente novamente",
                                                            Toast.LENGTH_SHORT)
                                                            .show();
                                                    break;

                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<Void> call, Throwable t) {

                                        }
                                    });
                                }
                            });
                        }catch(Exception e){

                        }
                    }
                }

            }
        });
    }

    private String getAddressFromLocation(double[] location){
        Geocoder coder = new Geocoder(this);
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
