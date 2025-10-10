package com.example.uniclub.security;

import com.example.uniclub.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * CustomUserDetails dùng để ánh xạ thông tin người dùng từ entity User
 * sang định dạng mà Spring Security có thể hiểu (UserDetails).
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    /**
     * Trả về danh sách quyền của người dùng.
     * Bắt buộc phải có tiền tố "ROLE_" để Spring Security nhận diện đúng.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ✅ Lấy tên role, nếu null thì mặc định là STUDENT
        String roleName = (user.getRole() != null && user.getRole().getRoleName() != null)
                ? user.getRole().getRoleName()
                : "STUDENT";

        // ✅ Trả về quyền hợp lệ theo format ROLE_<ROLE_NAME>
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    /**
     * Các phương thức bắt buộc của UserDetails
     */
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
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
