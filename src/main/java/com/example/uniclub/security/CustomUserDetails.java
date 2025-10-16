package com.example.uniclub.security;

import com.example.uniclub.entity.Role;
import com.example.uniclub.entity.User;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomUserDetails implements UserDetails {

    private Long userId;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private Role role;

    // ✅ Tạo CustomUserDetails từ entity User
    public static CustomUserDetails fromUser(User user, Collection<? extends GrantedAuthority> authorities) {
        return CustomUserDetails.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .role(user.getRole())
                .build();
    }

    // ✅ Lấy ID người dùng
    public Long getId() {
        return userId;
    }

    // ✅ Trả lại entity User rút gọn
    public User getUser() {
        User u = new User();
        u.setUserId(this.userId);
        u.setEmail(this.email);
        u.setRole(this.role);
        return u;
    }

    // ✅ Lấy tên Role (VD: "ADMIN", "STUDENT", "CLUB_LEADER")
    public String getRoleName() {
        if (this.role != null && this.role.getRoleName() != null) {
            // Ưu tiên lấy từ entity Role nếu có
            return this.role.getRoleName().replace("ROLE_", "");
        }

        // Nếu chưa có Role entity, fallback từ authorities
        if (this.authorities != null && !this.authorities.isEmpty()) {
            String fullRole = this.authorities.iterator().next().getAuthority();
            return fullRole.startsWith("ROLE_") ? fullRole.substring(5) : fullRole;
        }
        return null;
    }

    // ✅ Chuẩn hóa authorities (đảm bảo prefix ROLE_)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities.stream()
                .map(auth -> {
                    String roleName = auth.getAuthority();
                    final String fixedName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                    return (GrantedAuthority) () -> fixedName;
                })
                .collect(Collectors.toList());
    }

    // === Spring Security Required Methods ===
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
