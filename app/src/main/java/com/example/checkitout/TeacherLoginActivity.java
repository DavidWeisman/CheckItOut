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

public class TeacherLoginActivity extends AppCompatActivity {

    private EditText usernameField;
    private EditText passwordField;
    private Button loginButton;
    private FirebaseAuth mAuth;
    private DatabaseReference teachresRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_login);

        usernameField = findViewById(R.id.teacherUsername);
        passwordField = findViewById(R.id.teacherPassword);
        loginButton = findViewById(R.id.teacherLoginButton);

        mAuth = FirebaseAuth.getInstance();
        teachresRef = FirebaseDatabase.getInstance().getReference("Teachers");

        loginButton.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String email = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please enter email and password");
            return;
        }

        loginButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    loginButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        verifyTeacherRole(mAuth.getCurrentUser());
                    } else {
                        showToast("Authentication failed.");
                    }
                });
    }
    private void verifyTeacherRole(FirebaseUser user) {
        if (user == null) {
            showToast("Unexpected error occurred.");
            return;
        }

        String userId = user.getUid();
        teachresRef.child(userId).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);

                if ("teacher".equalsIgnoreCase(role)) {
                    navigateToStudentMain();
                } else {
                    showToast("Access denied! You are not a teacher.");
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
        Intent intent = new Intent(TeacherLoginActivity.this, TeacherMainActivity.class);
        startActivity(intent);
        finish();
    }
    private void showToast(String message) {
        Toast.makeText(TeacherLoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
