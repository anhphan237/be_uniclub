package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ForgotPasswordRequest;
import com.example.uniclub.dto.request.LoginRequest;
import com.example.uniclub.dto.request.RegisterRequest;
import com.example.uniclub.dto.request.ResetPasswordRequest;
import com.example.uniclub.dto.response.AuthResponse;
import com.example.uniclub.entity.Role;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.UserStatusEnum;
import com.example.uniclub.repository.RoleRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.security.GoogleTokenVerifier;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.uniclub.dto.request.ChangePasswordRequest;
import com.example.uniclub.service.UserService;
import org.springframework.security.core.Authentication;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authServiceImpl;
    private final GoogleTokenVerifier googleVerifier;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final JwtUtil jwtUtil;
    private final UserService userService;


    // ===== Login/Register =====
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authServiceImpl.login(req));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authServiceImpl.register(req));
    }

    // ===== Google OAuth =====
    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> body) {
        String googleToken = body.get("token");
        if (googleToken == null || googleToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing Google token"));
        }

        // ✅ Bước 1: Verify token thật với Google
        var payload = googleVerifier.verify(googleToken);
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid Google token"));
        }

        // ✅ Bước 2: Lấy thông tin người dùng từ Google
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        // ✅ Bước 3: Chỉ cho phép domain FPT University
        if (!email.endsWith("@fpt.edu.vn") && !email.endsWith("@fe.edu.vn")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Only FPT University accounts are allowed to login."));
        }

        // ✅ Bước 4: Tìm user theo email, nếu chưa có thì tạo mới với role STUDENT
        var user = userRepo.findByEmail(email).orElseGet(() -> {
            var studentRole = roleRepo.findByRoleName("STUDENT")
                    .orElseThrow(() -> new RuntimeException("Role STUDENT not found in database"));

            var newUser = User.builder()
                    .email(email)
                    .passwordHash("{noop}-") // không cần mật khẩu thật
                    .fullName(name)
                    .status(UserStatusEnum.ACTIVE.name())
                    .role(studentRole)
                    .avatarUrl(picture)
                    .build();

            return userRepo.save(newUser);
        });

        // ✅ Bước 5: Cập nhật thông tin nếu thiếu
        boolean updated = false;
        if (user.getFullName() == null) { user.setFullName(name); updated = true; }
        if (user.getAvatarUrl() == null) { user.setAvatarUrl(picture); updated = true; }
        if (updated) userRepo.save(user);

        // ✅ Bước 6: Sinh JWT nội bộ
        String jwt = jwtUtil.generateToken(user.getEmail());

        // ✅ Bước 7: Trả về cho FE
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "jwt", jwt,
                "user", Map.of(
                        "email", user.getEmail(),
                        "fullName", user.getFullName(),
                        "avatar", user.getAvatarUrl(),
                        "role", user.getRole().getRoleName()
                )
        )));
    }


    // ===== Forgot & Reset Password (KHÔNG yêu cầu bearer) =====
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authServiceImpl.sendResetPasswordEmail(req.getEmail());
        return ResponseEntity.ok(ApiResponse.msg("Reset password link has been sent to your email."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authServiceImpl.resetPassword(req.getEmail(), req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.msg("Your password has been successfully reset."));
    }
    // ===== Change Password (Yêu cầu JWT) =====
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            Authentication authentication
    ) {
        String email = authentication.getName(); // lấy từ JWT
        userService.changePassword(email, req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.msg("Password changed successfully. Please re-login."));
    }

}
