package com.example.uniclub.controller;

import com.example.uniclub.entity.User;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/me")
    public Wallet getMyWallet(HttpServletRequest request) {
        // Lấy token từ header
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.replace("Bearer ", "");

        // Lấy email từ token
        String email = jwtUtil.getSubject(token);

        // Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Lấy wallet theo userId
        return walletService.getWalletByUserId(user.getUserId());
    }
}
