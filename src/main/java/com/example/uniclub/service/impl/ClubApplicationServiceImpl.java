package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ClubApplicationService;
import com.example.uniclub.service.EmailService;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class ClubApplicationServiceImpl implements ClubApplicationService {

    private final ClubApplicationRepository appRepo;
    private final ClubRepository clubRepo;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final MajorRepository majorRepository;
    private final RoleRepository roleRepo;
    private final MembershipRepository membershipRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private static final Map<String, OtpInfo> otpCache = new ConcurrentHashMap<>();
    private final OtpTokenRepository otpTokenRepository;

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
    // 🟠 2. UniStaff duyệt đơn (approve / reject)
    // ============================================================
    @Transactional
    @Override
    public ClubApplicationResponse decide(Long id, Long staffId, ClubApplicationDecisionRequest req) {
        ClubApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        User staff = userRepo.findById(staffId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff not found"));

        if (app.getStatus() != ClubApplicationStatusEnum.PENDING)
            throw new ApiException(BAD_REQUEST, "Application already reviewed");

        app.setReviewedBy(staff);
        app.setReviewedAt(LocalDateTime.now());

        // ❌ Từ chối
        if (!req.approve()) {
            if (req.rejectReason() == null || req.rejectReason().isBlank())
                throw new ApiException(BAD_REQUEST, "Reject reason required");

            app.setRejectReason(req.rejectReason());
            app.setStatus(ClubApplicationStatusEnum.REJECTED);
            appRepo.save(app);

            emailService.sendEmail(
                    app.getProposer().getEmail(),
                    "Club creation request rejected",
                    String.format("""
            The request to establish the club <b>%s</b> has been rejected.<br>
            <b>Reason:</b> %s<br><br>
            Please review and resubmit if necessary.
            """, app.getClubName(), req.rejectReason())
            );


            return ClubApplicationResponse.fromEntity(app);
        }

        // ✅ Duyệt đơn
        app.setStatus(ClubApplicationStatusEnum.APPROVED);
        appRepo.save(app);

        // 🏫 Tạo CLB mới
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
                .balancePoints(0L)
                .build();
        walletRepo.save(wallet);
        club.setWallet(wallet);
        clubRepo.save(club);

        // 🔗 Liên kết Club với đơn
        app.setClub(club);
        appRepo.save(app);

        // 📧 Thông báo cho người nộp đơn
        emailService.sendEmail(
                app.getProposer().getEmail(),
                "Club creation request approved",
                String.format("""
            Hello <b>%s</b>,<br><br>
            Your club creation request for <b>%s</b> has been successfully approved <br><br>
            The club has now been created in the UniClub system.<br><br>
            <b>Note:</b><br>
            - The school will manually create 2 accounts (President & Vice President).<br>
            - These accounts will use the domain <b>@uniclub.edu.vn</b> and will be sent to you via email once ready.<br><br>
            Best regards,<br>
            <b>UniClub System</b>
            """, app.getProposer().getFullName(), app.getClubName())
        );


        return ClubApplicationResponse.fromEntity(app);
    }

    // ============================================================
    // 🟢 3. UniStaff nhập tài khoản CLB (Leader & Vice Leader)
    // ============================================================
    @Transactional
    @Override
    public ApiResponse<?> createClubAccounts(CreateClubAccountsRequest req) {

        // 1️⃣ Lấy club
        Club club = clubRepo.findById(req.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        // 2️⃣ Lấy role CLUB_LEADER
        Role clubLeaderRole = roleRepo.findByRoleName("CLUB_LEADER")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role CLUB_LEADER not found"));

        // 3️⃣ Tạo User Leader
        User leader = new User();
        leader.setFullName(req.getLeaderFullName());
        leader.setEmail(req.getLeaderEmail());
        leader.setPasswordHash(passwordEncoder.encode(req.getDefaultPassword()));
        leader.setStatus(UserStatusEnum.ACTIVE.name());
        leader.setRole(clubLeaderRole);
        userRepo.save(leader);

        // 4️⃣ Tạo User Vice Leader
        User vice = new User();
        vice.setFullName(req.getViceFullName());
        vice.setEmail(req.getViceEmail());
        vice.setPasswordHash(passwordEncoder.encode(req.getDefaultPassword()));
        vice.setStatus(UserStatusEnum.ACTIVE.name());
        vice.setRole(clubLeaderRole);
        userRepo.save(vice);

        // 5️⃣ Membership Leader
        Membership leaderMember = new Membership();
        leaderMember.setUser(leader);
        leaderMember.setClub(club);
        leaderMember.setClubRole(ClubRoleEnum.LEADER);
        leaderMember.setState(MembershipStateEnum.ACTIVE);
        leaderMember.setStaff(true);
        leaderMember.setJoinedDate(LocalDate.now());
        // Default tự chạy:
        // memberLevel = BASIC
        // memberMultiplier = 1.0
        membershipRepo.save(leaderMember);

        // 6️⃣ Membership Vice Leader
        Membership viceMember = new Membership();
        viceMember.setUser(vice);
        viceMember.setClub(club);
        viceMember.setClubRole(ClubRoleEnum.VICE_LEADER);
        viceMember.setState(MembershipStateEnum.ACTIVE);
        viceMember.setStaff(true);
        viceMember.setJoinedDate(LocalDate.now());
        membershipRepo.save(viceMember);

        // 7️⃣ Set leader cho Club
        club.setLeader(leader);
        clubRepo.save(club);

        // 8️⃣ Update trạng thái đơn thành COMPLETE
        ClubApplication app = appRepo.findByClub(club).orElse(null);
        if (app != null) {
            app.setStatus(ClubApplicationStatusEnum.COMPLETED);
            app.setReviewedAt(LocalDateTime.now());
            appRepo.save(app);
        }

        // 9️⃣ Gửi email
        if (app != null && app.getProposer() != null) {
            User proposer = app.getProposer();
            try {
                String subject = "[UniClub] Your club " + club.getName() + " has been successfully created";

                String content = String.format("""
                Hello %s,<br><br>
                The club <b>%s</b> that you proposed has been approved by UniStaff and successfully created! 🎉<br><br>
                Below are the details of your club’s two main accounts:<br><br>
                🔹 <b>President (Leader)</b><br>
                Full name: %s<br>
                Email: %s<br><br>
                🔹 <b>Vice President (Vice Leader)</b><br>
                Full name: %s<br>
                Email: %s<br><br>
                Default password for both accounts: <b>%s</b><br><br>
                Both accounts can log in at:<br>
                <a href='https://uniclub.id.vn/login'>https://uniclub.id.vn/login</a><br><br>
                The status of your club creation request is now: <b>COMPLETE </b><br><br>
                Best regards,<br>
                <b>UniClub System</b>
                """,
                        proposer.getFullName(),
                        club.getName(),
                        req.getLeaderFullName(), req.getLeaderEmail(),
                        req.getViceFullName(), req.getViceEmail(),
                        req.getDefaultPassword()
                );

                emailService.sendEmail(proposer.getEmail(), subject, content);
                System.out.println("Sent COMPLETE email to proposer: " + proposer.getEmail());

            } catch (Exception e) {
                System.err.println("Failed to send COMPLETE email: " + e.getMessage());
            }
        }

        return ApiResponse.ok("Created leader & vice leader successfully, status = COMPLETE");
    }


    // ============================================================
    // 🟣 4. Các hàm tiện ích khác
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
        stats.put("complete", appRepo.countByStatus(ClubApplicationStatusEnum.COMPLETED));
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
    @Getter
    @Setter
    @AllArgsConstructor
    private static class OtpInfo {
        private String otp;
        private LocalDateTime expiresAt;
    }

    @Override
    @Transactional
    public void saveOtp(String email, String otp) {
        otpTokenRepository.deleteByEmail(email);

        OtpToken token = OtpToken.builder()
                .email(email)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();

        otpTokenRepository.save(token);
    }



    @Override
    @Transactional
    public void verifyOtp(String email, String otp) {
        OtpToken token = otpTokenRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(BAD_REQUEST, "You have not been issued an OTP."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpTokenRepository.deleteByEmail(email);
            throw new ApiException(BAD_REQUEST, "Your OTP has expired.");
        }

        if (!token.getOtp().equals(otp)) {
            throw new ApiException(BAD_REQUEST, "Invalid OTP code.");
        }

        otpTokenRepository.deleteByEmail(email);
    }




    @Override
    public User findStudentByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found."));
    }

}
