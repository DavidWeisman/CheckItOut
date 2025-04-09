package com.example.checkitout;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
    private Map<String, List<Boolean>> studentList;
    private DatabaseReference eventRef;

    public StudentAdapter(Map<String, List<Boolean>> studentList, DatabaseReference eventRef) {
        this.studentList = studentList;
        this.eventRef = eventRef;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        // Get the student ID and the list of booleans (present, otp) from the map
        String studentId = new ArrayList<>(studentList.keySet()).get(position);
        List<Boolean> studentStatus = studentList.get(studentId);

        // Fetch the student's name from Firebase
        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference().child("Students").child(studentId);

        // Fetch the student's name
        studentRef.child("name").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String studentName = task.getResult().getValue(String.class);
                holder.studentNameTextView.setText(studentName);  // Set the student's name
            } else {
                holder.studentNameTextView.setText("Unknown Student");  // Fallback if name not found
            }
        });

        // Fetch the student's phone number
        studentRef.child("phoneNumber").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String phoneNumber = task.getResult().getValue(String.class);

                // Set the button click listener to open the dialer
                holder.callButton.setOnClickListener(v -> {
                    if (phoneNumber != null && !phoneNumber.isEmpty()) {
                        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
                        v.getContext().startActivity(dialIntent);  // Use v.getContext() to get the context
                    } else {
                        Toast.makeText(v.getContext(), "Phone number not available", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Get the 'present' value from the studentStatus list and set the checkbox
        boolean isPresent = studentStatus.get(0);  // First boolean is 'present'
        holder.attendanceCheckBox.setChecked(isPresent);

        boolean isOtpSuccess = studentStatus.get(1);  // Second boolean is 'otp'
        holder.otpSuccessCheckBox.setChecked(isOtpSuccess);

        // Update attendance status in the Firebase database when the checkbox is clicked
        holder.attendanceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DatabaseReference studentStatusRef = eventRef.child(studentId).child("present");
            studentStatusRef.setValue(isChecked);
        });
    }

    public String getStudentUID(int position) {
        // Assuming you have a List<Map<String, Object>> or something similar in your adapter
        // Modify this to match the structure you're using to hold student data
        Map.Entry<String, List<Boolean>> studentEntry = (Map.Entry<String, List<Boolean>>) studentList.entrySet().toArray()[position];
        return studentEntry.getKey();  // This returns the studentUID
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    // Method to delete a student
    public void deleteStudent(int position) {
        String studentId = new ArrayList<>(studentList.keySet()).get(position);
        studentList.remove(studentId);
        notifyItemRemoved(position);
    }


    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameTextView;
        ImageButton callButton;
        CheckBox attendanceCheckBox;
        CheckBox otpSuccessCheckBox;

        public StudentViewHolder(View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.studentNameTextView);
            attendanceCheckBox = itemView.findViewById(R.id.attendanceCheckBox);
            otpSuccessCheckBox = itemView.findViewById(R.id.otpSuccessCheckBox);
            callButton =  itemView.findViewById(R.id.callButton);
        }
    }
}
