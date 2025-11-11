package com.example.uniclub.service;

import com.example.uniclub.entity.WalletTransaction;

import java.util.List;

public interface WalletTransactionService {
    List<WalletTransaction> getUniToEventTransactions();
}
