package com.example.checkitout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentLoginActivity extends AppCompatActivity {

    private EditText usernameField;
    private EditText passwordField;
    private Button loginButton;
    private FirebaseAuth mAuth;
    private DatabaseReference studentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        usernameField = findViewById(R.id.studentUsername);
        passwordField = findViewById(R.id.studentPassword);
        loginButton = findViewById(R.id.studentLoginButton);

        mAuth = FirebaseAuth.getInstance();
        studentsRef = FirebaseDatabase.getInstance().getReference("Students");

        loginButton.setOnClickListener(v -> handleLogin());
    }
    private void handleLogin() {
        String email = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please enter email and password");
            return;
        }

        loginButton.setEnabled(false);  // Prevent double clicks

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    loginButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        verifyStudentRole(mAuth.getCurrentUser());
                    } else {
                        showToast("Authentication failed.");
                    }
                });
    }
    private void verifyStudentRole(FirebaseUser user) {
        if (user == null) {
            showToast("Unexpected error occurred.");
            return;
        }

        String userId = user.getUid();
        studentsRef.child(userId).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);

                if ("student".equalsIgnoreCase(role)) {
                    navigateToStudentMain();
                } else {
                    showToast("Access denied! You are not a student.");
                    mAuth.signOut();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Database error: " + error.getMessage());
            }
        });
    }
    private void navigateToStudentMain() {
        Intent intent = new Intent(StudentLoginActivity.this, StudentMainActivity.class);
        startActivity(intent);
        finish(); // Optional: prevent going back to login screen
    }
    private void showToast(String message) {
        Toast.makeText(StudentLoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
