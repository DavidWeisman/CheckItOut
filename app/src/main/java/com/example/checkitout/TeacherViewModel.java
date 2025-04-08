package com.example.checkitout;

import android.app.Application;
import android.util.Pair;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class TeacherViewModel extends AndroidViewModel {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private MutableLiveData<String> eventCreationStatus;
    private MutableLiveData<String> studentsAddedStatus;
    private MutableLiveData<Pair<String, String>> eventCreatedLiveData;  // date + eventName

    public TeacherViewModel(Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        eventCreationStatus = new MutableLiveData<>();
        studentsAddedStatus = new MutableLiveData<>();
        eventCreatedLiveData = new MutableLiveData<>();
    }

    public MutableLiveData<String> getEventCreationStatus() {
        return eventCreationStatus;
    }

    public MutableLiveData<String> getStudentsAddedStatus() {
        return studentsAddedStatus;
    }

    public MutableLiveData<Pair<String, String>> getEventCreatedLiveData() {
        return eventCreatedLiveData;
    }

    public void addEvent(String eventName, List<String> studentList, String currentDate) {
        if (eventName.isEmpty()) {
            eventCreationStatus.setValue("Event name is required");
            return;
        }

        String teacherId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (teacherId.isEmpty()) {
            eventCreationStatus.setValue("Authentication error");
            return;
        }

        DatabaseReference dateRef = mDatabase.child("Dates").child(currentDate);

        // Check if event already exists
        dateRef.child(eventName).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    // Event already exists, update students list
                    addStudentsToEvent(dateRef.child(eventName), studentList);
                    // Also notify UI to open the existing event
                    eventCreatedLiveData.setValue(new Pair<>(currentDate, eventName));
                } else {
                    // Event does not exist, create new event
                    createNewEvent(dateRef, eventName, teacherId, studentList);
                }
            } else {
                eventCreationStatus.setValue("Failed to access database");
            }
        });
    }

    private void addStudentsToEvent(DatabaseReference eventRef, List<String> studentList) {
        DatabaseReference studentsRef = eventRef.child("Students");

        for (String studentId : studentList) {
            studentsRef.child(studentId).setValue(false);  // False = unchecked
        }

        studentsAddedStatus.setValue("Students added to the event");
    }

    private void createNewEvent(DatabaseReference dateRef, String eventName, String teacherId, List<String> studentList) {
        DatabaseReference eventRef = dateRef.child(eventName);

        // Add teacher ID and initial empty OTP
        eventRef.child("TeacherId").setValue(teacherId);
        eventRef.child("enteredOpt").setValue("");

        // Add students with both "present" and "otp" fields
        DatabaseReference studentsRef = eventRef.child("Students");
        for (String studentId : studentList) {
            DatabaseReference studentRef = studentsRef.child(studentId);
            studentRef.child("present").setValue(false);
            studentRef.child("otp").setValue(false);
        }

        // Notify that the event was created
        eventCreationStatus.setValue("Event created successfully");

        // Notify UI to open the new event
        eventCreatedLiveData.setValue(new Pair<>(dateRef.getKey(), eventName));
    }

}
