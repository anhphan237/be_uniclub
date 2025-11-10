package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.AuthResponse;
import com.example.uniclub.dto.response.GoogleLoginResponse;
import com.example.uniclub.entity.Role;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.UserStatusEnum;
import com.example.uniclub.repository.RoleRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.security.GoogleTokenVerifier;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.UserService;
import com.example.uniclub.service.impl.AuthServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.enums.ClubRoleEnum;

import java.util.List;
import java.util.Map;

@Tag(
        name = "Authentication & Account Management",
        description = """
        Quản lý đăng nhập và bảo mật người dùng:
        - Đăng nhập / Đăng ký tài khoản
        - Đăng nhập bằng Google OAuth (mọi Gmail đều được phép)
        - Quên mật khẩu, đặt lại mật khẩu
        - Đổi mật khẩu với JWT
        """
)
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
    private final MembershipRepository membershipRepo;

    // ==========================================================
    // 🟢 1. ĐĂNG NHẬP
    // ==========================================================
    @Operation(
            summary = "Đăng nhập vào hệ thống",
            description = """
                Nhập email và mật khẩu để nhận JWT token.<br>
                Dành cho tất cả người dùng có tài khoản trong hệ thống.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Đăng nhập thành công")
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authServiceImpl.login(req));
    }

    // ==========================================================
    // 🟣 2. ĐĂNG KÝ
    // ==========================================================
    @Operation(
            summary = "Đăng ký tài khoản mới",
            description = """
                Dành cho sinh viên / người dùng mới muốn tạo tài khoản trong hệ thống.<br>
                Sau khi đăng ký thành công sẽ tự động đăng nhập và nhận JWT token.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Đăng ký thành công")
    )
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authServiceImpl.register(req));
    }

    // ==========================================================
// 🌐 3. GOOGLE OAUTH ĐĂNG NHẬP (KHÔNG GIỚI HẠN DOMAIN)
// ==========================================================
    @Operation(
            summary = "Đăng nhập bằng Google (mọi Gmail đều được phép)",
            description = """
            Cho phép **mọi tài khoản Google hợp lệ** đăng nhập hệ thống.<br>
            Nếu người dùng chưa tồn tại → tự động tạo tài khoản với role **STUDENT**.<br>
            Xác thực token thật với Google, lưu thông tin cơ bản và trả về JWT token.
            """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đăng nhập Google thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token không hợp lệ")
            }
    )
    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> body) {
        String googleToken = body.get("token");
        if (googleToken == null || googleToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing Google token"));
        }

        // ✅ Verify Google token
        var payload = googleVerifier.verify(googleToken);
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid Google token"));
        }

        // ✅ Extract user info from Google
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        if (picture == null) {
            picture = "https://res.cloudinary.com/uniclub/image/upload/v1/defaults/default-avatar.png";
        }
        final String finalEmail = email;
        final String finalName = name;
        final String finalPicture = picture;
        // ✅ Find existing user or create new
        var user = userRepo.findByEmail(finalEmail).orElseGet(() -> {
            Role studentRole = roleRepo.findByRoleName("STUDENT")
                    .orElseThrow(() -> new RuntimeException("Role STUDENT not found in database"));

            User newUser = User.builder()
                    .email(finalEmail)
                    .passwordHash("{noop}-") // no password needed
                    .fullName(finalName)
                    .status(UserStatusEnum.ACTIVE.name())
                    .role(studentRole)
                    .avatarUrl(finalPicture)
                    .build();

            return userRepo.save(newUser);
        });

        boolean updated = false;
        if (user.getFullName() == null) { user.setFullName(name); updated = true; }
        if (user.getAvatarUrl() == null) { user.setAvatarUrl(picture); updated = true; }
        if (updated) userRepo.save(user);

        // ✅ Generate JWT
        String jwt = jwtUtil.generateToken(user.getEmail());

        // ✅ Lấy danh sách CLB mà user đang tham gia
        List<Long> clubIds = membershipRepo.findActiveMembershipsByUserId(user.getUserId())
                .stream()
                .map(m -> m.getClub().getClubId())
                .toList();

        // ✅ Kiểm tra user có phải staff CLB nào không
        boolean isStaff = membershipRepo.findByUser_UserId(user.getUserId())
                .stream()
                .anyMatch(m -> m.getClubRole() == ClubRoleEnum.LEADER
                        || m.getClubRole() == ClubRoleEnum.VICE_LEADER
                        || m.getClubRole() == ClubRoleEnum.STAFF);

        // ✅ Build DTO cho response
        GoogleLoginResponse response = GoogleLoginResponse.builder()
                .token(jwt)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatar(user.getAvatarUrl())
                .userId(user.getUserId())
                .role(user.getRole().getRoleName())
                .clubIds(clubIds)
                .staff(isStaff)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ==========================================================
    // 🟠 4. QUÊN MẬT KHẨU (PUBLIC)
    // ==========================================================
    @Operation(
            summary = "Yêu cầu gửi link đặt lại mật khẩu",
            description = """
                Public API (không yêu cầu đăng nhập).<br>
                Gửi email chứa đường dẫn đặt lại mật khẩu cho người dùng.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Email đặt lại mật khẩu đã được gửi")
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authServiceImpl.sendResetPasswordEmail(req.getEmail());
        return ResponseEntity.ok(ApiResponse.msg("Reset password link has been sent to your email."));
    }

    // ==========================================================
    // 🔵 5. ĐẶT LẠI MẬT KHẨU (PUBLIC)
    // ==========================================================
    @Operation(
            summary = "Đặt lại mật khẩu bằng token email",
            description = """
                Public API.<br>
                Người dùng nhập email, token xác minh và mật khẩu mới để khôi phục tài khoản.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Đặt lại mật khẩu thành công")
    )
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authServiceImpl.resetPassword(req.getEmail(), req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.msg("Your password has been successfully reset."));
    }

    // ==========================================================
    // 🔐 6. ĐỔI MẬT KHẨU (CẦN JWT)
    // ==========================================================
    @Operation(
            summary = "Đổi mật khẩu (yêu cầu JWT)",
            description = """
                Dành cho người dùng đã đăng nhập.<br>
                Cần truyền mật khẩu cũ và mật khẩu mới.<br>
                Sau khi đổi mật khẩu thành công → cần đăng nhập lại.
                """,
            security = {@SecurityRequirement(name = "bearerAuth")},
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Đổi mật khẩu thành công")
    )
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            Authentication authentication
    ) {
        String email = authentication.getName();
        userService.changePassword(email, req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.msg("Password changed successfully. Please re-login."));
    }
}
