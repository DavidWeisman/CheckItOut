package com.example.checkitout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class StudentSyncActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference studentRef;

        private static final String TAG = "StudentSync";
    private static final int PICK_XLSX_FILE = 1001;
    private List<Student> studentList = new ArrayList<>();
    private ArrayList<String> syncedStudentIds  = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        studentRef = FirebaseDatabase.getInstance().getReference("Students");

        openFilePicker();
    }
    public void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        startActivityForResult(intent, PICK_XLSX_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_XLSX_FILE && resultCode == RESULT_OK && data != null) {
            Uri fileUri  = data.getData();
            if (fileUri  != null) {
                readStudentsFromUri(fileUri );
            }
        }
    }
    public void readStudentsFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Workbook workbook = new XSSFWorkbook(is);

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getPhysicalNumberOfCells() < 5) continue;

                String fullName = row.getCell(0).toString().trim() + " " + row.getCell(1).toString().trim();
                String studentId = getCellValueAsString(row.getCell(2));
                String phone = row.getCell(3).toString().trim();
                String email = row.getCell(4).toString().trim();

                studentList.add(new Student(fullName, phone, studentId, email));
            }
            syncStudents();

        } catch (Exception e) {
            Log.e("StudentSync", "Error reading Excel file", e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case STRING:
                return cell.getStringCellValue().trim();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private void syncStudents() {
        AtomicInteger pending = new AtomicInteger(studentList.size());

        for (Student student : studentList) {
            checkAndSyncStudent(student, pending);
        }
    }

    private void checkAndSyncStudent(Student student, AtomicInteger pending) {
        mAuth.fetchSignInMethodsForEmail(student.email).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error checking sign-in methods", task.getException());
                checkComplete(pending);
                return;
            }

            List<String> signInMethods = task.getResult().getSignInMethods();
            if (signInMethods != null && !signInMethods.isEmpty()) {
                // User exists in Auth → now find UID in database
                findStudentUidByEmail(student, pending);
            } else {
                // User doesn't exist → create and add to DB
                createStudentAccount(student, pending);
            }
        });
    }

    private void findStudentUidByEmail(Student student, AtomicInteger pending) {
        studentRef.orderByChild("email").equalTo(student.email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String existingUid = snapshot.getChildren().iterator().hasNext()
                                ? snapshot.getChildren().iterator().next().getKey()
                                : null;

                        if (existingUid != null) {
                            syncedStudentIds.add(existingUid);
                        } else {
                            Log.w(TAG, "User exists in Auth but not in DB");
                        }
                        checkComplete(pending);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error fetching UID", error.toException());
                        checkComplete(pending);
                    }
                });
    }

    private void createStudentAccount(Student student, AtomicInteger pending) {
        mAuth.createUserWithEmailAndPassword(student.email, student.studentId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        uploadStudentData(uid, student, pending);
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            // Try finding UID in DB since account exists
                            findStudentUidByEmail(student, pending);
                        } else {
                            Log.e(TAG, "Failed to create user", task.getException());
                            checkComplete(pending);
                        }
                    }
                });
    }


    private void uploadStudentData(String uid, Student student, AtomicInteger pending) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", student.name);
        data.put("phoneNumber", student.phoneNumber);
        data.put("studentId", student.studentId);
        data.put("email", student.email);
        data.put("role", student.role);
        data.put("opt", student.opt);

        studentRef.child(uid).setValue(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Student synced: " + uid);
                syncedStudentIds.add(uid);
            } else {
                Log.e(TAG, "DB write failed", task.getException());
            }
            checkComplete(pending);
        });
    }

    private void checkComplete(AtomicInteger counter) {
        if (counter.decrementAndGet() == 0) {
            Intent result = new Intent();
            result.putStringArrayListExtra("syncedStudents", syncedStudentIds);
            setResult(RESULT_OK, result);
            finish();
        }
    }
}
