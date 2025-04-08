package com.example.checkitout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        Button btnStudent = findViewById(R.id.btnStudent);
        Button btnTeacher = findViewById(R.id.btnTeacher);
        Button btnTeacherRegistration =  findViewById(R.id.btnTeacherRegistration);

        btnStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RoleSelectionActivity.this, StudentLoginActivity.class));
            }
        });

        btnTeacherRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RoleSelectionActivity.this, TeacherRegistrationActivity.class));
            }
        });

        btnTeacher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RoleSelectionActivity.this, TeacherLoginActivity.class));
            }
        });
    }
}
