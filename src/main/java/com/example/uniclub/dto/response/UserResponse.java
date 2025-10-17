package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String roleName;
    private String status;
    private String studentCode;
    private String majorName;
    private String bio;
    private String avatarUrl; // ✅ hiển thị avatar trong dashboard
}
