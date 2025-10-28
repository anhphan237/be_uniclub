package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.ClubService;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private final MembershipRepository membershipRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final ClubService clubService;
    private final EmailService emailService;

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
                .major(u.getMajorName())
                .build();
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

        Optional<Membership> existing = membershipRepo.findByUser_UserIdAndClub_ClubId(userId, clubId);

        if (existing.isPresent()) {
            Membership old = existing.get();

            if (old.getState() == MembershipStateEnum.KICKED ||
                    old.getState() == MembershipStateEnum.INACTIVE ||
                    old.getState() == MembershipStateEnum.REJECTED) {

                old.setState(MembershipStateEnum.PENDING);
                old.setJoinedDate(LocalDate.now());
                membershipRepo.save(old);

                // 📧 Gửi email thông báo cho Leader/Phó CLB
                List<Membership> leaders = membershipRepo.findByClub_ClubId(clubId)
                        .stream()
                        .filter(m -> m.getClubRole() == ClubRoleEnum.LEADER || m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                        .toList();

                String subject = "Member re-applied after being kicked - " + club.getName();
                String body = """
                    <p>Dear Leader/Vice Leader,</p>
                    <p>The member <b>%s</b> (%s) who was previously <b>kicked</b> has re-applied to join your club <b>%s</b>.</p>
                    <p>Please review their new application in the UniClub system.</p>
                    <br>
                    <p>UniClub Platform</p>
                """.formatted(user.getFullName(), user.getEmail(), club.getName());

                for (Membership leader : leaders) {
                    try {
                        emailService.sendEmail(leader.getUser().getEmail(), subject, body);
                    } catch (Exception e) {
                        System.out.println("⚠️ Failed to send re-apply email to " + leader.getUser().getEmail());
                    }
                }

                return toResp(old);
            } else {
                throw new ApiException(HttpStatus.CONFLICT, "Already applied or member of this club");
            }
        }

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
                if (membershipRepo.existsByClub_ClubIdAndClubRole(clubId, ClubRoleEnum.LEADER)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Mỗi CLB chỉ có 1 Chủ nhiệm");
                }
            }
            case VICE_LEADER -> {
                if (membershipRepo.existsByClub_ClubIdAndClubRole(clubId, ClubRoleEnum.VICE_LEADER)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Mỗi CLB chỉ có 1 Phó chủ nhiệm");
                }
            }
            case STAFF -> {
                long count = membershipRepo.countByClub_ClubIdAndClubRole(clubId, ClubRoleEnum.STAFF);
                if (count >= 5) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Mỗi CLB chỉ có tối đa 5 staff");
                }
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
        <br>
        <p>Best regards,<br>
        %s<br>
        %s Club<br>
        UniClub Platform</p>
        """.formatted(receiverName, clubName, kickerName, kickerName, clubName);

        try {
            emailService.sendEmail(membership.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            System.out.println("⚠️ Failed to send email to " + membership.getUser().getEmail());
        }

        return "👢 Member " + receiverName + " has been kicked from " + clubName + " by " + kickerName;
    }
}
