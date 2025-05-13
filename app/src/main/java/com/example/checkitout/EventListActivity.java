package com.example.checkitout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class EventListActivity extends AppCompatActivity {

    private RecyclerView eventRecyclerView;
    private EventAdapter eventAdapter;
    private List<EventItem> eventList = new ArrayList<>();
    private DatabaseReference datesRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        eventRecyclerView = findViewById(R.id.eventRecyclerView);
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        datesRef = FirebaseDatabase.getInstance().getReference().child("Dates");

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();


        if (currentUser == null) {
            Toast.makeText(this, "You need to log in to view events", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        loadEvents();
    }

    private void loadEvents() {
        String currentUserUID = mAuth.getCurrentUser().getUid();

        datesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                eventList.clear(); // Clear to avoid duplicates on reload

                for (DataSnapshot dateSnapshot : task.getResult().getChildren()) {
                    String date = dateSnapshot.getKey();

                    for (DataSnapshot eventSnapshot : dateSnapshot.getChildren()) {
                        String eventName = eventSnapshot.getKey();
                        String teacherId = eventSnapshot.child("TeacherId").getValue(String.class);

                        if (teacherId != null && teacherId.equals(currentUserUID)) {
                            eventList.add(new EventItem(date, eventName));
                        }
                    }
                }

                if (eventList.isEmpty()) {
                    Toast.makeText(this, "No events found for you", Toast.LENGTH_SHORT).show();
                }

                eventAdapter = new EventAdapter(eventList, this::openEvent);
                eventRecyclerView.setAdapter(eventAdapter);
            } else {
                Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void openEvent(String date, String eventName) {
        Intent intent = new Intent(this, StudentListActivity.class);
        intent.putExtra("date", date);
        intent.putExtra("eventName", eventName);
        startActivity(intent);
    }
}
