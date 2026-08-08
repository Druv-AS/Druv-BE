package com.dhruv.dto;

public class StudentAuthDto {
    private String name;
    private String userId;
    private String phoneNumber;
    private String parentPhoneNumber;
    private String examTarget;
    private String password;
    private String mode; // 'login' or 'register'

    public StudentAuthDto() {}

    public StudentAuthDto(String name, String userId, String phoneNumber, String parentPhoneNumber, String examTarget) {
        this.name = name;
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.parentPhoneNumber = parentPhoneNumber;
        this.examTarget = examTarget;
    }

    public StudentAuthDto(String name, String userId, String phoneNumber, String parentPhoneNumber, String examTarget, String password) {
        this.name = name;
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.parentPhoneNumber = parentPhoneNumber;
        this.examTarget = examTarget;
        this.password = password;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getParentPhoneNumber() { return parentPhoneNumber; }
    public void setParentPhoneNumber(String parentPhoneNumber) { this.parentPhoneNumber = parentPhoneNumber; }

    public String getExamTarget() { return examTarget; }
    public void setExamTarget(String examTarget) { this.examTarget = examTarget; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
