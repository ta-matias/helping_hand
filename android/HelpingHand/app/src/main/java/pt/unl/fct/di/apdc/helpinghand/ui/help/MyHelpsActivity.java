package pt.unl.fct.di.apdc.helpinghand.ui.help;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.media.Image;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.EventData;
import pt.unl.fct.di.apdc.helpinghand.data.model.HelpData;
import pt.unl.fct.di.apdc.helpinghand.data.model.HelperStats;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandProvider;
import pt.unl.fct.di.apdc.helpinghand.network.HelpingHandService;
import pt.unl.fct.di.apdc.helpinghand.ui.events.MyEventsActivity;
import pt.unl.fct.di.apdc.helpinghand.utility.AppPreferenceTools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyHelpsActivity extends AppCompatActivity {

    private HelpingHandProvider provider;
    private HelpingHandService service;
    private AppPreferenceTools preferenceTools;

    private ImageButton backBtn;

    private ListView listView;

    private List<HelpData> listOfHelps;
    private List<HelperStats> listOfHelpers;

    private HelpAdapter helpAdapter;

    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setContentView(R.layout.activity_my_helps);

        provider = HelpingHandProvider.getInstance();
        service = provider.getMService();
        preferenceTools = provider.getmAppPreferenceTools();

        backBtn = findViewById(R.id.my_help_back_btn);


        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent back = new Intent( MyHelpsActivity.this, HelpActivity.class);
                startActivity(back);
                finish();
            }
        });

        Call<List<HelpData>> call = null;
        if(preferenceTools.getRole().equals("USER")){
            call = service.getUserHelps(preferenceTools.getUsername(), preferenceTools.getAccessToken());
        }else {
            call = service.getInstHelps(preferenceTools.getUsername(), preferenceTools.getAccessToken());
        }

        call.enqueue(new Callback<List<HelpData>>() {
            @Override
            public void onResponse(Call<List<HelpData>> call, Response<List<HelpData>> response) {
                switch (response.code()) {
                    case 200:
                        Toast.makeText(MyHelpsActivity.this,
                                "Ajudas obtidas com sucesso",
                                Toast.LENGTH_SHORT).show();
                        listOfHelps = response.body();
                        helpAdapter = new HelpAdapter(MyHelpsActivity.this, listOfHelps);
                        listView = findViewById(R.id.my_help_list);
                        listView.setAdapter(helpAdapter);
                        break;
                }
            }

            @Override
            public void onFailure(Call<List<HelpData>> call, Throwable t) {

            }
        });

    }

    public class HelpAdapter extends BaseAdapter{

        private Context context;
        private List<HelpData> list;
        private int rate = 0;

        public HelpAdapter(Context context, List<HelpData> list){
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public HelpData getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) MyHelpsActivity.this.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View layout = inflater.inflate(R.layout.item_list_event, null);

            TextView name = layout.findViewById(R.id.item_name);
            TextView id = layout.findViewById(R.id.item_id);

            HelpData data = this.list.get(position);

            name.setText(data.name);
            id.setText(String.valueOf(data.id));

            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try{
                        View view = inflater.inflate(R.layout.popup_window_my_helps,
                                findViewById(R.id.popup_window_help_layout));

                        PopupWindow pw = new PopupWindow(view, 1000, 1700, true);

                        pw.showAtLocation(view, Gravity.CENTER, 0,0);

                        TextView name = view.findViewById(R.id.help_popup_name);
                        TextView start = view.findViewById(R.id.help_popup_start);
                        TextView loc = view.findViewById(R.id.help_popup_loc);

                        Button finish = view.findViewById(R.id.help_popup_finish);
                        Button cancel = view.findViewById(R.id.help_popup_cancel);
                        Button choose = view.findViewById(R.id.choose_helper);

                        ImageButton rate1 = view.findViewById(R.id.rate_1);
                        ImageButton rate2 = view.findViewById(R.id.rate_2);
                        ImageButton rate3 = view.findViewById(R.id.rate_3);
                        ImageButton rate4 = view.findViewById(R.id.rate_4);
                        ImageButton rate5 = view.findViewById(R.id.rate_5);
                        ListView helpers_list = view.findViewById(R.id.pop_list);


                        finish.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                rate1.setVisibility(View.VISIBLE);
                                rate2.setVisibility(View.VISIBLE);
                                rate3.setVisibility(View.VISIBLE);
                                rate4.setVisibility(View.VISIBLE);
                                rate5.setVisibility(View.VISIBLE);
                                helpers_list.setVisibility(View.GONE);
                                rate1.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        rate = 1;
                                        makeCall();
                                    }
                                });
                                rate2.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        rate = 2;
                                        makeCall();
                                    }
                                });
                                rate3.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        rate = 3;
                                        makeCall();
                                    }
                                });

                                rate4.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        rate = 4;
                                        makeCall();
                                    }
                                });
                                rate5.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        rate = 5;
                                        makeCall();
                                    }
                                });


                            }
                            private void makeCall(){
                                Call<Void> call = service.finishHelp(String.valueOf(data.id), preferenceTools.getAccessToken(),
                                        String.valueOf(rate));

                                call.enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        switch (response.code()){
                                            case 200:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "Ajuda concluida com sucesso",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 400:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "Ajuda não concluida devido a erro nos dados",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 403:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "O utilizador não está autorizado a realizar a operação",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 404:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "A ajuda escolhida não existe",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 409:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "Antes de concluir deve escolher um ajudante",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 500:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "Ajuda não concluida devido a erro no servidor",
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



                        name.setText("Nome: " + data.name);
                        start.setText("Inicio: " + data.time);
                        loc.setText("Localização: " +
                                getAddressFromLocation(data.location[0], data.location[1]));

                        cancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                rate1.setVisibility(View.GONE);
                                rate2.setVisibility(View.GONE);
                                rate3.setVisibility(View.GONE);
                                rate4.setVisibility(View.GONE);
                                rate5.setVisibility(View.GONE);
                                helpers_list.setVisibility(View.GONE);
                                Call<Void> call = service.cancelHelp(String.valueOf(data.id), preferenceTools.getAccessToken());
                                call.enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        switch(response.code()){
                                            case 200:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "Ajuda cancelada com sucesso.",
                                                        Toast.LENGTH_LONG).show();
                                                Intent canceled = new Intent(MyHelpsActivity.this, HelpActivity.class);
                                                startActivity(canceled);
                                                finish();
                                                break;

                                            case 400:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "Ajuda não cancelada devido a um erro nos dados do pedido",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 403:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "Token do utilizador não é válido para realizar esta operação",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 404:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "A ajuda escolhida não existe ou já foi cancelada",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 500:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "A ajuda não foi cancelada devido a um erro no servidor. Tente Novamente.",
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

                        choose.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                rate1.setVisibility(View.GONE);
                                rate2.setVisibility(View.GONE);
                                rate3.setVisibility(View.GONE);
                                rate4.setVisibility(View.GONE);
                                rate5.setVisibility(View.GONE);

                                helpers_list.setVisibility(View.VISIBLE);




                                Call<List<HelperStats>> call = service.getHelpers(
                                        String.valueOf(data.id),
                                        preferenceTools.getAccessToken());

                                call.enqueue(new Callback<List<HelperStats>>() {
                                    @Override
                                    public void onResponse(Call<List<HelperStats>> call, Response<List<HelperStats>> response) {
                                        switch(response.code()){
                                            case 200:
                                                listOfHelpers = response.body();
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "Lista de ajudantes obtida com sucesso.",
                                                        Toast.LENGTH_SHORT).show();
                                                HelperAdapter adapter = new HelperAdapter(listOfHelpers, data);
                                                helpers_list.setAdapter(adapter);
                                                break;

                                            case 400:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "Pedido não efetuado devido a erro nos dados",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 403:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "O utilizador não tem autorização para efetuar o pedido", Toast.LENGTH_SHORT).show();
                                                break;

                                            case 404:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "O pedido de ajuda não foi encontrado",
                                                        Toast.LENGTH_SHORT).show();
                                                break;

                                            case 500:
                                                Toast.makeText(MyHelpsActivity.this,
                                                        "Pedido não efetuado devido a erro no servidor.",
                                                        Toast.LENGTH_SHORT).show();
                                                break;


                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<List<HelperStats>> call, Throwable t) {

                                    }
                                });
                            }

                        });


                    }catch (Exception e) {

                    }
                }



            });
            return layout;
        }
    }

    private String getAddressFromLocation(double lat, double lon) {

        Geocoder coder = new Geocoder(MyHelpsActivity.this);
        LatLng latLng = new LatLng(lat, lon);
        try {
            List<Address> addresses = coder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            return addresses.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public class HelperAdapter extends BaseAdapter{

        private List<HelperStats> helperStats;
        private HelpData help;

        public HelperAdapter(List<HelperStats> list, HelpData help){
            this.helperStats = list;
            this.help = help;
        }


        @Override
        public int getCount() {
            return helperStats.size();
        }

        @Override
        public HelperStats getItem(int position) {
            return helperStats.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) MyHelpsActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.choose_helper, findViewById(R.id.choose_helper_layout));


            HelperStats helper = helperStats.get(position);
            TextView name = view.findViewById(R.id.helper_name);
            TextView rating = view.findViewById(R.id.helper_rating);
            TextView reliability = view.findViewById(R.id.helper_reliability);
            Button chooseBtn = view.findViewById(R.id.choose);

            chooseBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Call<Void> call = service.chooseHelper(String.valueOf(help.id), preferenceTools.getAccessToken(), helper.id);

                    call.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            switch (response.code()){
                                case 200:
                                    Toast.makeText(MyHelpsActivity.this,
                                            "Ajudante escolhido com sucesso.",
                                            Toast.LENGTH_SHORT).show();
                                    break;

                                case 400:
                                    Toast.makeText(MyHelpsActivity.this,
                                            "Pedido nao efetuado devido a erro no input",
                                            Toast.LENGTH_SHORT).show();
                                    break;

                                case 403:
                                    Toast.makeText(MyHelpsActivity.this,
                                            "O utilizador não pode realizar a operação",
                                            Toast.LENGTH_SHORT).show();
                                    break;

                                case 404:
                                    Toast.makeText(MyHelpsActivity.this,
                                            "A ajuda pretendida não existe",
                                            Toast.LENGTH_SHORT).show();
                                    break;

                                case 409:
                                    Toast.makeText(MyHelpsActivity.this,
                                            "Este utilizador já é o ajudante",
                                            Toast.LENGTH_SHORT).show();
                                    break;

                                case 500:
                                    Toast.makeText(MyHelpsActivity.this,
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

            name.setText("ID: " + helper.id);
            rating.setText("Rating: " + String.valueOf(helper.rating));
            reliability.setText("Fiabilidade: " + String.valueOf(helper.reliability));

            return view;
        }
    }
}
