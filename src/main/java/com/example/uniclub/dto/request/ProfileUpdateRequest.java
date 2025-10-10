package com.example.uniclub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    private String majorName;    // ✅ Có thể cập nhật
    private String phone;        // ✅ Có thể cập nhật
    @Size(max = 500)
    private String bio;          // ✅ Có thể cập nhật
}
