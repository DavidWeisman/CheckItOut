package com.example.checkitout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

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
        studentRef.child("name").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String studentName = task.getResult().getValue(String.class);
                holder.studentNameTextView.setText(studentName);  // Set the student's name
            } else {
                holder.studentNameTextView.setText("Unknown Student");  // Fallback if name not found
            }
        });

        // Get the 'present' value from the studentStatus list and set the checkbox

        boolean isPresent = studentStatus.get(0);  // First boolean is 'present'
        holder.attendanceCheckBox.setChecked(isPresent);

        boolean isOtpSuccess = studentStatus.get(1);  // Second boolean is 'otp'
        holder.otpSuccessCheckBox.setChecked(isOtpSuccess);


        holder.attendanceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DatabaseReference studentStatusRef = eventRef.child(studentId).child("present");
            studentStatusRef.setValue(isChecked);
        });
    }


    @Override
    public int getItemCount() {
        return studentList.size();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameTextView;
        CheckBox attendanceCheckBox;
        CheckBox otpSuccessCheckBox;

        public StudentViewHolder(View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.studentNameTextView);
            attendanceCheckBox = itemView.findViewById(R.id.attendanceCheckBox);
            otpSuccessCheckBox = itemView.findViewById(R.id.otpSuccessCheckBox);
        }
    }
}
