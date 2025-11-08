package com.example.uniclub.service;

import com.example.uniclub.dto.response.AdminTransactionResponse;
import com.example.uniclub.dto.response.AdminWalletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminWalletService {
    Page<AdminWalletResponse> getAllWallets(Pageable pageable);
    Page<AdminTransactionResponse> getAllTransactions(Pageable pageable);
    AdminTransactionResponse getTransactionDetail(Long id);
    void adjustWalletBalance(Long walletId, long amount, String note);
}
