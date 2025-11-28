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

        // ✅ Thay thế majorName → majorId
        String studentCode,   // Mã số sinh viên
        Long majorId, // ID của chuyên ngành (tham chiếu bảng majors)
        String bio             // Giới thiệu bản thân (optional)
) {}
