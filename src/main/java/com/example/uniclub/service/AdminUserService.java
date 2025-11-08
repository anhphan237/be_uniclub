package com.example.uniclub.service;


import com.example.uniclub.dto.response.AdminUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    Page<AdminUserResponse> getAllUsers(String keyword, Pageable pageable);
    AdminUserResponse getUserDetail(Long id);
    void banUser(Long id);
    void unbanUser(Long id);
    void changeUserRole(Long userId, String roleName);

}
