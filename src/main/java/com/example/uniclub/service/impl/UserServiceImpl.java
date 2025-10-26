package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.UserResponse;
import com.example.uniclub.dto.response.WalletResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MembershipRepository membershipRepo;
    private final ClubRepository clubRepo;
    private final WalletRepository walletRepo;

    // ===================== Helper =====================
    private UserResponse toResp(User u) {
        List<Membership> memberships = membershipRepo.findByUser_UserId(u.getUserId());
        List<UserResponse.ClubInfo> clubInfos = memberships.stream()
                .map(m -> new UserResponse.ClubInfo(
                        m.getClub().getClubId(),
                        m.getClub().getName()
                ))
                .collect(Collectors.toList());

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
                .clubs(clubInfos)
                .build();
    }

    private WalletResponse toWalletResp(Wallet w) {
        if (w == null) return null;
        return WalletResponse.builder()
                .walletId(w.getWalletId())
                .balancePoints(w.getBalancePoints())
                .ownerType(w.getOwnerType())
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

        try {
            emailService.sendEmail(
                    req.email(),
                    "Welcome to UniClub 🎉",
                    String.format(
                            "Hi %s,<br><br>Welcome to UniClub!<br>Your account has been successfully created.<br><br>Best regards,<br>UniClub Team 💌",
                            req.fullName()
                    )
            );
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send welcome email: " + e.getMessage());
        }

        return toResp(user);
    }

    @Override
    public UserResponse update(Long id, UserUpdateRequest req) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (req.fullName() != null && !req.fullName().isBlank()) user.setFullName(req.fullName());
        if (req.phone() != null && !req.phone().isBlank()) user.setPhone(req.phone());
        if (req.bio() != null && !req.bio().isBlank()) user.setBio(req.bio());
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
                )
                .map(this::toResp);
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

        emailService.sendEmail(
                user.getEmail(),
                "Your UniClub password has been reset",
                String.format("Hi %s,<br><br>Your password has been reset.<br>— UniClub Support 💬", user.getFullName())
        );
    }

    @Override
    public Page<UserResponse> getByRole(String roleName, Pageable pageable) {
        if (roleName == null || roleName.isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "roleName is required");
        return userRepo.findByRole_RoleNameIgnoreCase(roleName.trim(), pageable).map(this::toResp);
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
            byRole.put((String) row[0], (Long) row[1]);
        }
        stats.put("byRole", byRole);
        return stats;
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

    // ===================== Profile Response =====================
    @Override
    public UserResponse getProfileResponse(String email) {
        User user = getByEmail(email);
        Wallet wallet = walletRepo.findByUser(user).orElse(null);

        UserResponse resp = toResp(user);
        resp.setWallet(toWalletResp(wallet));
        return resp;
    }

    @Override
    public UserResponse updateProfileResponse(String email, ProfileUpdateRequest req) {
        User user = getByEmail(email);
        if (req.getPhone() != null && !req.getPhone().isBlank()) user.setPhone(req.getPhone());
        if (req.getBio() != null && !req.getBio().isBlank()) user.setBio(req.getBio());
        if (req.getMajorName() != null && !req.getMajorName().isBlank()) user.setMajorName(req.getMajorName());
        userRepo.save(user);

        Wallet wallet = walletRepo.findByUser(user).orElse(null);
        UserResponse resp = toResp(user);
        resp.setWallet(toWalletResp(wallet));
        return resp;
    }

    @Override
    public UserResponse updateAvatarResponse(String email, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "avatarUrl is required");
        User user = getByEmail(email);
        user.setAvatarUrl(avatarUrl);
        userRepo.save(user);

        Wallet wallet = walletRepo.findByUser(user).orElse(null);
        UserResponse resp = toResp(user);
        resp.setWallet(toWalletResp(wallet));
        return resp;
    }

    // ===================== Internal use =====================
    @Override
    public User getByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFirstLogin(false);
        userRepo.save(user);
    }
}
