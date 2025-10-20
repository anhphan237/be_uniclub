package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ClubApplicationService;
import com.example.uniclub.service.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder; // ✅ Thêm import này
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class ClubApplicationServiceImpl implements ClubApplicationService {

    private final ClubApplicationRepository appRepo;
    private final ClubRepository clubRepo;
    private final UserRepository userRepo;
    private final MembershipRepository membershipRepo;
    private final WalletRepository walletRepo;
    private final RoleRepository roleRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder; // ✅ Inject password encoder

    // ============================================================
    // 🟢 1. Sinh viên nộp đơn xin tạo CLB
    // ============================================================
    @Override
    public ClubApplicationResponse createOnline(Long proposerId, ClubApplicationCreateRequest req) {
        User proposer = userRepo.findById(proposerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (appRepo.findByClubName(req.clubName()).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "Club name already exists");

        ClubApplication app = ClubApplication.builder()
                .proposer(proposer)
                .submittedBy(proposer)
                .clubName(req.clubName())
                .description(req.description())
                .major(req.major())
                .vision(req.vision())
                .proposerReason(req.proposerReason())
                .status(ClubApplicationStatusEnum.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        appRepo.save(app);
        return ClubApplicationResponse.fromEntity(app);
    }

    // ============================================================
    // 🟠 2. UniStaff duyệt hoặc từ chối đơn
    // ============================================================
    @Transactional
    @Override
    public ClubApplicationResponse decide(Long id, Long staffId, ClubApplicationDecisionRequest req) {
        ClubApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        User staff = userRepo.findById(staffId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff not found"));

        if (app.getStatus() != ClubApplicationStatusEnum.PENDING)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Application already reviewed");

        app.setReviewedBy(staff);
        app.setReviewedAt(LocalDateTime.now());

        // ❌ Từ chối
        if (!req.approve()) {
            if (req.rejectReason() == null || req.rejectReason().isBlank())
                throw new ApiException(HttpStatus.BAD_REQUEST, "Reject reason required");

            app.setRejectReason(req.rejectReason());
            app.setStatus(ClubApplicationStatusEnum.REJECTED);
            appRepo.save(app);

            emailService.sendEmail(app.getProposer().getEmail(),
                    "Đơn xin tạo CLB bị từ chối",
                    "Đơn xin thành lập CLB \"" + app.getClubName() + "\" đã bị từ chối.<br>Lý do: <b>" + req.rejectReason() + "</b>");
            return ClubApplicationResponse.fromEntity(app);
        }

        // ✅ Duyệt
        app.setStatus(ClubApplicationStatusEnum.APPROVED);
        appRepo.save(app);

        emailService.sendEmail(app.getProposer().getEmail(),
                "Đơn xin tạo CLB được phê duyệt",
                """
                Đơn xin thành lập CLB của bạn đã được phê duyệt.<br>
                Nhà trường sẽ cung cấp 2 tài khoản (Chủ nhiệm & Phó chủ nhiệm) có domain @uniclub.edu.vn.<br>
                Sau khi đăng nhập, họ cần đổi mật khẩu và cập nhật thông tin cá nhân.
                """);
        return ClubApplicationResponse.fromEntity(app);
    }

    // ============================================================
    // 🟣 3. UniStaff xác nhận khởi tạo CLB chính thức
    // ============================================================
    @Transactional
    @Override
    public void finalizeClubCreation(Long appId, ClubFinalizeRequest req) {
        ClubApplication app = appRepo.findById(appId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (app.getStatus() != ClubApplicationStatusEnum.APPROVED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Application must be approved first");

        // 🏫 Tạo CLB chính thức
        Club club = Club.builder()
                .name(app.getClubName())
                .description(app.getDescription())
                .majorName(app.getMajor())
                .vision(app.getVision())
                .createdBy(app.getReviewedBy())
                .build();
        clubRepo.save(club);

        // 💰 Tạo ví cho CLB
        Wallet wallet = Wallet.builder()
                .club(club)
                .ownerType(WalletOwnerTypeEnum.CLUB)
                .balancePoints(0)
                .build();
        walletRepo.save(wallet);
        club.setWallet(wallet);
        clubRepo.save(club);

        // 👥 Tự tạo 2 tài khoản Leader & ViceLeader
        Role leaderSystemRole = roleRepo.findByRoleName("CLUB_LEADER")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role CLUB_LEADER not found"));

        String slug = club.getName().trim().toLowerCase().replaceAll("\\s+", "");

        // ✅ Mã hóa mật khẩu 123
        String encodedPassword = passwordEncoder.encode("123");

        // 🟢 Chủ nhiệm
        User leader = User.builder()
                .email("leader_" + slug + "@uniclub.edu.vn")
                .passwordHash(encodedPassword)
                .fullName("Leader of " + club.getName())
                .studentCode("LEAD_" + slug)
                .status(UserStatusEnum.ACTIVE.name())
                .role(leaderSystemRole)
                .build();
        userRepo.save(leader);

        // 🟠 Phó chủ nhiệm
        User viceLeader = User.builder()
                .email("viceleader_" + slug + "@uniclub.edu.vn")
                .passwordHash(encodedPassword)
                .fullName("Vice Leader of " + club.getName())
                .studentCode("VICE_" + slug)
                .status(UserStatusEnum.ACTIVE.name())
                .role(leaderSystemRole)
                .build();
        userRepo.save(viceLeader);

        // 💼 Tạo ví cho 2 tài khoản
        walletRepo.save(Wallet.builder()
                .ownerType(WalletOwnerTypeEnum.USER)
                .user(leader)
                .balancePoints(0)
                .build());

        walletRepo.save(Wallet.builder()
                .ownerType(WalletOwnerTypeEnum.USER)
                .user(viceLeader)
                .balancePoints(0)
                .build());

        // 🤝 Thêm membership cho 2 người
        membershipRepo.save(Membership.builder()
                .user(leader)
                .club(club)
                .clubRole(ClubRoleEnum.LEADER)
                .state(MembershipStateEnum.ACTIVE)
                .joinedDate(java.time.LocalDate.now())
                .staff(true)
                .build());

        membershipRepo.save(Membership.builder()
                .user(viceLeader)
                .club(club)
                .clubRole(ClubRoleEnum.VICE_LEADER)
                .state(MembershipStateEnum.ACTIVE)
                .joinedDate(java.time.LocalDate.now())
                .staff(true)
                .build());

        // 🔗 Liên kết lại với ClubApplication
        app.setClub(club);
        appRepo.save(app);

        // 📧 Gửi mail thông báo
        emailService.sendEmail(app.getProposer().getEmail(),
                "CLB đã được khởi tạo thành công",
                """
                Xin chúc mừng! CLB "%s" đã được khởi tạo chính thức.<br>
                Hai tài khoản đã được cấp cho bạn:<br>
                - Chủ nhiệm: leader_%s@uniclub.edu.vn<br>
                - Phó chủ nhiệm: viceleader_%s@uniclub.edu.vn<br><br>
                Mật khẩu mặc định: <b>123</b><br>
                Vui lòng đăng nhập và đổi mật khẩu.
                """.formatted(club.getName(), slug, slug)
        );
    }

    // ============================================================
    // 🟤 4. Các hàm tiện ích còn lại
    // ============================================================
    @Override
    public List<ClubApplicationResponse> getPending() {
        return appRepo.findByStatus(ClubApplicationStatusEnum.PENDING)
                .stream().map(ClubApplicationResponse::fromEntity).toList();
    }

    @Override
    public List<ClubApplicationResponse> getByUser(Long userId) {
        return appRepo.findByProposer_UserId(userId)
                .stream().map(ClubApplicationResponse::fromEntity).toList();
    }

    @Override
    public ClubApplicationResponse getById(Long userId, String roleName, Long id) {
        ClubApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (roleName.equals("STUDENT") && !app.getProposer().getUserId().equals(userId))
            throw new ApiException(HttpStatus.FORBIDDEN, "Not authorized to view this application");

        return ClubApplicationResponse.fromEntity(app);
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", appRepo.count());
        stats.put("pending", appRepo.countByStatus(ClubApplicationStatusEnum.PENDING));
        stats.put("approved", appRepo.countByStatus(ClubApplicationStatusEnum.APPROVED));
        stats.put("rejected", appRepo.countByStatus(ClubApplicationStatusEnum.REJECTED));
        return stats;
    }

    @Override
    public List<ClubApplicationResponse> search(String keyword) {
        return appRepo.searchByKeyword(keyword)
                .stream().map(ClubApplicationResponse::fromEntity).toList();
    }

    @Override
    public void delete(Long id) {
        if (!appRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Application not found");
        appRepo.deleteById(id);
    }

    @Override
    public List<ClubApplicationResponse> getAllApplications() {
        return appRepo.findAll().stream()
                .map(ClubApplicationResponse::fromEntity)
                .toList();
    }
}
