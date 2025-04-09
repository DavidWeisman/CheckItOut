package com.example.checkitout;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class TeacherRegistrationActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;

    private EditText fullName;
    private Button registerButton;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_registration);

        auth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        fullName = findViewById(R.id.fullName);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> registerTeacher());
    }

    private void registerTeacher() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String name = fullName.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(name) ) {
            Toast.makeText(this, "Please enter email, full name and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            FirebaseDatabase.getInstance().getReference("Teachers")
                                    .child(uid)
                                    .setValue(new Teacher(email, "teacher", name))
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                                            // Redirect to main activity or login
                                            startActivity(new Intent(this, TeacherMainActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(this, "Failed to save teacher to database", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper class for writing to the DB
    public static class Teacher {
        public String email;
        public String role;
        public String name;

        public Teacher() {
            // Required for Firebase
        }

        public Teacher(String email, String role, String name) {
            this.email = email;
            this.role = role;
            this.name = name;
        }
    }
}
