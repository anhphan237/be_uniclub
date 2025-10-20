package com.example.uniclub.security;

import com.example.uniclub.entity.Role;
import com.example.uniclub.entity.User;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomUserDetails implements UserDetails {

    private User user;
    private Collection<? extends GrantedAuthority> authorities;

    // ✅ Factory method: tạo từ entity User
    public static CustomUserDetails fromUser(User user, Collection<? extends GrantedAuthority> authorities) {
        return CustomUserDetails.builder()
                .user(user)
                .authorities(authorities)
                .build();
    }

    // ✅ Lấy ID người dùng (chuẩn)
    public Long getId() {
        return user.getUserId();
    }

    // ✅ Alias để tương thích với các controller cũ
    public Long getUserId() {
        return user.getUserId();
    }

    // ✅ Lấy Role
    public Role getRole() {
        return user.getRole();
    }

    // ✅ Lấy tên Role (VD: "ADMIN", "STUDENT", "CLUB_LEADER")
    public String getRoleName() {
        if (user.getRole() == null) return null;
        String roleName = user.getRole().getRoleName();
        return roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
    }

    // ✅ Chuẩn hóa authorities (đảm bảo prefix ROLE_)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities == null) return List.of();
        return authorities.stream()
                .map(auth -> {
                    String roleName = auth.getAuthority();
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    String finalRole = roleName;
                    return (GrantedAuthority) () -> finalRole;
                })
                .collect(Collectors.toList());
    }

    // === Spring Security Required Methods ===
    @Override
    public String getPassword() {
        // ✅ Lấy trực tiếp từ entity
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return user.getStatus() != null && user.getStatus().equals("ACTIVE");
    }
}
