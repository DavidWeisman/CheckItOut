package com.example.checkitout;

public class Teacher {
    public String email;
    public String role;
    public String name;

    public Teacher() {
        // Default constructor for Firebase
    }

    public Teacher(String email, String role, String name) {
        this.email = email;
        this.role = role;
        this.name = name;
    }
}
