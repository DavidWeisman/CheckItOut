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
    private DatabaseReference databaseRef;

    private static final int PICK_XLSX_FILE = 1001;
    private List<Student> studentList = new ArrayList<>();
    private ArrayList<String> selectedStudentIds = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Students");

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
            Uri uri = data.getData();
            if (uri != null) {
                readStudentsFromUri(uri);
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

                String firstName = row.getCell(0).toString().trim();
                String lastName = row.getCell(1).toString().trim();

                // Read the studentId cell as a string to avoid scientific notation
                String studentId = getCellValueAsString(row.getCell(2));

                String phone = row.getCell(3).toString().trim();
                String email = row.getCell(4).toString().trim();

                Student student = new Student(firstName + " " + lastName, phone, studentId, email);
                studentList.add(student);
            }

            workbook.close();
            is.close();
            syncStudents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to read cell as string
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case NUMERIC:
                // If it's a number, convert it to a string and prevent scientific notation
                return String.valueOf((long) cell.getNumericCellValue());
            case STRING:
                return cell.getStringCellValue().trim();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }


    public void syncStudents() {
        // Create a counter to track pending database writes
        AtomicInteger pendingWrites = new AtomicInteger(studentList.size());

        for (Student student : studentList) {
            String email = student.email;
            String password = student.studentId; // Using student ID as password

            mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    boolean exists = !Objects.requireNonNull(task.getResult().getSignInMethods()).isEmpty();

                    if (!exists) {
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(authTask -> {
                                    if (authTask.isSuccessful()) {
                                        FirebaseUser newUser = mAuth.getCurrentUser();
                                        if (newUser != null) {
                                            addStudentToDatabase(newUser.getUid(), student, pendingWrites);
                                        }
                                    } else if (authTask.getException() instanceof FirebaseAuthUserCollisionException) {
                                        Log.d("StudentSync", "User already exists in Auth but not in DB.");
                                        // This could happen if a user was created in Firebase Auth but not in the DB.
                                        addStudentToDatabase(null, student, pendingWrites); // Attempt to add student to DB anyway
                                    } else {
                                        Log.e("StudentSync", "Error creating user: " + authTask.getException().getMessage());
                                    }
                                });
                    } else {
                        Log.d("StudentSync", "User already exists in Auth");
                        // Optional: Sync with DB if needed
                        addStudentToDatabase(null, student, pendingWrites); // Skip creating user, just add to DB
                    }
                } else {
                    Log.e("StudentSync", "Error checking email: " + task.getException());
                }
            });
        }
    }

    private void addStudentToDatabase(String uid, Student student, AtomicInteger pendingWrites) {
        if (uid == null) {
            // This case happens if the user already exists in Auth
            // We still need to retrieve their UID later
            databaseRef.orderByChild("email").equalTo(student.email).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Get the UID of the existing student from DB
                        String existingUid = dataSnapshot.getChildren().iterator().next().getKey();
                        if (existingUid != null) {
                            selectedStudentIds.add(existingUid); // Add existing UID to list
                        }
                    }

                    // Decrement the pendingWrites counter
                    if (pendingWrites.decrementAndGet() == 0) {
                        Intent resultIntent = new Intent();
                        resultIntent.putStringArrayListExtra("syncedStudents", selectedStudentIds);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                        //getStudentUIDsFromEmails(); // All student data is processed, now fetch UIDs
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("StudentSync", "Error retrieving existing student UID: " + databaseError.getMessage());
                }
            });
        } else {
            // Add the new student to the database
            Map<String, Object> studentData = new HashMap<>();
            studentData.put("name", student.name);
            studentData.put("phoneNumber", student.phoneNumber);
            studentData.put("studentId", student.studentId);
            studentData.put("email", student.email);
            studentData.put("role", student.role);
            studentData.put("opt", student.opt);

            databaseRef.child(uid).setValue(studentData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("StudentSync", "Student added to DB: " + uid);
                    selectedStudentIds.add(uid); // Add new student UID to the list
                } else {
                    Log.e("StudentSync", "DB error: " + task.getException().getMessage());
                }

                // Decrement the pendingWrites counter
                if (pendingWrites.decrementAndGet() == 0) {
                    Intent resultIntent = new Intent();
                    resultIntent.putStringArrayListExtra("syncedStudents", selectedStudentIds);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                    //getStudentUIDsFromEmails(); // All student data is processed, now fetch UIDs
                }
            });
        }
    }


    public void getStudentUIDsFromEmails() {
        // Reference to the "Students" node in the Firebase Database
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("Students");

        // Counter to track the number of queries
        final int[] pendingQueries = {studentList.size()}; // Initialize counter to the size of the studentList

        // Loop through each student in the list to get their emails
        for (final Student student : studentList) {
            final String email = student.email; // Get email from the student object

            // Fetch all students from Firebase and manually check their emails
            studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Loop through all students in the "Students" node
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        // Retrieve the student data as a Student object
                        Student studentFromDB = snapshot.getValue(Student.class);
                        String uid = snapshot.getKey(); // UID is the key of the student node

                        // Check if the email matches
                        if (studentFromDB != null && studentFromDB.email != null && studentFromDB.email.equals(email)) {
                            selectedStudentIds.add(uid); // Add the UID to the list if the email matches
                        }
                    }

                    // Decrease the pending query counter after this query is completed
                    pendingQueries[0]--;

                    // If all queries have been completed, send the result
                    if (pendingQueries[0] == 0) {
                        Intent resultIntent = new Intent();
                        resultIntent.putStringArrayListExtra("syncedStudents", selectedStudentIds);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle database errors here
                    pendingQueries[0]--; // Decrease counter even if there is an error
                }
            });
        }
    }


    // Student class
    public static class Student {
        public String name;
        public String phoneNumber;
        public String studentId;
        public String email;
        public String role;
        public String opt;
        public Student() {
        }

        public Student(String name, String phoneNumber, String studentId, String email) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.studentId = studentId;
            this.email = email;
            this.role = "student";
            this.opt = "";
        }

        public String getName() {
            return name;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getEmail() {
            return email;
        }
    }
}
