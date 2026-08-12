package com.dhruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Credentials for the parent login/registration endpoint. See {@link StudentAuthDto}. */
public class ParentAuthDto {

    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Size(max = 100, message = "User ID must be at most 100 characters")
    @Pattern(regexp = "^$|^[a-zA-Z0-9_.-]{3,100}$",
             message = "User ID may contain only letters, numbers, dot, dash and underscore (3-100 characters)")
    private String userId;

    @NotBlank(message = "Mobile number is required")
    @Size(max = 20, message = "Mobile number must be at most 20 characters")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    @Pattern(regexp = "(?i)^(login|register)$", message = "Mode must be either 'login' or 'register'")
    private String mode;

    public ParentAuthDto() {}

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
