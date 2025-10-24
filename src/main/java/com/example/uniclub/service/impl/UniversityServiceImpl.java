package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.*;
import com.example.uniclub.entity.Club;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.repository.AttendanceRecordRepository;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.WalletRepository;
import com.example.uniclub.service.UniversityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class UniversityServiceImpl implements UniversityService {

    private final ClubRepository clubRepo;
    private final MembershipRepository membershipRepo;
    private final WalletRepository walletRepo;
    private final AttendanceRecordRepository attendanceRecordRepo;

    @Override
    public UniversityStatisticsResponse getUniversitySummary() {
        long totalClubs = clubRepo.count();
        long totalMembers = membershipRepo.countByStateV2(MembershipStateEnum.ACTIVE);
        long totalPoints = walletRepo.sumAllPoints();

        return UniversityStatisticsResponse.builder()
                .totalClubs(totalClubs)
                .totalMembers(totalMembers)
                .totalPoints(totalPoints)
                .build();
    }

    @Override
    public ClubStatisticsResponse getClubSummary(Long clubId) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));

        long totalMembers = membershipRepo.countByClubIdAndState(clubId, MembershipStateEnum.ACTIVE);
        long totalPoints = walletRepo.sumPointsByClub(clubId);

        return ClubStatisticsResponse.builder()
                .clubId(club.getClubId())
                .clubName(club.getName())
                .totalMembers(totalMembers)
                .totalPoints(totalPoints)
                .build();
    }

    @Override
    public UniversityPointsResponse getPointsRanking() {
        long totalPoints = walletRepo.sumAllPoints();
        List<Object[]> rawRank = walletRepo.findClubPointsRanking();

        AtomicInteger rankCounter = new AtomicInteger(1);

        List<ClubRankingResponse> rankings = rawRank.stream()
                .map(obj -> ClubRankingResponse.builder()
                        .rank(rankCounter.getAndIncrement())
                        .clubId((Long) obj[0])
                        .clubName((String) obj[1])
                        .totalPoints((Long) obj[2])
                        .build())
                .toList();

        return UniversityPointsResponse.builder()
                .totalUniversityPoints(totalPoints)
                .clubRankings(rankings)
                .build();
    }

    @Override
    public UniversityAttendanceResponse getAttendanceRanking() {
        long totalAttendances = attendanceRecordRepo.countTotalAttendances();
        List<Object[]> rawRank = attendanceRecordRepo.getClubAttendanceRanking();

        AtomicInteger rankCounter = new AtomicInteger(1);

        List<ClubAttendanceRankingResponse> rankings = rawRank.stream()
                .map(obj -> ClubAttendanceRankingResponse.builder()
                        .rank(rankCounter.getAndIncrement())
                        .clubId(((Number) obj[0]).longValue())
                        .clubName((String) obj[1])
                        .attendanceCount(((Number) obj[2]).longValue())
                        .build())
                .toList();

        return UniversityAttendanceResponse.builder()
                .totalAttendances(totalAttendances)
                .clubRankings(rankings)
                .build();
    }
}

