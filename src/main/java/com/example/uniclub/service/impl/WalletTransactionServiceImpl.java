package com.example.uniclub.service.impl;

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

    public List<WalletTransaction> getUniToEventTransactions() {
        return walletTransactionRepo.findAllUniToEventTransactions();
    }
}
