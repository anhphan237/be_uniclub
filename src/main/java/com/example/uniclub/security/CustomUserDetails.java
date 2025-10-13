package com.example.uniclub.security;

import com.example.uniclub.entity.Role;
import com.example.uniclub.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomUserDetails implements UserDetails {

    private Long userId;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private Role role; // để dễ truy cập role nếu cần

    // ✅ Constructor tiện lợi để tạo từ entity User
    public static CustomUserDetails fromUser(User user, Collection<? extends GrantedAuthority> authorities) {
        return CustomUserDetails.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .role(user.getRole())
                .build();
    }

    // ✅ Thêm hàm getId() để dùng trong Controller / Service
    public Long getId() {
        return userId;
    }

    // ✅ Thêm hàm getUser() để tương thích với các service cũ (như AuthServiceImpl)
    public User getUser() {
        User u = new User();
        u.setUserId(this.userId);
        u.setEmail(this.email);
        u.setRole(this.role);
        return u;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
