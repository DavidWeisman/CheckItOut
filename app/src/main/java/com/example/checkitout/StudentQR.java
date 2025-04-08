package com.example.checkitout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class StudentQR extends AppCompatActivity {

    private ImageView qrCodeIV;
    private Button returnbtn;
    private FirebaseAuth mAuth;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_qr);

        // Initialize UI components
        qrCodeIV = findViewById(R.id.idIVQrcode);
        returnbtn = findViewById(R.id.returnbtn);
        mAuth = FirebaseAuth.getInstance(); 

        returnbtn.setOnClickListener(v -> startActivity(new Intent(StudentQR.this, StudentMainActivity.class)));

        // Check if user is logged in and get student ID
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        studentId = currentUser.getUid();
        generateQRCode(studentId);
    }

    // Method to generate and display the QR code
    private void generateQRCode(String data) {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        try {
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 400, 400);
            qrCodeIV.setImageBitmap(bitmap); // Set the QR code to the ImageView
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }
}
