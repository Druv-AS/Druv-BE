package com.dhruv.dto;

import jakarta.validation.constraints.Size;

/**
 * A student nominating (or clearing) the parent mobile number allowed to see their reports.
 * A blank value removes the nomination and revokes any existing parent link.
 */
public class ParentContactRequestDto {

    @Size(max = 20, message = "Mobile number must be at most 20 characters")
    private String parentPhoneNumber;

    public ParentContactRequestDto() {}

    public ParentContactRequestDto(String parentPhoneNumber) {
        this.parentPhoneNumber = parentPhoneNumber;
    }

    public String getParentPhoneNumber() { return parentPhoneNumber; }
    public void setParentPhoneNumber(String parentPhoneNumber) { this.parentPhoneNumber = parentPhoneNumber; }
}
