package com.example.uniclub.service.impl;

import com.example.uniclub.entity.User;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

        // ✅ Thêm tiền tố ROLE_ để Spring nhận diện đúng quyền
        String roleName = user.getRole().getRoleName();
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

        // ✅ Debug log kiểm tra
        System.out.println("DEBUG >>> Loaded user: " + user.getEmail());
        System.out.println("DEBUG >>> Role: " + roleName);
        System.out.println("DEBUG >>> PasswordHash: " + user.getPasswordHash());

        // ✅ Dùng factory method trong CustomUserDetails
        return CustomUserDetails.fromUser(user, authorities);
    }
}
