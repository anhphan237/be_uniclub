package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.AdminSummaryResponse;
import com.example.uniclub.repository.*;

import com.example.uniclub.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;
    private final ProductOrderRepository orderRepo;
    private final WalletTransactionRepository walletTxRepo;

    @Override
    public AdminSummaryResponse getSummary() {
        return AdminSummaryResponse.builder()
                .totalUsers(userRepo.count())
                .totalClubs(clubRepo.count())
                .totalEvents(eventRepo.count())
                .totalRedeems(orderRepo.count())
                .totalTransactions(walletTxRepo.count())
                .build();
    }

}
