package com.dhruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to attach a student to the calling parent's portal.
 *
 * <p>{@code parentPhoneNumber} was deliberately removed: it used to be supplied by the
 * client, which let any caller link any student to any phone number and then read that
 * student's reports. The parent is now taken from the authenticated session.
 */
public class LinkStudentRequestDto {

    @NotBlank(message = "Student user ID or mobile number is required")
    @Size(max = 100, message = "Identifier must be at most 100 characters")
    private String studentIdentifier;

    public LinkStudentRequestDto() {}

    public LinkStudentRequestDto(String studentIdentifier) {
        this.studentIdentifier = studentIdentifier;
    }

    public String getStudentIdentifier() { return studentIdentifier; }
    public void setStudentIdentifier(String studentIdentifier) { this.studentIdentifier = studentIdentifier; }
}
