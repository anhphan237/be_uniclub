package com.example.uniclub.service;

import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.UserResponse;
import com.example.uniclub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface UserService {

    // ===================== CRUD =====================
    UserResponse create(UserCreateRequest req);
    UserResponse update(Long id, UserUpdateRequest req);
    void delete(Long id);
    UserResponse get(Long id);
    Page<UserResponse> list(Pageable pageable);

    // ===================== Search & Filter =====================
    Page<UserResponse> search(String keyword, Pageable pageable);
    Page<UserResponse> getByRole(String roleName, Pageable pageable);

    // ===================== Account Management =====================
    UserResponse updateStatus(Long id, boolean active);
    void resetPassword(Long id, String newPassword);
    Map<String, Object> getUserStatistics();

    // ===================== Profile (DTO-based) =====================
    UserResponse getProfileResponse(String email);
    UserResponse updateProfileResponse(String email, ProfileUpdateRequest req);
    UserResponse updateAvatarResponse(String email, String avatarUrl);

    // ===================== Internal Use =====================
    User getByEmail(String email);

    void changePassword(String email, String oldPassword, String newPassword);

}
