package com.example.checkitout;

public class Student {
    public String name;
    public String phoneNumber;
    public String studentId;
    public String email;
    public String role = "student";
    public String opt = "";
    public Student() {}

    public Student(String name, String phoneNumber, String studentId, String email) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.studentId = studentId;
        this.email = email;
    }
}