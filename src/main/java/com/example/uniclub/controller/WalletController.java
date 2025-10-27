package com.example.uniclub.controller;

import com.example.uniclub.dto.request.WalletAdjustRequest;
import com.example.uniclub.dto.request.WalletTransferRequest;
import com.example.uniclub.dto.response.WalletResponse;
import com.example.uniclub.entity.User;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Wallet;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletRewardService walletRewardService;
    private final WalletService walletService;
    private final WalletRepository walletRepo;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepo;
    private final JwtUtil jwtUtil;

    // ================================================================
    // 🧩 1️⃣ LẤY VÍ CỦA NGƯỜI DÙNG HIỆN TẠI
    // ------------------------------------------------
    // ✅ Endpoint: GET /api/wallets/me
    // ✅ Quyền: bất kỳ người dùng đã đăng nhập (STUDENT / CLUB_LEADER / STAFF / ADMIN)
    // ✅ Chức năng: lấy thông tin ví cá nhân (user wallet)
    // ================================================================
    @GetMapping("/me")
    public ResponseEntity<?> getMyWallet(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer "))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");

        String email = jwtUtil.getSubject(token.replace("Bearer ", ""));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        // Nếu chưa có ví thì tự động tạo
        Wallet wallet = walletRewardService.getWalletByUserId(user.getUserId());
        if (wallet == null)
            wallet = walletService.getOrCreateUserWallet(user);

        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .balancePoints(wallet.getBalancePoints())
                .ownerType(wallet.getOwnerType())
                .userId(user.getUserId())
                .userFullName(user.getFullName())
                .build();

        return ResponseEntity.ok(response);
    }

    // ================================================================
    // 🎁 2️⃣ THƯỞNG ĐIỂM CHO 1 THÀNH VIÊN CỤ THỂ
    // ------------------------------------------------
    // ✅ Endpoint: POST /api/wallets/reward/{membershipId}?points=50&reason=Excellent+Work
    // ✅ Quyền: CLUB_LEADER / UNIVERSITY_STAFF / ADMIN
    // ✅ Chức năng: cộng điểm vào ví của 1 thành viên trong CLB
    // ✅ Tác động: trừ điểm ví CLB (nếu có logic Reward) và cộng vào ví cá nhân
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
        User operator = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operator not found"));

        Wallet updatedWallet = walletRewardService.rewardPointsByMembershipId(operator, membershipId, points, reason);

        WalletResponse.WalletResponseBuilder builder = WalletResponse.builder()
                .walletId(updatedWallet.getWalletId())
                .balancePoints(updatedWallet.getBalancePoints())
                .ownerType(updatedWallet.getOwnerType());

        if (updatedWallet.getUser() != null)
            builder.userId(updatedWallet.getUser().getUserId())
                    .userFullName(updatedWallet.getUser().getFullName());

        if (updatedWallet.getClub() != null)
            builder.clubId(updatedWallet.getClub().getClubId())
                    .clubName(updatedWallet.getClub().getName());

        return ResponseEntity.ok(builder.build());
    }

    // ================================================================
    // 🏫 3️⃣ THƯỞNG ĐIỂM CHO TOÀN BỘ THÀNH VIÊN TRONG CLB
    // ------------------------------------------------
    // ✅ Endpoint: POST /api/wallets/reward/club/{clubId}?points=100&reason=Club+Achievement
    // ✅ Quyền: UNIVERSITY_STAFF / ADMIN
    // ✅ Chức năng: cộng điểm cho tất cả thành viên của CLB
    // ================================================================
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
        User operator = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operator not found"));

        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");

        // ✅ Cho phép Leader hoặc Vice Leader của CLB này
        boolean isClubLeaderOrVice = membershipRepo.findByUser_UserId(operator.getUserId()).stream()
                .anyMatch(m -> m.getClub().getClubId().equals(clubId)
                        && (m.getClubRole().name().equalsIgnoreCase("LEADER")
                        || m.getClubRole().name().equalsIgnoreCase("VICE_LEADER")));

        if (!isAdminOrStaff && !isClubLeaderOrVice)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have permission to reward this club.");

        int count = walletRewardService.rewardPointsByClubId(operator, clubId, points, reason);
        return ResponseEntity.ok("Rewarded " + count + " members successfully.");
    }

    // ================================================================
    // 💰 4️⃣ XEM VÍ CỦA CLB
    // ------------------------------------------------
    // ✅ Endpoint: GET /api/wallets/club/{clubId}
    // ✅ Quyền: ADMIN / UNIVERSITY_STAFF / CLUB_LEADER
    // ✅ Chức năng: lấy thông tin ví của CLB
    // ================================================================
    @GetMapping("/club/{clubId}")
    public ResponseEntity<?> getClubWallet(@PathVariable Long clubId, HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token.");

        String email = jwtUtil.getSubject(authHeader.replace("Bearer ", ""));
        User operator = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operator not found"));

        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");

        // ✅ Club Leader chỉ được xem ví CLB của mình
        if (!isAdminOrStaff) {
            boolean isClubLeader = membershipRepo.findByUser_UserId(operator.getUserId()).stream()
                    .anyMatch(m -> m.getClub().getClubId().equals(clubId)
                            && m.getClubRole().name().equalsIgnoreCase("LEADER"));

            if (!isClubLeader)
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to view this club's wallet.");
        }

        Wallet wallet = walletService.getWalletByClubId(clubId);
        if (wallet == null) {
            Club club = clubRepository.findById(clubId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));
            wallet = walletService.getOrCreateClubWallet(club);
        }

        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .balancePoints(wallet.getBalancePoints())
                .ownerType(wallet.getOwnerType())
                .clubId(wallet.getClub().getClubId())
                .clubName(wallet.getClub().getName())
                .build();

        return ResponseEntity.ok(response);
    }

    // ================================================================
    // 🪙 5️⃣ NẠP ĐIỂM CHO CLB (Top-up)
    // ------------------------------------------------
    // ✅ Endpoint: POST /api/wallets/club/{clubId}/topup?points=1000&reason=Event+Budget
    // ✅ Quyền: UNIVERSITY_STAFF / ADMIN
    // ✅ Chức năng: nạp thêm điểm vào ví của CLB
    // ✅ Tác động: tăng balancePoints và ghi lại transaction log
    // ================================================================
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
        User operator = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operator not found"));

        String role = operator.getRole().getRoleName();
        if (!(role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF")))
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only Admin or University Staff can top up club wallets.");

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        Wallet wallet = walletService.getOrCreateClubWallet(club);
        walletService.addPoints(wallet, points, reason == null ? "Top-up by staff" : reason);

        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .balancePoints(wallet.getBalancePoints())
                .ownerType(wallet.getOwnerType())
                .clubId(wallet.getClub().getClubId())
                .clubName(wallet.getClub().getName())
                .build();

        return ResponseEntity.ok(response);
    }

    // ================================================================
    // ⚙️ 6️⃣ QUẢN TRỊ: CỘNG / TRỪ / CHUYỂN ĐIỂM THỦ CÔNG
    // ------------------------------------------------
    // ✅ Endpoint:
    //    - POST /api/wallets/{id}/add
    //    - POST /api/wallets/{id}/reduce
    //    - POST /api/wallets/transfer
    // ✅ Quyền: ADMIN / UNIVERSITY_STAFF
    // ✅ Chức năng: dùng cho thao tác thủ công hoặc xử lý đặc biệt
    // ================================================================
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
// 📜 7️⃣ LỊCH SỬ GIAO DỊCH CỦA 1 VÍ
// ------------------------------------------------
    @GetMapping("/{walletId}/transactions")
    public ResponseEntity<?> getTransactionsByWallet(@PathVariable Long walletId) {
        var txList = walletService.getTransactionsByWallet(walletId);
        return ResponseEntity.ok(txList);
    }

    // ================================================================
// 🏫 8️⃣ LỊCH SỬ PHÁT ĐIỂM: UNI → CLUB
// ------------------------------------------------
    @GetMapping("/transactions/uni-to-club")
    public ResponseEntity<?> getUniToClubTransactions() {
        var txList = walletService.getAllClubTopups();
        return ResponseEntity.ok(txList);
    }

    // ================================================================
// 👥 9️⃣ LỊCH SỬ PHÁT ĐIỂM: CLUB → MEMBER
// ------------------------------------------------
    @GetMapping("/transactions/club-to-member")
    public ResponseEntity<?> getClubToMemberTransactions() {
        var txList = walletService.getAllMemberRewards();
        return ResponseEntity.ok(txList);
    }

}
