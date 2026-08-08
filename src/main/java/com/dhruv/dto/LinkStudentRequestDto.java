package com.dhruv.dto;

public class LinkStudentRequestDto {
    private String parentPhoneNumber;
    private String studentIdentifier; // userId or phone number

    public LinkStudentRequestDto() {}

    public LinkStudentRequestDto(String parentPhoneNumber, String studentIdentifier) {
        this.parentPhoneNumber = parentPhoneNumber;
        this.studentIdentifier = studentIdentifier;
    }

    public String getParentPhoneNumber() { return parentPhoneNumber; }
    public void setParentPhoneNumber(String parentPhoneNumber) { this.parentPhoneNumber = parentPhoneNumber; }

    public String getStudentIdentifier() { return studentIdentifier; }
    public void setStudentIdentifier(String studentIdentifier) { this.studentIdentifier = studentIdentifier; }
}
