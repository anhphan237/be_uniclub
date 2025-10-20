package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.UserResponse;
import com.example.uniclub.entity.Role;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.UserStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.RoleRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ===================== Helper =====================
    private UserResponse toResp(User u) {
        return UserResponse.builder()
                .id(u.getUserId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .roleName(u.getRole() != null ? u.getRole().getRoleName() : null)
                .status(u.getStatus())
                .studentCode(u.getStudentCode())
                .majorName(u.getMajorName())
                .bio(u.getBio())
                .avatarUrl(u.getAvatarUrl())
                .build();
    }

    // ===================== CRUD =====================
    @Override
    public UserResponse create(UserCreateRequest req) {
        if (userRepo.existsByEmail(req.email()))
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");

        if (req.studentCode() != null && userRepo.existsByStudentCode(req.studentCode()))
            throw new ApiException(HttpStatus.CONFLICT, "Student code already exists");

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .phone(req.phone())
                .studentCode(req.studentCode())
                .majorName(req.majorName())
                .bio(req.bio())
                .role(Role.builder().roleId(req.roleId()).build())
                .status(UserStatusEnum.ACTIVE.name())
                .build();

        userRepo.save(user);

        // ✅ Gửi email chào mừng
        try {
            String subject = "Welcome to UniClub 🎉";
            String content = String.format(
                    "Hi %s,<br><br>Welcome to UniClub!<br>Your account has been successfully created.<br><br>Best regards,<br>UniClub Team 💌",
                    req.fullName());
            emailService.sendEmail(req.email(), subject, content);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send welcome email: " + e.getMessage());
        }

        return toResp(user);
    }

    @Override
    public UserResponse update(Long id, UserUpdateRequest req) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (req.fullName() != null && !req.fullName().isBlank())
            user.setFullName(req.fullName());
        if (req.phone() != null && !req.phone().isBlank())
            user.setPhone(req.phone());
        if (req.bio() != null && !req.bio().isBlank())
            user.setBio(req.bio());
        if (req.majorName() != null && !req.majorName().isBlank()) {
            validateMajor(req.majorName());
            user.setMajorName(req.majorName());
        }

        return toResp(userRepo.save(user));
    }

    @Override
    public void delete(Long id) {
        if (!userRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        userRepo.deleteById(id);
    }

    @Override
    public UserResponse get(Long id) {
        return userRepo.findById(id)
                .map(this::toResp)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Override
    public Page<UserResponse> list(Pageable pageable) {
        return userRepo.findAll(pageable).map(this::toResp);
    }

    // ===================== Search & Statistics =====================
    @Override
    public Page<UserResponse> search(String keyword, Pageable pageable) {
        String kw = (keyword == null) ? "" : keyword.trim();
        return userRepo
                .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrStudentCodeContainingIgnoreCase(
                        kw, kw, kw, pageable
                ).map(this::toResp);
    }

    @Override
    public UserResponse updateStatus(Long id, boolean active) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        user.setStatus(active ? UserStatusEnum.ACTIVE.name() : UserStatusEnum.INACTIVE.name());
        return toResp(userRepo.save(user));
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password is required");

        User user = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // ✅ Gửi thông báo
        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "Your UniClub password has been reset",
                    "Hi " + user.getFullName() + ",<br><br>Your password has been reset by an administrator.<br>Please log in again using your new credentials.<br><br>— UniClub Support 💬"
            );
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send reset email: " + e.getMessage());
        }
    }

    @Override
    public Page<UserResponse> getByRole(String roleName, Pageable pageable) {
        if (roleName == null || roleName.isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "roleName is required");
        return userRepo.findByRole_RoleNameIgnoreCase(roleName.trim(), pageable)
                .map(this::toResp);
    }

    @Override
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", userRepo.count());
        stats.put("active", userRepo.countByStatus(UserStatusEnum.ACTIVE.name()));
        stats.put("inactive", userRepo.countByStatus(UserStatusEnum.INACTIVE.name()));

        Map<String, Long> byRole = new HashMap<>();
        List<Object[]> roleRows = userRepo.countGroupByRole();
        for (Object[] row : roleRows) {
            String role = (String) row[0];
            Long cnt = (Long) row[1];
            byRole.put(role, cnt);
        }
        stats.put("byRole", byRole);
        return stats;
    }

    // ===================== PROFILE =====================
    @Override
    public User getProfile(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Override
    public User updateProfile(String email, ProfileUpdateRequest req) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (req.getPhone() != null && !req.getPhone().isBlank())
            user.setPhone(req.getPhone());
        if (req.getBio() != null && !req.getBio().isBlank())
            user.setBio(req.getBio());
        if (req.getMajorName() != null && !req.getMajorName().isBlank()) {
            validateMajor(req.getMajorName());
            user.setMajorName(req.getMajorName());
        }

        return userRepo.save(user);
    }

    @Override
    public User updateAvatar(String email, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "avatarUrl is required");

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        user.setAvatarUrl(avatarUrl);
        return userRepo.save(user);
    }

    @Override
    public User getByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    // ===================== ⚙️ Tạo tài khoản CLB (UniStaff) =====================
    @Override
    public void createClubAccounts(CreateClubAccountsRequest req) {
        String leaderEmail = req.getClubCode().toLowerCase() + "_leader@uniclub.edu.vn";
        String viceEmail = req.getClubCode().toLowerCase() + "_vice@uniclub.edu.vn";

        Role leaderRole = roleRepo.findByRoleName("CLUB_LEADER")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role CLUB_LEADER not found"));
        Role viceRole = roleRepo.findByRoleName("CLUB_VICE_LEADER")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role CLUB_VICE_LEADER not found"));

        String pwd1 = UUID.randomUUID().toString().substring(0, 8);
        String pwd2 = UUID.randomUUID().toString().substring(0, 8);

        User leader = User.builder()
                .email(leaderEmail)
                .fullName(req.getLeaderName())
                .passwordHash(passwordEncoder.encode(pwd1))
                .studentCode("LEADER_" + req.getClubCode().toUpperCase())
                .role(leaderRole)
                .status(UserStatusEnum.ACTIVE.name())
                .build();

        User vice = User.builder()
                .email(viceEmail)
                .fullName(req.getViceLeaderName())
                .passwordHash(passwordEncoder.encode(pwd2))
                .studentCode("VICE_" + req.getClubCode().toUpperCase())
                .role(viceRole)
                .status(UserStatusEnum.ACTIVE.name())
                .build();

        userRepo.saveAll(List.of(leader, vice));

        // ✅ Gửi email tài khoản
        emailService.sendEmail(leaderEmail, "Tài khoản CLB - Chủ nhiệm",
                "Xin chào " + req.getLeaderName() + ",<br>Tài khoản CLB của bạn là <b>" + leaderEmail +
                        "</b><br>Mật khẩu: <b>" + pwd1 + "</b>");
        emailService.sendEmail(viceEmail, "Tài khoản CLB - Phó chủ nhiệm",
                "Xin chào " + req.getViceLeaderName() + ",<br>Tài khoản CLB của bạn là <b>" + viceEmail +
                        "</b><br>Mật khẩu: <b>" + pwd2 + "</b>");
    }

    // ===================== Validate Major =====================
    private void validateMajor(String majorName) {
        Set<String> validMajors = Set.of(
                "Software Engineering", "Artificial Intelligence", "Information Assurance",
                "Data Science", "Business Administration", "Digital Marketing",
                "Graphic Design", "Multimedia Communication", "Hospitality Management",
                "International Business", "Finance and Banking",
                "Japanese Language", "Korean Language",
                "SE", "AI", "IA", "DS", "BA", "DM", "GD", "MC", "HM", "IB", "FB", "JP", "KR"
        );
        if (!validMajors.contains(majorName))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid major name");
    }
    @Override
    public UserResponse getProfileResponse(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return toResp(user);
    }

    @Override
    public UserResponse updateProfileResponse(String email, ProfileUpdateRequest req) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (req.getPhone() != null && !req.getPhone().isBlank())
            user.setPhone(req.getPhone());
        if (req.getBio() != null && !req.getBio().isBlank())
            user.setBio(req.getBio());
        if (req.getMajorName() != null && !req.getMajorName().isBlank())
            user.setMajorName(req.getMajorName());

        userRepo.save(user);
        return toResp(user);
    }

    @Override
    public UserResponse updateAvatarResponse(String email, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "avatarUrl is required");

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        user.setAvatarUrl(avatarUrl);
        userRepo.save(user);
        return toResp(user);
    }

}
