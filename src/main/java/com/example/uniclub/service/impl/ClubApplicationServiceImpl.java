package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.dto.response.ClubApplicationResponse.SimpleUser;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.mapper.ClubApplicationMapper;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ClubApplicationService;
import com.example.uniclub.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubApplicationServiceImpl implements ClubApplicationService {

    private final ClubApplicationRepository appRepo;
    private final UserRepository userRepo;
    private final ClubService clubService;
    private final ClubApplicationMapper mapper;
    // ============================================================
    // 🟢 1. Submit online application
    // ============================================================
    @Override
    public ClubApplicationResponse createOnline(Long proposerId, ClubApplicationCreateRequest req) {
        User proposer = userRepo.findById(proposerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (appRepo.findByClubName(req.clubName()).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "Club name already exists");

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

    // ============================================================
    // 🟩 2. Create offline application (already approved)
    // ============================================================
    @Override
    public ClubApplicationResponse createOffline(Long staffId, ClubApplicationOfflineRequest req) {
        User staff = userRepo.findById(staffId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff not found"));

        if (appRepo.findByClubName(req.clubName()).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "Club name already exists");

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

    // ============================================================
    // 🟦 3. Get list of pending applications
    // ============================================================
    @Override
    public List<ClubApplicationResponse> getPending() {
        return appRepo.findByStatus(ClubApplicationStatusEnum.PENDING)
                .stream().map(this::toResp).toList();
    }

    // ============================================================
    // 🟠 4. Approve or reject application
    // ============================================================
    @Override
    public ClubApplicationResponse decide(Long id, Long staffId, ClubApplicationDecisionRequest req) {
        ClubApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        User staff = userRepo.findById(staffId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff not found"));

        if (app.getStatus() != ClubApplicationStatusEnum.PENDING)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Application has already been reviewed");

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

    // ============================================================
    // 🟣 5. Get applications submitted by a specific user
    // ============================================================
    @Override
    public List<ClubApplicationResponse> getByUser(Long userId) {
        return appRepo.findByProposer_UserId(userId)
                .stream().map(this::toResp).toList();
    }

    // ============================================================
    // 🔵 6. Get details of a specific application
    // ============================================================
    @Override
    public ClubApplicationResponse getById(Long userId, String roleName, Long id) {
        ClubApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        // If role is STUDENT → only allow viewing own applications
        if (roleName.equals("STUDENT") && (app.getProposer() == null ||
                !Objects.equals(app.getProposer().getUserId(), userId))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not authorized to view this application");
        }

        return toResp(app);
    }

    // ============================================================
    // 🟤 7. Filter applications by status / category
    // ============================================================
    @Override
    public List<ClubApplicationResponse> filter(String status, String clubType) {
        List<ClubApplication> apps = appRepo.findAll();

        return apps.stream()
                .filter(a -> status == null || a.getStatus().name().equalsIgnoreCase(status))
                .filter(a -> clubType == null || a.getCategory().equalsIgnoreCase(clubType))
                .map(this::toResp)
                .collect(Collectors.toList());
    }

    // ============================================================
    // ⚪ 8. Update internal note
    // ============================================================
    @Override
    public ClubApplicationResponse updateNote(Long id, Long staffId, String note) {
        ClubApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        app.setInternalNote(note);
        app.setReviewedAt(LocalDateTime.now());
        appRepo.save(app);
        return toResp(app);
    }

    // ============================================================
    // 🟠 9. Delete application (Admin only)
    // ============================================================
    @Override
    public void delete(Long id) {
        if (!appRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Application not found");
        appRepo.deleteById(id);
    }

    // ============================================================
    // 🟢 10. Upload supporting document
    // ============================================================
    @Override
    public String uploadFile(Long id, Long userId, MultipartFile file) {
        ClubApplication app = appRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        try {
            // (Mock implementation — replace with AWS S3, Firebase, or local storage)
            String url = "https://storage.uniclub.vn/applications/" + file.getOriginalFilename();
            app.setAttachmentUrl(url);
            appRepo.save(app);
            return url;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed");
        }
    }

    // ============================================================
    // 🟣 11. Get application statistics by status
    // ============================================================
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", appRepo.count());
        stats.put("pending", appRepo.countByStatus(ClubApplicationStatusEnum.PENDING));
        stats.put("approved", appRepo.countByStatus(ClubApplicationStatusEnum.APPROVED));
        stats.put("rejected", appRepo.countByStatus(ClubApplicationStatusEnum.REJECTED));
        return stats;
    }

    // ============================================================
    // 🔵 12. Search by keyword (club name or proposer)
    // ============================================================
    @Override
    public List<ClubApplicationResponse> search(String keyword) {
        return appRepo.searchByKeyword(keyword)
                .stream().map(this::toResp).toList();
    }

    // ============================================================
    // 🧩 Helper: Convert entity → DTO
    // ============================================================
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
                .attachmentUrl(app.getAttachmentUrl())
                .internalNote(app.getInternalNote())
                .build();
    }
    @Override
    public List<ClubApplicationResponse> getAllApplications() {
        return appRepo.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }


}
