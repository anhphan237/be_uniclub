package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private final MembershipRepository membershipRepo;

    @Override
    public List<MembershipResponse> getMyMemberships(Long userId) {
        List<Membership> memberships = membershipRepo.findByUser_UserId(userId);

        return memberships.stream().map(m -> MembershipResponse.builder()
                .membershipId(m.getMembershipId())
                .userId(m.getUser().getUserId())
                .clubId(m.getClub().getClubId())
                .level(m.getLevel())          // ✅ Đã bỏ .name() vì level là String
                .state(m.getState())          // ✅ Đã bỏ .name() vì state là String
                .staff(m.isStaff())
                .joinedDate(m.getJoinedDate())
                .fullName(m.getUser().getFullName())
                .studentCode(m.getUser().getStudentCode())
                .build()
        ).toList();
    }

    @Override
    public boolean isMemberOfClub(Long userId, Long clubId) {
        return membershipRepo.existsByUser_UserIdAndClub_ClubId(userId, clubId);
    }
}
