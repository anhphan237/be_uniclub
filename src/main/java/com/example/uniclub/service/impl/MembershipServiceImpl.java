package com.example.uniclub.service.impl;

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
    public List<Membership> getMyMemberships(Long userId) {
        return membershipRepo.findByUser_UserId(userId);
    }

    @Override
    public boolean isMemberOfClub(Long userId, Long clubId) {
        return membershipRepo.existsByUser_UserIdAndClub_ClubId(userId, clubId);
    }
}
