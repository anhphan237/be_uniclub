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

    // ✅ Get wallet of the current logged-in user
    @GetMapping("/me")
    public ResponseEntity<Wallet> getMyWallet(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        String email = jwtUtil.getSubject(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(walletRewardService.getWalletByUserId(user.getUserId()));
    }

    // ✅ Reward points to a single member (Leader / Staff / Admin)
    @PostMapping("/reward/{membershipId}")
    public ResponseEntity<?> rewardMember(
            @PathVariable Long membershipId,
            @RequestParam int points,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {

        if (points <= 0) {
            return ResponseEntity.badRequest().body("Points must be greater than zero.");
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token.");
        }

        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.getSubject(token);
        User operator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        Wallet updatedWallet = walletRewardService.rewardPointsByMembershipId(operator, membershipId, points, reason);
        return ResponseEntity.ok(updatedWallet);
    }

    // ✅ Reward points to all members of a club (Staff / Admin only)
    @PostMapping("/reward/club/{clubId}")
    public ResponseEntity<?> rewardEntireClub(
            @PathVariable Long clubId,
            @RequestParam int points,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {

        if (points <= 0) {
            return ResponseEntity.badRequest().body("Points must be greater than zero.");
        }

        String token = request.getHeader("Authorization").replace("Bearer ", "");
        String email = jwtUtil.getSubject(token);
        User operator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        int count = walletRewardService.rewardPointsByClubId(operator, clubId, points, reason);
        return ResponseEntity.ok("Rewarded " + count + " members successfully.");
    }
}
