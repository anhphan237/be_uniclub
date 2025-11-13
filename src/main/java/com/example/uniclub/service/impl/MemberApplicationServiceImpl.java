package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.MemberApplicationCreateRequest;
import com.example.uniclub.dto.request.MemberApplicationStatusUpdateRequest;
import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.dto.response.MemberApplicationStatsResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.MemberApplicationService;
import com.example.uniclub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberApplicationServiceImpl implements MemberApplicationService {

    private final MemberApplicationRepository appRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final MembershipRepository membershipRepo;
    private final NotificationService notificationService;

    // ✅ Student submits application
    @Override
    @Transactional
    public MemberApplicationResponse createByEmail(String email, MemberApplicationCreateRequest req) {
        // 🔹 1. Tìm user và club
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Club club = clubRepo.findById(req.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        // 🔹 2. Kiểm tra xem user có đang là thành viên hợp lệ không
        List<MembershipStateEnum> blockedStates = List.of(
                MembershipStateEnum.ACTIVE,
                MembershipStateEnum.APPROVED,
                MembershipStateEnum.PENDING
        );

        boolean isAlreadyMember = membershipRepo.existsByUser_UserIdAndClub_ClubIdAndStateIn(
                user.getUserId(),
                club.getClubId(),
                blockedStates
        );

        if (isAlreadyMember) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You are already a member of this club");
        }

        // 🔹 3. Kiểm tra trùng đơn ứng tuyển đang pending
        boolean hasPendingApplication = appRepo.findAll().stream()
                .anyMatch(a -> a.getApplicant().getUserId().equals(user.getUserId())
                        && a.getClub().getClubId().equals(req.getClubId())
                        && a.getStatus() == MemberApplicationStatusEnum.PENDING);

        if (hasPendingApplication) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have a pending application for this club");
        }

        // 🔹 4. Tạo đơn ứng tuyển mới
        MemberApplication app = MemberApplication.builder()
                .club(club)
                .applicant(user)
                .message(req.getMessage())
                .status(MemberApplicationStatusEnum.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        MemberApplication saved = appRepo.save(app);

        // 🔹 5. Gửi email thông báo
        try {
            notificationService.sendApplicationSubmitted(user.getEmail(), club.getName());
        } catch (Exception e) {
            log.warn("Failed to send application notification email to {}", user.getEmail(), e);
        }

        return mapToResponse(saved);
    }


    // ✅ Leader / Admin update status
    @Override
    @Transactional
    public MemberApplicationResponse updateStatusByEmail(String email, Long applicationId, MemberApplicationStatusUpdateRequest req) {

        User actor = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        MemberApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        // Kiểm tra quyền
        if (!isClubLeaderOrVice(actor.getUserId(), app.getClub().getClubId()) && !hasAdminRole(actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        MemberApplicationStatusEnum newStatus = MemberApplicationStatusEnum.valueOf(req.getStatus().toUpperCase());
        app.setStatus(newStatus);
        app.setNote(req.getNote());
        app.setUpdatedAt(LocalDateTime.now());
        app.setHandledBy(actor);

        // ================================
        // ✅ KHI LEADER APPROVE ĐƠN
        // ================================
        if (newStatus == MemberApplicationStatusEnum.APPROVED) {

            List<MembershipStateEnum> activeStates = List.of(
                    MembershipStateEnum.ACTIVE,
                    MembershipStateEnum.APPROVED,
                    MembershipStateEnum.PENDING
            );

            boolean alreadyMember = membershipRepo.existsByUser_UserIdAndClub_ClubIdAndStateIn(
                    app.getApplicant().getUserId(),
                    app.getClub().getClubId(),
                    activeStates
            );

            if (alreadyMember) {
                throw new ApiException(HttpStatus.CONFLICT, "User is already a member or has a pending request");
            }

            // ================================
            // 🔄 Reuse membership cũ nếu có
            // ================================
            Optional<Membership> existingOpt = membershipRepo.findByUser_UserIdAndClub_ClubId(
                    app.getApplicant().getUserId(),
                    app.getClub().getClubId()
            );

            if (existingOpt.isPresent()) {
                Membership existing = existingOpt.get();

                existing.setState(MembershipStateEnum.ACTIVE);
                existing.setJoinedDate(LocalDate.now());
                existing.setEndDate(null);
                existing.setClubRole(ClubRoleEnum.MEMBER);

                // DEFAULTS BẮT BUỘC
                existing.setMemberLevel(MemberLevelEnum.BASIC);
                existing.setMemberMultiplier(1.0);
                existing.setStaff(false);

                membershipRepo.save(existing);

            } else {
                // ================================
                // 🆕 Tạo membership mới
                // ================================
                Membership m = new Membership();
                m.setUser(app.getApplicant());
                m.setClub(app.getClub());
                m.setClubRole(ClubRoleEnum.MEMBER);
                m.setState(MembershipStateEnum.ACTIVE);
                m.setJoinedDate(LocalDate.now());

                // DEFAULTS BẮT BUỘC
                m.setMemberLevel(MemberLevelEnum.BASIC);
                m.setMemberMultiplier(1.0);
                m.setStaff(false);

                membershipRepo.save(m);
            }

            // Gửi email thông báo APPROVED
            notificationService.sendApplicationResult(
                    app.getApplicant().getEmail(),
                    app.getClub().getName(),
                    true
            );
        }

        // ================================
        // ❌ Khi Leader từ chối
        // ================================
        if (newStatus == MemberApplicationStatusEnum.REJECTED) {
            notificationService.sendApplicationResult(
                    app.getApplicant().getEmail(),
                    app.getClub().getName(),
                    false
            );
        }

        MemberApplication saved = appRepo.save(app);
        return mapToResponse(saved);
    }



    // ✅ Admin / Staff view all
    @Override
    public List<MemberApplicationResponse> findAll() {
        return appRepo.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ✅ Student / Leader view by email
    @Override
    public List<MemberApplicationResponse> findApplicationsByEmail(String email) {
        // 🔹 1. Lấy user hiện tại
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        // 🔹 2. Kiểm tra vai trò
        boolean isAdminOrStaff = hasAdminRole(user);

        // 🔹 3. Lấy danh sách membership của user
        var memberships = membershipRepo.findByUser_UserId(user.getUserId());

        boolean isLeaderOrVice = memberships.stream()
                .anyMatch(m -> m.getClubRole() == ClubRoleEnum.LEADER ||
                        m.getClubRole() == ClubRoleEnum.VICE_LEADER);

        // ✅ Nếu là Admin/Staff → xem toàn bộ
        if (isAdminOrStaff) {
            return appRepo.findAll().stream()
                    .filter(a -> a.getClub() != null)
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        // ✅ Nếu là Leader/Vice → xem đơn thuộc CLB mà mình quản lý
        if (isLeaderOrVice) {
            var managedClubIds = memberships.stream()
                    .filter(m -> m.getClubRole() == ClubRoleEnum.LEADER ||
                            m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                    .map(m -> m.getClub().getClubId())
                    .collect(Collectors.toSet());

            return appRepo.findAll().stream()
                    .filter(a -> managedClubIds.contains(a.getClub().getClubId()))
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        // ✅ Nếu là Student → chỉ xem đơn của chính mình
        return appRepo.findByApplicant_UserId(user.getUserId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }



    // ✅ Get by clubId (leader / staff)
    @Override
    public List<MemberApplicationResponse> getByClubId(CustomUserDetails principal, Long clubId) {
        Long userId = principal.getId();
        if (!isClubLeaderOrVice(userId, clubId) && !hasAdminRole(principal.getUser())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return appRepo.findAll().stream()
                .filter(a -> a.getClub().getClubId().equals(clubId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 🔧 Helper mapping
    private MemberApplicationResponse mapToResponse(MemberApplication app) {
        return MemberApplicationResponse.builder()
                .applicationId(app.getApplicationId())
                .clubId(app.getClub().getClubId())
                .clubName(app.getClub().getName())
                .applicantId(app.getApplicant().getUserId())
                .applicantName(app.getApplicant().getFullName())
                .applicantEmail(app.getApplicant().getEmail())
                .status(app.getStatus().name())
                .message(app.getMessage())
                .reason(app.getNote())
                .handledById(app.getHandledBy() != null ? app.getHandledBy().getUserId() : null)
                .handledByName(app.getHandledBy() != null ? app.getHandledBy().getFullName() : null)
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    private boolean isClubLeaderOrVice(Long userId, Long clubId) {
        return membershipRepo.findByUser_UserIdAndClub_ClubId(userId, clubId)
                .map(m -> m.getClubRole() == ClubRoleEnum.LEADER ||
                        m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                .orElse(false);
    }

    private boolean hasAdminRole(User user) {
        if (user == null || user.getRole() == null) return false;
        String role = user.getRole().getRoleName();
        return role.equals("ADMIN") || role.equals("UNIVERSITY_STAFF");
    }
    // ========================== PHẦN MỞ RỘNG ==========================

    @Override
    public MemberApplicationResponse getApplicationById(CustomUserDetails principal, Long id) {
        MemberApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));
        User user = principal.getUser();

        boolean isOwner = app.getApplicant().getUserId().equals(principal.getId());
        boolean canView = hasAdminRole(user) || isClubLeaderOrVice(principal.getId(), app.getClub().getClubId());
        if (!isOwner && !canView)
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        return mapToResponse(app);
    }

    @Override
    @Transactional
    public void cancelApplication(CustomUserDetails principal, Long id) {
        MemberApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!app.getApplicant().getUserId().equals(principal.getId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only cancel your own applications");

        if (app.getStatus() != MemberApplicationStatusEnum.PENDING)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending applications can be cancelled");

        appRepo.delete(app);
    }

    @Override
    public List<MemberApplicationResponse> getPendingByClub(CustomUserDetails principal, Long clubId) {
        if (!isClubLeaderOrVice(principal.getId(), clubId) && !hasAdminRole(principal.getUser()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        return appRepo.findAll().stream()
                .filter(a -> a.getClub().getClubId().equals(clubId)
                        && a.getStatus() == MemberApplicationStatusEnum.PENDING)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MemberApplicationResponse approve(CustomUserDetails principal, Long id) {

        MemberApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!isClubLeaderOrVice(principal.getId(), app.getClub().getClubId())
                && !hasAdminRole(principal.getUser()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        app.setStatus(MemberApplicationStatusEnum.APPROVED);
        app.setHandledBy(principal.getUser());
        app.setUpdatedAt(LocalDateTime.now());
        appRepo.save(app);

        // CHECK active membership
        List<MembershipStateEnum> activeStates = List.of(
                MembershipStateEnum.ACTIVE,
                MembershipStateEnum.APPROVED,
                MembershipStateEnum.PENDING
        );

        boolean hasActiveMembership = membershipRepo
                .existsByUser_UserIdAndClub_ClubIdAndStateIn(
                        app.getApplicant().getUserId(),
                        app.getClub().getClubId(),
                        activeStates
                );

        if (!hasActiveMembership) {

            Optional<Membership> existingOpt = membershipRepo
                    .findByUser_UserIdAndClub_ClubId(
                            app.getApplicant().getUserId(),
                            app.getClub().getClubId()
                    );

            if (existingOpt.isPresent()) {
                // ===== RE-ACTIVATE OLD MEMBERSHIP =====
                Membership existing = existingOpt.get();
                existing.setState(MembershipStateEnum.ACTIVE);
                existing.setJoinedDate(LocalDate.now());
                existing.setEndDate(null);
                existing.setClubRole(ClubRoleEnum.MEMBER);

                // DEFAULT VALUES — BẮT BUỘC
                existing.setMemberLevel(MemberLevelEnum.BASIC);
                existing.setMemberMultiplier(1.0);
                existing.setStaff(false);

                membershipRepo.save(existing);

            } else {
                // ===== CREATE NEW MEMBERSHIP =====
                Membership m = new Membership();
                m.setUser(app.getApplicant());
                m.setClub(app.getClub());
                m.setClubRole(ClubRoleEnum.MEMBER);
                m.setState(MembershipStateEnum.ACTIVE);
                m.setJoinedDate(LocalDate.now());

                // DEFAULT VALUES — BẮT BUỘC
                m.setMemberLevel(MemberLevelEnum.BASIC);
                m.setMemberMultiplier(1.0);
                m.setStaff(false);

                membershipRepo.save(m);
            }
        }

        notificationService.sendApplicationResult(
                app.getApplicant().getEmail(),
                app.getClub().getName(),
                true
        );

        return mapToResponse(app);
    }



    @Override
    @Transactional
    public MemberApplicationResponse reject(CustomUserDetails principal, Long id, String note) {
        MemberApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!isClubLeaderOrVice(principal.getId(), app.getClub().getClubId()) && !hasAdminRole(principal.getUser()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        app.setStatus(MemberApplicationStatusEnum.REJECTED);
        app.setNote(note);
        app.setHandledBy(principal.getUser());
        app.setUpdatedAt(LocalDateTime.now());
        appRepo.save(app);

        notificationService.sendApplicationResult(app.getApplicant().getEmail(), app.getClub().getName(), false);
        return mapToResponse(app);
    }

    @Override
    public MemberApplicationStatsResponse getStatsByClub(Long clubId) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        long total = appRepo.countByClubAndStatus(club, MemberApplicationStatusEnum.PENDING)
                + appRepo.countByClubAndStatus(club, MemberApplicationStatusEnum.APPROVED)
                + appRepo.countByClubAndStatus(club, MemberApplicationStatusEnum.REJECTED);

        return MemberApplicationStatsResponse.builder()
                .clubId(club.getClubId())
                .clubName(club.getName())
                .total(total)
                .pending(appRepo.countByClubAndStatus(club, MemberApplicationStatusEnum.PENDING))
                .approved(appRepo.countByClubAndStatus(club, MemberApplicationStatusEnum.APPROVED))
                .rejected(appRepo.countByClubAndStatus(club, MemberApplicationStatusEnum.REJECTED))
                .build();
    }
    @Override
    public List<MemberApplicationResponse> getHandledApplications(CustomUserDetails principal, Long clubId) {
        // ✅ Chỉ cho phép Leader/Vice/Admin xem
        if (!isClubLeaderOrVice(principal.getId(), clubId) && !hasAdminRole(principal.getUser())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return appRepo.findAll().stream()
                .filter(a -> a.getClub().getClubId().equals(clubId)
                        && (a.getStatus() == MemberApplicationStatusEnum.APPROVED
                        || a.getStatus() == MemberApplicationStatusEnum.REJECTED))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional
    public MemberApplicationResponse resubmitApplication(
            CustomUserDetails principal,
            Long id,
            MemberApplicationCreateRequest req) {

        MemberApplication oldApp = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        // ✅ Chỉ cho phép sinh viên chủ đơn resubmit
        if (!oldApp.getApplicant().getUserId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only resubmit your own applications");
        }

        // ✅ Chỉ cho phép resubmit nếu đơn trước bị từ chối
        if (oldApp.getStatus() != MemberApplicationStatusEnum.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only rejected applications can be resubmitted");
        }

        // ✅ Cập nhật nội dung đơn
        oldApp.setMessage(req.getMessage());
        oldApp.setStatus(MemberApplicationStatusEnum.PENDING);
        oldApp.setNote(null);
        oldApp.setHandledBy(null);
        oldApp.setUpdatedAt(LocalDateTime.now());

        appRepo.save(oldApp);

        // Gửi thông báo nếu cần
        notificationService.sendApplicationSubmitted(
                principal.getUsername(),
                oldApp.getClub().getName()
        );

        return mapToResponse(oldApp);
    }

    // ========================= PHẦN BỔ SUNG CUỐI =========================

    @Override
    @Transactional
    public MemberApplicationResponse updateNoteForApplication(CustomUserDetails principal, Long id, String note) {
        MemberApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!isClubLeaderOrVice(principal.getId(), app.getClub().getClubId()) && !hasAdminRole(principal.getUser())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        app.setNote(note);
        app.setUpdatedAt(LocalDateTime.now());
        appRepo.save(app);
        return mapToResponse(app);
    }

    @Override
    public List<MemberApplicationResponse> getApplicationsByStatus(String status) {
        MemberApplicationStatusEnum st;
        try {
            st = MemberApplicationStatusEnum.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }

        return appRepo.findAll().stream()
                .filter(a -> a.getStatus() == st)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberApplicationResponse> getRecentApplications() {
        return appRepo.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberApplicationStatsResponse> getDailyStats(Long clubId) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        return appRepo.findAll().stream()
                .filter(a -> a.getClub().equals(club) && a.getCreatedAt().isAfter(sevenDaysAgo))
                .collect(Collectors.groupingBy(a -> a.getCreatedAt().toLocalDate(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> MemberApplicationStatsResponse.builder()
                        .clubId(club.getClubId())
                        .clubName(club.getName())
                        .date(e.getKey().toString())
                        .count(e.getValue())
                        .build())
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberApplicationResponse> getApplicationsByApplicant(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        return appRepo.findAll().stream()
                .filter(a -> a.getApplicant().getUserId().equals(userId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

}


