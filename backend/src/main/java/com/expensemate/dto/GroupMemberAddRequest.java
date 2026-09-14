package com.expensemate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GroupMemberAddRequest {

    @NotBlank(message = "Member email is required")
    @Email(message = "A valid email address is required")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}