package pt.unl.fct.di.apdc.helpinghand.ui.adapters;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

import pt.unl.fct.di.apdc.helpinghand.HelpingHandApp;
import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.EventData;
import pt.unl.fct.di.apdc.helpinghand.ui.events.JoinEventActivity;

public class JoinEventAdapter extends RecyclerView.Adapter<JoinEventAdapter.EventHolder> {

    private final List<EventData> list;
    private ViewGroup parent;


    public JoinEventAdapter(List<EventData> listOfEvents){
        this.list = listOfEvents;
    }

    @NonNull
    @NotNull
    @Override
    public JoinEventAdapter.EventHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent,
                                                           int viewType) {
        View eventList = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_join_event, parent, true);
        this.parent = parent;
        return new EventHolder(eventList);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull JoinEventAdapter.EventHolder holder, int position) {
        EventData eventData = list.get(position);

        holder.setName(eventData.name);
        holder.setCreator(eventData.creator);
        holder.setDescription(eventData.description);
        holder.setStart(eventData.start);
        holder.setEnd(eventData.end);
        holder.setLocation(locationToAddress(eventData.location));
    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    private String locationToAddress(double[] location){
        String address = "";
        Geocoder coder = new Geocoder(parent.getContext(), Locale.getDefault());
        try {
            List<Address> addresses = coder.getFromLocation(location[0], location[1],1);
            if (addresses != null && addresses.size() > 0) {
                address = addresses.get(0).getAddressLine(0);
            }
        }catch (Exception e){
            e.printStackTrace();
        }


        return address;
    }

    public class EventHolder extends RecyclerView.ViewHolder{

        private TextView name_txt;
        private TextView creator_txt;
        private TextView description_txt;
        private TextView start_txt;
        private TextView end_txt;
        private TextView loc_txt;

        public EventHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            name_txt = itemView.findViewById(R.id.name_adapter);
            creator_txt = itemView.findViewById(R.id.creator_adapter);
            description_txt = itemView.findViewById(R.id.description_adapter);
            start_txt = itemView.findViewById(R.id.start_adapter);
            end_txt = itemView.findViewById(R.id.end_card);
            loc_txt = itemView.findViewById(R.id.location_adapter);
        }

        public void setName(String name){
            name_txt.setText(name);
        }

        public void setCreator(String creator) {
            this.creator_txt.setText(creator);
        }

        public void setDescription(String description){
            this.description_txt.setText(description);
        }

        public void setStart(String startDate){
            this.start_txt.setText(startDate);
        }

        public void setEnd(String endDate) {
            this.end_txt.setText(endDate);
        }

        public void setLocation(String location){
            this.loc_txt.setText(location);
        }
    }
}


