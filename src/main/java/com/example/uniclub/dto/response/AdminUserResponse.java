package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String majorName;
    private boolean active;
    private int joinedClubs;
    private String studentCode; // số CLB đã tham gia
}