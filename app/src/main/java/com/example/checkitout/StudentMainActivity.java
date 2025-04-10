package com.example.checkitout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class StudentMainActivity extends AppCompatActivity {

    private Button showQrbrn;
    private TextView studentNameTextView;
    private Button applybtn;
    private EditText otpEditText;

    private StudentViewModel studentViewModel;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_main);

        studentNameTextView = findViewById(R.id.studentName);
        showQrbrn = findViewById(R.id.showQrbrn);
        applybtn = findViewById(R.id.applybtn);
        otpEditText = findViewById(R.id.otpEditText);

        studentViewModel = new ViewModelProvider(this).get(StudentViewModel.class);

        studentViewModel.getStudentName().observe(this, name -> {
            if (name != null) {
                studentNameTextView.setText("Welcome, " + name);
            }
        });

        studentViewModel.loadStudentData();

        showQrbrn.setOnClickListener(v -> startActivity(new Intent(StudentMainActivity.this, StudentQR.class)));

        applybtn.setOnClickListener(v -> {
            String otp = otpEditText.getText().toString();
            studentViewModel.setOtp(otp);
        });
    }
}
