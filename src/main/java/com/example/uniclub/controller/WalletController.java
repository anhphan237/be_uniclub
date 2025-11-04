package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.WalletAdjustRequest;
import com.example.uniclub.dto.request.WalletRewardBatchRequest;
import com.example.uniclub.dto.request.WalletTransferRequest;
import com.example.uniclub.dto.response.WalletResponse;
import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.WalletRewardService;
import com.example.uniclub.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletRewardService walletRewardService;
    private final WalletService walletService;
    private final WalletRepository walletRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final JwtUtil jwtUtil;

    // ================================================================
    // 🧩 1️⃣ LẤY VÍ USER (ME)
    // ------------------------------------------------
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer "))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");

        String email = jwtUtil.getSubject(token.replace("Bearer ", ""));
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Wallet wallet = walletService.getOrCreateUserWallet(user);

        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .balancePoints(wallet.getBalancePoints())
                .ownerType(wallet.getOwnerType())
                .userId(user.getUserId())
                .userFullName(user.getFullName())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ================================================================
    // 🎁 2️⃣ THƯỞNG ĐIỂM CHO 1 USER
    // ------------------------------------------------
    @PostMapping("/reward/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<WalletResponse>> rewardUser(
            @PathVariable Long userId,
            @RequestParam int points,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {

        if (points <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Points must be greater than zero.");

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer "))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing or invalid token.");

        String email = jwtUtil.getSubject(token.replace("Bearer ", ""));
        User operator = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operator not found"));

        Wallet updatedWallet = walletRewardService.rewardPointsByUser(operator, userId, points, reason);

        WalletResponse response = WalletResponse.builder()
                .walletId(updatedWallet.getWalletId())
                .balancePoints(updatedWallet.getBalancePoints())
                .ownerType(updatedWallet.getOwnerType())
                .userId(updatedWallet.getUser().getUserId())
                .userFullName(updatedWallet.getUser().getFullName())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ================================================================
    // 🏫 3️⃣ NẠP ĐIỂM CHO CLB (UNI → CLUB)
    // ------------------------------------------------
    @PostMapping("/reward/club/{clubId}")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    public ResponseEntity<ApiResponse<?>> topupClub(
            HttpServletRequest request,
            @PathVariable Long clubId,
            @RequestParam long points,
            @RequestParam(required = false) String reason
    ) {
        if (points <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Points must be greater than zero.");

        User operator = jwtUtil.getUserFromRequest(request);
        Wallet wallet = walletRewardService.topUpClubWallet(operator, clubId, points, reason);

        return ResponseEntity.ok(new ApiResponse<>(true, "Top-up success", wallet));
    }

    // ================================================================
    // 💰 4️⃣ XEM VÍ CLB
    // ------------------------------------------------
    @GetMapping("/club/{clubId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getClubWallet(
            @PathVariable Long clubId,
            HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer "))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing or invalid token.");

        jwtUtil.getSubject(token.replace("Bearer ", "")); // check token hợp lệ

        Wallet wallet = walletService.getOrCreateClubWallet(
                clubRepo.findById(clubId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"))
        );

        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .balancePoints(wallet.getBalancePoints())
                .ownerType(wallet.getOwnerType())
                .clubId(clubId)
                .clubName(wallet.getClub().getName())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ================================================================
    // ⚙️ 5️⃣ CỘNG / TRỪ / CHUYỂN ĐIỂM THỦ CÔNG
    // ------------------------------------------------
    @PostMapping("/{id}/add")
    public ResponseEntity<Void> add(@PathVariable Long id, @Valid @RequestBody WalletAdjustRequest req) {
        Wallet wallet = walletRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        walletService.addPoints(wallet, req.amount(), req.description());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reduce")
    public ResponseEntity<Void> reduce(@PathVariable Long id, @Valid @RequestBody WalletAdjustRequest req) {
        Wallet wallet = walletRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        walletService.reducePoints(wallet, req.amount(), req.description());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody WalletTransferRequest req) {
        Wallet from = walletRepo.findById(req.fromWalletId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "From wallet not found"));
        Wallet to = walletRepo.findById(req.toWalletId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "To wallet not found"));
        walletService.transferPoints(from, to, req.amount(), req.description());
        return ResponseEntity.ok().build();
    }

    // ================================================================
    // 📜 6️⃣ LỊCH SỬ GIAO DỊCH
    // ------------------------------------------------
    @GetMapping("/{walletId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT')")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getWalletTransactions(
            @PathVariable Long walletId) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getWalletTransactions(walletId)));
    }

    @GetMapping("/transactions/uni-to-club")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getUniToClubTransactions() {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getAllClubTopups()));
    }

    @GetMapping("/transactions/club-to-member")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getClubToMemberTransactions() {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getAllMemberRewards()));
    }

    // ================================================================
    // 🎯 7️⃣ PHÁT ĐIỂM HÀNG LOẠT
    // ------------------------------------------------
    @PostMapping("/reward/clubs")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> rewardMultipleClubs(
            @Valid @RequestBody WalletRewardBatchRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(walletRewardService.rewardMultipleClubs(req)));
    }

    @PostMapping("/reward/members")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF','ADMIN')")
    public ResponseEntity<ApiResponse<?>> rewardMultipleMembers(
            HttpServletRequest request,
            @Valid @RequestBody WalletRewardBatchRequest req
    ) {
        User operator = jwtUtil.getUserFromRequest(request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Reward success",
                        walletRewardService.rewardMultipleMembers(operator, req))
        );
    }
}
