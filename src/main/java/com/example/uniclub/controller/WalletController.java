package com.example.uniclub.controller;

import com.example.uniclub.entity.User;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.WalletRewardService;
import com.example.uniclub.service.WalletService;
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
    private final MembershipRepository membershipRepo;
    private final WalletService walletService;

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
    // ✅ Get club wallet (for Admin, University Staff, or Club Leader of that club)
    @GetMapping("/club/{clubId}")
    public ResponseEntity<?> getClubWallet(
            @PathVariable Long clubId,
            HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token.");
        }

        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.getSubject(token);
        User operator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        // Only ADMIN, STAFF, or the Club Leader of this club can view it
        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");

        if (!isAdminOrStaff) {
            // Check if the operator is a club leader of this club
            boolean isClubLeader = membershipRepo.findByUser_UserId(operator.getUserId()).stream()
                    .anyMatch(m -> m.getClub().getClubId().equals(clubId)
                            && m.getClubRole().name().equalsIgnoreCase("LEADER"));

            if (!isClubLeader) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to view this club's wallet.");
            }
        }

        Wallet wallet = walletService.getWalletByClubId(clubId);
        return ResponseEntity.ok(wallet);
    }

}
