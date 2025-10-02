package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.UserCreateRequest;
import com.example.uniclub.dto.request.UserUpdateRequest;
import com.example.uniclub.dto.response.UserResponse;
import com.example.uniclub.entity.Role;
import com.example.uniclub.entity.User;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder; // cần bean PasswordEncoder (đã có nếu bạn cấu hình Security)

    private UserResponse toResp(User u){
        return UserResponse.builder()
                .id(u.getUserId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .roleName(u.getRole()!=null? u.getRole().getRoleName(): null)
                .status(u.getStatus())
                .build();
    }

    @Override
    public UserResponse create(UserCreateRequest req) {
        if (userRepo.existsByEmail(req.email()))
            throw new ApiException(HttpStatus.CONFLICT, "Email đã tồn tại");

        User u = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .phone(req.phone())
                .role(Role.builder().roleId(req.roleId()).build())
                .build();

        return toResp(userRepo.save(u));
    }

    @Override
    public UserResponse update(Long id, UserUpdateRequest req) {
        var u = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        u.setFullName(req.fullName());
        u.setPhone(req.phone());
        return toResp(userRepo.save(u));
    }

    @Override
    public void delete(Long id) {
        if (!userRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "User không tồn tại");
        userRepo.deleteById(id);
    }

    @Override
    public UserResponse get(Long id) {
        return userRepo.findById(id).map(this::toResp)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User không tồn tại"));
    }

    @Override
    public Page<UserResponse> list(Pageable pageable) {
        return userRepo.findAll(pageable).map(this::toResp);
    }
}
