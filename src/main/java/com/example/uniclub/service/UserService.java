package com.example.uniclub.service;

import com.example.uniclub.dto.request.ProfileUpdateRequest;
import com.example.uniclub.dto.request.UserCreateRequest;
import com.example.uniclub.dto.request.UserUpdateRequest;
import com.example.uniclub.dto.response.UserResponse;
import com.example.uniclub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface UserService {

    // 🔹 CRUD cơ bản
    UserResponse create(UserCreateRequest req);
    UserResponse update(Long id, UserUpdateRequest req);
    void delete(Long id);
    UserResponse get(Long id);
    Page<UserResponse> list(Pageable pageable);

    // 🔹 Tính năng mở rộng
    Page<UserResponse> search(String keyword, Pageable pageable);
    UserResponse updateStatus(Long id, boolean active);
    void resetPassword(Long id, String newPassword);
    Page<UserResponse> getByRole(String roleName, Pageable pageable);
    Map<String, Object> getUserStatistics();

    // 🔹 Dành cho người dùng cá nhân (ProfileController dùng)
    User getProfile(String email);
    User updateProfile(String email, ProfileUpdateRequest req);
    User updateAvatar(String email, String avatarUrl);
}
