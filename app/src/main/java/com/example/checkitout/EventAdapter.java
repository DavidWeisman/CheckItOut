package com.example.checkitout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<EventItem> eventList;
    private OnEventClickListener onEventClickListener;

    public EventAdapter(List<EventItem> eventList, OnEventClickListener onEventClickListener) {
        this.eventList = eventList;
        this.onEventClickListener = onEventClickListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_item, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventItem eventItem = eventList.get(position);
        holder.bind(eventItem);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public interface OnEventClickListener {
        void onEventClick(String date, String eventName);
    }

    public class EventViewHolder extends RecyclerView.ViewHolder {
        private TextView eventNameTextView;
        private TextView eventDateTextView;

        public EventViewHolder(View itemView) {
            super(itemView);
            eventNameTextView = itemView.findViewById(R.id.eventNameTextView);
            eventDateTextView = itemView.findViewById(R.id.eventDateTextView);

            itemView.setOnClickListener(v -> {
                EventItem eventItem = eventList.get(getAdapterPosition());
                onEventClickListener.onEventClick(eventItem.getDate(), eventItem.getEventName());
            });
        }

        public void bind(EventItem eventItem) {
            eventNameTextView.setText(eventItem.getEventName());
            eventDateTextView.setText(eventItem.getDate());
        }
    }
}
