package com.example.uniclub.dto.request;

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


}
