package com.dhruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Credentials for the student login/registration endpoint.
 *
 * <p>Bean validation is enforced here rather than in the service. {@code name},
 * {@code parentPhoneNumber} and {@code examTarget} are optional at the DTO level because
 * login does not supply them; {@link com.dhruv.service.AuthService} requires them for
 * registration.
 */
public class StudentAuthDto {

    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Size(max = 100, message = "User ID must be at most 100 characters")
    @Pattern(regexp = "^$|^[a-zA-Z0-9_.-]{3,100}$",
             message = "User ID may contain only letters, numbers, dot, dash and underscore (3-100 characters)")
    private String userId;

    @NotBlank(message = "Mobile number is required")
    @Size(max = 20, message = "Mobile number must be at most 20 characters")
    private String phoneNumber;

    @Size(max = 20, message = "Parent mobile number must be at most 20 characters")
    private String parentPhoneNumber;

    @Size(max = 100, message = "Exam target must be at most 100 characters")
    private String examTarget;

    /**
     * Minimum eight characters, following NIST SP 800-63B: length is the control that
     * matters, so no composition rules are imposed. The upper bound guards against a
     * long-password denial of service, since BCrypt cost is paid per request.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    @Pattern(regexp = "(?i)^(login|register)$", message = "Mode must be either 'login' or 'register'")
    private String mode;

    public StudentAuthDto() {}

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
