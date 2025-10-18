package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.dto.response.ClubApplicationResponse.SimpleUser;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.mapper.ClubApplicationMapper;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ClubApplicationService;
import com.example.uniclub.service.ClubService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubApplicationServiceImpl implements ClubApplicationService {

    private final ClubApplicationRepository appRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final RoleRepository roleRepo;
    private final MajorPolicyRepository majorPolicyRepo;
    private final MembershipRepository membershipRepo;
    private final WalletRepository walletRepo;
    private final ClubService clubService;
    private final PasswordEncoder passwordEncoder;

    // ============================================================
    // 🟢 1. Submit online application
    // ============================================================
    @Override
    public ClubApplicationResponse createOnline(Long proposerId, ClubApplicationCreateRequest req) {
        User proposer = userRepo.findById(proposerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (appRepo.findByClubName(req.clubName()).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "Club name already exists");

        ClubApplication app = ClubApplication.builder()
                .proposer(proposer)
                .clubName(req.clubName())
                .description(req.description())
                .category(req.category())
                .proposerReason(req.proposerReason())
                .sourceType(ApplicationSourceTypeEnum.ONLINE)
                .status(ClubApplicationStatusEnum.PENDING)
                .submittedBy(proposer)
                .createdAt(LocalDateTime.now())
                .build();

        appRepo.save(app);
        return toResp(app);
    }

    // ============================================================
    // 🟩 2. Create offline application (already approved)
    // ============================================================
    @Override
    public ClubApplicationResponse createOffline(Long staffId, ClubApplicationOfflineRequest req) {
        User staff = userRepo.findById(staffId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff not found"));

        if (appRepo.findByClubName(req.clubName()).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "Club name already exists");

        ClubApplication app = ClubApplication.builder()
                .clubName(req.clubName())
                .description(req.description())
                .category(req.category())
                .sourceType(ApplicationSourceTypeEnum.OFFLINE)
                .status(ClubApplicationStatusEnum.APPROVED)
                .reviewedBy(staff)
                .reviewedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        appRepo.save(app);
        clubService.createFromOfflineApplication(app, req);
        return toResp(app);
    }

    // ============================================================
    // 🟦 3. Get list of pending applications
    // ============================================================
    @Override
    public List<ClubApplicationResponse> getPending() {
        return appRepo.findByStatus(ClubApplicationStatusEnum.PENDING)
                .stream().map(this::toResp).toList();
    }

    // ============================================================
    // 🟠 4. Approve or reject application (AUTO CREATE CLUB)
    // ============================================================
    @Override
    @Transactional
    public ClubApplicationResponse decide(Long id, Long staffId, ClubApplicationDecisionRequest req) {
        ClubApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        User staff = userRepo.findById(staffId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff not found"));

        if (app.getStatus() != ClubApplicationStatusEnum.PENDING)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Application has already been reviewed");

        app.setReviewedBy(staff);
        app.setReviewedAt(LocalDateTime.now());
        app.setInternalNote(req.internalNote());

        // ❌ TỪ CHỐI
        if (!req.approve()) {
            if (req.rejectReason() == null || req.rejectReason().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Reject reason is required");
            }
            app.setRejectReason(req.rejectReason());
            app.setStatus(ClubApplicationStatusEnum.REJECTED);
            appRepo.save(app);
            return toResp(app);
        }

        // ✅ DUYỆT ĐƠN
        app.setStatus(ClubApplicationStatusEnum.APPROVED);
        appRepo.save(app);

        MajorPolicy majorPolicy = MajorPolicy.builder()
                .policyName("Default Policy for " + app.getClubName())
                .name("Default Policy for " + app.getClubName())
                .description("Chính sách mặc định cho CLB " + app.getClubName())
                .majorId(1L) // hoặc lấy majorId từ application nếu có
                .majorName(app.getCategory()) // hoặc app.getMajorName() nếu có
                .maxClubJoin(3)               // ví dụ mặc định
                .rewardMultiplier(1.0)
                .active(true)
                .build();

        // ✅ Lưu policy trước -> có ID
        majorPolicy = majorPolicyRepo.save(majorPolicy);

        // === Tạo CLB mới ===
        Club club = Club.builder()
                .name(app.getClubName())
                .description(app.getDescription())
                .createdBy(app.getProposer())
                .majorPolicy(majorPolicy)
                .build();
        clubRepo.save(club);

        // === Cập nhật người nộp đơn → Chủ nhiệm CLB ===
        Role leaderRole = roleRepo.findByRoleName("CLUB_LEADER")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role CLUB_LEADER not found"));
        User proposer = app.getProposer();
        proposer.setRole(leaderRole);
        userRepo.save(proposer);

        Membership leaderMembership = Membership.builder()
                .user(proposer)
                .club(club)
                .clubRole(ClubRoleEnum.LEADER)
                .state(MembershipStateEnum.ACTIVE)
                .staff(true)
                .joinedDate(java.time.LocalDate.now())
                .build();
        membershipRepo.save(leaderMembership);

        // === Tạo hoặc gán tài khoản Phó chủ nhiệm (nếu có) ===
        if (req.viceLeaderEmail() != null && !req.viceLeaderEmail().isBlank()) {
            User viceLeader = userRepo.findByEmail(req.viceLeaderEmail()).orElseGet(() -> {
                if (req.viceLeaderStudentCode() == null || req.viceLeaderStudentCode().isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Student code required for new vice leader");
                }
                Role viceRole = roleRepo.findByRoleName("CLUB_VICE_LEADER")
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role CLUB_VICE_LEADER not found"));
                User u = User.builder()
                        .email(req.viceLeaderEmail())
                        .fullName(req.viceLeaderFullName())
                        .passwordHash(passwordEncoder.encode("123456")) // ✅ mã hóa mật khẩu
                        .studentCode(req.viceLeaderStudentCode())
                        .status(UserStatusEnum.ACTIVE.name())
                        .role(viceRole)
                        .build();
                return userRepo.save(u);
            });

            // Gắn membership cho Phó chủ nhiệm
            Membership viceMem = Membership.builder()
                    .user(viceLeader)
                    .club(club)
                    .clubRole(ClubRoleEnum.VICE_LEADER)
                    .state(MembershipStateEnum.ACTIVE)
                    .staff(true)
                    .joinedDate(java.time.LocalDate.now())
                    .build();
            membershipRepo.save(viceMem);
        }

        // === Tạo ví CLB ===
        Wallet wallet = Wallet.builder()
                .ownerType(WalletOwnerTypeEnum.CLUB)
                .balancePoints(0)
                .club(club)
                .build();
        walletRepo.save(wallet);
        club.setWallet(wallet);
        clubRepo.save(club);

        return toResp(app);
    }

    // ============================================================
    // 🟣 5. Get applications submitted by a specific user
    // ============================================================
    @Override
    public List<ClubApplicationResponse> getByUser(Long userId) {
        return appRepo.findByProposer_UserId(userId)
                .stream().map(this::toResp).toList();
    }

    // ============================================================
    // 🔵 6. Get details of a specific application
    // ============================================================
    @Override
    public ClubApplicationResponse getById(Long userId, String roleName, Long id) {
        ClubApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (roleName.equals("STUDENT") && (app.getProposer() == null ||
                !Objects.equals(app.getProposer().getUserId(), userId))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not authorized to view this application");
        }

        return toResp(app);
    }

    // ============================================================
    // 🟤 7. Filter applications by status / category
    // ============================================================
    @Override
    public List<ClubApplicationResponse> filter(String status, String clubType) {
        return appRepo.findAll().stream()
                .filter(a -> status == null || a.getStatus().name().equalsIgnoreCase(status))
                .filter(a -> clubType == null || a.getCategory().equalsIgnoreCase(clubType))
                .map(this::toResp)
                .collect(Collectors.toList());
    }

    // ============================================================
    // ⚪ 8. Update internal note
    // ============================================================
    @Override
    public ClubApplicationResponse updateNote(Long id, Long staffId, String note) {
        ClubApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));
        app.setInternalNote(note);
        app.setReviewedAt(LocalDateTime.now());
        appRepo.save(app);
        return toResp(app);
    }

    // ============================================================
    // 🟠 9. Delete application (Admin only)
    // ============================================================
    @Override
    public void delete(Long id) {
        if (!appRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Application not found");
        appRepo.deleteById(id);
    }

    // ============================================================
    // 🟢 10. Upload supporting document
    // ============================================================
    @Override
    public String uploadFile(Long id, Long userId, MultipartFile file) {
        ClubApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));
        try {
            String url = "https://storage.uniclub.vn/applications/" + file.getOriginalFilename();
            app.setAttachmentUrl(url);
            appRepo.save(app);
            return url;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed");
        }
    }

    // ============================================================
    // 🟣 11. Get application statistics by status
    // ============================================================
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", appRepo.count());
        stats.put("pending", appRepo.countByStatus(ClubApplicationStatusEnum.PENDING));
        stats.put("approved", appRepo.countByStatus(ClubApplicationStatusEnum.APPROVED));
        stats.put("rejected", appRepo.countByStatus(ClubApplicationStatusEnum.REJECTED));
        return stats;
    }

    // ============================================================
    // 🔵 12. Search by keyword
    // ============================================================
    @Override
    public List<ClubApplicationResponse> search(String keyword) {
        return appRepo.searchByKeyword(keyword)
                .stream().map(this::toResp).toList();
    }

    // ============================================================
    // 🧩 Helper: Convert entity → DTO
    // ============================================================
    private ClubApplicationResponse toResp(ClubApplication app) {
        SimpleUser proposer = app.getProposer() == null ? null :
                SimpleUser.builder().fullName(app.getProposer().getFullName()).email(app.getProposer().getEmail()).build();

        SimpleUser reviewer = app.getReviewedBy() == null ? null :
                SimpleUser.builder().fullName(app.getReviewedBy().getFullName()).email(app.getReviewedBy().getEmail()).build();

        return ClubApplicationResponse.builder()
                .applicationId(app.getApplicationId())
                .clubName(app.getClubName())
                .description(app.getDescription())
                .category(app.getCategory())
                .submittedBy(proposer)
                .reviewedBy(reviewer)
                .status(app.getStatus())
                .sourceType(app.getSourceType())
                .rejectReason(app.getRejectReason())
                .submittedAt(app.getCreatedAt())
                .reviewedAt(app.getReviewedAt())
                .attachmentUrl(app.getAttachmentUrl())
                .internalNote(app.getInternalNote())
                .build();
    }

    @Override
    public List<ClubApplicationResponse> getAllApplications() {
        return appRepo.findAll()
                .stream()
                .map(ClubApplicationMapper.INSTANCE::toResponse)
                .collect(Collectors.toList());
    }
}
