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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClubApplicationServiceImpl implements ClubApplicationService {

    private final ClubApplicationRepository appRepo;
    private final ClubRepository clubRepo;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final MajorRepository majorRepository;
    private final EmailService emailService;

    // ============================================================
    // 🟢 1. Sinh viên nộp đơn xin tạo CLB
    // ============================================================
    @Override
    public ClubApplicationResponse createOnline(Long proposerId, ClubApplicationCreateRequest req) {
        User proposer = userRepo.findById(proposerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (appRepo.findByClubName(req.clubName()).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "Club name already exists");

        Major major = majorRepository.findById(req.majorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Major not found"));

        ClubApplication app = ClubApplication.builder()
                .proposer(proposer)
                .submittedBy(proposer)
                .clubName(req.clubName())
                .description(req.description())
                .major(major)
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
                    String.format("""
                            Đơn xin thành lập CLB <b>%s</b> đã bị từ chối.<br>
                            <b>Lý do:</b> %s<br><br>
                            Vui lòng chỉnh sửa và nộp lại nếu cần thiết.
                            """, app.getClubName(), req.rejectReason()));

            return ClubApplicationResponse.fromEntity(app);
        }

        // ✅ Duyệt đơn
        app.setStatus(ClubApplicationStatusEnum.APPROVED);
        appRepo.save(app);

        // 🏫 Tạo CLB
        Club club = Club.builder()
                .name(app.getClubName())
                .description(app.getDescription())
                .major(app.getMajor())
                .vision(app.getVision())
                .createdBy(app.getReviewedBy())
                .memberCount(0)
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

        // 🔗 Liên kết Club với đơn
        app.setClub(club);
        appRepo.save(app);

        // 📧 Gửi mail thông báo cho người nộp đơn
        emailService.sendEmail(app.getProposer().getEmail(),
                "Đơn xin tạo CLB đã được phê duyệt",
                String.format("""
                        Xin chào <b>%s</b>,<br><br>
                        Đơn xin tạo CLB <b>%s</b> của bạn đã được phê duyệt thành công 🎉<br><br>
                        CLB đã được khởi tạo trong hệ thống UniClub.<br><br>
                        <b>Lưu ý:</b><br>
                        - Nhà trường sẽ tạo thủ công 2 tài khoản (Chủ nhiệm & Phó chủ nhiệm).<br>
                        - Hai tài khoản này có domain <b>@uniclub.edu.vn</b> và sẽ được gửi cho bạn qua email khi sẵn sàng.<br><br>
                        Trân trọng,<br>
                        <b>UniClub System</b>
                        """, app.getProposer().getFullName(), app.getClubName()));

        return ClubApplicationResponse.fromEntity(app);
    }

    // ============================================================
    // 🟣 3. Các hàm tiện ích khác
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
