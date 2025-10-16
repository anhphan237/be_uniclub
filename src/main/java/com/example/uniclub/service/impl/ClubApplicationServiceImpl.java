package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.dto.response.ClubApplicationResponse.SimpleUser;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ClubApplicationService;
import com.example.uniclub.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubApplicationServiceImpl implements ClubApplicationService {

    private final ClubApplicationRepository appRepo;
    private final UserRepository userRepo;
    private final ClubService clubService;

    @Override
    public ClubApplicationResponse createOnline(Long proposerId, ClubApplicationCreateRequest req) {
        User proposer = userRepo.findById(proposerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (appRepo.findByClubName(req.clubName()).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "Tên CLB đã tồn tại");

        ClubApplication app = ClubApplication.builder()
                .proposer(proposer)
                .clubName(req.clubName())
                .description(req.description())
                .category(req.category())
                .proposerReason(req.proposerReason())
                .sourceType(ApplicationSourceTypeEnum.ONLINE)
                .status(ClubApplicationStatusEnum.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        appRepo.save(app);
        return toResp(app);
    }

    @Override
    public ClubApplicationResponse createOffline(Long staffId, ClubApplicationOfflineRequest req) {
        User staff = userRepo.findById(staffId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff not found"));

        if (appRepo.findByClubName(req.clubName()).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "Tên CLB đã tồn tại");

        ClubApplication app = ClubApplication.builder()
                .clubName(req.clubName())
                .description(req.description())
                .category(req.category())
                .sourceType(ApplicationSourceTypeEnum.OFFLINE)
                .status(ClubApplicationStatusEnum.APPROVED)
                .reviewedBy(staff)
                .reviewedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        appRepo.save(app);
        clubService.createFromOfflineApplication(app, req);
        return toResp(app);
    }

    @Override
    public List<ClubApplicationResponse> getPending() {
        return appRepo.findByStatus(ClubApplicationStatusEnum.PENDING)
                .stream().map(this::toResp).toList();
    }

    @Override
    public ClubApplicationResponse decide(Long id, Long staffId, ClubApplicationDecisionRequest req) {
        ClubApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        User staff = userRepo.findById(staffId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff not found"));

        if (app.getStatus() != ClubApplicationStatusEnum.PENDING)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Application already reviewed");

        app.setReviewedBy(staff);
        app.setReviewedAt(LocalDateTime.now());

        if (req.approve()) {
            app.setStatus(ClubApplicationStatusEnum.APPROVED);
            appRepo.save(app);
            clubService.createFromOnlineApplication(app);
        } else {
            app.setStatus(ClubApplicationStatusEnum.REJECTED);
            app.setRejectReason(req.rejectReason());
            appRepo.save(app);
        }

        return toResp(app);
    }

    private ClubApplicationResponse toResp(ClubApplication app) {
        SimpleUser proposer = null;
        if (app.getProposer() != null) {
            proposer = SimpleUser.builder()
                    .fullName(app.getProposer().getFullName())
                    .email(app.getProposer().getEmail())
                    .build();
        }

        SimpleUser reviewer = null;
        if (app.getReviewedBy() != null) {
            reviewer = SimpleUser.builder()
                    .fullName(app.getReviewedBy().getFullName())
                    .email(app.getReviewedBy().getEmail())
                    .build();
        }

        return ClubApplicationResponse.builder()
                .applicationId(app.getApplicationId())
                .clubName(app.getClubName())
                .description(app.getDescription())
                .category(app.getCategory())
                .submittedBy(proposer)
                .reviewedBy(reviewer)
                .status(app.getStatus())
                .sourceType(app.getSourceType())
                .rejectReason(app.getRejectReason())
                .submittedAt(app.getCreatedAt())
                .reviewedAt(app.getReviewedAt())
                .build();
    }
}
