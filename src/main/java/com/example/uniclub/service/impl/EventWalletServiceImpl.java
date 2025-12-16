package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.EventWalletResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.entity.WalletTransaction;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.WalletRepository;
import com.example.uniclub.repository.WalletTransactionRepository;
import com.example.uniclub.service.EventWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventWalletServiceImpl implements EventWalletService {

    private final EventRepository eventRepo;
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository transactionRepo;

    // =========================================================
    // 🟢 Xem chi tiết ví sự kiện + danh sách giao dịch
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public EventWalletResponse getEventWalletDetail(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        Wallet wallet = event.getWallet();
        if (wallet == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event does not have a wallet");

        List<WalletTransaction> transactions =
                transactionRepo.findEventWalletFullHistory(
                        wallet.getWalletId(),
                        event.getHostClub().getClubId()
                );


        List<EventWalletResponse.Transaction> transactionList = transactions.stream()
                .map(tx -> EventWalletResponse.Transaction.builder()
                        .id(tx.getId())
                        .type(tx.getType() != null ? tx.getType().name() : "UNKNOWN")
                        .amount(tx.getAmount() != null ? tx.getAmount() : 0L)
                        .description(tx.getDescription())
                        .createdAt(tx.getCreatedAt())
                        .senderName(tx.getSenderName())
                        .receiverName(tx.getReceiverName())

                        .build())
                .toList();


        return EventWalletResponse.builder()
                .eventId(event.getEventId())
                .eventName(event.getName())
                .hostClubName(event.getHostClub() != null ? event.getHostClub().getName() : null)
                .budgetPoints(event.getBudgetPoints() != null ? event.getBudgetPoints() : 0L)
                .balancePoints(wallet.getBalancePoints() != null ? wallet.getBalancePoints() : 0L)
                .ownerType(wallet.getOwnerType() != null ? wallet.getOwnerType().name() : "UNKNOWN")
                .createdAt(wallet.getCreatedAt())
                .transactions(transactionList)
                .build();
    }
}
