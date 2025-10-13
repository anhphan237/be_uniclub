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

    public Long getId() {
        return userId;
    }

    public User getUser() {
        User u = new User();
        u.setUserId(this.userId);
        u.setEmail(this.email);
        u.setRole(this.role);
        return u;
    }

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


    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
