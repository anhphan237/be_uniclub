package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.ProfileUpdateRequest;
import com.example.uniclub.entity.User;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;

    // ✅ Danh sách ngành học hợp lệ
    private static final Map<String, String> MAJOR_MAP = new HashMap<>() {{
        put("SE", "Software Engineering");
        put("AI", "Artificial Intelligence");
        put("IA", "Information Assurance");
        put("DS", "Data Science");
        put("BA", "Business Administration");
        put("DM", "Digital Marketing");
        put("GD", "Graphic Design");
        put("MC", "Multimedia Communication");
        put("HM", "Hospitality Management");
        put("IB", "International Business");
        put("FB", "Finance and Banking");
        put("JL", "Japanese Language");
        put("KL", "Korean Language");

        // Full names (để hỗ trợ người nhập đầy đủ)
        put("Software Engineering", "Software Engineering");
        put("Artificial Intelligence", "Artificial Intelligence");
        put("Information Assurance", "Information Assurance");
        put("Data Science", "Data Science");
        put("Business Administration", "Business Administration");
        put("Digital Marketing", "Digital Marketing");
        put("Graphic Design", "Graphic Design");
        put("Multimedia Communication", "Multimedia Communication");
        put("Hospitality Management", "Hospitality Management");
        put("International Business", "International Business");
        put("Finance and Banking", "Finance and Banking");
        put("Japanese Language", "Japanese Language");
        put("Korean Language", "Korean Language");
    }};

    /**
     * ✅ Cập nhật thông tin hồ sơ người dùng
     * - Cho phép cập nhật: majorName, phone, bio
     * - Không cho đổi MSSV
     * - majorName phải hợp lệ (viết đầy đủ hoặc viết tắt)
     */
    @Transactional
    public User updateProfile(String email, ProfileUpdateRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        boolean updated = false;

        // ✅ Kiểm tra & cập nhật majorName
        if (req.getMajorName() != null && !req.getMajorName().isEmpty()) {
            String input = req.getMajorName().trim();
            String normalizedMajor = MAJOR_MAP.get(input);

            if (normalizedMajor == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid major name. Valid majors: " + String.join(", ", MAJOR_MAP.values().stream().distinct().toList()));
            }

            user.setMajorName(normalizedMajor);
            updated = true;
        }

        // ✅ Cập nhật phone (nếu có)
        if (req.getPhone() != null && !req.getPhone().isEmpty()) {
            user.setPhone(req.getPhone());
            updated = true;
        }

        // ✅ Cập nhật bio (nếu có)
        if (req.getBio() != null && !req.getBio().isEmpty()) {
            user.setBio(req.getBio());
            updated = true;
        }

        if (!updated) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No valid fields to update");
        }

        return userRepository.save(user);
    }

    /**
     * ✅ Lấy thông tin hồ sơ hiện tại (cho trang Profile FE)
     */
    @Transactional(readOnly = true)
    public User getProfile(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
