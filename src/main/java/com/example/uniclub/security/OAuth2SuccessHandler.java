package com.example.uniclub.security;

import com.example.uniclub.entity.Role;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.UserStatusEnum;
import com.example.uniclub.repository.UserRepository;
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
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;

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
                log.error("❌ OAuth2 login failed: missing email from Google profile");
                response.sendRedirect(redirectFailUrl + "?error=missing_email");
                return;
            }

            // ✅ Tất cả user Google đều là STUDENT (roleId = 5)
            long studentRoleId = 5L;

            Optional<User> userOpt = userRepo.findByEmail(email);
            User user;

            if (userOpt.isEmpty()) {
                // 🧩 Tạo user mới
                user = User.builder()
                        .email(email)
                        .passwordHash("{noop}-") // Không cần password
                        .fullName(name)
                        .avatarUrl(picture)
                        .status(UserStatusEnum.ACTIVE.name())
                        .role(Role.builder().roleId(studentRoleId).build())
                        .studentCode(generateStudentCode(email))
                        .isFirstLogin(true)
                        .build();
                userRepo.save(user);
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

            // 🔐 Sinh JWT token để FE nhận
            String token = jwtUtil.generateToken(user.getEmail());
            String redirect = redirectSuccessUrl + "?token=" + token + "&role=" + user.getRole().getRoleName();
            response.sendRedirect(redirect);

        } catch (Exception e) {
            log.error("OAuth2 success handler error:", e);
            response.sendRedirect(redirectFailUrl + "?error=server_error");
        }
    }

    // 🧮 Sinh student code ngẫu nhiên hoặc từ email
    private String generateStudentCode(String email) {
        String prefix = email.split("@")[0];
        if (email.endsWith("@fpt.edu.vn") || email.endsWith("@fe.edu.vn")) {
            return prefix.toUpperCase();
        }
        return "STU-" + Math.abs(email.hashCode() % 1000000);
    }
}
