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
            String name  = oauthUser.getAttribute("name");
            String picture = oauthUser.getAttribute("picture"); // 🧩 lấy avatar từ Google

            if (email == null) {
                log.error("OAuth2 login: thiếu email trong profile Google");
                response.sendRedirect(redirectFailUrl);
                return;
            }

            Optional<User> userOpt = userRepo.findByEmail(email);

            User user = userOpt.orElseGet(() -> {
                boolean isSchool = isSchoolEmail(email);
                Long roleIdForNewUser = 5L; // mặc định role Student
                return userRepo.save(User.builder()
                        .email(email)
                        .passwordHash("{noop}-")
                        .fullName(name)
                        .avatarUrl(picture) // 🧩 lưu avatar
                        .status(UserStatusEnum.ACTIVE.name())
                        .role(Role.builder().roleId(roleIdForNewUser).build())
                        .studentCode(generateStudentCode(email))
                        .build());
            });

            // 🧩 Nếu user đã tồn tại nhưng chưa có avatar thì cập nhật thêm
            if (user.getAvatarUrl() == null && picture != null) {
                user.setAvatarUrl(picture);
                userRepo.save(user);
            }

            String token = jwtUtil.generateToken(user.getEmail());
            String redirect = redirectSuccessUrl + "?token=" + token;

            response.sendRedirect(redirect);

        } catch (Exception e) {
            log.error("OAuth2 success handler lỗi: ", e);
            response.sendRedirect(redirectFailUrl);
        }
    }

    private boolean isSchoolEmail(String email) {
        if (schoolDomains == null || schoolDomains.isBlank()) return false;
        String lower = email.toLowerCase();
        for (String d : schoolDomains.split(",")) {
            if (lower.endsWith(d.trim().toLowerCase())) return true;
        }
        return false;
    }

    private String generateStudentCode(String email) {
        // 🧩 Nếu là FPT email: lấy phần đầu làm MSSV (ví dụ: he180123@fpt.edu.vn)
        if (email.endsWith("@fpt.edu.vn")) {
            return email.split("@")[0].toUpperCase();
        }
        // Gmail cá nhân thì sinh MSSV giả định
        return "STU-" + Math.abs(email.hashCode() % 1000000);
    }
}
