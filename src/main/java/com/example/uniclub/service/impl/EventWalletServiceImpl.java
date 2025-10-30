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
                .club(null)
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
    public void grantBudgetToEvent(Event event, long points) { // ✅ long thay vì int
        if (points <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Points must be positive");

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

        long leftover = eventWallet.getBalancePoints() == null ? 0L : eventWallet.getBalancePoints();
        if (leftover <= 0) return;

        List<Club> clubsToReward = new ArrayList<>();
        clubsToReward.add(event.getHostClub());
        if (event.getCoHostedClubs() != null)
            clubsToReward.addAll(event.getCoHostedClubs());

        long share = leftover / clubsToReward.size();
        for (Club c : clubsToReward) {
            Wallet clubWallet = walletService.getOrCreateClubWallet(c);
            walletService.decrease(eventWallet, share);
            walletService.increase(clubWallet, share);
        }

        // ✅ Không xóa ví, chỉ reset số dư
        eventWallet.setBalancePoints(0L);
        walletRepo.save(eventWallet);
    }

    // =========================================================
    // 🟢 4. Chi tiết ví sự kiện + danh sách giao dịch
    // =========================================================
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

        // ✅ Map transaction entity sang DTO con
        List<EventWalletResponse.Transaction> transactionList = transactions.stream()
                .map(tx -> EventWalletResponse.Transaction.builder()
                        .id(tx.getId())
                        .type(tx.getType() != null ? tx.getType().name() : "UNKNOWN")
                        .amount(tx.getAmount() != null ? tx.getAmount() : 0L) // ✅ Long đồng bộ
                        .description(tx.getDescription())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .toList();

        return EventWalletResponse.builder()
                .eventId(event.getEventId())
                .eventName(event.getName())
                .hostClubName(event.getHostClub() != null ? event.getHostClub().getName() : null)
                .budgetPoints(event.getBudgetPoints() != null ? event.getBudgetPoints().longValue() : 0L)
                .balancePoints(wallet.getBalancePoints() != null ? wallet.getBalancePoints() : 0L)
                .ownerType(wallet.getOwnerType() != null ? wallet.getOwnerType().name() : "UNKNOWN")
                .createdAt(wallet.getCreatedAt())
                .transactions(transactionList)
                .build();
    }
}
