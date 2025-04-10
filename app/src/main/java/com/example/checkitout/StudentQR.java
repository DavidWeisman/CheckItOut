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

        qrCodeIV = findViewById(R.id.idIVQrcode);
        returnbtn = findViewById(R.id.returnbtn);
        mAuth = FirebaseAuth.getInstance(); 

        returnbtn.setOnClickListener(v -> startActivity(new Intent(StudentQR.this, StudentMainActivity.class)));

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showToast("User not logged in!");
            finish();
            return;
        }

        studentId = currentUser.getUid();
        generateQRCode(studentId);
    }

    private void generateQRCode(String uid) {
        try {
            int size = getResources().getDimensionPixelSize(R.dimen.qr_code_size);
            Bitmap qrBitmap = new BarcodeEncoder().encodeBitmap(uid, BarcodeFormat.QR_CODE, size, size);
            qrCodeIV.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            showToast("Failed to generate QR code");
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show();
    }
}
