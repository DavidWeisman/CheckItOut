package com.example.checkitout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendAttendanceReportActivity extends AppCompatActivity {

    private String date;
    private String eventName;
    private Map<String, String> studentNameMap = new HashMap<>(); // UID -> name


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get date and event name from intent
        Intent intent = getIntent();
        date = intent.getStringExtra("date");
        eventName = intent.getStringExtra("eventName");

        if (date == null || eventName == null) {
            Toast.makeText(this, "Missing event data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadStudentNamesThenGenerateReport();
    }

    private void getReportFromFirebase() {
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference()
                .child("Dates")
                .child(date)
                .child(eventName);

        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(SendAttendanceReportActivity.this, "Event not found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String teacherId = snapshot.child("TeacherId").getValue(String.class);
                DataSnapshot attendanceSnapshot = snapshot.child("Students");

                DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference()
                        .child("Teachers").child(teacherId).child("name");

                DatabaseReference allStudentsRef = FirebaseDatabase.getInstance().getReference()
                        .child("Students");

                teachersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot teacherSnapshot) {
                        String teacherName = teacherSnapshot.getValue(String.class);

                        allStudentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot studentsSnapshot) {

                                // Create Excel workbook and sheet
                                Workbook workbook = new XSSFWorkbook();
                                Sheet sheet = workbook.createSheet("Attendance Report");

                                int rowIdx = 0;

                                // Title row
                                Row titleRow = sheet.createRow(rowIdx++);
                                titleRow.createCell(0).setCellValue("Event:");
                                titleRow.createCell(1).setCellValue(eventName);

                                Row dateRow = sheet.createRow(rowIdx++);
                                dateRow.createCell(0).setCellValue("Date:");
                                dateRow.createCell(1).setCellValue(date);

                                Row teacherRow = sheet.createRow(rowIdx++);
                                teacherRow.createCell(0).setCellValue("Teacher:");
                                teacherRow.createCell(1).setCellValue(teacherName != null ? teacherName : teacherId);

                                rowIdx++; // Add an empty row

                                // Header row
                                Row header = sheet.createRow(rowIdx++);
                                header.createCell(0).setCellValue("Student Name");
                                header.createCell(1).setCellValue("Attendance");

                                for (DataSnapshot studentEntry : attendanceSnapshot.getChildren()) {
                                    String uid = studentEntry.getKey();
                                    Boolean present = studentEntry.child("present").getValue(Boolean.class);

                                    String studentName = uid; // default to UID
                                    if (studentsSnapshot.hasChild(uid)) {
                                        studentName = studentsSnapshot.child(uid).child("name").getValue(String.class);
                                    }

                                    Row row = sheet.createRow(rowIdx++);
                                    row.createCell(0).setCellValue(studentName);
                                    row.createCell(1).setCellValue(present != null && present ? "Present" : "Absent");
                                }

                                // Save Excel file to external storage
                                try {
                                    File dir = new File(getExternalFilesDir(null), "Reports");
                                    if (!dir.exists()) {
                                        dir.mkdirs();
                                    }

                                    File file = new File(dir, "Attendance_Report_" + eventName + "_" + date + ".xlsx");

                                    FileOutputStream fos = new FileOutputStream(file);
                                    workbook.write(fos);
                                    fos.close();
                                    workbook.close();

                                    Toast.makeText(SendAttendanceReportActivity.this, "Excel file saved.", Toast.LENGTH_SHORT).show();

                                    // Share the file
                                    Uri fileUri = FileProvider.getUriForFile(
                                            SendAttendanceReportActivity.this,
                                            getApplicationContext().getPackageName() + ".fileprovider",
                                            file
                                    );

                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(Intent.createChooser(shareIntent, "Share Excel Report"));

                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(SendAttendanceReportActivity.this, "Failed to create Excel file.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(SendAttendanceReportActivity.this, "Error loading students.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SendAttendanceReportActivity.this, "Error loading teacher.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SendAttendanceReportActivity.this, "Database error.", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private File generateExcelReport(String eventName, String date, String teacherName, List<String[]> data) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance");

        int rowNum = 0;

        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Event Name");
        headerRow.createCell(1).setCellValue(eventName);

        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Date");
        dateRow.createCell(1).setCellValue(date);

        Row teacherRow = sheet.createRow(rowNum++);
        teacherRow.createCell(0).setCellValue("Teacher ID");
        teacherRow.createCell(1).setCellValue(teacherName);

        rowNum++; // blank row

        for (String[] line : data) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < line.length; i++) {
                row.createCell(i).setCellValue(line[i]);
            }
        }

        File file = new File(getExternalFilesDir(null), eventName + "_" + date + ".xlsx");
        FileOutputStream fos = new FileOutputStream(file);
        workbook.write(fos);
        fos.close();
        workbook.close();

        return file;
    }

    private void shareExcelFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Excel Report"));
    }

    private void loadStudentNamesThenGenerateReport() {
        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference().child("Students");
        studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    if (uid != null && name != null) {
                        studentNameMap.put(uid, name);
                    }
                }
                getReportFromFirebase(); // Now safe to call
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SendAttendanceReportActivity.this, "Failed to load student names", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
