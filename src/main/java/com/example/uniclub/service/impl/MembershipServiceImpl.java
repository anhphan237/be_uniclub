package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.ClubService;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.EventLogService;
import com.example.uniclub.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.enums.EventRegistrationStatusEnum;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {
    private final ClubLeaveRequestEntityRepository leaveRequestRepo;
    private final EventRegistrationRepository eventRegistrationRepo;

    private final EventLogService eventLogService;
    private final MembershipRepository membershipRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final ClubService clubService;
    private final EmailService emailService;
    private final MajorPolicyRepository majorPolicyRepository;

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
            log.warn("User {} has no major assigned, skipping policy validation", user.getEmail());
            return;
        }

        // 🔍 Retrieve first active policy for this major (if any)
        MajorPolicy policy = major.getPolicies()
                .stream()
                .filter(MajorPolicy::isActive)
                .findFirst()
                .orElse(null);

        if (policy == null) {
            log.info("Major {} has no active policy, skipping limit check", major.getName());
            return;
        }

        // 📊 Count ACTIVE club memberships
        int joinedCount = membershipRepo.countByUser_UserIdAndState(user.getUserId(), MembershipStateEnum.ACTIVE);

        if (joinedCount >= policy.getMaxClubJoin()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "You have reached the maximum number of clubs allowed (" + policy.getMaxClubJoin() +
                            ") for your major " + major.getName());
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

        // ✅ Check if membership exists
        Optional<Membership> existingOpt = membershipRepo.findByUser_UserIdAndClub_ClubId(userId, clubId);

        if (existingOpt.isPresent()) {
            Membership existing = existingOpt.get();
            MembershipStateEnum state = existing.getState();

            switch (state) {
                // ❌ Nếu đang hoạt động hoặc đang chờ duyệt — không được apply lại
                case ACTIVE, APPROVED, PENDING -> {
                    throw new ApiException(HttpStatus.CONFLICT, "You are already a member or have a pending request.");
                }

                // ✅ Nếu bị kick / inactive / rejected → cho phép reapply
                case KICKED, INACTIVE, REJECTED -> {
                    existing.setState(MembershipStateEnum.PENDING);
                    existing.setJoinedDate(LocalDate.now());
                    existing.setEndDate(null);
                    existing.setClubRole(ClubRoleEnum.MEMBER);
                    membershipRepo.save(existing);
                    return toResp(existing);
                }

                default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unexpected membership state: " + state);
            }
        }

        // ⚖️ Check major policy limit
        validateMajorPolicy(user);

        // ✅ Create new membership
        Membership newMembership = Membership.builder()
                .user(user)
                .club(club)
                .clubRole(ClubRoleEnum.MEMBER)
                .state(MembershipStateEnum.PENDING)
                .joinedDate(LocalDate.now())
                .build();

        membershipRepo.save(newMembership);
        eventLogService.logAction(
                userId,
                user.getFullName(),
                null,
                null,
                UserActionEnum.JOIN_CLUB,
                "User joined club " + club.getName()
        );
        return toResp(newMembership);
    }


    // ========================== 🔹 2. Membership Approval Management ==========================
    @Override
    @Transactional
    public MembershipResponse approveMember(Long membershipId, Long approverId) {
        Membership m = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        validateMajorPolicy(m.getUser());

        m.setState(MembershipStateEnum.ACTIVE);
        membershipRepo.save(m);
        clubService.updateMemberCount(m.getClub().getClubId());
        return toResp(m);
    }

    @Override
    @Transactional
    public MembershipResponse rejectMember(Long membershipId, Long approverId, String reason) {
        Membership m = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));
        m.setState(MembershipStateEnum.REJECTED);
        membershipRepo.save(m);
        clubService.updateMemberCount(m.getClub().getClubId());
        return toResp(m);
    }

    @Override
    @Transactional
    public void removeMember(Long membershipId, Long approverId) {
        Membership m = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));
        m.setState(MembershipStateEnum.INACTIVE);
        membershipRepo.save(m);
        clubService.updateMemberCount(m.getClub().getClubId());
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
        Membership actorMembership = membershipRepo
                .findByUser_UserIdAndClub_ClubId(principal.getUser().getUserId(), club.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club"));

        if (!(actorMembership.getClubRole() == ClubRoleEnum.LEADER
                || actorMembership.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the Leader or Vice Leader can remove members");
        }

        if (Objects.equals(membership.getUser().getUserId(), principal.getUser().getUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot remove yourself");
        }

        if (membership.getClubRole() == ClubRoleEnum.LEADER
                || membership.getClubRole() == ClubRoleEnum.VICE_LEADER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot remove the Club Leader or Vice Leader");
        }

        membership.setState(MembershipStateEnum.KICKED);
        membershipRepo.save(membership);

        String kickerName = principal.getUser().getFullName();
        String receiverName = membership.getUser().getFullName();
        String clubName = club.getName();

        String subject = "You have been removed from " + clubName + " by " + kickerName;

        String body = """
        <p>Dear %s,</p>
        <p>You have been <b>removed</b> from the club <b>%s</b> by <b>%s</b>.</p>
        <p>If you believe this was a mistake, please contact your Club Leader or University Staff.</p>
        <br><p>Best regards,<br>%s<br>%s Club<br>UniClub Platform</p>
        """.formatted(receiverName, clubName, kickerName, kickerName, clubName);

        try {
            emailService.sendEmail(membership.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            log.warn("⚠Failed to send email to {}", membership.getUser().getEmail());
        }
        eventLogService.logAction(
                membership.getUser().getUserId(),
                membership.getUser().getFullName(),
                null,
                null,
                UserActionEnum.LEAVE_CLUB,
                "User left club " + membership.getClub().getName()
        );
        return "Member " + receiverName + " has been removed from " + clubName + " by " + kickerName;
    }

    @Override
    @Transactional
    public String requestLeave(Long userId, Long clubId, String reason) {
        Membership membership = membershipRepo.findByUser_UserIdAndClub_ClubIdAndState(
                userId, clubId, MembershipStateEnum.ACTIVE
        ).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "You are not an active member of this club."));

        if (membership.getClubRole() == ClubRoleEnum.LEADER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Leader cannot leave the club. Please transfer your role first.");
        }

        if (leaveRequestRepo.existsByMembershipAndStatus(membership, LeaveRequestStatusEnum.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have a pending leave request.");
        }

        var req = ClubLeaveRequestEntity.builder()
                .membership(membership)
                .status(LeaveRequestStatusEnum.PENDING)
                .reason(reason)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        leaveRequestRepo.save(req);

        // 📧 Send email to Club Leader
        User leader = membership.getClub().getLeader();
        if (leader != null) {
            String subject = "[UniClub] Member submitted a club leave request";
            String body = """
                <p>Dear %s,</p>
                <p>Member <b>%s</b> has submitted a request to leave the club <b>%s</b>.</p>
                <p><b>Reason:</b> %s</p>
                <p>Please log in to the UniClub platform to review this request.</p>
                <br>
                <p>Regards,<br><b>UniClub System</b></p>
                """.formatted(
                    leader.getFullName(),
                    membership.getUser().getFullName(),
                    membership.getClub().getName(),
                    (reason == null || reason.isBlank()) ? "No reason provided." : reason
            );
            emailService.sendEmail(leader.getEmail(), subject, body);
        }

        return "Leave request submitted successfully. Please wait for Leader approval.";
    }

    @Override
    @Transactional
    public String reviewLeaveRequest(Long requestId, Long approverId, String action) {
        var req = leaveRequestRepo.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Leave request not found."));

        Membership membership = req.getMembership();
        Club club = membership.getClub();

        // ✅ Verify approver is club leader
        Membership approver = membershipRepo.findByUser_UserIdAndClub_ClubId(approverId, club.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));
        if (approver.getClubRole() != ClubRoleEnum.LEADER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the Club Leader can review leave requests.");
        }

        // ✅ Convert action safely (Swagger Enum → string → Enum)
        LeaveRequestStatusEnum newStatus;
        try {
            newStatus = LeaveRequestStatusEnum.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid action. Must be APPROVED or REJECTED.");
        }

        req.setStatus(newStatus);
        req.setProcessedAt(java.time.LocalDateTime.now());
        leaveRequestRepo.save(req);

        if (newStatus == LeaveRequestStatusEnum.APPROVED) {
            membership.setState(MembershipStateEnum.INACTIVE);
            membership.setEndDate(java.time.LocalDate.now());
            membershipRepo.save(membership);
            clubService.updateMemberCount(club.getClubId());
        }

        // ✅ Email notify member
        User member = membership.getUser();
        String subject;
        String body;

        if (newStatus == LeaveRequestStatusEnum.APPROVED) {
            subject = "[UniClub] Your club leave request has been approved";
            body = """
            <p>Dear %s,</p>
            <p>Your request to leave the club <b>%s</b> has been <b>approved</b>.</p>
            <p>You are no longer a member of this club.</p>
            <br>
            <p>Thank you for being part of UniClub,<br><b>UniClub Team</b></p>
            """.formatted(member.getFullName(), club.getName());
        } else {
            subject = "[UniClub] Your club leave request has been rejected";
            body = """
            <p>Dear %s,</p>
            <p>Your request to leave the club <b>%s</b> has been <b>rejected</b> by Leader <b>%s</b>.</p>
            <p>Please contact your Club Leader directly if you need further clarification.</p>
            <br>
            <p>Regards,<br><b>UniClub Team</b></p>
            """.formatted(member.getFullName(), club.getName(), approver.getUser().getFullName());
        }

        try {
            emailService.sendEmail(member.getEmail(), subject, body);
        } catch (Exception e) {
            log.warn("Failed to send leave request email to {}", member.getEmail(), e);
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



}
