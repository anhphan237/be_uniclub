package com.example.uniclub.controller;

import com.example.uniclub.dto.response.WalletResponse;
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
import com.example.uniclub.enums.WalletOwnerTypeEnum;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletRewardService walletRewardService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final MembershipRepository membershipRepo;
    private final WalletService walletService;

    // ✅ Get wallet of the current logged-in user (return DTO)
    @GetMapping("/me")
    public ResponseEntity<?> getMyWallet(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        String email = jwtUtil.getSubject(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = walletRewardService.getWalletByUserId(user.getUserId());

        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .balancePoints(wallet.getBalancePoints())
                .ownerType(wallet.getOwnerType())
                .userId(user.getUserId())
                .userFullName(user.getFullName())
                .build();

        return ResponseEntity.ok(response);
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

        // ✅ Build WalletResponse with full owner info
        WalletResponse.WalletResponseBuilder builder = WalletResponse.builder()
                .walletId(updatedWallet.getWalletId())
                .balancePoints(updatedWallet.getBalancePoints())
                .ownerType(updatedWallet.getOwnerType());

        if (updatedWallet.getUser() != null) {
            builder.userId(updatedWallet.getUser().getUserId());
            builder.userFullName(updatedWallet.getUser().getFullName());
        }

        if (updatedWallet.getClub() != null) {
            builder.clubId(updatedWallet.getClub().getClubId());
            builder.clubName(updatedWallet.getClub().getName());
        }

        return ResponseEntity.ok(builder.build());
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

    // ✅ Get club wallet (Admin / Staff / Club Leader)
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

        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");

        if (!isAdminOrStaff) {
            boolean isClubLeader = membershipRepo.findByUser_UserId(operator.getUserId()).stream()
                    .anyMatch(m -> m.getClub().getClubId().equals(clubId)
                            && m.getClubRole().name().equalsIgnoreCase("LEADER"));

            if (!isClubLeader) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to view this club's wallet.");
            }
        }

        Wallet wallet = walletService.getWalletByClubId(clubId);

        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .balancePoints(wallet.getBalancePoints())
                .ownerType(wallet.getOwnerType())
                .clubId(wallet.getClub().getClubId())
                .clubName(wallet.getClub().getName())
                .build();

        return ResponseEntity.ok(response);
    }
    // ✅ UniStaff/Admin top-up club wallet
    @PostMapping("/club/{clubId}/topup")
    public ResponseEntity<?> topUpClubWallet(
            @PathVariable Long clubId,
            @RequestParam int points,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {

        // Kiểm tra token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token.");
        }

        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.getSubject(token);
        User operator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        // Chỉ Admin hoặc UniStaff được phép nạp điểm
        String role = operator.getRole().getRoleName();
        if (!(role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only Admin or University Staff can top up club wallets.");
        }

        if (points <= 0) {
            return ResponseEntity.badRequest().body("Points must be greater than zero.");
        }

        // ✅ Lấy ví CLB, nếu chưa có thì tạo mới
        Wallet wallet = walletService.getWalletByClubId(clubId);
        if (wallet == null) {
            wallet = new Wallet();
            wallet.setOwnerType(WalletOwnerTypeEnum.CLUB);
            wallet.setClub(wallet.getClub());
            wallet.setBalancePoints(0);
        }

        walletService.increase(wallet, points);

        // ✅ Chuẩn bị response DTO
        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .balancePoints(wallet.getBalancePoints())
                .ownerType(wallet.getOwnerType())
                .clubId(wallet.getClub().getClubId())
                .clubName(wallet.getClub().getName())
                .build();

        return ResponseEntity.ok(response);
    }

}
