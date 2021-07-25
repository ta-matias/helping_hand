package pt.unl.fct.di.apdc.helpinghand.ui.help;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.CreateHelp;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateHelpActivity extends AppCompatActivity implements OnMapReadyCallback,OnMapClickListener  {

    ImageButton backBtn;

    EditText name_et, desc_et, start_date_et, start_hour_et, loc_et;

    Button conf_btn;

    HelpingHandProvider provider;
    HelpingHandService service;
    AppPreferenceTools preferenceTools;

    String local;

    CreateHelp data;

    Calendar calendar = Calendar.getInstance();

    PopupWindow pw;

    GoogleMap map;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    protected void onCreate(Bundle savedInstance) {

        super.onCreate(savedInstance);

        setContentView(R.layout.activity_create_help);

        provider = HelpingHandProvider.getInstance();
        service = provider.getMService();
        preferenceTools = provider.getmAppPreferenceTools();

        data = new CreateHelp();

        /*Setup of the views*/
        backBtn = findViewById(R.id.ch_back_btn);

        name_et = findViewById(R.id.create_help_name_et);
        desc_et = findViewById(R.id.create_help_description_et);
        start_date_et = findViewById(R.id.create_help_startdate_et);
        start_hour_et = findViewById(R.id.create_help_starthour_et);
        loc_et = findViewById(R.id.create_help_location_et);

        conf_btn = findViewById(R.id.create_help_btn);

        /*Setup of the clickListeners for both buttons*/
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent back = new Intent(CreateHelpActivity.this, HelpActivity.class);
                startActivity(back);
                finish();
            }
        });

        conf_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Call<Void> call = service.createHelp(preferenceTools.getAccessToken(),
                        data);

                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        switch (response.code()){
                            case 200:
                                AlertDialog.Builder builder = new AlertDialog.Builder(
                                        CreateHelpActivity.this);
                                builder.setTitle("Pedido de ajuda efetuado com sucesso!").
                                        setNeutralButton("OK!", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                                Intent done = new Intent(
                                                        CreateHelpActivity.this,
                                                        HelpActivity.class);
                                                startActivity(done);
                                                finish();
                                            }
                                        });

                                AlertDialog dialog = builder.create();
                                dialog.show();
                                break;

                            case 400:
                                Toast.makeText(CreateHelpActivity.this,
                                        "Pedido não efetuado devido a erro nos dados.",
                                        Toast.LENGTH_SHORT).show();
                                break;

                            case 403:
                                Toast.makeText(CreateHelpActivity.this,
                                        "O utilizador não está autorizado a realizar esta acção",
                                        Toast.LENGTH_SHORT).show();
                                break;

                            case 404:
                                Toast.makeText(CreateHelpActivity.this,
                                        "O utilizador não foi encontrado.",
                                        Toast.LENGTH_SHORT).show();
                                break;

                            case 500:
                                Toast.makeText(CreateHelpActivity.this,
                                        "Ocorreu um erro no servidor.Tente novamente",
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

        name_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                data.name = s.toString();
            }
        });

        desc_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                data.description = s.toString();
            }
        });

        DatePickerDialog.OnDateSetListener sDate = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                data.time = dateTimeFormatter.format(calendar.getTime().toInstant());
                start_date_et.setText(sdf.format(calendar.getTime()));
            }
        };

        start_date_et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(CreateHelpActivity.this,
                        sDate,
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH))
                        .show();
            }
        });

        TimePickerDialog.OnTimeSetListener sTime = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                calendar.set(Calendar.HOUR_OF_DAY,hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                data.time = dateTimeFormatter.format(calendar.getTime().toInstant());
                start_hour_et.setText(sdf.format(calendar.getTime()));
            }
        };

        start_hour_et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(
                        CreateHelpActivity.this,
                        sTime,
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true)
                        .show();
                ;
            }
        });

        loc_et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    LayoutInflater inflater = (LayoutInflater) CreateHelpActivity.this.
                            getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View layout = inflater.inflate(R.layout.popup_map_location,
                            findViewById(R.id.pop_map_layout));

                    pw = new PopupWindow(layout, 800, 1900, true);

                    pw.showAtLocation(layout, Gravity.CENTER, 0,0);

                    ((SupportMapFragment)getSupportFragmentManager().
                            findFragmentById(R.id.popup_map_window)).
                            getMapAsync(CreateHelpActivity.this);

                    Button confirmBtn = layout.findViewById(R.id.confirm_btn_popup);

                    confirmBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pw.dismiss();
                        }
                    });

                }catch (Exception e){

                }
            }
        });
    }


    @Override
    public void onMapReady(@NonNull @NotNull GoogleMap googleMap) {
        map = googleMap;
        map.setOnMapClickListener(this);

        getLocationPermission();
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    public void onMapClick(@NonNull @NotNull LatLng latLng) {

        map.clear();
        Geocoder coder = new Geocoder(CreateHelpActivity.this);
        try {
            List<Address> addresses = coder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            local = addresses.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }


        map.addMarker(new MarkerOptions().position(latLng));

        data.location = new double[]{latLng.latitude, latLng.longitude};
        loc_et.setText(local);
    }
}
