package com.example.uniclub.security;

import com.example.uniclub.entity.Role;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.UserStatusEnum;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;
    private final MembershipRepository membershipRepo;
    private final EmailService emailService;

    @Value("${app.oauth2.redirect-success}")
    private String redirectSuccessUrl;

    @Value("${app.oauth2.redirect-fail}")
    private String redirectFailUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            String email = oauthUser.getAttribute("email");
            String name = oauthUser.getAttribute("name");
            String picture = oauthUser.getAttribute("picture");

            if (email == null) {
                log.error("OAuth2 login failed: missing email from Google profile");
                response.sendRedirect(redirectFailUrl + "?error=missing_email");
                return;
            }

            long studentRoleId = 5L;

            Optional<User> userOpt = userRepo.findByEmail(email);
            User user;
            boolean isNewUser = false;

            if (userOpt.isEmpty()) {
                // 🧩 Tạo user mới (Google login lần đầu)
                isNewUser = true;
                user = User.builder()
                        .email(email)
                        .passwordHash("{noop}-")
                        .fullName(name)
                        .avatarUrl(picture)
                        .status(UserStatusEnum.ACTIVE.name())
                        .role(Role.builder().roleId(studentRoleId).build())
                        .studentCode(generateStudentCode(email))
                        .isFirstLogin(true)
                        .build();
                userRepo.save(user);

                // ⭐ Gửi email welcome
                sendWelcomeEmail(user);

            } else {
                user = userOpt.get();

                boolean updated = false;

                if (name != null && !name.equals(user.getFullName())) {
                    user.setFullName(name);
                    updated = true;
                }
                if (picture != null && (user.getAvatarUrl() == null || !user.getAvatarUrl().equals(picture))) {
                    user.setAvatarUrl(picture);
                    updated = true;
                }

                if (updated) userRepo.save(user);
            }

            // 🔐 Sinh JWT token
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleName());

            // ⭐ Tránh load Membership entity → chỉ lấy clubId
            List<Long> clubIds = membershipRepo.findActiveClubIds(user.getUserId());

            boolean isStaff = membershipRepo.findActiveStaffClubId(user.getUserId()) != null;

            String clubIdsParam = clubIds.isEmpty()
                    ? ""
                    : URLEncoder.encode(
                    String.join(",", clubIds.stream().map(String::valueOf).toList()),
                    StandardCharsets.UTF_8
            );

            String redirect = String.format(
                    "%s?token=%s&role=%s&clubIds=%s&staff=%s&newUser=%s",
                    redirectSuccessUrl,
                    token,
                    user.getRole().getRoleName(),
                    clubIdsParam,
                    isStaff,
                    isNewUser
            );

            response.sendRedirect(redirect);

        } catch (Exception e) {
            log.error("OAuth2 success handler error:", e);
            response.sendRedirect(redirectFailUrl + "?error=server_error");
        }
    }

    // ⭐ Gửi email chào mừng
    private void sendWelcomeEmail(User user) {
        String subject = "[UniClub] Welcome to UniClub 🎉";
        String content = """
                <h2>Hello %s,</h2>
                <p>Welcome to <b>UniClub</b>! Your account has been successfully created via Google Login. 🎉</p>
                <p>You can now explore clubs, join events, and start earning reward points within the system.</p>
                <p>👉 Access UniClub here: <a href="https://uniclub.id.vn/login">https://uniclub.id.vn/login</a></p>
                <br>
                <p>Best regards,<br><b>UniClub Vietnam Team</b></p>
                """.formatted(user.getFullName() != null ? user.getFullName() : "there");

        emailService.sendEmail(user.getEmail(), subject, content);
    }

    // 🧮 Generate student code
    private String generateStudentCode(String email) {
        String prefix = email.split("@")[0];
        if (email.endsWith("@fpt.edu.vn") || email.endsWith("@fe.edu.vn")) {
            return prefix.toUpperCase();
        }
        return "STU-" + Math.abs(email.hashCode() % 1000000);
    }
}
