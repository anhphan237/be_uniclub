package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.WalletTransaction;
import com.example.uniclub.repository.WalletTransactionRepository;
import com.example.uniclub.service.WalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl implements WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepo;

    @Override
    public List<WalletTransactionResponse> getUniToEventTransactions() {
        // 🔹 Lấy danh sách entity, sau đó map sang DTO
        return walletTransactionRepo.findAllUniToEventTransactions()
                .stream()
                .map(WalletTransactionResponse::from)
                .toList();
    }
}
