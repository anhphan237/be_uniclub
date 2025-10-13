package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.LoginRequest;
import com.example.uniclub.dto.request.RegisterRequest;
import com.example.uniclub.dto.response.AuthResponse;
import com.example.uniclub.entity.Role;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.UserStatusEnum;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.security.GoogleTokenVerifier;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.impl.AuthServiceImpl;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuthController {

    private final AuthServiceImpl authServiceImpl;
    private final GoogleTokenVerifier googleVerifier;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    // ==========================
    // 🔹 Login/Register truyền thống
    // ==========================

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authServiceImpl.login(req));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authServiceImpl.register(req));
    }

    // ==========================
    // 🔹 Login bằng Google OAuth (Cách 2)
    // ==========================
    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing Google token"));
        }

        var payload = googleVerifier.verify(token);
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid Google token"));
        }

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        Optional<User> userOpt = userRepo.findByEmail(email);
        User user = userOpt.orElseGet(() -> {
            Role role = Role.builder().roleId(5L).build(); // role STUDENT
            User u = User.builder()
                    .email(email)
                    .passwordHash("{noop}-")
                    .fullName(name)
                    .status(UserStatusEnum.ACTIVE.name())
                    .role(role)
                    .build();
            return userRepo.save(u);
        });

        if (user.getFullName() == null) user.setFullName(name);
        if (user.getAvatarUrl() == null) {
            try {
                var avatarField = User.class.getDeclaredField("avatarUrl");
                if (avatarField != null) {
                    user.setAvatarUrl(picture);
                    userRepo.save(user);
                }
            } catch (Exception ignored) {}
        }

        String jwt = jwtUtil.generateToken(user.getEmail());

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "token", jwt,
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "avatar", picture
        )));
    }
}
