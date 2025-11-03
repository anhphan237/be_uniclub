package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.UserStatsResponse;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserStatsServiceImpl implements UserStatsService {

    private final MembershipRepository membershipRepo;
    private final EventRegistrationRepository eventRegRepo;
    private final WalletTransactionRepository walletTransRepo;
    private final AttendanceRecordRepository attendanceRepo;

    @Override
    public UserStatsResponse getUserStats(Long userId) {

        long clubs = membershipRepo.countDistinctActiveClubsByUserId(userId);
        long events = eventRegRepo.countDistinctEventsByUserId(userId);
        long points = walletTransRepo.sumRewardPointsByUserId(userId);
        long attendance = attendanceRepo.countAttendanceDaysByUserId(userId);

        return UserStatsResponse.builder()
                .totalClubsJoined(clubs)
                .totalEventsJoined(events)
                .totalPointsEarned(points)
                .totalAttendanceDays(attendance)
                .build();
    }
}
