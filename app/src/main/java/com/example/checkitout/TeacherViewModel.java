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
    private DatabaseReference mDatabase;
    private MutableLiveData<String> eventCreationStatus;
    private MutableLiveData<String> studentsAddedStatus;
    private MutableLiveData<Pair<String, String>> eventCreatedLiveData;

    public TeacherViewModel(Application application) {
        super(application);
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

    public void addEvent(String eventName, List<String> studentList, String currentDate, String teacherId) {
        if (eventName.isEmpty()) {
            eventCreationStatus.setValue("Event name is required");
            return;
        }

        if (teacherId.isEmpty()) {
            eventCreationStatus.setValue("Authentication error");
            return;
        }

        DatabaseReference dateRef = mDatabase.child("Dates").child(currentDate);

        dateRef.child(eventName).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    addStudentsToEvent(dateRef.child(eventName), studentList);
                    eventCreatedLiveData.setValue(new Pair<>(currentDate, eventName));
                } else {
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
            studentsRef.child(studentId).setValue(false);
        }

        studentsAddedStatus.setValue("Students added to the event");
    }

    private void createNewEvent(DatabaseReference dateRef, String eventName, String teacherId, List<String> studentList) {
        DatabaseReference eventRef = dateRef.child(eventName);

        eventRef.child("TeacherId").setValue(teacherId);
        eventRef.child("enteredOpt").setValue("");

        DatabaseReference studentsRef = eventRef.child("Students");
        for (String studentId : studentList) {
            DatabaseReference studentRef = studentsRef.child(studentId);
            studentRef.child("present").setValue(false);
            studentRef.child("otp").setValue(false);
        }

        eventCreationStatus.setValue("Event created successfully");
        eventCreatedLiveData.setValue(new Pair<>(dateRef.getKey(), eventName));
    }

}
