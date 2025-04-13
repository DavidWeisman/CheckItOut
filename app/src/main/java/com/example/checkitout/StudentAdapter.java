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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
    private final Map<String, List<Boolean>> studentMap;
    private final List<String> studentKeys;
    private final DatabaseReference eventRef;

    public StudentAdapter(Map<String, List<Boolean>> studentMap, DatabaseReference eventRef) {
        this.studentMap = studentMap;
        this.studentKeys = new ArrayList<>(studentMap.keySet());
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
        String studentId = studentKeys.get(position);
        List<Boolean> status = studentMap.get(studentId);
        if (status == null || status.size() < 2) return;

        boolean isPresent = status.get(0);
        boolean isOtpSuccess = status.get(1);

        holder.attendanceCheckBox.setOnCheckedChangeListener(null);
        holder.attendanceCheckBox.setChecked(isPresent);
        holder.otpSuccessCheckBox.setChecked(isOtpSuccess);

        fetchStudentDetailsAndBind(studentId, holder);

        holder.attendanceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            eventRef.child(studentId).child("present").setValue(isChecked);
        });
    }

    private void fetchStudentDetailsAndBind(String studentId, StudentViewHolder holder) {
        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("Students").child(studentId);

        studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String phone = snapshot.child("phoneNumber").getValue(String.class);

                holder.studentNameTextView.setText(name != null ? name : "Unknown Student");

                holder.callButton.setOnClickListener(v -> {
                    if (phone != null && !phone.isEmpty()) {
                        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                        v.getContext().startActivity(dialIntent);
                    } else {
                        Toast.makeText(v.getContext(), "Phone number not available", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                holder.studentNameTextView.setText("Error loading student");
            }
        });
    }


    public String getStudentUID(int position) {
        return studentKeys.get(position);
    }

    @Override
    public int getItemCount() {
        return studentKeys.size();
    }

    // Method to delete a student
    public void deleteStudent(int position) {
        String studentId = studentKeys.get(position);
        studentMap.remove(studentId);
        studentKeys.remove(position);
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
