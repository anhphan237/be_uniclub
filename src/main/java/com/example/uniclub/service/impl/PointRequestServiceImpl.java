package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.PointRequestCreateRequest;
import com.example.uniclub.dto.response.PointRequestResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.PointRequest;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.enums.PointRequestStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.PointRequestRepository;
import com.example.uniclub.repository.WalletRepository;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.PointRequestService;
import com.example.uniclub.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointRequestServiceImpl implements PointRequestService {

    private final ClubRepository clubRepo;
    private final PointRequestRepository pointRequestRepo;
    private final WalletService walletService;
    private final WalletRepository walletRepo;
    private final PointRequestRepository pointRequestRepository;
    @Override
    @Transactional
    public PointRequestResponse createRequest(CustomUserDetails principal, PointRequestCreateRequest req) {
        Club club = clubRepo.findById(req.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        PointRequest request = PointRequest.builder()
                .club(club)
                .requestedPoints(req.getRequestedPoints())
                .reason(req.getReason())
                .status(PointRequestStatusEnum.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        pointRequestRepo.save(request);

        return mapToResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PointRequestResponse> getPendingRequests() {
        return pointRequestRepo.findByStatus(PointRequestStatusEnum.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Override
    @Transactional
    public String reviewRequest(Long requestId, boolean approve, String note) {
        PointRequest req = pointRequestRepo.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));

        if (req.getStatus() != PointRequestStatusEnum.PENDING)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request already reviewed");

        req.setStaffNote(note);
        req.setReviewedAt(LocalDateTime.now());
        req.setStatus(approve ? PointRequestStatusEnum.APPROVED : PointRequestStatusEnum.REJECTED);
        pointRequestRepo.save(req);

        // 🧩 UPDATED: Không cần ví University — dùng mint trực tiếp
        if (approve) {
            Wallet clubWallet = walletService.getOrCreateClubWallet(req.getClub());
            walletService.topupPointsFromUniversity(
                    clubWallet,
                    req.getRequestedPoints(),
                    "Approved point request from " + req.getClub().getName()
            );
        }

        return approve ? "✅ Request approved & points granted." : "❌ Request rejected.";
    }


    @Override
    @Transactional(readOnly = true)
    public Page<PointRequestResponse> list(Pageable pageable) {
        return pointRequestRepo.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PointRequestResponse get(Long id) {
        PointRequest req = pointRequestRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        return mapToResponse(req);
    }

    private PointRequestResponse mapToResponse(PointRequest pr) {
        return PointRequestResponse.builder()
                .id(pr.getId())
                .clubName(pr.getClub().getName())
                .requestedPoints(pr.getRequestedPoints())
                .reason(pr.getReason())
                .status(pr.getStatus())
                .staffNote(pr.getStaffNote())
                .createdAt(pr.getCreatedAt())
                .reviewedAt(pr.getReviewedAt())
                .build();
    }
    @Override
    public List<PointRequestResponse> getAllRequests() {
        return pointRequestRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

}