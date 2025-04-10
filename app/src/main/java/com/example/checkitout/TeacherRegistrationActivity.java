package com.example.checkitout;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

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

        emailEditText = findViewById(R.id.emailEditText);
        fullName = findViewById(R.id.fullName);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);

        auth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(v -> registerTeacher());
    }
    private void registerTeacher() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String name = fullName.getText().toString().trim();

        if (!validateInput(email, password, name)) return;

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveTeacherToDatabase(email, name);
                    } else {
                        showToast("Registration failed: " + Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
    }
    private boolean validateInput(String email, String password, String name) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(name)) {
            showToast("Please enter email, full name and password");
            return false;
        }

        if (password.length() < 6) {
            showToast("Password must be at least 6 characters");
            return false;
        }

        return true;
    }
    private void saveTeacherToDatabase(String email, String name) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showToast("User not found after registration");
            return;
        }

        String uid = user.getUid();
        Teacher teacher = new Teacher(email, "teacher", name);

        FirebaseDatabase.getInstance().getReference("Teachers")
                .child(uid)
                .setValue(teacher)
                .addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        showToast("Registration successful");
                        navigateToMain();
                    } else {
                        showToast("Failed to save teacher to database");
                    }
                });
    }
    private void navigateToMain() {
        startActivity(new Intent(this, TeacherMainActivity.class));
        finish();
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
