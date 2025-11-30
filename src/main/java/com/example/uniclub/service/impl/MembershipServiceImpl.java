package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.ClubLeaveRequestResponse;
import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.uniclub.enums.MembershipStateEnum;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {
    private final ClubLeaveRequestEntityRepository leaveRequestRepo;
    private final EventRegistrationRepository eventRegistrationRepo;
    private final MajorPolicyRepository majorPolicyRepo;
    private final EventLogService eventLogService;
    private final MembershipRepository membershipRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final ClubService clubService;
    private final EmailService emailService;

    // ========================== 🔹 Helper: Mapping ==========================
    private MembershipResponse toResp(Membership m) {
        User u = m.getUser();
        Club c = m.getClub();

        return MembershipResponse.builder()
                .membershipId(m.getMembershipId())
                .userId(u.getUserId())
                .clubId(c.getClubId())
                .clubRole(m.getClubRole())
                .state(m.getState())
                .staff(m.isStaff())
                .joinedDate(m.getJoinedDate())
                .endDate(m.getEndDate())
                .fullName(u.getFullName())
                .studentCode(u.getStudentCode())
                .clubName(c.getName())
                .email(u.getEmail())
                .avatarUrl(u.getAvatarUrl())
                .major(u.getMajor() != null ? u.getMajor().getName() : null)
                .build();
    }

    // ========================== 🔹 Helper: Validate Major Policy ==========================
    private void validateMajorPolicy(User user) {

        Major major = user.getMajor();
        if (major == null) {
            log.warn("User {} has no major assigned -> skipping policy validation", user.getEmail());
            return;
        }

        Long majorId = major.getId();

        // 🎯 Load active policies from database
        List<MajorPolicy> policies =
                majorPolicyRepo.findByMajor_IdAndActiveTrue(majorId);

        if (policies.isEmpty()) {
            log.info("Major {} has no active policy -> skipping limit check", major.getName());
            return;
        }

        // 🎯 Extract the first policy that has maxClubJoin
        Integer maxJoin = policies.stream()
                .map(MajorPolicy::getMaxClubJoin)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        // 🚫 No maxClubJoin defined → skip
        if (maxJoin == null) {
            log.info("Major {} has no maxClubJoin policy -> skipping limit check", major.getName());
            return;
        }

        // ⭐ FIX: Count ACTIVE + PENDING memberships
        int activeOrPendingCount = membershipRepo.countByUser_UserIdAndStateIn(
                user.getUserId(),
                List.of(MembershipStateEnum.ACTIVE, MembershipStateEnum.PENDING)
        );

        // 🚫 Vi phạm chính sách
        if (activeOrPendingCount >= maxJoin) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    String.format(
                            "You have reached the maximum number of clubs allowed for your major (%d). Current: %d",
                            maxJoin, activeOrPendingCount
                    )
            );
        }
    }



    // ========================== 🔹 1. Basic Membership Operations ==========================
    @Override
    public List<MembershipResponse> getMyMemberships(Long userId) {
        return membershipRepo.findActiveMembershipsByUserId(userId)
                .stream()
                .map(this::toResp)
                .toList();
    }


    @Override
    public boolean isMemberOfClub(Long userId, Long clubId) {
        return membershipRepo.existsByUser_UserIdAndClub_ClubIdAndStateIn(
                userId,
                clubId,
                List.of(
                        MembershipStateEnum.ACTIVE,
                        MembershipStateEnum.PENDING,
                        MembershipStateEnum.APPROVED
                )
        );
    }
    @Override
    public boolean isActiveMember(Long userId, Long clubId) {
        return membershipRepo.existsByUser_UserIdAndClub_ClubIdAndState(
                userId,
                clubId,
                MembershipStateEnum.ACTIVE
        );
    }

    @Override
    @Transactional
    public MembershipResponse joinClub(Long userId, Long clubId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        // Check if membership exists
        Optional<Membership> existingOpt = membershipRepo.findByUser_UserIdAndClub_ClubId(userId, clubId);

        if (existingOpt.isPresent()) {
            Membership existing = existingOpt.get();
            MembershipStateEnum state = existing.getState();

            switch (state) {
                case ACTIVE, APPROVED, PENDING -> {
                    throw new ApiException(HttpStatus.CONFLICT,
                            "You are already a member or have a pending request.");
                }

                case KICKED, INACTIVE, REJECTED -> {
                    existing.setState(MembershipStateEnum.PENDING);
                    existing.setJoinedDate(LocalDate.now());
                    existing.setEndDate(null);
                    existing.setClubRole(ClubRoleEnum.MEMBER);

                    // Reset fields
                    existing.setMemberMultiplier(1.0);
                    existing.setStaff(false);

                    membershipRepo.save(existing);

                    // 🔥 Notify LEADER
                    membershipRepo.findActiveLeaderByClubId(clubId).ifPresent(leader ->
                            emailService.sendNewMembershipRequestToLeader(
                                    leader.getEmail(),
                                    leader.getFullName(),
                                    club.getName(),
                                    user.getFullName()
                            )
                    );

                    // 🔔 Notify applicant
                    emailService.sendMemberApplicationSubmitted(
                            user.getEmail(),
                            user.getFullName(),
                            club.getName()
                    );

                    return toResp(existing);
                }

                default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Unexpected membership state: " + state);
            }
        }

        // Check major policies
        validateMajorPolicy(user);

        // Create new membership
        Membership newMembership = new Membership();
        newMembership.setUser(user);
        newMembership.setClub(club);
        newMembership.setClubRole(ClubRoleEnum.MEMBER);
        newMembership.setState(MembershipStateEnum.PENDING);
        newMembership.setJoinedDate(LocalDate.now());
        newMembership.setMemberMultiplier(1.0);
        newMembership.setStaff(false);

        membershipRepo.save(newMembership);

        // Log action
        eventLogService.logAction(
                userId,
                user.getFullName(),
                null,
                null,
                UserActionEnum.JOIN_CLUB,
                "User joined club " + club.getName()
        );

        // 🔥 Notify LEADER
        membershipRepo.findActiveLeaderByClubId(clubId).ifPresent(leader ->
                emailService.sendNewMembershipRequestToLeader(
                        leader.getEmail(),
                        leader.getFullName(),
                        club.getName(),
                        user.getFullName()
                )
        );

        // 🔔 Notify applicant
        emailService.sendMemberApplicationSubmitted(
                user.getEmail(),
                user.getFullName(),
                club.getName()
        );

        return toResp(newMembership);
    }

    // ========================== 🔹 2. Membership Approval Management ==========================
    @Override
    @Transactional
    public MembershipResponse approveMember(Long membershipId, Long approverId) {

        Membership m = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        Club club = m.getClub();


        //  1. Approver phải là Leader hoặc Vice Leader
        Membership approver = membershipRepo
                .findByUser_UserIdAndClub_ClubId(approverId, club.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club"));

        if (!(approver.getClubRole() == ClubRoleEnum.LEADER ||
                approver.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only Leader or Vice Leader can approve members");
        }

        //  2. Không approve khi đã ACTIVE
        if (m.getState() == MembershipStateEnum.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "This member is already approved");
        }


        //  3. Kiểm tra Major Policy trước khi duyệt
        validateMajorPolicy(m.getUser());


        //  4. Reset thông tin cần thiết
        if (m.getMemberMultiplier() == null) {
            m.setMemberMultiplier(1.0);
        }
        if (!m.isStaff()) {
            m.setStaff(false);
        }
        // 🟢 5. Approve
        m.setState(MembershipStateEnum.ACTIVE);
        membershipRepo.save(m);
        // Cập nhật số lượng thành viên
        clubService.updateMemberCount(club.getClubId());
        //✉ 6. Gửi email qua EmailService CHUẨN
        emailService.sendMemberApplicationResult(
                m.getUser().getEmail(),
                m.getUser().getFullName(),
                m.getClub().getName(),
                true // approved
        );

        return toResp(m);
    }




    @Override
    @Transactional
    public MembershipResponse rejectMember(Long membershipId, Long approverId, String reason) {

        Membership m = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        Club club = m.getClub();

        //  1. Approver must be Leader or Vice Leader
        Membership approver = membershipRepo
                .findByUser_UserIdAndClub_ClubId(approverId, club.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club"));

        if (!(approver.getClubRole() == ClubRoleEnum.LEADER ||
                approver.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only Leader or Vice Leader can reject members");
        }

        // ✔ 2. Cannot reject if already decided
        if (m.getState() == MembershipStateEnum.REJECTED) {
            throw new ApiException(HttpStatus.CONFLICT, "This member has already been rejected");
        }

        if (m.getState() == MembershipStateEnum.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot reject an already approved member");
        }

        // 3. Reject the application
        m.setState(MembershipStateEnum.REJECTED);
        membershipRepo.save(m);

        // cập nhật member count (optional nhưng an toàn)
        clubService.updateMemberCount(club.getClubId());

        // 4. Email using new EmailService format
        emailService.sendMemberApplicationResult(
                m.getUser().getEmail(),
                m.getUser().getFullName(),
                club.getName(),
                false // rejected
        );

        return toResp(m);
    }



    @Override
    @Transactional
    public void removeMember(Long membershipId, Long approverId) {

        Membership m = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        Club club = m.getClub();


        //  1. Check approver belongs to the same club
        Membership approver = membershipRepo
                .findByUser_UserIdAndClub_ClubId(approverId, club.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club"));

        //  2. Only Leader or Vice Leader can remove
        if (!(approver.getClubRole() == ClubRoleEnum.LEADER
                || approver.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only Leader or Vice Leader can remove members");
        }

        //  3. Cannot remove Leader or Vice Leader
        if (m.getClubRole() == ClubRoleEnum.LEADER || m.getClubRole() == ClubRoleEnum.VICE_LEADER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot remove a Leader or Vice Leader");
        }

        //  4. Cannot remove yourself
        if (Objects.equals(m.getUser().getUserId(), approverId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot remove yourself");
        }

        //  5. Remove member
        m.setState(MembershipStateEnum.INACTIVE);
        m.setEndDate(LocalDate.now());
        membershipRepo.save(m);

        clubService.updateMemberCount(club.getClubId());

        // ✉ 6. Send email using unified EmailService
        emailService.sendMemberKickedEmail(
                m.getUser().getEmail(),
                m.getUser().getFullName(),
                club.getName(),
                approver.getUser().getFullName()
        );
    }


    // ========================== 🔹 3. Role Management ==========================
    @Override
    public MembershipResponse updateClubRole(Long membershipId, ClubRoleEnum newRole, Long approverId) {
        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        Long clubId = membership.getClub().getClubId();

        switch (newRole) {
            case LEADER -> {
                if (membershipRepo.existsByClub_ClubIdAndClubRole(clubId, ClubRoleEnum.LEADER))
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Each club can have only one Leader");
            }
            case VICE_LEADER -> {
                if (membershipRepo.existsByClub_ClubIdAndClubRole(clubId, ClubRoleEnum.VICE_LEADER))
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Each club can have only one Vice Leader");
            }
            case STAFF -> {
                long count = membershipRepo.countByClub_ClubIdAndClubRole(clubId, ClubRoleEnum.STAFF);
                if (count >= 5)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Each club can have a maximum of 5 staff members");
            }
            default -> {}
        }

        membership.setClubRole(newRole);
        membershipRepo.save(membership);
        return toResp(membership);
    }

    // ========================== 🔹 4. Member Queries ==========================
    @Override
    public List<MembershipResponse> getMembersByClub(Long clubId) {
        return membershipRepo.findByClub_ClubIdAndState(clubId, MembershipStateEnum.ACTIVE)
                .stream()
                .map(this::toResp)
                .toList();
    }

    @Override
    public List<MembershipResponse> getStaffMembers(Long clubId) {
        return membershipRepo.findByClub_ClubIdAndClubRole(clubId, ClubRoleEnum.STAFF)
                .stream()
                .map(this::toResp)
                .toList();
    }

    @Override
    public List<MembershipResponse> getMembersByLeaderName(String leaderName) {
        Membership leaderMembership = membershipRepo
                .findFirstByUser_FullNameAndClubRole(leaderName, ClubRoleEnum.LEADER)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Leader not found"));

        Long clubId = leaderMembership.getClub().getClubId();
        return getMembersByClub(clubId);
    }

    @Override
    public MembershipResponse updateRole(Long membershipId, Long approverId, String newRole) {
        ClubRoleEnum roleEnum;
        try {
            roleEnum = ClubRoleEnum.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role: " + newRole);
        }
        return updateClubRole(membershipId, roleEnum, approverId);
    }

    @Transactional
    public void updateClubMemberCount(Long clubId) {
        int total = (int) membershipRepo.countByClub_ClubIdAndState(clubId, MembershipStateEnum.ACTIVE);
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));
        club.setMemberCount(total);
        clubRepo.save(club);
    }

    @Override
    public List<MembershipResponse> getPendingMembers(Long clubId) {
        return membershipRepo.findByClub_ClubIdAndState(clubId, MembershipStateEnum.PENDING)
                .stream()
                .map(this::toResp)
                .toList();
    }

    @Override
    @Transactional
    public String kickMember(CustomUserDetails principal, Long membershipId) {

        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        Club club = membership.getClub();
        Membership actor = membershipRepo
                .findByUser_UserIdAndClub_ClubId(principal.getUser().getUserId(), club.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club"));

        // ✔ Only Leader or Vice Leader can remove
        if (!(actor.getClubRole() == ClubRoleEnum.LEADER ||
                actor.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the Leader or Vice Leader can remove members");
        }

        // ✔ Cannot remove yourself
        if (Objects.equals(membership.getUser().getUserId(), principal.getUser().getUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot remove yourself");
        }

        // ✔ Cannot remove Leader/Vice Leader
        if (membership.getClubRole() == ClubRoleEnum.LEADER ||
                membership.getClubRole() == ClubRoleEnum.VICE_LEADER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot remove the Club Leader or Vice Leader");
        }

        // ✔ Perform removal
        membership.setState(MembershipStateEnum.KICKED);
        membership.setEndDate(LocalDate.now());
        membershipRepo.save(membership);

        // ✔ Update club member count
        clubService.updateMemberCount(club.getClubId());

        // ✔ Unified EmailService
        emailService.sendMemberKickedEmail(
                membership.getUser().getEmail(),
                membership.getUser().getFullName(),
                club.getName(),
                actor.getUser().getFullName()
        );

        return "Member " + membership.getUser().getFullName() +
                " has been removed from " + club.getName() +
                " by " + actor.getUser().getFullName();
    }


    @Override
    @Transactional
    public String requestLeave(Long userId, Long clubId, String reason) {

        Membership membership = membershipRepo.findByUser_UserIdAndClub_ClubIdAndState(
                userId, clubId, MembershipStateEnum.ACTIVE
        ).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "You are not an active member of this club."));

        if (membership.getClubRole() == ClubRoleEnum.LEADER) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Leader cannot leave the club. Please transfer your role first.");
        }

        if (leaveRequestRepo.existsByMembershipAndStatus(membership, LeaveRequestStatusEnum.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "You already have a pending leave request.");
        }

        ClubLeaveRequestEntity req = ClubLeaveRequestEntity.builder()
                .membership(membership)
                .status(LeaveRequestStatusEnum.PENDING)
                .reason(reason)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        leaveRequestRepo.save(req);

        // ✔ Notify only the Leader of the club
        User leader = membership.getClub().getLeader();
        if (leader != null) {
            emailService.sendLeaveRequestSubmittedToLeader(
                    leader.getEmail(),
                    leader.getFullName(),
                    membership.getUser().getFullName(),
                    membership.getClub().getName(),
                    (reason == null || reason.isBlank()) ? "No reason provided" : reason
            );
        }

        return "Leave request submitted successfully. Please wait for Leader approval.";
    }


    @Override
    @Transactional
    public String reviewLeaveRequest(Long requestId, Long approverId, String action) {

        ClubLeaveRequestEntity req = leaveRequestRepo.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Leave request not found."));

        Membership membership = req.getMembership();
        Club club = membership.getClub();

        Membership approver = membershipRepo
                .findByUser_UserIdAndClub_ClubId(approverId, club.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));

        if (approver.getClubRole() != ClubRoleEnum.LEADER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the Club Leader can review leave requests.");
        }

        LeaveRequestStatusEnum newStatus;
        try {
            newStatus = LeaveRequestStatusEnum.valueOf(action.toUpperCase());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid action. Must be APPROVED or REJECTED.");
        }

        req.setStatus(newStatus);
        req.setProcessedAt(java.time.LocalDateTime.now());
        leaveRequestRepo.save(req);

        User member = membership.getUser();

        // ***********************************************
        // ✔ If APPROVED → set member inactive + email
        // ***********************************************
        if (newStatus == LeaveRequestStatusEnum.APPROVED) {
            membership.setState(MembershipStateEnum.INACTIVE);
            membership.setEndDate(LocalDate.now());
            membershipRepo.save(membership);

            clubService.updateMemberCount(club.getClubId());

            emailService.sendLeaveRequestApprovedToMember(
                    member.getEmail(),
                    member.getFullName(),
                    club.getName()
            );
        }
        else {
            // ***********************************************
            // ✔ REJECTED
            // ***********************************************
            emailService.sendLeaveRequestRejectedToMember(
                    member.getEmail(),
                    member.getFullName(),
                    club.getName(),
                    approver.getUser().getFullName()
            );
        }

        return "Leave request " + newStatus.name().toLowerCase() + " successfully.";
    }

    @Override
    public Map<String, Object> getMemberOverview(Long userId) {
        Map<String, Object> result = new HashMap<>();

        long clubCount = membershipRepo.countByUser_UserIdAndState(
                userId, MembershipStateEnum.ACTIVE);

        long eventCount = eventRegistrationRepo.countByUser_UserIdAndStatus(
                userId, RegistrationStatusEnum.CONFIRMED);

        result.put("clubsJoined", clubCount);
        result.put("eventsJoined", eventCount);
        return result;
    }

    @Override
    public List<ClubLeaveRequestResponse> getLeaveRequestsByClub(Long clubId, Long leaderId) {
        // ✅ Check permission
        Membership leaderMembership = membershipRepo
                .findByUser_UserIdAndClub_ClubId(leaderId, clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));

        if (leaderMembership.getClubRole() != ClubRoleEnum.LEADER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the Club Leader can view leave requests.");
        }

        // ✅ Fetch all leave requests
        var requests = leaveRequestRepo.findByMembership_Club_ClubIdOrderByCreatedAtDesc(clubId);

        // ✅ Map to DTO
        return requests.stream()
                .map(req -> ClubLeaveRequestResponse.builder()
                        .requestId(req.getId())
                        .membershipId(req.getMembership().getMembershipId())
                        .memberName(req.getMembership().getUser().getFullName())
                        .memberEmail(req.getMembership().getUser().getEmail())
                        .memberRole(req.getMembership().getClubRole().name())
                        .reason(req.getReason())
                        .status(req.getStatus())
                        .createdAt(req.getCreatedAt())
                        .processedAt(req.getProcessedAt())
                        .build())
                .toList();
    }

    @Override
    public List<ClubLeaveRequestResponse> getLeaveRequestsByClubAndStatus(Long clubId, Long leaderId, LeaveRequestStatusEnum status) {
        // ✅ Kiểm tra quyền xem
        Membership leaderMembership = membershipRepo
                .findByUser_UserIdAndClub_ClubId(leaderId, clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));

        if (leaderMembership.getClubRole() != ClubRoleEnum.LEADER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the Club Leader can view leave requests.");
        }

        // ✅ Lấy danh sách theo status
        var requests = leaveRequestRepo.findByMembership_Club_ClubIdAndStatusOrderByCreatedAtDesc(clubId, status);

        return requests.stream()
                .map(req -> ClubLeaveRequestResponse.builder()
                        .requestId(req.getId())
                        .membershipId(req.getMembership().getMembershipId())
                        .memberName(req.getMembership().getUser().getFullName())
                        .memberEmail(req.getMembership().getUser().getEmail())
                        .memberRole(req.getMembership().getClubRole().name())
                        .reason(req.getReason())
                        .status(req.getStatus())
                        .createdAt(req.getCreatedAt())
                        .processedAt(req.getProcessedAt())
                        .build())
                .toList();
    }


}
