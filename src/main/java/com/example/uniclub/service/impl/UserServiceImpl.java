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

    // ==========================================
    // 🔹 Dùng cho ADMIN / STAFF (CRUD User)
    // ==========================================

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
        // ✅ Kiểm tra email & MSSV trùng
        if (userRepo.existsByEmail(req.email()))
            throw new ApiException(HttpStatus.CONFLICT, "Email đã tồn tại");

        if (req.studentCode() != null && userRepo.existsByStudentCode(req.studentCode()))
            throw new ApiException(HttpStatus.CONFLICT, "Mã số sinh viên đã tồn tại");

        // ✅ Tạo mới user
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .phone(req.phone())
                .studentCode(req.studentCode())
                .majorName(req.majorName())
                .bio(req.bio())
                .role(Role.builder().roleId(req.roleId()).build())
                .build();

        return toResp(userRepo.save(user));
    }

    @Override
    public UserResponse update(Long id, UserUpdateRequest req) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        // ✅ Chỉ cập nhật trường nào có giá trị mới
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
            throw new ApiException(HttpStatus.NOT_FOUND, "User không tồn tại");
        userRepo.deleteById(id);
    }

    @Override
    public UserResponse get(Long id) {
        return userRepo.findById(id)
                .map(this::toResp)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User không tồn tại"));
    }

    @Override
    public Page<UserResponse> list(Pageable pageable) {
        return userRepo.findAll(pageable).map(this::toResp);
    }

    // ==========================================
    // 🔹 Dùng cho STUDENT / MEMBER / CLUB_LEADER
    // ==========================================

    /** ✅ Lấy hồ sơ của chính người dùng (qua email đăng nhập) */
    public User getProfile(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    /** ✅ Cập nhật hồ sơ của chính người dùng */
    public User updateProfile(String email, ProfileUpdateRequest req) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        // ✅ Cập nhật linh hoạt (chỉ update trường có giá trị)
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

    // ==========================================
    // 🔹 Hàm dùng chung kiểm tra chuyên ngành
    // ==========================================
    private void validateMajor(String majorName) {
        Set<String> validMajors = Set.of(
                "Software Engineering", "Artificial Intelligence", "Information Assurance",
                "Data Science", "Business Administration", "Digital Marketing",
                "Graphic Design", "Multimedia Communication", "Hospitality Management",
                "International Business", "Finance and Banking",
                "Japanese Language", "Korean Language",
                // viết tắt
                "SE", "AI", "IA", "DS", "BA", "DM", "GD", "MC", "HM", "IB", "FB", "JP", "KR"
        );

        if (!validMajors.contains(majorName))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ngành học không hợp lệ");
    }
    // ✅ Cập nhật avatar URL
    public User updateAvatar(String email, String avatarUrl) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        user.setAvatarUrl(avatarUrl);
        return userRepo.save(user);
    }

}
