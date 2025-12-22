package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.AdminClubResponse;
import com.example.uniclub.dto.response.AdminClubStatResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.ClubActivityStatusEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.AdminClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.example.uniclub.enums.EventStatusEnum;

@Service
@RequiredArgsConstructor
public class AdminClubServiceImpl implements AdminClubService {

    private final ClubRepository clubRepo;
    private final MembershipRepository membershipRepo;
    private final EventRepository eventRepo;

    @Override
    public Page<AdminClubResponse> getAllClubs(String keyword, Pageable pageable) {
        Page<Club> clubs = (keyword == null || keyword.isBlank())
                ? clubRepo.findAll(pageable)
                : clubRepo.findByNameContainingIgnoreCase(keyword, pageable);

        return clubs.map(this::toResponse);
    }

    @Override
    public AdminClubResponse getClubDetail(Long id) {
        Club club = clubRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));
        return toResponse(club);
    }

    @Override
    public void approveClub(Long id) {
        Club club = clubRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));
        club.setActivityStatus(ClubActivityStatusEnum.ACTIVE);
        clubRepo.save(club);
    }

    @Override
    public void suspendClub(Long id) {
        Club club = clubRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));
        club.setActivityStatus(ClubActivityStatusEnum.SUSPENDED);
        clubRepo.save(club);
    }

    private AdminClubResponse toResponse(Club club) {
        long memberCount = membershipRepo.countByClub_ClubIdAndState(
                club.getClubId(),
                MembershipStateEnum.ACTIVE
        );

        int eventCount = (int) eventRepo.countByHostClub_ClubId(club.getClubId());

        return AdminClubResponse.builder()
                .id(club.getClubId())
                .name(club.getName())
                .description(club.getDescription())
                .majorName(club.getMajor() != null ? club.getMajor().getName() : null)
                .leaderName(club.getLeader() != null ? club.getLeader().getFullName() : null)
                .leaderEmail(club.getLeader() != null ? club.getLeader().getEmail() : null)
                .memberCount(memberCount)
                .eventCount(eventCount)
                .active(club.getActivityStatus() == ClubActivityStatusEnum.ACTIVE)
                .build();
    }
    @Override
    public AdminClubStatResponse getClubStats(Long clubId) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        long memberCount = membershipRepo.countByClub_ClubIdAndState(
                clubId,
                MembershipStateEnum.ACTIVE
        );

        long totalEvents = eventRepo.countByHostClub_ClubId(clubId);
        long activeEvents = eventRepo.countByHostClub_ClubIdAndStatus(clubId, EventStatusEnum.ONGOING);
        long completedEvents = eventRepo.countByHostClub_ClubIdAndStatus(clubId, EventStatusEnum.COMPLETED);

        return AdminClubStatResponse.builder()
                .clubId(club.getClubId())
                .clubName(club.getName())
                .leaderName(club.getLeader() != null ? club.getLeader().getFullName() : "N/A")
                .memberCount(memberCount)
                .totalEvents(totalEvents)
                .activeEvents(activeEvents)
                .completedEvents(completedEvents)
                .walletBalance(
                        club.getWallet() != null ? club.getWallet().getBalancePoints() : 0
                )
                .build();
    }

}
