package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.WalletAdjustRequest;
import com.example.uniclub.dto.request.WalletRewardBatchRequest;
import com.example.uniclub.dto.request.WalletTransferRequest;
import com.example.uniclub.dto.response.WalletResponse;
import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.WalletOwnerTypeEnum;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletRewardService walletRewardService;
    private final WalletService walletService;
    private final WalletRepository walletRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final MembershipRepository membershipRepo;
    private final JwtUtil jwtUtil;

    // ================================================================
    // 🧩 1️⃣ LẤY TẤT CẢ VÍ CỦA NGƯỜI DÙNG HIỆN TẠI (THEO CLB)
    // ------------------------------------------------
    // ✅ Endpoint: GET /api/wallets/me/memberships
    // ✅ Trả về danh sách ví (1 ví cho mỗi CLB user đã tham gia)
    // ================================================================
    @GetMapping("/me/memberships")
    public ResponseEntity<?> getMyMembershipWallets(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer "))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");

        String email = jwtUtil.getSubject(token.replace("Bearer ", ""));
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        // Lấy tất cả membership của user
        List<Membership> memberships = membershipRepo.findByUser_UserId(user.getUserId());
        if (memberships.isEmpty())
            return ResponseEntity.ok(List.of());

        // Map sang danh sách ví
        List<WalletResponse> responses = memberships.stream()
                .map(m -> {
                    Wallet wallet = walletService.getOrCreateMembershipWallet(m);
                    return WalletResponse.builder()
                            .walletId(wallet.getWalletId())
                            .balancePoints(wallet.getBalancePoints())
                            .ownerType(wallet.getOwnerType())
                            .clubId(m.getClub().getClubId())
                            .clubName(m.getClub().getName())
                            .userId(user.getUserId())
                            .userFullName(user.getFullName())
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // ================================================================
    // 🎁 2️⃣ THƯỞNG ĐIỂM CHO 1 THÀNH VIÊN CỤ THỂ
    // ------------------------------------------------
    // ✅ Endpoint: POST /api/wallets/reward/{membershipId}?points=50&reason=Excellent+Work
    // ================================================================
    @PostMapping("/reward/{membershipId}")
    public ResponseEntity<?> rewardMember(
            @PathVariable Long membershipId,
            @RequestParam int points,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {

        if (points <= 0)
            return ResponseEntity.badRequest().body("Points must be greater than zero.");

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token.");

        String email = jwtUtil.getSubject(authHeader.replace("Bearer ", ""));
        User operator = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operator not found"));

        Wallet updatedWallet = walletRewardService.rewardPointsByMembershipId(operator, membershipId, points, reason);

        WalletResponse response = WalletResponse.builder()
                .walletId(updatedWallet.getWalletId())
                .balancePoints(updatedWallet.getBalancePoints())
                .ownerType(updatedWallet.getOwnerType())
                .clubId(updatedWallet.getMembership().getClub().getClubId())
                .clubName(updatedWallet.getMembership().getClub().getName())
                .userId(updatedWallet.getMembership().getUser().getUserId())
                .userFullName(updatedWallet.getMembership().getUser().getFullName())
                .build();

        return ResponseEntity.ok(response);
    }

    // ================================================================
    // 🏫 3️⃣ THƯỞNG ĐIỂM CHO TOÀN BỘ THÀNH VIÊN CLB
    // ------------------------------------------------
    @PostMapping("/reward/club/{clubId}")
    public ResponseEntity<?> rewardEntireClub(
            @PathVariable Long clubId,
            @RequestParam int points,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {

        if (points <= 0)
            return ResponseEntity.badRequest().body("Points must be greater than zero.");

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer "))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token.");

        String email = jwtUtil.getSubject(token.replace("Bearer ", ""));
        User operator = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operator not found"));

        int count = walletRewardService.rewardPointsByClubId(operator, clubId, points, reason);
        return ResponseEntity.ok("Rewarded " + count + " members successfully.");
    }

    // ================================================================
    // 💰 4️⃣ XEM VÍ CLB
    // ------------------------------------------------
    @GetMapping("/club/{clubId}")
    public ResponseEntity<?> getClubWallet(@PathVariable Long clubId, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token.");

        String email = jwtUtil.getSubject(authHeader.replace("Bearer ", ""));
        User operator = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operator not found"));

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

        return ResponseEntity.ok(response);
    }

    // ================================================================
    // 🪙 5️⃣ NẠP ĐIỂM CHO CLB (Top-up)
    // ------------------------------------------------
    @PostMapping("/club/{clubId}/topup")
    public ResponseEntity<?> topUpClubWallet(
            @PathVariable Long clubId,
            @RequestParam int points,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {

        if (points <= 0)
            return ResponseEntity.badRequest().body("Points must be greater than zero.");

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token.");

        String email = jwtUtil.getSubject(authHeader.replace("Bearer ", ""));
        User operator = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operator not found"));

        Wallet wallet = walletRewardService.topUpClubWallet(operator, clubId, points, reason);
        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .balancePoints(wallet.getBalancePoints())
                .ownerType(wallet.getOwnerType())
                .clubId(clubId)
                .clubName(wallet.getClub().getName())
                .build();

        return ResponseEntity.ok(response);
    }

    // ================================================================
    // ⚙️ 6️⃣ CỘNG / TRỪ / CHUYỂN ĐIỂM THỦ CÔNG
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
    // 📜 7️⃣ LỊCH SỬ GIAO DỊCH
    // ------------------------------------------------
    @GetMapping("/{walletId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT')")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getWalletTransactions(
            @PathVariable Long walletId) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getWalletTransactions(walletId)));
    }

    @GetMapping("/transactions/uni-to-club")
    public ResponseEntity<?> getUniToClubTransactions() {
        return ResponseEntity.ok(walletService.getAllClubTopups());
    }

    @GetMapping("/transactions/club-to-member")
    public ResponseEntity<?> getClubToMemberTransactions() {
        return ResponseEntity.ok(walletService.getAllMemberRewards());
    }
    @PostMapping("/reward/clubs")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> rewardMultipleClubs(
            @Valid @RequestBody WalletRewardBatchRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(walletRewardService.rewardMultipleClubs(req)));
    }

    @PostMapping("/reward/members")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> rewardMultipleMembers(
            @Valid @RequestBody WalletRewardBatchRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(walletRewardService.rewardMultipleMembers(req)));
    }


}
