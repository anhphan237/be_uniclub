package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.MemberCreateRequest;
import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.UserStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;

    // ✅ Tạo mới thành viên CLB
    @Override
    public MembershipResponse create(MemberCreateRequest req) {
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Club club = clubRepository.findById(req.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        if (membershipRepository.existsByUserAndClub(user, club)) {
            throw new ApiException(HttpStatus.CONFLICT, "Member already exists in this club");
        }

        Membership membership = Membership.builder()
                .user(user)
                .club(club)
                .level(req.getLevel())
                .state(UserStatusEnum.ACTIVE.name())
                .staff(false)
                .build();

        membership = membershipRepository.save(membership);

        return MembershipResponse.builder()
                .membershipId(membership.getMembershipId())
                .userId(user.getUserId())
                .clubId(club.getClubId())
                .level(membership.getLevel())
                .state(membership.getState())
                .staff(membership.isStaff())
                .build();
    }

    // ✅ Xoá thành viên khỏi CLB
    @Override
    public void delete(Long id) {
        Membership membership = membershipRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));
        membershipRepository.delete(membership);
    }

    // ✅ Leader cập nhật staff (bổ nhiệm / gỡ staff)
    @Override
    public String updateStaffStatus(CustomUserDetails principal, Long membershipId, boolean value) {
        var leader = principal.getUser();

        var clubOpt = clubRepository.findByLeader_UserId(leader.getUserId());
        if (clubOpt.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a leader of any club.");
        }

        var club = clubOpt.get();

        var membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found."));

        if (!membership.getClub().getClubId().equals(club.getClubId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This membership does not belong to your club.");
        }

        membership.setStaff(value);
        membershipRepository.save(membership);

        return value
                ? "Member has been promoted to staff."
                : "Member is no longer a staff.";
    }

    // ✅ Leader xem danh sách member theo clubId (chỉ CLB mình quản lý)
    @Override
    public List<MembershipResponse> getMembersByClub(CustomUserDetails principal, Long clubId) {
        var leader = principal.getUser();

        var myClub = clubRepository.findByLeader_UserId(leader.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a leader of any club."));

        if (!myClub.getClubId().equals(clubId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only view members of your own club.");
        }

        return membershipRepository.findAllByClub_ClubId(clubId).stream()
                .map(m -> MembershipResponse.builder()
                        .membershipId(m.getMembershipId())
                        .userId(m.getUser().getUserId())
                        .clubId(m.getClub().getClubId())
                        .level(m.getLevel())
                        .state(m.getState())
                        .staff(m.isStaff())
                        .build())
                .toList();
    }

    // ✅ Leader xem danh sách member CLB của mình (khỏi truyền clubId)
    @Override
    public List<MembershipResponse> getMembersOfMyClub(CustomUserDetails principal) {
        var leader = principal.getUser();

        var myClub = clubRepository.findByLeader_UserId(leader.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a leader of any club."));

        return membershipRepository.findAllByClub_ClubId(myClub.getClubId()).stream()
                .map(m -> MembershipResponse.builder()
                        .membershipId(m.getMembershipId())
                        .userId(m.getUser().getUserId())
                        .clubId(m.getClub().getClubId())
                        .level(m.getLevel())
                        .state(m.getState())
                        .staff(m.isStaff())
                        .build())
                .toList();
    }
}
