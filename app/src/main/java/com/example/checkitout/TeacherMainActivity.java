package com.example.checkitout;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

    private static final int STUDENT_SYNC_REQUEST_CODE = 123;
    private TeacherViewModel teacherViewModel;
    private EditText eventNameEditText;
    private Button createEventButton;
    private Button viewEventsButton;

    private String eventName;
    private String currentDate;
    private ArrayList<String> selectedStudentIds;
    private String teacherId;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);

        teacherViewModel = new TeacherViewModel(getApplication());

        eventNameEditText = findViewById(R.id.eventNameEditText);
        createEventButton = findViewById(R.id.addEventButton);
        viewEventsButton = findViewById(R.id.openEventButton);

        mAuth = FirebaseAuth.getInstance();
        teacherId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        if (TextUtils.isEmpty(teacherId)) {
            showToast("Teacher not authenticated");
            finish();
            return;
        }

        teacherViewModel.getEventCreationStatus().observe(this,
                status -> Toast.makeText(this, status, Toast.LENGTH_SHORT).show());

        teacherViewModel.getStudentsAddedStatus().observe(this,
                status -> Toast.makeText(this, status, Toast.LENGTH_SHORT).show());

        teacherViewModel.getEventCreatedLiveData().observe(this, pair -> {
            if (pair != null) {
                openEvent(pair.first, pair.second);
            }
        });

        createEventButton.setOnClickListener(v -> createEvent());
        viewEventsButton.setOnClickListener(v -> openExistingEvents());
    }

    private void createEvent() {
        eventName = eventNameEditText.getText().toString().trim();

        if (TextUtils.isEmpty(eventName)) {
            showToast("Please enter an event name");
            return;
        }

        currentDate = getCurrentDate();
        Intent intent = new Intent(this, StudentSyncActivity.class);
        startActivityForResult(intent, STUDENT_SYNC_REQUEST_CODE);
    }
    private String getCurrentDate() {
        return new SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(new Date());
    }

    private void openExistingEvents() {
        Intent intent = new Intent(this, EventListActivity.class);
        startActivity(intent);
    }

    private void openEvent(String date, String eventName) {
        Intent intent = new Intent(this, StudentListActivity.class);
        intent.putExtra("date", date);
        intent.putExtra("eventName", eventName);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == STUDENT_SYNC_REQUEST_CODE  && resultCode == RESULT_OK && data != null) {
            selectedStudentIds = data.getStringArrayListExtra("syncedStudents");

            if (selectedStudentIds != null && !selectedStudentIds.isEmpty()) {
                teacherViewModel.addEvent(eventName, selectedStudentIds, currentDate, teacherId);
            } else {
                showToast("No students selected");
            }
        }
    }
    private void showToast(String message) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show();
    }
}
