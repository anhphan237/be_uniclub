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
        String token = body.get("token");
        if (token == null) return ResponseEntity.badRequest().body(ApiResponse.error("Missing Google token"));

        var payload = googleVerifier.verify(token);
        if (payload == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid Google token"));

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        var user = userRepo.findByEmail(email).orElseGet(() -> {
            var studentRole = roleRepo.findByRoleName("STUDENT").orElse(Role.builder().roleId(5L).build());
            var u = User.builder()
                    .email(email)
                    .passwordHash("{noop}-")
                    .fullName(name)
                    .status(UserStatusEnum.ACTIVE.name())
                    .role(studentRole)
                    .avatarUrl(picture)
                    .build();
            return userRepo.save(u);
        });

        if (user.getFullName() == null) user.setFullName(name);
        if (user.getAvatarUrl() == null) user.setAvatarUrl(picture);
        userRepo.save(user);

        String jwt = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "token", jwt,
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "avatar", user.getAvatarUrl()
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
}
