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
import com.example.uniclub.service.MemberApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

        // Kiểm tra nếu đã có đơn active
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

    // ✅ Cập nhật trạng thái đơn (chỉ Leader, Staff, Admin)
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

    // ✅ Xem tất cả (admin / staff / leader)
    @Override
    @Transactional(readOnly = true)
    public List<MemberApplicationResponse> findAll() {
        return repo.findAll()
                .stream()
                .map(MemberApplicationMapper::toResponse)
                .toList();
    }

    // ✅ Xem theo email (student / member → chỉ thấy của mình)
    @Transactional(readOnly = true)
    public List<MemberApplicationResponse> findApplicationsByEmail(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String role = user.getRole().getRoleName();

        // Nếu là student hoặc member → chỉ xem đơn của chính mình
        if (role.equals("STUDENT") || role.equals("MEMBER")) {
            return repo.findByUser(user)
                    .stream()
                    .map(MemberApplicationMapper::toResponse)
                    .toList();
        }

        // Nếu là leader / staff / admin → xem tất cả
        return repo.findAll()
                .stream()
                .map(MemberApplicationMapper::toResponse)
                .toList();
    }
    // ✅ Lấy danh sách đơn theo CLB (leader, staff, admin)

    @Transactional(readOnly = true)
    public List<MemberApplicationResponse> getByClubId(Long clubId) {
        // Kiểm tra CLB tồn tại
        var club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        return repo.findAllByClub_ClubId(clubId)
                .stream()
                .map(MemberApplicationMapper::toResponse)
                .toList();
    }

}
