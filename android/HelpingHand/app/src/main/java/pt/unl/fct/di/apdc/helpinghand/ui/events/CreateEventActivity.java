package pt.unl.fct.di.apdc.helpinghand.ui.events;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.model.MarkerOptions;


import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.CreateEvent;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateEventActivity extends AppCompatActivity implements OnMapReadyCallback, OnMapClickListener {

    private CreateEvent create;

    private HelpingHandService service;
    private AppPreferenceTools preferenceTools;

    private GoogleMap map;
    private PopupWindow pw;
    String local = "";

    final Calendar startCalendar = Calendar.getInstance();
    final Calendar endCalendar = Calendar.getInstance();

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    protected void onCreate(Bundle savedInstance){

        super.onCreate(savedInstance);

        setContentView(R.layout.activity_create_event);

        HelpingHandProvider provider = HelpingHandProvider.getInstance();
        service = provider.getMService();
        preferenceTools = provider.getmAppPreferenceTools();




        create = new CreateEvent();

        ImageButton backButton = findViewById(R.id.create_event_back_btn);

        Button create_event_btn = findViewById(R.id.create_event_btn);

        EditText event_name_et = findViewById(R.id.create_event_name_et);
        EditText event_description_et = findViewById(R.id.create_event_description_et);
        EditText event_start_date_et = findViewById(R.id.create_event_startdate_et);
        EditText event_start_hour_et = findViewById(R.id.create_event_starthour_et);
        EditText event_end_date_et = findViewById(R.id.create_event_enddate_et);
        EditText event_end_hour_et = findViewById(R.id.create_event_endhour_et);
        EditText event_location_et = findViewById(R.id.create_event_location_et);

        event_name_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                create.name = s.toString();

            }
        });

        event_description_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                create.description = s.toString();

            }
        });

        DatePickerDialog.OnDateSetListener sDate = (view, year, month, dayOfMonth) -> {
            startCalendar.set(Calendar.YEAR, year);
            startCalendar.set(Calendar.MONTH, month);
            startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            create.start = dateTimeFormatter.format(startCalendar.getTime().toInstant());
            event_start_date_et.setText(sdf.format(startCalendar.getTime()));
            };

        event_start_date_et.setOnClickListener(v -> {

            new DatePickerDialog(CreateEventActivity.this, sDate,
                    startCalendar.get(Calendar.YEAR),
                    startCalendar.get(Calendar.MONTH),
                    startCalendar.get(Calendar.DAY_OF_MONTH))
                    .show();

        });

        TimePickerDialog.OnTimeSetListener sTime = (view, hourOfDay, minute) -> {

            startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            startCalendar.set(Calendar.MINUTE, minute);
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            create.start = dateTimeFormatter.format(startCalendar.getTime().toInstant());
            event_start_hour_et.setText(sdf.format(startCalendar.getTime()));
        };

        event_start_hour_et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(CreateEventActivity.this, sTime,
                        startCalendar.get(Calendar.HOUR_OF_DAY),
                        startCalendar.get(Calendar.MINUTE),
                        true)
                        .show();
            }
        });

        DatePickerDialog.OnDateSetListener eDate = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                endCalendar.set(Calendar.YEAR, year);
                endCalendar.set(Calendar.MONTH, month);
                endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                create.end = dateTimeFormatter.format(endCalendar.getTime().toInstant());
                event_end_date_et.setText(sdf.format(endCalendar.getTime()));
            }
        };

        event_end_date_et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(CreateEventActivity.this, eDate,
                        endCalendar.get(Calendar.YEAR),
                        endCalendar.get(Calendar.MONTH),
                        endCalendar.get(Calendar.DAY_OF_MONTH))
                        .show();
            }
        });

        TimePickerDialog.OnTimeSetListener eTime = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                endCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                endCalendar.set(Calendar.MINUTE, minute);
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                create.end = dateTimeFormatter.format(endCalendar.getTime().toInstant());
                event_end_hour_et.setText(sdf.format(endCalendar.getTime()));}
        };

        event_end_hour_et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(CreateEventActivity.this, eTime,
                        endCalendar.get(Calendar.HOUR_OF_DAY),
                        endCalendar.get(Calendar.MINUTE),
                        true)
                        .show();
            }
        });

        event_location_et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    LayoutInflater inflater = (LayoutInflater) CreateEventActivity.this.
                            getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View layout = inflater.inflate(R.layout.popup_map_location,
                            findViewById(R.id.pop_map_layout));

                    pw = new PopupWindow(layout, 800, 1900, true);

                    pw.showAtLocation(layout, Gravity.CENTER, 0,0);

                    ((SupportMapFragment)getSupportFragmentManager().
                            findFragmentById(R.id.popup_map_window)).
                            getMapAsync(CreateEventActivity.this);


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

        backButton.setOnClickListener(v -> {
            Intent back = new Intent(CreateEventActivity.this, EventsActivity.class);
            startActivity(back);
            finish();
        });

        create_event_btn.setOnClickListener(v -> {

            Call<String> call = service.createEvent(preferenceTools.getAccessToken(), create);

            Logger.getAnonymousLogger().info(create.name);
            Logger.getAnonymousLogger().info(create.description);
            Logger.getAnonymousLogger().info(create.start);
            Logger.getAnonymousLogger().info(create.end);
            Logger.getAnonymousLogger().info(create.location[0] + ", " + create.location[1]);

            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse( Call<String> call,  Response<String> response) {
                    if(response.isSuccessful()){
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                CreateEventActivity.this);

                        builder.setTitle("Evento criado com sucesso.");

                        builder.setNeutralButton("Ok!", (dialog, which) -> dialog.cancel());

                        AlertDialog dialog = builder.create();
                        dialog.show();

                    }
                    else if (response.code() == 400){
                        Toast.makeText(CreateEventActivity.this,
                                "Bad request check the data", Toast.LENGTH_SHORT).show();
                    }
                    else if (response.code() == 403){
                        Toast.makeText(CreateEventActivity.this,
                                "Request forbidden, try again", Toast.LENGTH_SHORT).show();
                    }
                    else if (response.code() == 404){
                        Toast.makeText(CreateEventActivity.this, "User not found",
                                Toast.LENGTH_SHORT).show();
                    }
                    else if (response.code() == 500){
                        Toast.makeText(CreateEventActivity.this,
                                "Internal server error", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(CreateEventActivity.this);
                    builder.setTitle("Algo de errado aconteceu, tente novamente.");

                    builder.setNeutralButton("Compreendi", (dialog, which) -> dialog.cancel());

                    AlertDialog dialog = builder.create();
                    dialog.show();

                }
            });

        });
    }



    @Override
    public void onMapReady(@NonNull @NotNull GoogleMap googleMap) {

        this.map = googleMap;
        this.map.setOnMapClickListener(this);

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

    @Override
    public void onMapClick(@NonNull @NotNull LatLng latLng) {

        map.clear();
        Geocoder coder = new Geocoder(CreateEventActivity.this);
        try {
            List<Address> addresses = coder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            local = addresses.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }


        map.addMarker(new MarkerOptions().position(latLng));

        create.location = new double[]{latLng.latitude, latLng.longitude};

    }

    public LatLng getLocationFromAddress(String name, Context context){

        Geocoder geocoder = new Geocoder(context);

        List<Address> addressList;

        LatLng point = null;

        try{
            addressList = geocoder.getFromLocationName(name, 10);

            if (addressList == null){
                return null;
            }
            Address location = addressList.get(0);

            point = new LatLng(location.getLatitude(), location.getLongitude());

        }catch (Exception e){
            return null;
        }
        return point;
    }

}
