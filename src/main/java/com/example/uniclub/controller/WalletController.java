package com.example.uniclub.controller;

import com.example.uniclub.entity.User;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.WalletRewardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletRewardService walletRewardService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // ✅ Lấy ví của user đang đăng nhập
    @GetMapping("/me")
    public Wallet getMyWallet(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.getSubject(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return walletRewardService.getWalletByUserId(user.getUserId());
    }

    // ✅ Phát điểm cho thành viên (ClubLeader trở lên)
    @PostMapping("/reward/{membershipId}")
    public ResponseEntity<?> rewardByMembershipId(
            @PathVariable Long membershipId,
            @RequestParam int points,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {

        if (points <= 0) {
            return ResponseEntity.badRequest().body("Points must be > 0");
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");
        }

        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.getSubject(token);
        User operator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator user not found"));

        Wallet updated = walletRewardService.rewardPointsByMembershipId(operator, membershipId, points, reason);
        return ResponseEntity.ok(updated);
    }
}
