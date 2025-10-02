package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.MembershipCreateRequest;
import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.User;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private final MembershipRepository membershipRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;

    private MembershipResponse toResp(Membership m){
        return MembershipResponse.builder()
                .id(m.getMembershipId())
                .userId(m.getUser().getUserId())
                .clubId(m.getClub().getClubId())
                .level(m.getLevel())
                .state(m.getState())
                .build();
    }

    @Override
    public MembershipResponse create(MembershipCreateRequest req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        Club club = clubRepo.findById(req.clubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club không tồn tại"));

        if (membershipRepo.existsByUserAndClub(user, club))
            throw new ApiException(HttpStatus.CONFLICT, "Đã là thành viên CLB");

        Membership m = Membership.builder()
                .user(user).club(club)
                .level(req.level())
                .state("active")
                .build();

        return toResp(membershipRepo.save(m));
    }

    @Override
    public void delete(Long id) {
        if (!membershipRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Membership không tồn tại");
        membershipRepo.deleteById(id);
    }
}
