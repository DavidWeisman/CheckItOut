package com.example.checkitout;

import android.app.Application;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentViewModel extends AndroidViewModel {

    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private String studentId;

    private MutableLiveData<String> studentName = new MutableLiveData<>();

    public StudentViewModel(Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
    }

    public LiveData<String> getStudentName() {
        return studentName;
    }

    public void loadStudentData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Handle user not logged in
            Toast.makeText(getApplication(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        studentId = currentUser.getUid();
        DatabaseReference userRef = mDatabase.getReference("Students").child(studentId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    studentName.setValue(name);  // Set the student's name
                } else {
                    Toast.makeText(getApplication(), "Student not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplication(), "Error loading data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void setOtp(String otp) {
        DatabaseReference userRef = mDatabase.getReference("Students").child(studentId);
        userRef.child("opt").setValue(otp).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getApplication(), "OPT set successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplication(), "Failed to set OPT", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
