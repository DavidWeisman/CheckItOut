package com.example.checkitout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class StudentListActivity extends AppCompatActivity {
    private RecyclerView studentRecyclerView;
    private Button saveAttendanceButton;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private DatabaseReference eventRef;
    private StudentAdapter studentAdapter;
    private List<Map.Entry<String, Boolean>> studentList = new ArrayList<>();
    private static final int QR_SCANNER_REQUEST_CODE = 100;

    private Button scannerbtn;
    private FirebaseDatabase mDatabase;

    private String date;
    private String eventName;
    private Button OTPbtn;
    private Button refresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        TextView eventTitleTextView = findViewById(R.id.eventTitleTextView);
        studentRecyclerView = findViewById(R.id.studentRecyclerView);
        saveAttendanceButton = findViewById(R.id.saveAttendanceButton);
        scannerbtn = findViewById(R.id.scannerbtn);
        OTPbtn = findViewById(R.id.OTPbtn);
        refresh = findViewById(R.id.refresh);

        // Get event details from intent
        date = getIntent().getStringExtra("date");
        eventName = getIntent().getStringExtra("eventName");

        if (date == null || eventName == null) {
            Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventTitleTextView.setText(eventName);
        eventRef = FirebaseDatabase.getInstance().getReference().child("Dates").child(date).child(eventName).child("Students");

        // Load students
        loadStudents();

        saveAttendanceButton.setOnClickListener(v -> saveAttendance());
        refresh.setOnClickListener(v -> startOTPMonitoring());

        scannerbtn.setOnClickListener(v -> {
            Intent intent = new Intent(StudentListActivity.this, QRScanner.class);
            startActivityForResult(intent, QR_SCANNER_REQUEST_CODE);  // Start QRScanner with a request code
        });

        OTPbtn.setOnClickListener(v -> sendOTP());
    }

    private void sendOTP(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_CODE);
        } else {
            sendOPTS();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendOPTS();
            }
        }
    }

    private void sendOPTS() {
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference()
                .child("Dates").child(date).child(eventName).child("Students");

        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot eventSnapshot) {
                if (!eventSnapshot.exists()) {
                    Toast.makeText(StudentListActivity.this, "No students found for this event", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot studentSnapshot : eventSnapshot.getChildren()) {
                    String studentUID = studentSnapshot.getKey();

                    DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference()
                            .child("Students").child(studentUID);

                    studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot studentData) {
                            if (studentData.exists()) {
                                String phoneNumber = studentData.child("phoneNumber").getValue(String.class);
                                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                                    String otp = String.format("%04d", new Random().nextInt(10000));

                                    DatabaseReference otpRef = FirebaseDatabase.getInstance().getReference()
                                            .child("Dates").child(date).child(eventName)
                                            .child("enteredOpt");

                                    otpRef.setValue(otp);

                                    // Send SMS
                                    sendSms(phoneNumber, otp);
                                } else {
                                    Toast.makeText(StudentListActivity.this, "Missing phone number for student " + studentUID, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(StudentListActivity.this, "Error fetching student: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentListActivity.this, "Error loading event students: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendSms(String phoneNumber, String otp) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, "Your OTP is: " + otp, null, null);
        } catch (Exception e) {
            Toast.makeText(StudentListActivity.this, "Failed to send SMS to " + phoneNumber + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startOTPMonitoring() {
        DatabaseReference baseRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference eventRef = baseRef.child("Dates").child(date).child(eventName);

        eventRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot eventSnapshot) {
                String enteredOpt = eventSnapshot.child("enteredOpt").getValue(String.class);
                if (enteredOpt == null || enteredOpt.isEmpty()) return;

                DataSnapshot studentsSnapshot = eventSnapshot.child("Students");
                for (DataSnapshot studentEntry : studentsSnapshot.getChildren()) {
                    String uid = studentEntry.getKey();

                    // Skip if already checked
                    Boolean isChecked = studentEntry.getValue(Boolean.class);
                    if (isChecked != null && isChecked) continue;

                    // Get the student's correct OPT
                    DatabaseReference studentRef = baseRef.child("Students").child(uid).child("opt");
                    studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot optSnapshot) {
                            String correctOpt = optSnapshot.getValue(String.class);
                            optSnapshot.getRef().setValue("");
                            if (correctOpt != null && correctOpt.equals(enteredOpt)) {
                                // Mark the student's UID key under "Students" as true (representing the boolean value)
                                eventRef.child("Students").child(uid).setValue(true);
                                loadStudents();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("OTPMonitor", "Failed to get opt for " + uid + ": " + error.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("OTPMonitor", "Failed to read event data: " + error.getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == QR_SCANNER_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                String scannedUID = data.getStringExtra("QR_RESULT");

                if (scannedUID != null && eventName != null && date != null) {
                    DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
                    DatabaseReference studentStatusRef = databaseRef.child("Dates")
                            .child(date)
                            .child(eventName)
                            .child("Students")
                            .child(scannedUID);

                    // Update the student's status to "present" (true)
                    studentStatusRef.setValue(true);

                    Toast.makeText(this, "Student marked as present!", Toast.LENGTH_SHORT).show();
                    loadStudents();
                }
            }
        }
    }

    private void loadStudents() {
        eventRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                studentList.clear();
                for (DataSnapshot studentSnapshot : task.getResult().getChildren()) {
                    studentList.add(new AbstractMap.SimpleEntry<>(studentSnapshot.getKey(), studentSnapshot.getValue(Boolean.class)));
                }

                studentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                studentAdapter = new StudentAdapter(studentList, eventRef);
                studentRecyclerView.setAdapter(studentAdapter);
            } else {
                Toast.makeText(this, "No students found for this event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAttendance() {
        Toast.makeText(this, "Attendance saved successfully!", Toast.LENGTH_SHORT).show();
        finish();  // Close the activity after saving
    }
}
