package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.MemberApplicationCreateRequest;
import com.example.uniclub.dto.request.MemberApplicationStatusUpdateRequest;
import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.MemberApplication;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.MemberApplyStatusEnum;
import com.example.uniclub.mapper.MemberApplicationMapper;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MemberApplicationRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.MemberApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberApplicationServiceImpl implements MemberApplicationService {

    private final MemberApplicationRepository repo;
    private final ClubRepository clubRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional
    public MemberApplicationResponse createByEmail(String email, MemberApplicationCreateRequest req) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Club club = clubRepo.findById(req.clubId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found"));

        if (repo.existsByUser_UserIdAndClub_ClubIdAndActiveFlagTrue(user.getUserId(), club.getClubId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already have an active application for this club");
        }

        MemberApplication app = MemberApplication.builder()
                .user(user)
                .club(club)
                .reason(req.reason())
                .status(MemberApplyStatusEnum.PENDING)
                .activeFlag(true)
                .submittedAt(LocalDateTime.now())
                .build();

        repo.save(app);
        return MemberApplicationMapper.toResponse(app);
    }

    @Override
    @Transactional
    public MemberApplicationResponse updateStatusByEmail(String email, Long applicationId, MemberApplicationStatusUpdateRequest req) {
        User reviewer = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reviewer not found"));

        MemberApplication app = repo.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        app.setStatus(req.getStatus());
        app.setReviewedBy(reviewer);
        app.setReason(req.getReason());
        app.setUpdatedAt(LocalDateTime.now());

        repo.save(app);
        return MemberApplicationMapper.toResponse(app);
    }
}
