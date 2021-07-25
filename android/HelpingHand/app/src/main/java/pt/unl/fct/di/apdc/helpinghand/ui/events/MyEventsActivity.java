package pt.unl.fct.di.apdc.helpinghand.ui.events;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.EventData;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyEventsActivity extends AppCompatActivity {

    private HelpingHandProvider provider;
    private HelpingHandService service;
    private AppPreferenceTools preferenceTools;

    private ItemAdapter itemAdapter;

    private ImageButton backBtn;

    private ListView eventsListView;

    private List<EventData> listOfEvents;

    public void onCreate(Bundle savedInstance) {

        super.onCreate(savedInstance);

        setContentView(R.layout.activity_mine_events);

        provider= HelpingHandProvider.getInstance();
        service = provider.getMService();
        preferenceTools = provider.getmAppPreferenceTools();

        backBtn = findViewById(R.id.my_ev_back_btn);

        Call<List<EventData>> call = service.getAccountEvents(preferenceTools.getUsername(),
                preferenceTools.getAccessToken());


       call.enqueue(new Callback<List<EventData>>() {
           @Override
           public void onResponse(Call<List<EventData>> call, Response<List<EventData>> response) {
               switch (response.code()){
                   case 200:
                       Toast.makeText(MyEventsActivity.this,
                               "Meus eventos obtidos com sucesso",
                               Toast.LENGTH_SHORT).show();
                       listOfEvents = response.body();
                       itemAdapter = new ItemAdapter(MyEventsActivity.this, listOfEvents);
                       eventsListView = findViewById(R.id.my_eve_list);
                       eventsListView.setAdapter(itemAdapter);
                       break;

                   case 400:
                       Toast.makeText(MyEventsActivity.this,
                               "Ocorreu um erro na recolha dos eventos",
                               Toast.LENGTH_SHORT).show();
                       break;

                   case 403:
                       Toast.makeText(MyEventsActivity.this,
                               "Token do utilizador não é válido",
                               Toast.LENGTH_SHORT).show();
                       break;

                   case 404:
                       Toast.makeText(MyEventsActivity.this,
                               "Instituição não foi encontrada",
                               Toast.LENGTH_SHORT).show();
                       break;

                   case 500:
                       Toast.makeText(MyEventsActivity.this,
                               "Ocorreu um erro no servidor tente novamente",
                               Toast.LENGTH_SHORT).show();

                       break;
               }

           }

           @Override
           public void onFailure(Call<List<EventData>> call, Throwable t) {

           }
       });





                /**/

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent back = new Intent(MyEventsActivity.this, EventsActivity.class);
                startActivity(back);
                finish();
            }
        });







    }

    public class ItemAdapter extends BaseAdapter {

        private List<EventData> list;
        private Context context;

        public ItemAdapter(Context context, List<EventData> list){
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public EventData getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) MyEventsActivity.this.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.item_list_event, null);

            TextView name = view.findViewById(R.id.item_name);
            TextView id = view.findViewById(R.id.item_id);

            EventData data = this.getItem(position);

            name.setText(data.name);
            id.setText(String.valueOf(data.id));

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try{

                        View layout = inflater.inflate(R.layout.popup_window_my_events,
                                findViewById(R.id.pop_my_ev_layout));

                        PopupWindow pw = new PopupWindow(layout, 1000,1200, true);

                        pw.showAtLocation(layout, Gravity.CENTER, 0, 0);

                        TextView nameWindow = layout.findViewById(R.id.pop_my_ev_name);
                        TextView startWindow = layout.findViewById(R.id.pop_my_ev_start);
                        TextView endWindow = layout.findViewById(R.id.pop_my_ev_end);
                        TextView locWindow = layout.findViewById(R.id.pop_my_ev_loc);

                        Button conclude = layout.findViewById(R.id.conclude_btn);
                        Button cancel = layout.findViewById(R.id.cancel_btn);

                        nameWindow.setText("Nome: " + data.name);
                        startWindow.setText("Inicio: " + data.start);
                        endWindow.setText("Fim: " + data.end);
                        locWindow.setText("Localização: " + getAddressFromLocation(data.location[0], data.location[1]) );

                        conclude.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Call<Void> call = service.finishEvent(String.valueOf(data.id), preferenceTools.getAccessToken());

                                call.enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        switch(response.code()){
                                            case 200:
                                                Toast.makeText(MyEventsActivity.this,
                                                        "Evento Concluído",
                                                        Toast.LENGTH_SHORT).show();
                                                pw.dismiss();
                                                break;

                                            case 400:
                                                Toast.makeText(MyEventsActivity.this,
                                                        "Ocorreu um erro",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 500:
                                                Toast.makeText(MyEventsActivity.this,
                                                        "Servidor falhou",
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

                        cancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Call<Void> call = service.cancelEvent(String.valueOf(data.id), preferenceTools.getAccessToken());

                                call.enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        switch(response.code()){
                                            case 200:
                                                Toast.makeText(MyEventsActivity.this,
                                                        "Evento cancelado",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 400:
                                                Toast.makeText(MyEventsActivity.this,
                                                        "Ocorreu um erro num input",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 500:
                                                Toast.makeText(MyEventsActivity.this,
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

                    }catch (Exception e){

                    }
                }
            });

            return view;
        }
    }

    private String getAddressFromLocation(double lat, double lon) {

        Geocoder coder = new Geocoder(MyEventsActivity.this);
        LatLng latLng = new LatLng(lat, lon);
        try {
            List<Address> addresses = coder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            return addresses.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


}
