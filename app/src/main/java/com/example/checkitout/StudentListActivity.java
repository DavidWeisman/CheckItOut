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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.util.*;

public class StudentListActivity extends AppCompatActivity {
    private RecyclerView studentRecyclerView;
    private Button saveAttendanceButton, scannerBtn, otpBtn, refreshBtn;
    private TextView eventTitleTextView;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int QR_SCANNER_REQUEST_CODE = 100;
    private DatabaseReference eventRef;
    private FirebaseDatabase mDatabase;
    private StudentAdapter studentAdapter;
    private final Map<String, List<Boolean>> studentList = new HashMap<>();
    private String date;
    private String eventName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        initializeUI();
        getIntentExtras();
        setupEventReference();
        setupRecyclerView();
        setupListeners();
        loadStudents();
    }

    private void initializeUI() {
        eventTitleTextView = findViewById(R.id.eventTitleTextView);
        studentRecyclerView = findViewById(R.id.studentRecyclerView);
        saveAttendanceButton = findViewById(R.id.saveAttendanceButton);
        scannerBtn = findViewById(R.id.scannerbtn);
        otpBtn = findViewById(R.id.OTPbtn);
        refreshBtn = findViewById(R.id.refresh);
    }

    private void getIntentExtras() {
        date = getIntent().getStringExtra("date");
        eventName = getIntent().getStringExtra("eventName");

        if (date == null || eventName == null) {
            Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            eventTitleTextView.setText(eventName);
        }
    }

    private void setupEventReference() {
        mDatabase = FirebaseDatabase.getInstance();
        eventRef = mDatabase.getReference().child("Dates").child(date).child(eventName).child("Students");
    }

    private void setupRecyclerView() {
        studentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(getSwipeCallback());
        itemTouchHelper.attachToRecyclerView(studentRecyclerView);
    }

    private ItemTouchHelper.SimpleCallback getSwipeCallback() {
        return new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                String studentUID = studentAdapter.getStudentUID(position);
                if (studentUID != null) {
                    deleteStudentData(studentUID);
                    studentAdapter.deleteStudent(position);
                }
            }
        };
    }
    private void setupListeners() {
        refreshBtn.setOnClickListener(v -> {
            startOTPMonitoring();
            loadStudents();
        });

        scannerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScanner.class);
            startActivityForResult(intent, QR_SCANNER_REQUEST_CODE);
        });

        saveAttendanceButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SendAttendanceReportActivity.class);
            intent.putExtra("date", date);
            intent.putExtra("eventName", eventName);
            startActivity(intent);
        });

        otpBtn.setOnClickListener(v -> requestSmsPermissionOrSend());
    }

    private void requestSmsPermissionOrSend() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_CODE);
        } else {
            sendOTPsToStudents();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sendOTPsToStudents();
        }
    }

    private void sendOTPsToStudents() {
        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(StudentListActivity.this, "No students found", Toast.LENGTH_SHORT).show();
                    return;
                }

                String otp = String.format("%04d", new Random().nextInt(10000));
                mDatabase.getReference().child("Dates").child(date).child(eventName).child("enteredOpt").setValue(otp);

                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String uid = studentSnapshot.getKey();
                    if (uid == null) continue;

                    resetStudentOtpStatus(uid);
                    fetchAndSendSmsToStudent(uid, otp);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentListActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetStudentOtpStatus(String uid) {
        mDatabase.getReference()
                .child("Dates").child(date).child(eventName).child("Students")
                .child(uid).child("otp").setValue(false);
    }

    private void fetchAndSendSmsToStudent(String uid, String otp) {
        mDatabase.getReference().child("Students").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String phone = snapshot.child("phoneNumber").getValue(String.class);
                        if (phone != null && !phone.isEmpty()) {
                            sendSms(phone, otp);
                        } else {
                            Toast.makeText(StudentListActivity.this, "Missing phone number for " + uid, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(StudentListActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendSms(String phone, String otp) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phone, null, "Your OTP is: " + otp, null, null);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS to " + phone + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startOTPMonitoring() {
        DatabaseReference baseRef = mDatabase.getReference();
        DatabaseReference currentEventRef = baseRef.child("Dates").child(date).child(eventName);

        currentEventRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String enteredOtp = snapshot.child("enteredOpt").getValue(String.class);
                if (enteredOtp == null || enteredOtp.isEmpty()) return;

                for (DataSnapshot student : snapshot.child("Students").getChildren()) {
                    String uid = student.getKey();
                    if (uid == null) continue;

                    Boolean otpValid = student.child("otp").getValue(Boolean.class);
                    if (otpValid != null && otpValid) continue;

                    DatabaseReference optRef = baseRef.child("Students").child(uid).child("opt");
                    optRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot optSnapshot) {
                            String correctOtp = optSnapshot.getValue(String.class);
                            optSnapshot.getRef().setValue("");

                            if (enteredOtp.equals(correctOtp)) {
                                currentEventRef.child("Students").child(uid).child("otp").setValue(true);
                                loadStudents();
                            }
                        }

                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("OTPMonitor", "Failed to read OTP for " + uid + ": " + error.getMessage());
                        }
                    });
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("OTPMonitor", "OTP monitoring error: " + error.getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == QR_SCANNER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String scannedUID = data.getStringExtra("QR_RESULT");
            if (scannedUID != null) {
                markStudentPresent(scannedUID);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void markStudentPresent(String uid) {
        DatabaseReference presentRef = mDatabase.getReference()
                .child("Dates").child(date).child(eventName).child("Students").child(uid).child("present");

        presentRef.setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Student marked present!", Toast.LENGTH_SHORT).show();
                loadStudents();
            }
        });
    }

    private void loadStudents() {
        eventRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                studentList.clear();
                for (DataSnapshot studentSnapshot : task.getResult().getChildren()) {
                    String studentId = studentSnapshot.getKey();
                    boolean present = Boolean.TRUE.equals(studentSnapshot.child("present").getValue(Boolean.class));
                    boolean otp = Boolean.TRUE.equals(studentSnapshot.child("otp").getValue(Boolean.class));

                    studentList.put(studentId, Arrays.asList(present, otp));
                }

                studentAdapter = new StudentAdapter(studentList, eventRef);
                studentRecyclerView.setAdapter(studentAdapter);
            } else {
                Toast.makeText(this, "No students found for this event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteStudentData(String studentUID) {
        eventRef.child(studentUID).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Student removed", Toast.LENGTH_SHORT).show();
                loadStudents();
            } else {
                Toast.makeText(this, "Failed to delete student", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
