package com.example.checkitout;

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

    private StudentViewModel studentViewModel;  // ViewModel for handling Firebase data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_main);

        // Initialize UI components
        studentNameTextView = findViewById(R.id.studentName);
        showQrbrn = findViewById(R.id.showQrbrn);
        applybtn = findViewById(R.id.applybtn);
        otpEditText = findViewById(R.id.otpEditText);

        // Initialize ViewModel
        studentViewModel = new ViewModelProvider(this).get(StudentViewModel.class);

        // Observe changes in student data
        studentViewModel.getStudentName().observe(this, name -> {
            if (name != null) {
                studentNameTextView.setText("Welcome, " + name);
            }
        });

        // Get current user
        studentViewModel.loadStudentData();

        // QR Button click listener
        showQrbrn.setOnClickListener(v -> startActivity(new Intent(StudentMainActivity.this, StudentQR.class)));

        // Apply OTP button click listener
        applybtn.setOnClickListener(v -> {
            String otp = otpEditText.getText().toString();
            studentViewModel.setOtp(otp);
        });
    }
}
