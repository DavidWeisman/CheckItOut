package com.example.checkitout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeacherMainActivity extends AppCompatActivity {

    private TeacherViewModel teacherViewModel;

    private EditText eventNameEditText;
    private Button addEventButton;
    private Button openEventButton;

    private String eventName;
    private String currentDate;
    private ArrayList<String> selectedStudentIds;
    private String teacherId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);

        // Initialize ViewModel
        teacherViewModel = new TeacherViewModel(getApplication());

        // Initialize UI components
        eventNameEditText = findViewById(R.id.eventNameEditText);
        addEventButton = findViewById(R.id.addEventButton);
        openEventButton = findViewById(R.id.openEventButton);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        teacherId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Observe LiveData for event creation and student addition statuses
        teacherViewModel.getEventCreationStatus().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String status) {
                Toast.makeText(TeacherMainActivity.this, status, Toast.LENGTH_SHORT).show();
            }
        });

        teacherViewModel.getStudentsAddedStatus().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String status) {
                Toast.makeText(TeacherMainActivity.this, status, Toast.LENGTH_SHORT).show();
            }
        });

        teacherViewModel.getEventCreatedLiveData().observe(this, pair -> {
            if (pair != null) {
                String date = pair.first;
                String eventName = pair.second;
                openEvent(date, eventName); // this will open StudentListActivity
            }
        });

        // Set click listeners for buttons
        addEventButton.setOnClickListener(v -> addEvent());

        openEventButton.setOnClickListener(v -> openExistingEvents());

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 123 && resultCode == RESULT_OK && data != null) {
            selectedStudentIds = data.getStringArrayListExtra("syncedStudents");
            if (selectedStudentIds != null && !selectedStudentIds.isEmpty()) {
                // Proceed with adding the event only if students are synced
                teacherViewModel.addEvent(eventName, selectedStudentIds, currentDate, teacherId);
            } else {
                Toast.makeText(this, "No students selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Open activity to show all past events
    private void openExistingEvents() {
        Intent intent = new Intent(this, EventListActivity.class);
        startActivity(intent);
    }

    // Add an event
    private void addEvent() {
        eventName = eventNameEditText.getText().toString().trim();

        // Get the current date in dd-MM-yy format
        currentDate = new SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(new Date());

        // Start the StudentSyncActivity to sync students
        Intent intent = new Intent(TeacherMainActivity.this, StudentSyncActivity.class);
        startActivityForResult(intent, 123);
    }



    private void openEvent(String date, String eventName) {
        Intent intent = new Intent(this, StudentListActivity.class);
        intent.putExtra("date", date);
        intent.putExtra("eventName", eventName);
        startActivity(intent);
    }
}
