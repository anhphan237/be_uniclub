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

    @Value("${app.oauth2.school-domains}")
    private String schoolDomains;

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
                log.error("OAuth2 login: thiếu email trong profile Google");
                response.sendRedirect(redirectFailUrl + "?error=no_email");
                return;
            }

            // ✅ Chỉ cho phép các email trong danh sách domain hợp lệ (fpt.edu.vn hoặc gmail.com)
            if (!isAllowedEmail(email)) {
                log.warn("❌ Email không hợp lệ hoặc không thuộc domain sinh viên: {}", email);
                response.sendRedirect(redirectFailUrl + "?error=invalid_domain");
                return;
            }

            // 🔍 Tìm user theo email, nếu chưa có thì tạo mới
            Optional<User> userOpt = userRepo.findByEmail(email);
            User user = userOpt.orElseGet(() -> {
                Long roleIdForStudent = 5L; // role STUDENT
                return userRepo.save(User.builder()
                        .email(email)
                        .passwordHash("{noop}-")
                        .fullName(name)
                        .status(UserStatusEnum.ACTIVE.getCode())
                        .role(Role.builder().roleId(roleIdForStudent).build())

                        .build());
            });

            // 🔑 Sinh JWT token và redirect về FE
            String token = jwtUtil.generateToken(user.getEmail());
            response.sendRedirect(redirectSuccessUrl + "?token=" + token);

        } catch (Exception e) {
            log.error("OAuth2 success handler lỗi: ", e);
            response.sendRedirect(redirectFailUrl + "?error=exception");
        }
    }

    // 🧠 Chấp nhận cả FPT domain và Gmail
    private boolean isAllowedEmail(String email) {
        if (schoolDomains == null || schoolDomains.isBlank()) return false;
        String lower = email.toLowerCase();
        for (String domain : schoolDomains.split(",")) {
            if (lower.endsWith(domain.trim().toLowerCase())) return true;
        }
        return false;
    }
}
