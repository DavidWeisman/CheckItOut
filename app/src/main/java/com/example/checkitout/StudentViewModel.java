package com.example.checkitout;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private MutableLiveData<String> studentName = new MutableLiveData<>();
    private String studentId;

    public StudentViewModel(Application application) {
        super(application);
    }

    public LiveData<String> getStudentName() {
        return studentName;
    }

    public void loadStudentData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            showToast("User not logged in!");
            return;
        }

        studentId = currentUser.getUid();
        DatabaseReference userRef = mDatabase.getReference("Students").child(studentId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    studentName.setValue(name);
                } else {
                    showToast("Student not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showToast("Error loading data: " + databaseError.getMessage());
            }
        });
    }

    public void setOtp(String otp) {
        if (studentId == null) {
            showToast("User ID not available");
            return;
        }

        DatabaseReference userRef = mDatabase.getReference("Students").child(studentId);
        userRef.child("opt").setValue(otp).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showToast("OTP set successfully");
            } else {
                showToast("Failed to set OTP");
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show();
    }
}
