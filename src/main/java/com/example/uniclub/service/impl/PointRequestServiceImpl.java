package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.PointRequestCreateRequest;
import com.example.uniclub.dto.response.PointRequestResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.PointRequestStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.PointRequestService;
import com.example.uniclub.service.WalletService;
import lombok.RequiredArgsConstructor;
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

        return PointRequestResponse.builder()
                .id(request.getId())
                .clubName(club.getName())
                .requestedPoints(req.getRequestedPoints())
                .reason(req.getReason())
                .status(PointRequestStatusEnum.PENDING)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PointRequestResponse> getPendingRequests() {
        return pointRequestRepo.findByStatus(PointRequestStatusEnum.PENDING)
                .stream()
                .map(req -> PointRequestResponse.builder()
                        .id(req.getId())
                        .clubName(req.getClub().getName())
                        .requestedPoints(req.getRequestedPoints())
                        .reason(req.getReason())
                        .status(req.getStatus())
                        .build())
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

        if (approve) {
            Wallet uniWallet = walletService.getUniversityWallet();
            Wallet clubWallet = walletService.getOrCreateClubWallet(req.getClub());
            walletService.transferPoints(uniWallet, clubWallet, req.getRequestedPoints(),
                    "Approved point request from " + req.getClub().getName());
        }

        return approve ? "✅ Request approved & points transferred." : "❌ Request rejected.";
    }
}
