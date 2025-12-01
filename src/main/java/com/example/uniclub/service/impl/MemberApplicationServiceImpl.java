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
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.MemberApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    private final EmailService emailService;
    private final MajorPolicyRepository majorPolicyRepo;

    // =====================================================================================
    //  STUDENT SUBMITS APPLICATION
    // =====================================================================================
    @Override
    @Transactional
    public MemberApplicationResponse createByEmail(String email, MemberApplicationCreateRequest req) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Club club = clubRepo.findById(req.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        // =============== FIX: Validate Major Policy Before Submitting ===============
        List<MembershipStateEnum> activeStates = List.of(
                MembershipStateEnum.ACTIVE,
                MembershipStateEnum.PENDING
        );

        int activeOrPendingCount = membershipRepo.countByUser_UserIdAndStateIn(
                user.getUserId(),
                activeStates
        );

        Major major = user.getMajor();
        if (major != null) {
            List<MajorPolicy> policies = majorPolicyRepo.findByMajor_IdAndActiveTrue(major.getId());
            Integer maxJoin = policies.stream()
                    .map(MajorPolicy::getMaxClubJoin)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if (maxJoin != null && activeOrPendingCount >= maxJoin) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "You have reached the maximum number of clubs allowed for your major (" +
                                maxJoin + "). Current: " + activeOrPendingCount);
            }
        }
        // ============================================================================

        // Check membership status (cannot join again)
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

        // Check existing pending application
        boolean hasPending = appRepo.findAll().stream()
                .anyMatch(a -> a.getApplicant().getUserId().equals(user.getUserId())
                        && a.getClub().getClubId().equals(req.getClubId())
                        && a.getStatus() == MemberApplicationStatusEnum.PENDING);

        if (hasPending) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have a pending application for this club");
        }

        MemberApplication app = MemberApplication.builder()
                .club(club)
                .applicant(user)
                .message(req.getMessage())
                .status(MemberApplicationStatusEnum.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        MemberApplication saved = appRepo.save(app);

        emailService.sendMemberApplicationSubmitted(
                user.getEmail(),
                user.getFullName(),
                club.getName()
        );

        return mapToResponse(saved);
    }



    // =====================================================================================
    //  LEADER / ADMIN UPDATE APPLICATION STATUS
    // =====================================================================================
    @Override
    @Transactional
    public MemberApplicationResponse updateStatusByEmail(
            String email,
            Long applicationId,
            MemberApplicationStatusUpdateRequest req
    ) {
        User actor = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        MemberApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!isClubLeaderOrVice(actor.getUserId(), app.getClub().getClubId()) && !hasAdminRole(actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        MemberApplicationStatusEnum newStatus = MemberApplicationStatusEnum.valueOf(req.getStatus().toUpperCase());
        app.setStatus(newStatus);
        app.setNote(req.getNote());
        app.setUpdatedAt(LocalDateTime.now());
        app.setHandledBy(actor);

        // =============== FIX: Validate Major Policy Before Approving ===============
        if (newStatus == MemberApplicationStatusEnum.APPROVED) {

            User user = app.getApplicant();

            List<MembershipStateEnum> activeStatesCheck = List.of(
                    MembershipStateEnum.ACTIVE,
                    MembershipStateEnum.PENDING
            );

            int activeOrPendingCount = membershipRepo.countByUser_UserIdAndStateIn(
                    user.getUserId(),
                    activeStatesCheck
            );

            Major major = user.getMajor();
            if (major != null) {
                List<MajorPolicy> policies = majorPolicyRepo.findByMajor_IdAndActiveTrue(major.getId());
                Integer maxJoin = policies.stream()
                        .map(MajorPolicy::getMaxClubJoin)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

                if (maxJoin != null && activeOrPendingCount >= maxJoin) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "User has reached the maximum number of clubs allowed for their major (" +
                                    maxJoin + "). Current: " + activeOrPendingCount);
                }
            }
        }
        // ============================================================================


        // Approve application (same logic)
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

            Optional<Membership> existingOpt =
                    membershipRepo.findByUser_UserIdAndClub_ClubId(
                            app.getApplicant().getUserId(),
                            app.getClub().getClubId()
                    );

            if (existingOpt.isPresent()) {
                Membership existing = existingOpt.get();
                existing.setState(MembershipStateEnum.ACTIVE);
                existing.setJoinedDate(LocalDate.now());
                existing.setEndDate(null);
                existing.setClubRole(ClubRoleEnum.MEMBER);
                existing.setMemberMultiplier(1.0);
                existing.setStaff(false);

                membershipRepo.save(existing);

            } else {

                Membership m = new Membership();
                m.setUser(app.getApplicant());
                m.setClub(app.getClub());
                m.setClubRole(ClubRoleEnum.MEMBER);
                m.setState(MembershipStateEnum.ACTIVE);
                m.setJoinedDate(LocalDate.now());
                m.setMemberMultiplier(1.0);
                m.setStaff(false);

                membershipRepo.save(m);
            }

            emailService.sendMemberApplicationResult(
                    app.getApplicant().getEmail(),
                    app.getApplicant().getFullName(),
                    app.getClub().getName(),
                    true
            );
        }

        // Reject application
        if (newStatus == MemberApplicationStatusEnum.REJECTED) {

            emailService.sendMemberApplicationResult(
                    app.getApplicant().getEmail(),
                    app.getApplicant().getFullName(),
                    app.getClub().getName(),
                    false
            );
        }

        return mapToResponse(appRepo.save(app));
    }



    // =====================================================================================
    //  QUERY FUNCTIONS
    // =====================================================================================

    @Override
    public List<MemberApplicationResponse> findAll() {
        return appRepo.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<MemberApplicationResponse> findApplicationsByEmail(String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isAdmin = hasAdminRole(user);

        var memberships = membershipRepo.findByUser_UserId(user.getUserId());

        boolean isLeaderOrVice = memberships.stream()
                .anyMatch(m -> m.getClubRole() == ClubRoleEnum.LEADER ||
                        m.getClubRole() == ClubRoleEnum.VICE_LEADER);

        if (isAdmin) {
            return appRepo.findAll().stream().map(this::mapToResponse).toList();
        }

        if (isLeaderOrVice) {
            var managedClubIds = memberships.stream()
                    .filter(m -> m.getClubRole() == ClubRoleEnum.LEADER ||
                            m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                    .map(m -> m.getClub().getClubId())
                    .collect(Collectors.toSet());

            return appRepo.findAll().stream()
                    .filter(a -> managedClubIds.contains(a.getClub().getClubId()))
                    .map(this::mapToResponse)
                    .toList();
        }

        return appRepo.findByApplicant_UserId(user.getUserId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    public List<MemberApplicationResponse> getByClubId(CustomUserDetails principal, Long clubId) {

        if (!isClubLeaderOrVice(principal.getId(), clubId) &&
                !hasAdminRole(principal.getUser())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return appRepo.findAll().stream()
                .filter(a -> a.getClub().getClubId().equals(clubId))
                .map(this::mapToResponse)
                .toList();
    }


    // =====================================================================================
    //  GET BY ID
    // =====================================================================================

    @Override
    public MemberApplicationResponse getApplicationById(CustomUserDetails principal, Long id) {

        MemberApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        boolean isOwner = app.getApplicant().getUserId().equals(principal.getId());
        boolean canView = hasAdminRole(principal.getUser())
                || isClubLeaderOrVice(principal.getId(), app.getClub().getClubId());

        if (!isOwner && !canView)
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        return mapToResponse(app);
    }


    // =====================================================================================
    //  CANCEL APPLICATION
    // =====================================================================================

    @Override
    @Transactional
    public void cancelApplication(CustomUserDetails principal, Long id) {

        MemberApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!app.getApplicant().getUserId().equals(principal.getId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only cancel your own applications");

        if (app.getStatus() != MemberApplicationStatusEnum.PENDING)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending applications can be cancelled");

        emailService.sendMemberApplicationCancelled(
                app.getApplicant().getEmail(),
                app.getApplicant().getFullName(),
                app.getClub().getName()
        );

        appRepo.delete(app);
    }


    // =====================================================================================
    //  PENDING BY CLUB
    // =====================================================================================

    @Override
    public List<MemberApplicationResponse> getPendingByClub(CustomUserDetails principal, Long clubId) {

        if (!isClubLeaderOrVice(principal.getId(), clubId) &&
                !hasAdminRole(principal.getUser())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return appRepo.findAll().stream()
                .filter(a -> a.getClub().getClubId().equals(clubId)
                        && a.getStatus() == MemberApplicationStatusEnum.PENDING)
                .map(this::mapToResponse)
                .toList();
    }


    // =====================================================================================
    //  APPROVE (LEADER)
    // =====================================================================================

    @Override
    @Transactional
    public MemberApplicationResponse approve(CustomUserDetails principal, Long id) {

        MemberApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!isClubLeaderOrVice(principal.getId(), app.getClub().getClubId()) &&
                !hasAdminRole(principal.getUser()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        // =============== FIX: Validate Major Policy Before Approving ===============
        User user = app.getApplicant();

        List<MembershipStateEnum> activeStatesCheck = List.of(
                MembershipStateEnum.ACTIVE,
                MembershipStateEnum.PENDING
        );

        int activeOrPendingCount = membershipRepo.countByUser_UserIdAndStateIn(
                user.getUserId(),
                activeStatesCheck
        );

        Major major = user.getMajor();
        if (major != null) {
            List<MajorPolicy> policies = majorPolicyRepo.findByMajor_IdAndActiveTrue(major.getId());
            Integer maxJoin = policies.stream()
                    .map(MajorPolicy::getMaxClubJoin)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if (maxJoin != null && activeOrPendingCount >= maxJoin) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "User has reached the maximum number of clubs allowed for their major (" +
                                maxJoin + "). Current: " + activeOrPendingCount);
            }
        }
        // ============================================================================

        app.setStatus(MemberApplicationStatusEnum.APPROVED);
        app.setHandledBy(principal.getUser());
        app.setUpdatedAt(LocalDateTime.now());
        appRepo.save(app);

        List<MembershipStateEnum> activeStates = List.of(
                MembershipStateEnum.ACTIVE,
                MembershipStateEnum.APPROVED,
                MembershipStateEnum.PENDING
        );

        boolean hasActive = membershipRepo
                .existsByUser_UserIdAndClub_ClubIdAndStateIn(
                        app.getApplicant().getUserId(),
                        app.getClub().getClubId(),
                        activeStates
                );

        if (!hasActive) {

            Optional<Membership> existingOpt =
                    membershipRepo.findByUser_UserIdAndClub_ClubId(
                            app.getApplicant().getUserId(),
                            app.getClub().getClubId()
                    );

            if (existingOpt.isPresent()) {
                Membership existing = existingOpt.get();
                existing.setState(MembershipStateEnum.ACTIVE);
                existing.setJoinedDate(LocalDate.now());
                existing.setEndDate(null);
                existing.setClubRole(ClubRoleEnum.MEMBER);
                existing.setMemberMultiplier(1.0);
                existing.setStaff(false);

                membershipRepo.save(existing);

            } else {

                Membership m = new Membership();
                m.setUser(app.getApplicant());
                m.setClub(app.getClub());
                m.setClubRole(ClubRoleEnum.MEMBER);
                m.setState(MembershipStateEnum.ACTIVE);
                m.setJoinedDate(LocalDate.now());
                m.setMemberMultiplier(1.0);
                m.setStaff(false);

                membershipRepo.save(m);
            }
        }

        emailService.sendMemberApplicationResult(
                app.getApplicant().getEmail(),
                app.getApplicant().getFullName(),
                app.getClub().getName(),
                true
        );

        return mapToResponse(app);
    }



    // =====================================================================================
    //  REJECT
    // =====================================================================================

    @Override
    @Transactional
    public MemberApplicationResponse reject(CustomUserDetails principal, Long id, String note) {

        MemberApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!isClubLeaderOrVice(principal.getId(), app.getClub().getClubId()
        ) && !hasAdminRole(principal.getUser()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        app.setStatus(MemberApplicationStatusEnum.REJECTED);
        app.setNote(note);
        app.setHandledBy(principal.getUser());
        app.setUpdatedAt(LocalDateTime.now());

        appRepo.save(app);

        emailService.sendMemberApplicationResult(
                app.getApplicant().getEmail(),
                app.getApplicant().getFullName(),
                app.getClub().getName(),
                false
        );

        return mapToResponse(app);
    }


    // =====================================================================================
    //  STATS
    // =====================================================================================

    @Override
    public MemberApplicationStatsResponse getStatsByClub(Long clubId) {

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        long total =
                appRepo.countByClubAndStatus(club, MemberApplicationStatusEnum.PENDING)
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


    // =====================================================================================
    //  HANDLED APPLICATIONS
    // =====================================================================================

    @Override
    public List<MemberApplicationResponse> getHandledApplications(CustomUserDetails principal, Long clubId) {

        if (!isClubLeaderOrVice(principal.getId(), clubId)
                && !hasAdminRole(principal.getUser())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return appRepo.findAll().stream()
                .filter(a -> a.getClub().getClubId().equals(clubId)
                        && (a.getStatus() == MemberApplicationStatusEnum.APPROVED
                        || a.getStatus() == MemberApplicationStatusEnum.REJECTED))
                .map(this::mapToResponse)
                .toList();
    }


    // =====================================================================================
    //  RE-SUBMIT REJECTED APPLICATION
    // =====================================================================================

    @Override
    @Transactional
    public MemberApplicationResponse resubmitApplication(
            CustomUserDetails principal,
            Long id,
            MemberApplicationCreateRequest req
    ) {

        MemberApplication oldApp = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!oldApp.getApplicant().getUserId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only resubmit your own applications");
        }

        if (oldApp.getStatus() != MemberApplicationStatusEnum.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only rejected applications can be resubmitted");
        }

        oldApp.setMessage(req.getMessage());
        oldApp.setStatus(MemberApplicationStatusEnum.PENDING);
        oldApp.setNote(null);
        oldApp.setHandledBy(null);
        oldApp.setUpdatedAt(LocalDateTime.now());

        appRepo.save(oldApp);

        emailService.sendMemberApplicationSubmitted(
                principal.getUser().getEmail(),
                principal.getUser().getFullName(),
                oldApp.getClub().getName()
        );

        return mapToResponse(oldApp);
    }


    // =====================================================================================
    //  UPDATE NOTE
    // =====================================================================================

    @Override
    @Transactional
    public MemberApplicationResponse updateNoteForApplication(
            CustomUserDetails principal,
            Long id,
            String note
    ) {
        MemberApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!isClubLeaderOrVice(principal.getId(), app.getClub().getClubId())
                && !hasAdminRole(principal.getUser())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        app.setNote(note);
        app.setUpdatedAt(LocalDateTime.now());
        appRepo.save(app);
        return mapToResponse(app);
    }


    // =====================================================================================
    //  FILTER BY STATUS
    // =====================================================================================

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
                .toList();
    }


    // =====================================================================================
    //  RECENT APPLICATIONS
    // =====================================================================================

    @Override
    public List<MemberApplicationResponse> getRecentApplications() {

        return appRepo.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10)
                .map(this::mapToResponse)
                .toList();
    }


    // =====================================================================================
    //  DAILY STATS
    // =====================================================================================

    @Override
    public List<MemberApplicationStatsResponse> getDailyStats(Long clubId) {

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        return appRepo.findAll().stream()
                .filter(a -> a.getClub().equals(club)
                        && a.getCreatedAt().isAfter(sevenDaysAgo))
                .collect(Collectors.groupingBy(
                        a -> a.getCreatedAt().toLocalDate(),
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> MemberApplicationStatsResponse.builder()
                        .clubId(club.getClubId())
                        .clubName(club.getName())
                        .date(e.getKey().toString())
                        .count(e.getValue())
                        .build())
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();
    }


    // =====================================================================================
    //  GET BY APPLICANT ID
    // =====================================================================================

    @Override
    public List<MemberApplicationResponse> getApplicationsByApplicant(Long userId) {

        userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        return appRepo.findAll().stream()
                .filter(a -> a.getApplicant().getUserId().equals(userId))
                .map(this::mapToResponse)
                .toList();
    }


    // =====================================================================================
    //  HELPER METHODS
    // =====================================================================================

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
                .map(m -> m.getClubRole() == ClubRoleEnum.LEADER
                        || m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                .orElse(false);
    }

    private boolean hasAdminRole(User user) {
        if (user == null || user.getRole() == null) return false;
        String role = user.getRole().getRoleName();
        return role.equals("ADMIN") || role.equals("UNIVERSITY_STAFF");
    }
}
