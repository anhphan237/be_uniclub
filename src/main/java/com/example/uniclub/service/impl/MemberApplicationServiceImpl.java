package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.MemberApplicationCreateRequest;
import com.example.uniclub.dto.request.MemberApplicationStatusUpdateRequest;
import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.MemberApplication;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.MemberApplyStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.mapper.MemberApplicationMapper;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MemberApplicationRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.MemberApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberApplicationServiceImpl implements MemberApplicationService {

    private final MemberApplicationRepository repo;
    private final ClubRepository clubRepo;
    private final UserRepository userRepo;

    // ✅ Sinh viên nộp đơn
    @Override
    @Transactional
    public MemberApplicationResponse createByEmail(String email, MemberApplicationCreateRequest req) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Club club = clubRepo.findById(req.clubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        if (repo.existsByUser_UserIdAndClub_ClubIdAndActiveFlagTrue(user.getUserId(), club.getClubId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You already have an active application for this club");
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

    // ✅ Duyệt / từ chối đơn
    @Override
    @Transactional
    public MemberApplicationResponse updateStatusByEmail(String email, Long applicationId, MemberApplicationStatusUpdateRequest req) {
        User reviewer = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reviewer not found"));

        MemberApplication app = repo.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        app.setStatus(req.getStatus());
        app.setReviewedBy(reviewer);
        app.setReason(req.getReason());
        app.setUpdatedAt(LocalDateTime.now());

        repo.save(app);
        return MemberApplicationMapper.toResponse(app);
    }

    // ✅ Admin / Staff xem tất cả
    @Override
    @Transactional(readOnly = true)
    public List<MemberApplicationResponse> findAll() {
        return repo.findAll()
                .stream()
                .map(MemberApplicationMapper::toResponse)
                .toList();
    }

    // ✅ Xem theo email (student → của mình, leader/staff → tất cả)
    @Override
    @Transactional(readOnly = true)
    public List<MemberApplicationResponse> findApplicationsByEmail(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String role = user.getRole().getRoleName();

        if (role.equals("STUDENT") || role.equals("MEMBER")) {
            return repo.findByUser(user)
                    .stream()
                    .map(MemberApplicationMapper::toResponse)
                    .toList();
        }

        return repo.findAll()
                .stream()
                .map(MemberApplicationMapper::toResponse)
                .toList();
    }

    // ✅ Xem danh sách đơn ứng tuyển theo CLB (Leader, Staff, Admin)
    @Override
    @Transactional(readOnly = true)
    public List<MemberApplicationResponse> getByClubId(CustomUserDetails principal, Long clubId) {
        var user = principal.getUser();

        var club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        String role = user.getRole().getRoleName();

        // ✅ Nếu là CLUB_LEADER → chỉ xem CLB của chính mình
        if (role.equals("CLUB_LEADER")) {
            var myClub = clubRepo.findByLeader_UserId(user.getUserId())
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a leader of any club."));

            if (!myClub.getClubId().equals(clubId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You can only view applications of your own club.");
            }
        }

        // ✅ Admin / Staff có thể xem mọi CLB
        return repo.findAllByClub_ClubId(clubId)
                .stream()
                .map(MemberApplicationMapper::toResponse)
                .toList();
    }
}
