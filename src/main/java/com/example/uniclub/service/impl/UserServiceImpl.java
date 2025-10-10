package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.ProfileUpdateRequest;
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

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    // ==============================
    // 🔹 Dùng cho ADMIN / STAFF
    // ==============================

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
                .build();
    }

    @Override
    public UserResponse create(UserCreateRequest req) {
        if (userRepo.existsByEmail(req.email()))
            throw new ApiException(HttpStatus.CONFLICT, "Email đã tồn tại");

        if (req.studentCode() != null && userRepo.existsByStudentCode(req.studentCode()))
            throw new ApiException(HttpStatus.CONFLICT, "Mã số sinh viên đã tồn tại");

        User u = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .phone(req.phone())
                .studentCode(req.studentCode())
                .majorName(req.majorName())
                .bio(req.bio())
                .role(Role.builder().roleId(req.roleId()).build())
                .build();

        return toResp(userRepo.save(u));
    }

    @Override
    public UserResponse update(Long id, UserUpdateRequest req) {
        var u = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        if (req.fullName() != null) u.setFullName(req.fullName());
        if (req.phone() != null) u.setPhone(req.phone());
        if (req.bio() != null) u.setBio(req.bio());
        if (req.majorName() != null) u.setMajorName(req.majorName());

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


    // ==============================
    // 🔹 Dùng cho STUDENT / MEMBER / CLUB_LEADER
    // ==============================

    /** ✅ Lấy hồ sơ của chính người dùng (qua email đăng nhập) */
    public User getProfile(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    /** ✅ Cập nhật hồ sơ của chính người dùng */
    public User updateProfile(String email, ProfileUpdateRequest req) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        // Cập nhật các trường có giá trị mới
        if (req.getPhone() != null && !req.getPhone().isBlank())
            user.setPhone(req.getPhone());

        if (req.getBio() != null && !req.getBio().isBlank())
            user.setBio(req.getBio());

        if (req.getMajorName() != null && !req.getMajorName().isBlank()) {
            Set<String> validMajors = Set.of(
                    "Software Engineering", "Artificial Intelligence", "Information Assurance",
                    "Data Science", "Business Administration", "Digital Marketing",
                    "Graphic Design", "Multimedia Communication", "Hospitality Management",
                    "International Business", "Finance and Banking",
                    "Japanese Language", "Korean Language",
                    // cho phép viết tắt
                    "SE", "AI", "IA", "DS", "BA", "DM", "GD", "MC", "HM", "IB", "FB", "JP", "KR"
            );

            if (!validMajors.contains(req.getMajorName()))
                throw new ApiException(HttpStatus.BAD_REQUEST, "Ngành học không hợp lệ");

            user.setMajorName(req.getMajorName());
        }

        return userRepo.save(user);
    }
}
