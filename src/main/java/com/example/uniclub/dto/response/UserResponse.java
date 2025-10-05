package com.example.uniclub.dto.response;

import com.example.uniclub.enums.UserStatusEnum;
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
}
