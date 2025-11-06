package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GoogleLoginResponse {
    private String token;
    private String email;
    private String fullName;
    private String avatar;
    private Long userId;
    private String role;
    private boolean staff;
    private List<Long> clubIds;
}

