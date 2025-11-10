package com.example.uniclub.security;

import com.example.uniclub.entity.Role;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.UserStatusEnum;
import com.example.uniclub.repository.MembershipRepository;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;
    private final MembershipRepository membershipRepo;
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

            if (userOpt.isEmpty()) {
                // 🧩 Tạo user mới nếu chưa có
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
            String token = jwtUtil.generateToken(user.getEmail());

            // ✅ Lấy danh sách CLB mà user đang tham gia (ACTIVE hoặc APPROVED)
            List<Long> clubIds = membershipRepo.findActiveMembershipsByUserId(user.getUserId())
                    .stream()
                    .map(m -> m.getClub().getClubId())
                    .collect(Collectors.toList());

            // ✅ Kiểm tra user có phải staff (LEADER / VICE_LEADER / STAFF)
            boolean isStaff = membershipRepo.findByUser_UserId(user.getUserId()).stream()
                    .anyMatch(m -> m.getClubRole() == ClubRoleEnum.LEADER
                            || m.getClubRole() == ClubRoleEnum.VICE_LEADER
                            || m.getClubRole() == ClubRoleEnum.STAFF);

            // ✅ Encode danh sách CLB để truyền qua FE
            String clubIdsParam = clubIds.isEmpty()
                    ? ""
                    : URLEncoder.encode(clubIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")), StandardCharsets.UTF_8);

            // ✅ Redirect URL trả luôn thông tin cần thiết cho FE
            String redirect = String.format("%s?token=%s&role=%s&clubIds=%s&staff=%s",
                    redirectSuccessUrl,
                    token,
                    user.getRole().getRoleName(),
                    clubIdsParam,
                    isStaff
            );

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
