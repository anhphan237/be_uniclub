package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.AdminUserResponse;
import com.example.uniclub.entity.Role;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.UserStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.AdminUserService;
import com.example.uniclub.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepo;
    private final MembershipRepository membershipRepo;
    private final EmailService emailService;
    private final RoleRepository roleRepo;
    @Override
    public Page<AdminUserResponse> getAllUsers(String keyword, Pageable pageable) {
        Page<User> users = (keyword == null || keyword.isBlank())
                ? userRepo.findAll(pageable)
                : userRepo.findByFullNameContainingIgnoreCase(keyword, pageable);
        return users.map(this::toResponse);
    }

    @Override
    public AdminUserResponse getUserDetail(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    @Override
    public void banUser(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        user.setStatus(UserStatusEnum.BANNED.getCode());
        userRepo.save(user);

        emailService.sendEmail(
                user.getEmail(),
                "Account Suspended",
                "Dear " + user.getFullName() + ",\n\n"
                        + "Your UniClub account has been temporarily locked by admin.\n"
                        + "Status: " + user.getStatus() + "\n\n"
                        + "If you believe this is a mistake, please contact UniClub support."
        );
    }

    @Override
    public void unbanUser(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        user.setStatus(UserStatusEnum.ACTIVE.getCode());
        userRepo.save(user);

        emailService.sendEmail(
                user.getEmail(),
                "Account Reactivated",
                "Dear " + user.getFullName() + ",\n\n"
                        + "Your UniClub account has been reactivated by admin.\n"
                        + "Welcome back to UniClub!"
        );
    }

    private AdminUserResponse toResponse(User user) {
        int joined = membershipRepo.countByUser_UserId(user.getUserId());
        boolean isActive = UserStatusEnum.ACTIVE.getCode().equals(user.getStatus());

        return AdminUserResponse.builder()
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                .majorName(user.getMajor() != null ? user.getMajor().getName() : null)
                .active(isActive)
                .joinedClubs(joined)
                .build();
    }
    @Override
    public void changeUserRole(Long userId, String roleName) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Role newRole = roleRepo.findByRoleName(roleName)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role not found: " + roleName));

        user.setRole(newRole);
        userRepo.save(user);
    }

}
