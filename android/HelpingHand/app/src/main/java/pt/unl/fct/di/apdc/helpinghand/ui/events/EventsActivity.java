package pt.unl.fct.di.apdc.helpinghand.ui.events;


import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.Locale;
import java.util.SimpleTimeZone;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.CreateEvent;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RequiresApi(api = Build.VERSION_CODES.O)
public class EventsActivity extends AppCompatActivity {

    Button create_event_btn;
    Button join_event_btn;
    Button mine_events_btn;
    
    //Create events
    EditText create_event_name;
    EditText create_event_description;
    EditText create_event_startdate;
    EditText create_event_starthour;
    EditText create_event_enddate;
    EditText create_event_endhour;
    EditText create_event_location;
    EditText create_event_conditions;
    Button confirm_create_btn;
    String startDate;
    String startHour;
    String endDate;
    String endHour;
    String format = "YYYY/MM/DD";
    Calendar startCalendar = Calendar.getInstance();
    Calendar endCalendar = Calendar.getInstance();
    CreateEvent createEvent;
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);

    HelpingHandProvider mProvider;
    HelpingHandService mService;
    AppPreferenceTools mPreferences;

    public void onCreate(Bundle savedInstance) {

        super.onCreate(savedInstance);

        setContentView(R.layout.activity_events);

        mProvider = HelpingHandProvider.getInstance();
        mService = mProvider.getMService();
        mPreferences = mProvider.getmAppPreferenceTools();

        setupButtons();

        setupClickListeners();
    }

    private void setupButtons(){

        create_event_btn = findViewById(R.id.create_event_btn);

        join_event_btn = findViewById(R.id.join_event_btn);

        mine_events_btn = findViewById(R.id.mine_events_btn);

    }

    private void setupClickListeners(){

        create_event_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.activity_create_event);

                create_event_name = findViewById(R.id.create_event_name_et);
                create_event_description = findViewById(R.id.create_event_description_et);
                create_event_startdate = findViewById(R.id.create_event_startdate_et);
                create_event_starthour = findViewById(R.id.create_event_starthour_et);
                create_event_enddate = findViewById(R.id.create_event_enddate_et);
                create_event_endhour = findViewById(R.id.create_event_endhour_et);
                create_event_location = findViewById(R.id.create_event_location_et);
                create_event_conditions = findViewById(R.id.create_event_conditions_et);
                confirm_create_btn = findViewById(R.id.create_event_btn);

                
                createEvent();

            }
        });

        join_event_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.activity_join_event);
            }
        });

       
    }

    private void createEvent() {

        create_event_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                createEvent.name = s.toString();

            }
        });

        create_event_description.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                createEvent.description = s.toString();

            }
        });

        create_event_startdate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                startDate = s.toString();

            }
        });

        DatePickerDialog.OnDateSetListener sDate = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                startCalendar.set(Calendar.YEAR, year);
                startCalendar.set(Calendar.MONTH, month);
                startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateStartDate();
            }
        };

        create_event_startdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               new DatePickerDialog(EventsActivity.this, sDate,
                       startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH),
                       startCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        create_event_starthour.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                startHour = s.toString();

            }
        });

        TimePickerDialog.OnTimeSetListener sTime = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                startCalendar.set(Calendar.HOUR, hourOfDay);
                startCalendar.set(Calendar.MINUTE, minute);
                updateStartHour();
            }
        };

        create_event_starthour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(EventsActivity.this, sTime, startCalendar.get(Calendar.HOUR),
                        startCalendar.get(Calendar.MINUTE), true).show();

            }
        });

        create_event_enddate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                endDate = s.toString();

            }
        });

        DatePickerDialog.OnDateSetListener eDate = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                endCalendar.set(Calendar.YEAR, year);
                endCalendar.set(Calendar.MONTH, month);
                endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateEndDate();
            }
        };

        create_event_endhour.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                endHour = s.toString();

            }
        });

        TimePickerDialog.OnTimeSetListener eTime = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                endCalendar.set(Calendar.HOUR, hourOfDay);
                endCalendar.set(Calendar.MINUTE, minute);
                updateEndHour();
            }
        };

        create_event_endhour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(EventsActivity.this, eTime, endCalendar.get(Calendar.HOUR),
                        endCalendar.get(Calendar.MINUTE), true).show();

            }
        });

        confirm_create_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Call<String> call = mService.createEvent(mPreferences.getAccessToken(), createEvent);

                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(EventsActivity.this);
                        if(response.isSuccessful()){
                            setContentView(R.layout.activity_events);
                        }else if(response.code() == 400){

                            builder.setTitle("Algum dos dados inseridos não estão corretos ou estão vazios, por favor corrija os antes de prosseguir.");



                        }else if(response.code() == 403){

                            builder.setTitle("O utilizador não está autorizado a criar eventos!");

                        }else if(response.code() == 500){

                            builder.setTitle("Ocorreu um erro com o pedido efetuado. Tente novamente!");

                        }
                        builder.setNeutralButton("Compreendi!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder.setIcon(R.drawable.ic_error_outline);



                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {

                    }
                });
            }
        });


    }

    private void updateEndHour() {
    }


    private void updateStartDate() {

        startDate = sdf.format(startCalendar.getTime());

    }

    private void updateEndDate(){

        endDate = sdf.format(endCalendar.getTime());

    }

    private void updateStartHour(){


    }

}
