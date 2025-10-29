package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.EventWalletResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.WalletOwnerTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.EventWalletService;
import com.example.uniclub.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventWalletServiceImpl implements EventWalletService {

    private final WalletRepository walletRepo;
    private final WalletService walletService;
    private final EventRepository eventRepo;
    private final WalletTransactionRepository transactionRepo;
    // =========================================================
    // 🟢 1. Tạo ví sự kiện
    // =========================================================
    @Override
    @Transactional
    public void createEventWallet(Event event) {
        if (walletRepo.findByEvent_EventId(event.getEventId()).isPresent())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Wallet already exists for this event");

        Wallet wallet = Wallet.builder()
                .ownerType(WalletOwnerTypeEnum.EVENT)
                .balancePoints(0L)
                .event(event)
                .build();

        walletRepo.save(wallet);
        event.setWallet(wallet);
        eventRepo.save(event);
    }

    // =========================================================
    // 🟢 2. UniStaff cấp ngân sách điểm cho sự kiện
    // =========================================================
    @Override
    @Transactional
    public void grantBudgetToEvent(Event event, int points) {
        Wallet wallet = walletRepo.findByEvent_EventId(event.getEventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event wallet not found"));

        walletService.increase(wallet, points);
    }

    // =========================================================
    // 🟢 3. Hoàn điểm dư lại cho CLB tổ chức sau khi kết thúc
    // =========================================================
    @Override
    @Transactional
    public void returnSurplusToClubs(Event event) {
        Wallet eventWallet = walletRepo.findByEvent_EventId(event.getEventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event wallet not found"));

        int leftover = eventWallet.getBalancePoints().intValue();
        if (leftover <= 0) return;

        List<Club> clubsToReward = new ArrayList<>();
        clubsToReward.add(event.getHostClub());
        if (event.getCoHostedClubs() != null)
            clubsToReward.addAll(event.getCoHostedClubs());

        int share = leftover / clubsToReward.size();
        for (Club c : clubsToReward) {
            Wallet clubWallet = walletService.getOrCreateClubWallet(c);
            walletService.decrease(eventWallet, share);
            walletService.increase(clubWallet, share);
        }

        // ✅ Không xóa ví, chỉ reset số dư
        eventWallet.setBalancePoints(0L);
        walletRepo.save(eventWallet);
    }

    @Override
    @Transactional(readOnly = true)
    public EventWalletResponse getEventWalletDetail(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        Wallet wallet = event.getWallet();
        if (wallet == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event does not have a wallet");

        List<WalletTransaction> transactions = transactionRepo
                .findByWallet_WalletIdOrderByCreatedAtDesc(wallet.getWalletId());

        return EventWalletResponse.builder()
                .eventId(event.getEventId())
                .eventName(event.getName())
                .hostClubName(event.getHostClub() != null ? event.getHostClub().getName() : null)
                .budgetPoints(event.getBudgetPoints())
                .balancePoints(wallet.getBalancePoints())
                .ownerType(wallet.getOwnerType().name())
                .createdAt(wallet.getCreatedAt())
                .transactions(
                        transactions.stream()
                                .map(tx -> EventWalletResponse.Transaction.builder()
                                        .id(tx.getId())
                                        .type(tx.getType().name())
                                        .amount(tx.getAmount())
                                        .description(tx.getDescription())
                                        .createdAt(tx.getCreatedAt())
                                        .build())
                                .toList()
                )
                .build();
    }
}
