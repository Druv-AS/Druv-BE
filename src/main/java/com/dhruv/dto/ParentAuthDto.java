package com.dhruv.dto;

public class ParentAuthDto {
    private String name;
    private String userId;
    private String phoneNumber;
    private String password;
    private String mode; // 'login' or 'register'

    public ParentAuthDto() {}

    public ParentAuthDto(String name, String userId, String phoneNumber) {
        this.name = name;
        this.userId = userId;
        this.phoneNumber = phoneNumber;
    }

    public ParentAuthDto(String name, String userId, String phoneNumber, String password) {
        this.name = name;
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
