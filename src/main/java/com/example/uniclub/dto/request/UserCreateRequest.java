package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UserCreateRequest(

        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String fullName,
        String phone,
        @NotNull Long roleId,

        // ✅ Thêm 3 thuộc tính mới
        String studentCode,   // Mã số sinh viên (duy nhất, không thể đổi)
        String majorName,     // Chuyên ngành
        String bio            // Giới thiệu bản thân (optional)
) {}
