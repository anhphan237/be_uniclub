package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteProfileRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String phone;

    @NotBlank
    private String studentCode;  // đúng theo entity

    private Long majorId;        // foreign key -> Major entity

    private String bio;          // optional
    private String backgroundUrl;
}
