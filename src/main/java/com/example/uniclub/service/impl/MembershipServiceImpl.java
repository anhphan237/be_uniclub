package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.ClubService;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private final MembershipRepository membershipRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final ClubService clubService;
    private final EmailService emailService;
    private final MajorPolicyRepository majorPolicyRepository;

    // ========================== 🔹 Helper Mapping ==========================
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

    // ========================== 🔹 Helper: Kiểm tra chính sách ngành ==========================
    private void validateMajorPolicy(User user) {
        Major major = user.getMajor();
        if (major == null) {
            log.warn("⚠️ User {} chưa có major, bỏ qua kiểm tra policy", user.getEmail());
            return;
        }

        // 🔍 Lấy policy active đầu tiên của ngành (nếu có)
        MajorPolicy policy = major.getPolicies()
                .stream()
                .filter(MajorPolicy::isActive)
                .findFirst()
                .orElse(null);

        if (policy == null) {
            log.info("ℹ️ Major {} chưa có policy active, bỏ qua giới hạn", major.getName());
            return;
        }

        // 📊 Đếm số CLB đang ACTIVE
        int joinedCount = membershipRepo.countByUser_UserIdAndState(user.getUserId(), MembershipStateEnum.ACTIVE);

        if (joinedCount >= policy.getMaxClubJoin()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Bạn đã đạt giới hạn số CLB có thể tham gia (" + policy.getMaxClubJoin() +
                            ") cho chuyên ngành " + major.getName());
        }
    }

    // ========================== 🔹 1. Membership cơ bản ==========================
    @Override
    public List<MembershipResponse> getMyMemberships(Long userId) {
        return membershipRepo.findByUser_UserId(userId)
                .stream()
                .map(this::toResp)
                .toList();
    }

    @Override
    public boolean isMemberOfClub(Long userId, Long clubId) {
        return membershipRepo.existsByUser_UserIdAndClub_ClubId(userId, clubId);
    }

    @Override
    @Transactional
    public MembershipResponse joinClub(Long userId, Long clubId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        // ✅ Kiểm tra membership cũ
        Optional<Membership> existing = membershipRepo.findByUser_UserIdAndClub_ClubId(userId, clubId);
        if (existing.isPresent()) {
            Membership old = existing.get();
            if (old.getState() == MembershipStateEnum.KICKED ||
                    old.getState() == MembershipStateEnum.INACTIVE ||
                    old.getState() == MembershipStateEnum.REJECTED) {
                old.setState(MembershipStateEnum.PENDING);
                old.setJoinedDate(LocalDate.now());
                membershipRepo.save(old);
                return toResp(old);
            } else {
                throw new ApiException(HttpStatus.CONFLICT, "Already applied or member of this club");
            }
        }

        // ⚖️ Kiểm tra giới hạn Major Policy
        Major major = user.getMajor();
        if (major == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User has no major assigned");
        }

        // 🔍 Lấy policy active đầu tiên
        MajorPolicy policy = major.getPolicies()
                .stream()
                .filter(MajorPolicy::isActive)
                .findFirst()
                .orElse(null);

        if (policy == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "No active policy found for this major (" + major.getName() + ")");
        }

        // 📊 Đếm số CLB user đang tham gia (ACTIVE + PENDING)
        int joinedCount = membershipRepo.countByUser_UserIdAndState(userId, MembershipStateEnum.ACTIVE)
                + membershipRepo.countByUser_UserIdAndState(userId, MembershipStateEnum.PENDING);

        if (joinedCount >= policy.getMaxClubJoin()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    String.format("You have reached the club limit for your major (%s). Max allowed: %d",
                            major.getName(), policy.getMaxClubJoin()));
        }

        // ✅ Cho phép apply mới
        Membership m = Membership.builder()
                .user(user)
                .club(club)
                .clubRole(ClubRoleEnum.MEMBER)
                .state(MembershipStateEnum.PENDING)
                .joinedDate(LocalDate.now())
                .build();

        membershipRepo.save(m);
        return toResp(m);
    }

    // ========================== 🔹 2. Quản lý đơn duyệt ==========================
    @Override
    @Transactional
    public MembershipResponse approveMember(Long membershipId, Long approverId) {
        Membership m = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        // ✅ Kiểm tra policy lần nữa khi approve
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

    // ========================== 🔹 3. Quản lý vai trò ==========================
    @Override
    public MembershipResponse updateClubRole(Long membershipId, ClubRoleEnum newRole, Long approverId) {
        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        Long clubId = membership.getClub().getClubId();

        switch (newRole) {
            case LEADER -> {
                if (membershipRepo.existsByClub_ClubIdAndClubRole(clubId, ClubRoleEnum.LEADER))
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Mỗi CLB chỉ có 1 Chủ nhiệm");
            }
            case VICE_LEADER -> {
                if (membershipRepo.existsByClub_ClubIdAndClubRole(clubId, ClubRoleEnum.VICE_LEADER))
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Mỗi CLB chỉ có 1 Phó chủ nhiệm");
            }
            case STAFF -> {
                long count = membershipRepo.countByClub_ClubIdAndClubRole(clubId, ClubRoleEnum.STAFF);
                if (count >= 5)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Mỗi CLB chỉ có tối đa 5 staff");
            }
            default -> {}
        }

        membership.setClubRole(newRole);
        membershipRepo.save(membership);
        return toResp(membership);
    }

    // ========================== 🔹 4. Lấy danh sách theo CLB ==========================
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
            throw new ApiException(HttpStatus.FORBIDDEN, "Only Leader or Vice Leader can kick members");
        }

        if (Objects.equals(membership.getUser().getUserId(), principal.getUser().getUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot kick yourself");
        }

        if (membership.getClubRole() == ClubRoleEnum.LEADER
                || membership.getClubRole() == ClubRoleEnum.VICE_LEADER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot kick the Club Leader or Vice Leader");
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
        <p>If you believe this was a mistake, please reach out to your Club Leader or University Staff.</p>
        <br><p>Best regards,<br>%s<br>%s Club<br>UniClub Platform</p>
        """.formatted(receiverName, clubName, kickerName, kickerName, clubName);

        try {
            emailService.sendEmail(membership.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            log.warn("⚠️ Failed to send email to {}", membership.getUser().getEmail());
        }

        return "👢 Member " + receiverName + " has been kicked from " + clubName + " by " + kickerName;
    }
}
