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

import java.util.List;
import java.util.Map;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
    private List<Map.Entry<String, Boolean>> studentList;
    private DatabaseReference eventRef;

    public StudentAdapter(List<Map.Entry<String, Boolean>> studentList, DatabaseReference eventRef) {
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
        Map.Entry<String, Boolean> student = studentList.get(position);
        String studentId = student.getKey();
        boolean isPresent = student.getValue();

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

        holder.attendanceCheckBox.setChecked(isPresent);

        holder.attendanceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DatabaseReference studentStatusRef = eventRef.child(studentId);
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

        public StudentViewHolder(View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.studentNameTextView);
            attendanceCheckBox = itemView.findViewById(R.id.attendanceCheckBox);
        }
    }
}
