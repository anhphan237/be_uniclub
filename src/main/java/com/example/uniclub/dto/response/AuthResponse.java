package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private String fullName;
    private String role;
}
