package com.example.uniclub.service;

import com.example.uniclub.dto.request.UserCreateRequest;
import com.example.uniclub.dto.request.UserUpdateRequest;
import com.example.uniclub.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse create(UserCreateRequest req);
    UserResponse update(Long id, UserUpdateRequest req);
    void delete(Long id);
    UserResponse get(Long id);
    Page<UserResponse> list(Pageable pageable);
}
