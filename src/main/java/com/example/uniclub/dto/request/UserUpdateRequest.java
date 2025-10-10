package com.example.uniclub.dto.request;

import lombok.Builder;

@Builder
public record UserUpdateRequest(
        String fullName,
        String phone,
        String majorName,
        String bio   // ✅ thêm thuộc tính mới
) {}
