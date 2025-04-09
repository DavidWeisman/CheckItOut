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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class StudentListActivity extends AppCompatActivity {
    private RecyclerView studentRecyclerView;
    private Button saveAttendanceButton;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private DatabaseReference eventRef;
    private StudentAdapter studentAdapter;
    Map<String, List<Boolean>> studentList = new HashMap<>();
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

        //saveAttendanceButton.setOnClickListener(v -> saveAttendance());

        refresh.setOnClickListener(v -> {
            startOTPMonitoring();  // Start OTP Monitoring
            loadStudents();        // Load students
        });
        scannerbtn.setOnClickListener(v -> {
            Intent intent = new Intent(StudentListActivity.this, QRScanner.class);
            startActivityForResult(intent, QR_SCANNER_REQUEST_CODE);  // Start QRScanner with a request code
        });

        saveAttendanceButton.setOnClickListener(v -> {
            Intent intent = new Intent(StudentListActivity.this, SendAttendanceReportActivity.class);
            intent.putExtra("date", date);
            intent.putExtra("eventName", eventName);
            startActivity(intent);
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

                    // Set the OTP field to false for each student
                    DatabaseReference studentOtpRef = FirebaseDatabase.getInstance().getReference()
                            .child("Dates").child(date).child(eventName)
                            .child("Students").child(studentUID).child("otp");

                    studentOtpRef.setValue(false); // Reset OTP value for each student

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
                // Get the entered OTP value
                String enteredOpt = eventSnapshot.child("enteredOpt").getValue(String.class);
                if (enteredOpt == null || enteredOpt.isEmpty()) return;

                // Get all students in the event
                DataSnapshot studentsSnapshot = eventSnapshot.child("Students");

                for (DataSnapshot studentEntry : studentsSnapshot.getChildren()) {
                    String uid = studentEntry.getKey();

                    // Skip if OTP is already marked as true for this student
                    Boolean isOtpSuccess = studentEntry.child("otp").getValue(Boolean.class);
                    if (isOtpSuccess != null && isOtpSuccess) {
                        continue;  // Skip if OTP is already correct
                    }

                    // Get the student's correct OTP from their profile
                    DatabaseReference studentRef = baseRef.child("Students").child(uid).child("opt");
                    studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot optSnapshot) {
                            String correctOpt = optSnapshot.getValue(String.class);

                            // Clear the 'opt' field to avoid using the same OTP again
                            optSnapshot.getRef().setValue("");

                            // If the correct OTP matches the entered OTP, update the student's OTP status
                            if (correctOpt != null && correctOpt.equals(enteredOpt)) {
                                eventRef.child("Students").child(uid).child("otp").setValue(true);
                                loadStudents();  // Refresh the student list or do any necessary updates
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("OTPMonitor", "Failed to get OTP for " + uid + ": " + error.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("OTPMonitor", "Failed to monitor OTP changes: " + error.getMessage());
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
                            .child(scannedUID)
                            .child("present");

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
                    String studentId = studentSnapshot.getKey();

                    // Extract present and otp values from the student data
                    boolean present = Boolean.TRUE.equals(studentSnapshot.child("present").getValue(Boolean.class));
                    boolean otp = Boolean.TRUE.equals(studentSnapshot.child("otp").getValue(Boolean.class));

                    // Create a list of booleans (present, otp)
                    List<Boolean> studentStatus = new ArrayList<>();
                    studentStatus.add(present);
                    studentStatus.add(otp);

                    // Add studentId and their status list to the map
                    studentList.put(studentId, studentStatus);
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
