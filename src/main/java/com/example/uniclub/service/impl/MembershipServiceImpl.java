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
import com.example.uniclub.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private final MembershipRepository membershipRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;

    private MembershipResponse toResp(Membership m) {
        return MembershipResponse.builder()
                .membershipId(m.getMembershipId())
                .userId(m.getUser().getUserId())
                .clubId(m.getClub().getClubId())
                .clubRole(m.getClubRole())
                .state(m.getState())
                .staff(m.isStaff())
                .joinedDate(m.getJoinedDate())
                .endDate(m.getEndDate())
                .fullName(m.getUser().getFullName())
                .studentCode(m.getUser().getStudentCode())
                .clubName(m.getClub().getName())
                .build();
    }

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
    public MembershipResponse joinClub(Long userId, Long clubId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        if (membershipRepo.existsByUser_UserIdAndClub_ClubId(userId, clubId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Already applied or member of this club");
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

    @Override
    public List<MembershipResponse> getPendingMembers(Long clubId) {
        return membershipRepo.findByClub_ClubIdAndState(clubId, MembershipStateEnum.PENDING)
                .stream()
                .map(this::toResp)
                .toList();
    }

    @Override
    public MembershipResponse approveMember(Long membershipId, Long approverId) {
        Membership m = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        m.setState(MembershipStateEnum.APPROVED);
        membershipRepo.save(m);
        return toResp(m);
    }

    @Override
    public MembershipResponse rejectMember(Long membershipId, Long approverId, String reason) {
        Membership m = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        m.setState(MembershipStateEnum.REJECTED);
        membershipRepo.save(m);
        return toResp(m);
    }

    @Override
    public void removeMember(Long membershipId, Long approverId) {
        Membership m = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));
        m.setState(MembershipStateEnum.INACTIVE);
        membershipRepo.save(m);
    }

    // ✅ Thay đổi vai trò trong CLB (ràng buộc số lượng)
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
    @Override
    public List<MembershipResponse> getMembersByClub(Long clubId) {
        return membershipRepo.findByClub_ClubIdAndState(clubId, MembershipStateEnum.APPROVED)
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


}
