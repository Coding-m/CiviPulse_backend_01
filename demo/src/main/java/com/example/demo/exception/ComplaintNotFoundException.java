package com.example.demo.exception;

public class ComplaintNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L; // optional but recommended

    public ComplaintNotFoundException(String message) {
        super(message);
    }
}
