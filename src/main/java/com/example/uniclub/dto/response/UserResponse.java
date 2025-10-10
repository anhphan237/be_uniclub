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

    // ✅ Thêm 3 thuộc tính mới cho profile
    private String studentCode;  // Mã số sinh viên (duy nhất, không đổi)
    private String majorName;    // Chuyên ngành
    private String bio;          // Giới thiệu bản thân (optional)
}
