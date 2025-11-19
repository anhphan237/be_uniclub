package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.LoginRequest;
import com.example.uniclub.dto.request.RegisterRequest;
import com.example.uniclub.dto.response.AuthResponse;
import com.example.uniclub.entity.PasswordResetToken;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.UserStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WalletRepository walletRepository;
    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final MajorRepository majorRepository;

    // ==============================================
    // 🔹 Đăng nhập (Email / Password) — FIXED FULLY
    // ==============================================
    public AuthResponse login(LoginRequest req) {

        // Authenticate
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        var cud = (CustomUserDetails) auth.getPrincipal();
        var user = cud.getUser();

        // ===== ROLE =====
        String roleName = user.getRole().getRoleName();

        // ===== CLUB INFO =====
        Long clubId = null;
        List<Long> clubIds = null;
        Boolean isClubStaff = null;

        if ("CLUB_LEADER".equals(roleName)) {
            clubId = membershipRepository.findLeaderClubId(user.getUserId());
        }
        else if ("STUDENT".equals(roleName)) {
            clubIds = membershipRepository.findActiveClubIds(user.getUserId());
            isClubStaff = membershipRepository.findActiveStaffClubId(user.getUserId()) != null;
        }

        // ===== JWT TOKEN — CUSTOM FULL TOKEN =====
        String token = jwtUtil.generateFullToken(
                user.getUserId(),
                user.getEmail(),
                roleName,
                clubId,
                clubIds,
                isClubStaff
        );

        boolean firstLogin = user.isFirstLogin();

        // Build response
        AuthResponse.AuthResponseBuilder responseBuilder = AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(roleName)
                .clubId(clubId)
                .clubIds(clubIds)
                .requirePasswordChange(firstLogin)
                .firstTimeGoogleLogin(firstLogin);

        if (isClubStaff != null) {
            responseBuilder.staff(isClubStaff);
        }

        // Reset first login flag
        if (firstLogin) {
            user.setFirstLogin(false);
            userRepository.save(user);
        }

        return responseBuilder.build();
    }



    // ==============================================
    // 🔹 Đăng ký
    // ==============================================
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }

        if (userRepository.existsByStudentCode(req.studentCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "Student code already exists");
        }

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .phone(req.phone())
                .role(roleRepository.findByRoleName(req.roleName())
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid role name")))
                .status(UserStatusEnum.ACTIVE.name())
                .studentCode(req.studentCode())
                .major(
                        majorRepository.findByName(req.majorName())
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                        "Major not found: " + req.majorName()))
                )
                .isFirstLogin(true)
                .build();

        user = userRepository.save(user);

        // Send Email
        String subject = "[UniClub] Welcome to the system 🎉";
        String content = """
                <h2>Hello %s,</h2>
                <p>Congratulations! You’ve successfully registered your <b>UniClub</b> account. 🎉</p>
                <p>You can now log in to explore clubs, join events, and start earning points within the system.</p>
                <p>👉 Access here: <a href="https://uniclub.id.vn/login">https://uniclub.id.vn/login</a></p>
                <br>
                <p>Best regards,<br><b>The UniClub Vietnam Team</b></p>
                """.formatted(user.getFullName());

        emailService.sendEmail(user.getEmail(), subject, content);

        // Create JWT
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().getRoleName()
        );

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getRoleName())
                .firstTimeGoogleLogin(true)
                .staff(false)
                .build();
    }



    // ==============================================
    // 🔹 Send Reset Password Email
    // ==============================================
    public void sendResetPasswordEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No account found with this email."));

        tokenRepository.deleteByUser_UserId(user.getUserId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();
        tokenRepository.save(resetToken);

        String resetLink = "https://uniclub-fpt.vercel.app/reset-password?token=" + token + "&email=" + email;
        String subject = "Reset your UniClub password";
        String content = """
                Hi %s,<br><br>
                We received a request to reset your UniClub password.<br>
                Click the button below to set a new password:<br><br>
                <a href="%s" style="display:inline-block;padding:10px 20px;
                background-color:#1E88E5;color:white;border-radius:6px;text-decoration:none;">
                Reset Password</a><br><br>
                This link will expire in 15 minutes.<br><br>
                Best regards,<br>
                <b>UniClub Vietnam</b> 
                """.formatted(user.getFullName(), resetLink);

        emailService.sendEmail(email, subject, content);
    }



    // ==============================================
    // 🔹 Reset Password
    // ==============================================
    public void resetPassword(String email, String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired token."));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token has expired.");
        }

        User user = resetToken.getUser();

        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token does not match this email.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.delete(resetToken);
    }
}
