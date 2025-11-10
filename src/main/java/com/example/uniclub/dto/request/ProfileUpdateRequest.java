package com.example.uniclub.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    private String fullName;
    private Long majorId;
    private String phone;
    @Size(max = 500)
    private String bio;
    private String avatarUrl;
    private String backgroundUrl;
    @Pattern(
            regexp = "^[A-Z]{2}\\d{6}$",
            message = "Student code must match format e.g., SE123456"
    )
    private String studentCode;

}
